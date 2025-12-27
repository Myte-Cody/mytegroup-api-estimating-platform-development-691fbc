import { BadRequestException, ConflictException, ForbiddenException, Injectable, NotFoundException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { expandRoles, Role } from '../../common/roles';
import { AuditLogService } from '../../common/services/audit-log.service';
import { TenantConnectionService } from '../../common/tenancy/tenant-connection.service';
import { normalizeKey, normalizeKeys, normalizeName } from '../../common/utils/normalize.util';
import { CreateOfficeDto } from './dto/create-office.dto';
import { ListOfficesQueryDto } from './dto/list-offices.dto';
import { UpdateOfficeDto } from './dto/update-office.dto';
import { Office, OfficeSchema } from './schemas/office.schema';
import { Organization } from '../organizations/schemas/organization.schema';
import { OrgTaxonomyService } from '../org-taxonomy/org-taxonomy.service';

type ActorContext = { userId?: string; orgId?: string; role?: Role };

@Injectable()
export class OfficesService {
  constructor(
    @InjectModel('Office') private readonly officeModel: Model<Office>,
    @InjectModel('Organization') private readonly orgModel: Model<Organization>,
    private readonly audit: AuditLogService,
    private readonly tenants: TenantConnectionService,
    private readonly taxonomy: OrgTaxonomyService
  ) {}

  private async model(orgId: string) {
    return this.tenants.getModelForOrg<Office>(orgId, 'Office', OfficeSchema, this.officeModel);
  }

  private ensureRole(actor: ActorContext, allowed: Role[]) {
    if (actor.role === Role.SuperAdmin) return;
    if (!actor.role) throw new ForbiddenException('Insufficient role');
    const effectiveRoles = expandRoles([actor.role]);
    const allowedEffective = allowed.some((role) => effectiveRoles.includes(role));
    if (!allowedEffective) {
      throw new ForbiddenException('Insufficient role');
    }
  }

  private resolveOrgId(requestedOrgId: string | undefined, actor: ActorContext) {
    if (actor.role === Role.SuperAdmin && requestedOrgId) return requestedOrgId;
    if (actor.orgId) return actor.orgId;
    throw new ForbiddenException('Missing organization context');
  }

  private ensureOrgScope(entityOrgId: string, actor: ActorContext) {
    if (actor.role === Role.SuperAdmin) return;
    if (!actor.orgId || actor.orgId !== entityOrgId) {
      throw new ForbiddenException('Cannot access office outside your organization');
    }
  }

  private canViewArchived(actor: ActorContext) {
    const effectiveRoles = expandRoles(actor.role ? [actor.role] : []);
    return [Role.SuperAdmin, Role.PlatformAdmin, Role.Admin, Role.Manager, Role.OrgOwner].some((role) =>
      effectiveRoles.includes(role)
    );
  }

  private ensureNotOnLegalHold(entity: { legalHold?: boolean } | null, action: string) {
    if (entity?.legalHold) {
      throw new ForbiddenException(`Cannot ${action} office while legal hold is active`);
    }
  }

  private toPlain(office: Office) {
    return office.toObject ? office.toObject() : office;
  }

  private summarizeDiff(before: Record<string, any>, after: Record<string, any>, fields: string[]) {
    const changes: Record<string, { from: any; to: any }> = {};
    fields.forEach((field) => {
      if (before[field] !== after[field]) {
        changes[field] = { from: before[field], to: after[field] };
      }
    });
    return Object.keys(changes).length ? changes : undefined;
  }

  private async validateOrg(orgId: string) {
    const org = await this.orgModel.findOne({ _id: orgId, archivedAt: null });
    if (!org) throw new NotFoundException('Organization not found or archived');
    if (org.legalHold) {
      throw new ForbiddenException('Organization is under legal hold');
    }
  }

  async create(dto: CreateOfficeDto, actor: ActorContext) {
    this.ensureRole(actor, [Role.Admin, Role.Manager, Role.OrgOwner]);
    const orgId = this.resolveOrgId(dto.orgId, actor);
    await this.validateOrg(orgId);
    const model = await this.model(orgId);
    const trimmedName = String(dto.name || '').trim();
    if (!trimmedName) throw new BadRequestException('name is required');

    const normalizedName = normalizeName(trimmedName);
    const existing = await model.findOne({ orgId, normalizedName, archivedAt: null });
    if (existing) throw new ConflictException('Office name already exists for this organization');

    const parentOrgLocationId = dto.parentOrgLocationId ? String(dto.parentOrgLocationId).trim() : null;
    if (parentOrgLocationId) {
      const parent = await model.findOne({ _id: parentOrgLocationId, orgId, archivedAt: null });
      if (!parent) throw new NotFoundException('Parent org location not found');
    }

    const orgLocationTypeKey = dto.orgLocationTypeKey ? normalizeKey(dto.orgLocationTypeKey) : null;
    const tagKeys = normalizeKeys(dto.tagKeys);
    if (orgLocationTypeKey) {
      await this.taxonomy.ensureKeysActive(actor, orgId, 'org_location_type', [orgLocationTypeKey]);
    }
    if (tagKeys.length) {
      await this.taxonomy.ensureKeysActive(actor, orgId, 'org_location_tag', tagKeys);
    }

    const office = await model.create({
      name: trimmedName,
      normalizedName,
      orgId,
      address: dto.address,
      description: dto.description ?? null,
      timezone: dto.timezone ?? null,
      orgLocationTypeKey,
      tagKeys,
      parentOrgLocationId,
      sortOrder: dto.sortOrder ?? null,
    });

    await this.audit.log({
      eventType: 'office.created',
      orgId,
      userId: actor.userId,
      entity: 'Office',
      entityId: office.id,
      metadata: { name: office.name, orgLocationTypeKey, parentOrgLocationId },
    });

    return this.toPlain(office);
  }

  async list(actor: ActorContext, orgId: string, query?: ListOfficesQueryDto) {
    this.ensureRole(actor, [Role.Admin, Role.Manager, Role.OrgOwner, Role.PM, Role.Viewer]);
    const resolvedOrgId = this.resolveOrgId(orgId, actor);
    const includeArchived = !!query?.includeArchived;
    if (includeArchived && !this.canViewArchived(actor)) {
      throw new ForbiddenException('Not allowed to include archived offices');
    }
    const filter: Record<string, any> = { orgId: resolvedOrgId };
    if (!includeArchived) filter.archivedAt = null;

    if (query?.parentOrgLocationId) {
      filter.parentOrgLocationId = query.parentOrgLocationId;
    }

    const search = query?.search?.trim();
    if (search) {
      const escaped = search.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
      const rx = new RegExp(escaped, 'i');
      filter.$or = [{ name: rx }, { normalizedName: rx }];
    }

    const model = await this.model(resolvedOrgId);

    const wantsPagination = query?.page !== undefined || query?.limit !== undefined;
    const page = Math.max(1, query?.page || 1);
    const limit = Math.min(Math.max(query?.limit || 25, 1), 100);

    if (!wantsPagination) {
      return model.find(filter).sort({ name: 1, _id: 1 }).lean();
    }

    const skip = (page - 1) * limit;
    const [data, total] = await Promise.all([
      model.find(filter).sort({ name: 1, _id: 1 }).skip(skip).limit(limit).lean(),
      model.countDocuments(filter),
    ]);

    return { data, total, page, limit };
  }

  async getById(id: string, actor: ActorContext, requestedOrgId?: string, includeArchived = false) {
    this.ensureRole(actor, [Role.Admin, Role.Manager, Role.OrgOwner, Role.PM, Role.Viewer]);
    const orgId = this.resolveOrgId(requestedOrgId, actor);
    const model = await this.model(orgId);
    const office = await model.findOne({ _id: id, orgId });
    if (!office) throw new NotFoundException('Office not found');
    if (office.archivedAt && !includeArchived) throw new NotFoundException('Office archived');
    if (office.archivedAt && includeArchived && !this.canViewArchived(actor)) {
      throw new ForbiddenException('Not allowed to view archived offices');
    }
    return this.toPlain(office);
  }

  async update(id: string, dto: UpdateOfficeDto, actor: ActorContext, requestedOrgId?: string) {
    this.ensureRole(actor, [Role.Admin, Role.Manager, Role.OrgOwner]);
    const orgId = this.resolveOrgId(requestedOrgId, actor);
    const model = await this.model(orgId);
    const office = await model.findOne({ _id: id, orgId });
    if (!office) throw new NotFoundException('Office not found');
    await this.validateOrg(orgId);
    if (office.archivedAt) throw new NotFoundException('Office archived');
    this.ensureNotOnLegalHold(office, 'update');

    const nextName = dto.name !== undefined ? String(dto.name || '').trim() : undefined;
    if (nextName !== undefined) {
      if (!nextName) throw new BadRequestException('name is required');
    }

    if (nextName !== undefined && nextName !== office.name) {
      const normalizedName = normalizeName(nextName);
      const existing = await model.findOne({
        orgId,
        normalizedName,
        archivedAt: null,
        _id: { $ne: id },
      });
      if (existing) throw new ConflictException('Office name already exists for this organization');
    }

    const before = this.toPlain(office);

    if (nextName !== undefined) office.name = nextName;
    if (nextName !== undefined) (office as any).normalizedName = normalizeName(nextName);
    if (dto.address !== undefined) office.address = dto.address;

    if (dto.description !== undefined) office.description = dto.description ? String(dto.description).trim() : null;
    if (dto.timezone !== undefined) office.timezone = dto.timezone ? String(dto.timezone).trim() : null;

    if (dto.orgLocationTypeKey !== undefined) {
      const nextTypeKey = dto.orgLocationTypeKey ? normalizeKey(dto.orgLocationTypeKey) : null;
      office.orgLocationTypeKey = nextTypeKey;
      if (nextTypeKey) {
        await this.taxonomy.ensureKeysActive(actor, orgId, 'org_location_type', [nextTypeKey]);
      }
    }

    if (dto.tagKeys !== undefined) {
      const nextTags = normalizeKeys(Array.isArray(dto.tagKeys) ? dto.tagKeys : []);
      office.tagKeys = nextTags as any;
      if (nextTags.length) {
        await this.taxonomy.ensureKeysActive(actor, orgId, 'org_location_tag', nextTags);
      }
    }

    if (dto.sortOrder !== undefined) {
      office.sortOrder = dto.sortOrder ?? null;
    }

    if (dto.parentOrgLocationId !== undefined) {
      const nextParent = dto.parentOrgLocationId ? String(dto.parentOrgLocationId).trim() : null;
      if (nextParent && nextParent === id) {
        throw new BadRequestException('parentOrgLocationId cannot be self');
      }
      if (nextParent) {
        const parent = await model.findOne({ _id: nextParent, orgId, archivedAt: null });
        if (!parent) throw new NotFoundException('Parent org location not found');
      }
      office.parentOrgLocationId = nextParent as any;
    }

    await office.save();
    const after = this.toPlain(office);

    await this.audit.log({
      eventType: 'office.updated',
      orgId,
      userId: actor.userId,
      entity: 'Office',
      entityId: office.id,
      metadata: {
        changes: this.summarizeDiff(before, after, [
          'name',
          'address',
          'description',
          'timezone',
          'orgLocationTypeKey',
          'tagKeys',
          'parentOrgLocationId',
          'sortOrder',
        ]),
      },
    });

    return after;
  }

  async archive(id: string, actor: ActorContext, requestedOrgId?: string) {
    this.ensureRole(actor, [Role.Admin, Role.Manager, Role.OrgOwner]);
    const orgId = this.resolveOrgId(requestedOrgId, actor);
    const model = await this.model(orgId);
    const office = await model.findOne({ _id: id, orgId });
    if (!office) throw new NotFoundException('Office not found');
    await this.validateOrg(orgId);
    this.ensureNotOnLegalHold(office, 'archive');

    if (!office.archivedAt) {
      office.archivedAt = new Date();
      await office.save();
    }

    await this.audit.log({
      eventType: 'office.archived',
      orgId,
      userId: actor.userId,
      entity: 'Office',
      entityId: office.id,
      metadata: { archivedAt: office.archivedAt },
    });
    return this.toPlain(office);
  }

  async unarchive(id: string, actor: ActorContext, requestedOrgId?: string) {
    this.ensureRole(actor, [Role.Admin, Role.Manager, Role.OrgOwner]);
    const orgId = this.resolveOrgId(requestedOrgId, actor);
    const model = await this.model(orgId);
    const office = await model.findOne({ _id: id, orgId });
    if (!office) throw new NotFoundException('Office not found');
    await this.validateOrg(orgId);
    this.ensureNotOnLegalHold(office, 'unarchive');

    if (office.archivedAt) {
      const collision = await model.findOne({
        orgId,
        normalizedName: (office as any).normalizedName || normalizeName(office.name),
        archivedAt: null,
        _id: { $ne: id },
      });
      if (collision) throw new ConflictException('Office name already exists for this organization');
      office.archivedAt = null;
      await office.save();
    }

    await this.audit.log({
      eventType: 'office.unarchived',
      orgId,
      userId: actor.userId,
      entity: 'Office',
      entityId: office.id,
      metadata: { archivedAt: office.archivedAt },
    });
    return this.toPlain(office);
  }
}
