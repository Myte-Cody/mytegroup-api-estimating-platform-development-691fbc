import {
  BadRequestException,
  ForbiddenException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { Role } from '../../common/roles';
import { AuditLogService } from '../../common/services/audit-log.service';
import { TenantConnectionService } from '../../common/tenancy/tenant-connection.service';
import { BatchArchiveDto } from './dto/batch-archive.dto';
import { SetLegalHoldDto } from './dto/set-legal-hold.dto';
import { StripPiiDto } from './dto/strip-pii.dto';
import { COMPLIANCE_ENTITY_TYPES, ComplianceEntityType } from './compliance.types';
import { User } from '../users/schemas/user.schema';
import { Contact, ContactSchema } from '../contacts/schemas/contact.schema';
import { Invite } from '../invites/schemas/invite.schema';
import { Project } from '../projects/schemas/project.schema';

type ActorContext = { id?: string; orgId?: string; role?: Role };

type EntityRecord = {
  id?: string;
  _id?: string;
  orgId?: string;
  organizationId?: string;
  archivedAt?: Date | null;
  piiStripped?: boolean;
  legalHold?: boolean;
  save?: () => Promise<any>;
  toObject?: () => any;
  status?: string;
  tokenExpires?: Date | null;
};

type RedactionRule = { field: string; replacement?: (record: EntityRecord, entityId: string) => any };

const COMPLIANCE_ROLES: Role[] = [
  Role.SuperAdmin,
  Role.PlatformAdmin,
  Role.Admin,
  Role.OrgOwner,
  Role.OrgAdmin,
  Role.Compliance,
  Role.ComplianceOfficer,
  Role.Security,
  Role.SecurityOfficer,
];

const PII_RULES: Record<ComplianceEntityType, RedactionRule[]> = {
  user: [
    { field: 'email', replacement: (_record, id) => `redacted+${id}@example.com` },
    { field: 'username', replacement: (_record, id) => `redacted-user-${id}` },
    { field: 'lastLogin' },
  ],
  contact: [
    { field: 'name', replacement: (_record, id) => `Redacted Contact ${id}` },
    { field: 'email' },
    { field: 'phone' },
    { field: 'notes' },
  ],
  invite: [
    { field: 'email', replacement: (_record, id) => `redacted+invite-${id}@example.com` },
    { field: 'tokenHash', replacement: () => 'redacted' },
    { field: 'tokenExpires', replacement: () => new Date(0) },
  ],
  project: [
    { field: 'name', replacement: (_record, id) => `redacted-project-${id}` },
    { field: 'description' },
  ],
};

@Injectable()
export class ComplianceService {
  constructor(
    @InjectModel('User') private readonly userModel: Model<User>,
    @InjectModel('Contact') private readonly contactModel: Model<Contact>,
    @InjectModel('Invite') private readonly inviteModel: Model<Invite>,
    @InjectModel('Project') private readonly projectModel: Model<Project>,
    private readonly audit: AuditLogService,
    private readonly tenants: TenantConnectionService
  ) {}

  private normalizeEntityType(entityType: string): ComplianceEntityType {
    const normalized = (entityType || '').trim().toLowerCase();
    const singular = normalized.endsWith('s') ? normalized.slice(0, -1) : normalized;
    if (!COMPLIANCE_ENTITY_TYPES.includes(singular as ComplianceEntityType)) {
      throw new BadRequestException(`Unsupported entity type: ${entityType}`);
    }
    return singular as ComplianceEntityType;
  }

  private ensureRole(actor: ActorContext) {
    if (!actor.role || !COMPLIANCE_ROLES.includes(actor.role)) {
      throw new ForbiddenException('Insufficient role for compliance action');
    }
  }

  private resolveOrgId(entityType: ComplianceEntityType, record: EntityRecord) {
    if (entityType === 'user' || entityType === 'project') return record.organizationId;
    if (entityType === 'contact' || entityType === 'invite') return record.orgId;
    return undefined;
  }

  private assertOrgScope(entityType: ComplianceEntityType, record: EntityRecord, actor: ActorContext) {
    const orgId = this.resolveOrgId(entityType, record);
    const isElevated = actor.role === Role.SuperAdmin || actor.role === Role.PlatformAdmin;
    if (isElevated) return;
    if (!orgId || !actor.orgId || actor.orgId !== orgId) {
      throw new ForbiddenException('Entity outside actor organization');
    }
  }

  private ensureNotOnLegalHold(record: EntityRecord) {
    if (record.legalHold) {
      throw new ForbiddenException('Entity on legal hold');
    }
  }

  private async resolveModel(entityType: ComplianceEntityType, orgId?: string) {
    if (entityType === 'user') return this.userModel;
    if (entityType === 'invite') return this.inviteModel;
    if (entityType === 'project') return this.projectModel;
    if (entityType === 'contact') {
      if (orgId) {
        return this.tenants.getModelForOrg<Contact>(orgId, 'Contact', ContactSchema, this.contactModel);
      }
      return this.contactModel;
    }
    throw new BadRequestException(`Unsupported entity type: ${entityType}`);
  }

  private async loadRecord(entityType: ComplianceEntityType, id: string, orgId?: string) {
    if (!id) throw new BadRequestException('entityId is required');
    const model = await this.resolveModel(entityType, orgId);
    const record = await model.findById(id);
    if (record) return record;
    // Fallback for contacts stored on default connection when a dedicated org is requested
    if (entityType === 'contact' && model !== this.contactModel) {
      const fallback = await this.contactModel.findById(id);
      if (fallback) return fallback;
    }
    throw new NotFoundException(`${entityType} not found`);
  }

  private entityLabel(entityType: ComplianceEntityType) {
    return entityType.charAt(0).toUpperCase() + entityType.slice(1);
  }

  private safeId(entityId: string) {
    return String(entityId || 'entity').replace(/[^a-zA-Z0-9_-]/g, '');
  }

  private redactedValue(
    rule: RedactionRule,
    record: EntityRecord,
    entityType: ComplianceEntityType,
    entityId: string
  ) {
    const safeId = this.safeId(entityId);
    if (typeof rule.replacement === 'function') {
      return rule.replacement(record, safeId);
    }
    if (entityType === 'invite' && rule.field === 'tokenExpires') {
      return new Date(0);
    }
    if (entityType === 'project' && rule.field === 'name') {
      return `redacted-project-${safeId}`;
    }
    return null;
  }

  private collectBeforeSnapshot(record: EntityRecord, fields: string[]) {
    return fields.reduce<Record<string, any>>((acc, field) => {
      const current = (record as Record<string, any>)[field];
      acc[field] = current === undefined || current === null ? null : '<redacted>';
      return acc;
    }, {});
  }

  private collectAfterSnapshot(record: EntityRecord, fields: string[]) {
    return fields.reduce<Record<string, any>>((acc, field) => {
      acc[field] = (record as Record<string, any>)[field] ?? null;
      return acc;
    }, {});
  }

  private applyEntitySpecificStrip(entityType: ComplianceEntityType, record: EntityRecord) {
    if (entityType === 'invite') {
      (record as any).status = 'expired';
      (record as any).tokenExpires = new Date(0);
    }
  }

  async stripPii(dto: StripPiiDto, actor: ActorContext) {
    const entityType = this.normalizeEntityType(dto.entityType);
    this.ensureRole(actor);
    const record = await this.loadRecord(entityType, dto.entityId, actor.orgId);
    this.assertOrgScope(entityType, record, actor);
    this.ensureNotOnLegalHold(record);

    const rules = PII_RULES[entityType] || [];
    const fieldNames = rules.map((rule) => rule.field);
    const before = this.collectBeforeSnapshot(record, fieldNames);
    const entityId = (record as any).id || (record as any)._id || dto.entityId;
    rules.forEach((rule) => {
      (record as Record<string, any>)[rule.field] = this.redactedValue(
        rule,
        record,
        entityType,
        entityId
      );
    });
    this.applyEntitySpecificStrip(entityType, record);
    (record as any).piiStripped = true;
    await (record.save ? record.save() : Promise.resolve());

    const after = this.collectAfterSnapshot(record, fieldNames);
    const orgId = this.resolveOrgId(entityType, record);
    await this.audit.log({
      eventType: 'compliance.strip_pii',
      orgId,
      actorId: actor.id,
      entity: this.entityLabel(entityType),
      entityId,
      metadata: { redactedFields: fieldNames, before, after },
    });

    return { success: true, entityId, piiStripped: true, redactedFields: fieldNames };
  }

  async setLegalHold(dto: SetLegalHoldDto, actor: ActorContext) {
    const entityType = this.normalizeEntityType(dto.entityType);
    this.ensureRole(actor);
    const record = await this.loadRecord(entityType, dto.entityId, actor.orgId);
    this.assertOrgScope(entityType, record, actor);
    const before = !!record.legalHold;
    (record as any).legalHold = dto.legalHold;
    await (record.save ? record.save() : Promise.resolve());

    const orgId = this.resolveOrgId(entityType, record);
    const entityId = (record as any).id || (record as any)._id || dto.entityId;
    await this.audit.log({
      eventType: 'compliance.legal_hold',
      orgId,
      actorId: actor.id,
      entity: this.entityLabel(entityType),
      entityId,
      metadata: { before, after: dto.legalHold, reason: dto.reason },
    });

    return { success: true, entityId, legalHold: dto.legalHold };
  }

  async batchArchive(dto: BatchArchiveDto, actor: ActorContext) {
    const entityType = this.normalizeEntityType(dto.entityType);
    this.ensureRole(actor);
    const archive = dto.archive !== false;
    const archived: string[] = [];
    const skipped: { id: string; reason: string }[] = [];
    let resolvedOrgForAudit: string | undefined;

    for (const entityId of dto.entityIds) {
      try {
        const record = await this.loadRecord(entityType, entityId, actor.orgId);
        this.assertOrgScope(entityType, record, actor);
        if (!resolvedOrgForAudit) {
          resolvedOrgForAudit = this.resolveOrgId(entityType, record);
        }
        if (record.legalHold) {
          skipped.push({ id: entityId, reason: 'legal_hold' });
          continue;
        }

        const alreadyArchived = !!record.archivedAt;
        if (archive) {
          if (!alreadyArchived) {
            record.archivedAt = new Date();
            await (record.save ? record.save() : Promise.resolve());
          }
        } else {
          if (alreadyArchived) {
            record.archivedAt = null;
            await (record.save ? record.save() : Promise.resolve());
          }
        }
        archived.push(entityId);
      } catch (err) {
        if (err instanceof NotFoundException) {
          skipped.push({ id: entityId, reason: 'not_found' });
          continue;
        }
        throw err;
      }
    }

    await this.audit.log({
      eventType: 'compliance.batch_archive',
      orgId: resolvedOrgForAudit || actor.orgId,
      actorId: actor.id,
      entity: this.entityLabel(entityType),
      metadata: { action: archive ? 'archive' : 'unarchive', archived, skipped },
    });

    return { success: true, action: archive ? 'archive' : 'unarchive', archived, skipped };
  }

  async status(entityType: string, entityId: string, actor: ActorContext) {
    const normalized = this.normalizeEntityType(entityType);
    this.ensureRole(actor);
    const record = await this.loadRecord(normalized, entityId, actor.orgId);
    this.assertOrgScope(normalized, record, actor);
    return {
      entityId,
      entityType: this.entityLabel(normalized),
      archivedAt: record.archivedAt || null,
      piiStripped: !!record.piiStripped,
      legalHold: !!record.legalHold,
    };
  }
}
