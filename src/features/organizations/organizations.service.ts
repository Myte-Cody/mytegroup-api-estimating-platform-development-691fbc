import {
  BadRequestException,
  ConflictException,
  ForbiddenException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { AuditLogService } from '../../common/services/audit-log.service';
import { CreateOrganizationDto } from './dto/create-organization.dto';
import { UpdateOrganizationDatastoreDto } from './dto/update-organization-datastore.dto';
import { UpdateOrganizationDto } from './dto/update-organization.dto';
import { UpdateOrganizationLegalHoldDto } from './dto/update-organization-legal-hold.dto';
import { UpdateOrganizationPiiDto } from './dto/update-organization-pii.dto';
import { Organization } from './schemas/organization.schema';
import { TenantConnectionService } from '../../common/tenancy/tenant-connection.service';
import { ListOrganizationsDto } from './dto/list-organizations.dto';
import { SeatsService } from '../seats/seats.service';
import { seatConfig } from '../../config/app.config';

type Actor = { id?: string | null };

@Injectable()
export class OrganizationsService {
  constructor(
    @InjectModel('Organization') private readonly orgModel: Model<Organization>,
    private readonly audit: AuditLogService,
    private readonly tenants: TenantConnectionService,
    private readonly seats: SeatsService
  ) {}

  private asObject(org: Organization) {
    return org.toObject ? org.toObject() : (org as any);
  }

  private currentDatastoreType(org: Organization) {
    return org.useDedicatedDb || org.datastoreType === 'dedicated' ? 'dedicated' : 'shared';
  }

  private sanitizeDatastoreSnapshot(org: Organization) {
    return {
      useDedicatedDb: org.useDedicatedDb,
      datastoreType: this.currentDatastoreType(org),
      databaseName: org.databaseName || null,
      dataResidency: org.dataResidency || null,
    };
  }

  private async getOrgOrThrow(id: string) {
    const org = await this.orgModel.findById(id);
    if (!org) throw new NotFoundException('Organization not found');
    return org;
  }

  async findByDomain(domain: string) {
    const normalized = (domain || '').toLowerCase();
    if (!normalized) return null;
    return this.orgModel.findOne({ primaryDomain: normalized, archivedAt: null });
  }

  async create(dto: CreateOrganizationDto, actor?: Actor) {
    const existing = await this.orgModel.findOne({ name: dto.name });
    if (existing) throw new ConflictException('Organization name already in use');
    const normalizedDomain = dto.primaryDomain ? dto.primaryDomain.toLowerCase() : undefined;
    if (normalizedDomain) {
      const domainCollision = await this.orgModel.findOne({ primaryDomain: normalizedDomain });
      if (domainCollision) throw new ConflictException('Organization domain already registered');
    }

    const datastoreType = dto.datastoreType || (dto.useDedicatedDb ? 'dedicated' : 'shared');
    if (datastoreType === 'dedicated' && !(dto.databaseUri || dto.datastoreUri)) {
      throw new BadRequestException('Dedicated datastore requires a connection URI');
    }

    const org = await this.orgModel.create({
      name: dto.name,
      metadata: dto.metadata || {},
      ...(normalizedDomain ? { primaryDomain: normalizedDomain } : {}),
      useDedicatedDb: datastoreType === 'dedicated',
      datastoreType,
      databaseUri: dto.databaseUri || dto.datastoreUri || null,
      databaseName: dto.databaseName || null,
      dataResidency: dto.dataResidency || datastoreType || 'shared',
      piiStripped: dto.piiStripped ?? false,
      legalHold: dto.legalHold ?? false,
    });
    await this.audit.log({
      eventType: 'organization.created',
      userId: actor?.id || undefined,
      orgId: org.id,
      entity: 'Organization',
      entityId: org.id,
      metadata: { datastore: this.sanitizeDatastoreSnapshot(org) },
    });

    await this.seats.ensureOrgSeats(org.id, seatConfig.defaultSeatsPerOrg);
    return this.asObject(org);
  }

  async setOwner(orgId: string, userId: string) {
    const org = await this.getOrgOrThrow(orgId);
    if (org.legalHold) {
      throw new ForbiddenException('Organization is under legal hold');
    }
    org.ownerUserId = userId;
    org.createdByUserId = org.createdByUserId || userId;
    await org.save();
    await this.audit.log({
      eventType: 'organization.owner_assigned',
      orgId: org.id,
      entity: 'Organization',
      entityId: org.id,
      metadata: { ownerUserId: userId },
    });
    return this.asObject(org);
  }

  async update(id: string, dto: UpdateOrganizationDto, actor?: Actor) {
    const org = await this.getOrgOrThrow(id);
    if (org.legalHold) {
      throw new ForbiddenException('Organization is under legal hold');
    }
    if (dto.name) {
      const collision = await this.orgModel.findOne({ name: dto.name, _id: { $ne: id } });
      if (collision) throw new ConflictException('Organization name already in use');
    }
    const before = this.asObject(org);
    if (dto.name) org.name = dto.name;
    if (dto.metadata) org.metadata = dto.metadata;
    await org.save();
    const after = this.asObject(org);
    await this.audit.log({
      eventType: 'organization.updated',
      userId: actor?.id || undefined,
      orgId: org.id,
      entity: 'Organization',
      entityId: org.id,
      metadata: { before, after },
    });
    return after;
  }

  async updateDatastore(id: string, dto: UpdateOrganizationDatastoreDto, actor?: Actor) {
    const org = await this.getOrgOrThrow(id);
    if (org.legalHold) {
      throw new ForbiddenException('Organization is under legal hold');
    }
    const currentType = this.currentDatastoreType(org);
    const targetType =
      dto.type ||
      (dto.useDedicatedDb !== undefined ? (dto.useDedicatedDb ? 'dedicated' : 'shared') : currentType);

    const targetUri = dto.datastoreUri ?? dto.databaseUri ?? org.databaseUri ?? null;
    const targetDbName = dto.databaseName ?? org.databaseName ?? null;
    const targetResidency = dto.dataResidency || (targetType === 'dedicated' ? 'dedicated' : 'shared');

    if (targetType === 'dedicated') {
      if (!targetUri) throw new BadRequestException('Dedicated datastore requires a connection URI');
      try {
        await this.tenants.testConnection(targetUri, targetDbName || undefined);
      } catch {
        throw new BadRequestException('Failed to validate datastore connection');
      }
    }

    const before = {
      useDedicatedDb: org.useDedicatedDb,
      datastoreType: this.currentDatastoreType(org),
      databaseUri: org.databaseUri,
      databaseName: org.databaseName,
      dataResidency: org.dataResidency,
    };

    org.useDedicatedDb = targetType === 'dedicated';
    org.datastoreType = targetType;
    org.databaseUri = targetType === 'dedicated' ? targetUri : null;
    org.databaseName = targetType === 'dedicated' ? targetDbName : null;
    org.dataResidency = targetResidency;
    org.datastoreHistory = org.datastoreHistory || [];
    org.datastoreHistory.push({
      fromType: before.datastoreType,
      toType: targetType,
      fromUri: before.databaseUri || null,
      toUri: org.databaseUri || null,
      actorId: actor?.id || null,
      switchedAt: new Date(),
    });

    await org.save();
    await this.tenants.resetConnectionForOrg(id);
    await this.audit.log({
      eventType: 'organization.datastore_switched',
      userId: actor?.id || undefined,
      orgId: org.id,
      entity: 'Organization',
      entityId: org.id,
      metadata: {
        before: this.sanitizeDatastoreSnapshot({ ...org, ...before } as unknown as Organization),
        after: this.sanitizeDatastoreSnapshot(org),
      },
    });
    return this.asObject(org);
  }

  async setLegalHold(id: string, dto: UpdateOrganizationLegalHoldDto, actor?: Actor) {
    const org = await this.getOrgOrThrow(id);
    if (org.legalHold === dto.legalHold) return this.asObject(org);
    org.legalHold = dto.legalHold;
    await org.save();
    await this.audit.log({
      eventType: 'organization.legal_hold_toggled',
      userId: actor?.id || undefined,
      orgId: org.id,
      entity: 'Organization',
      entityId: org.id,
      metadata: { legalHold: org.legalHold },
    });
    return this.asObject(org);
  }

  async setPiiStripped(id: string, dto: UpdateOrganizationPiiDto, actor?: Actor) {
    const org = await this.getOrgOrThrow(id);
    if (org.piiStripped === dto.piiStripped) return this.asObject(org);
    org.piiStripped = dto.piiStripped;
    await org.save();
    await this.audit.log({
      eventType: 'organization.pii_stripped_toggled',
      userId: actor?.id || undefined,
      orgId: org.id,
      entity: 'Organization',
      entityId: org.id,
      metadata: { piiStripped: org.piiStripped },
    });
    return this.asObject(org);
  }

  async findById(id: string) {
    const org = await this.getOrgOrThrow(id);
    return this.asObject(org);
  }

  async findAll() {
    return this.orgModel.find({ archivedAt: null });
  }

  private sanitizeListItem(raw: any) {
    const org = raw?.toObject ? raw.toObject() : raw;
    const id = org?.id || (org?._id ? String(org._id) : undefined);
    const datastoreType = this.currentDatastoreType(org as Organization);
    return {
      id,
      name: org?.name,
      primaryDomain: org?.primaryDomain || null,
      archivedAt: org?.archivedAt || null,
      legalHold: !!org?.legalHold,
      piiStripped: !!org?.piiStripped,
      datastoreType,
      dataResidency: org?.dataResidency || (datastoreType === 'dedicated' ? 'dedicated' : 'shared'),
      useDedicatedDb: !!org?.useDedicatedDb,
      databaseName: org?.databaseName || null,
    };
  }

  async list(params: ListOrganizationsDto) {
    const {
      search,
      includeArchived,
      datastoreType,
      legalHold,
      piiStripped,
      page = 1,
      limit = 25,
    } = params || {};

    const query: any = {};
    if (!includeArchived) query.archivedAt = null;
    if (datastoreType) query.datastoreType = datastoreType;
    if (typeof legalHold === 'boolean') query.legalHold = legalHold;
    if (typeof piiStripped === 'boolean') query.piiStripped = piiStripped;
    if (search) {
      const rx = new RegExp(search, 'i');
      query.$or = [{ name: rx }, { primaryDomain: rx }];
    }

    const safeLimit = Math.min(Math.max(limit, 1), 100);
    const safePage = Math.max(page, 1);
    const skip = (safePage - 1) * safeLimit;

    const [data, total] = await Promise.all([
      this.orgModel.find(query).sort({ createdAt: -1 }).skip(skip).limit(safeLimit).lean(),
      this.orgModel.countDocuments(query),
    ]);

    return {
      data: (data || []).map((org) => this.sanitizeListItem(org)),
      total,
      page: safePage,
      limit: safeLimit,
    };
  }

  async hasAnyOrganization() {
    const existing = await this.orgModel.exists({ archivedAt: null });
    return !!existing;
  }

  async archive(id: string, actor?: Actor) {
    const org = await this.getOrgOrThrow(id);
    if (org.legalHold) throw new ForbiddenException('Organization is under legal hold');
    if (org.archivedAt) return this.asObject(org);
    org.archivedAt = new Date();
    await org.save();
    await this.audit.log({
      eventType: 'organization.archived',
      userId: actor?.id || undefined,
      orgId: org.id,
      entity: 'Organization',
      entityId: org.id,
    });
    return this.asObject(org);
  }

  async unarchive(id: string, actor?: Actor) {
    const org = await this.getOrgOrThrow(id);
    if (!org.archivedAt) return this.asObject(org);
    org.archivedAt = null;
    await org.save();
    await this.audit.log({
      eventType: 'organization.unarchived',
      userId: actor?.id || undefined,
      orgId: org.id,
      entity: 'Organization',
      entityId: org.id,
    });
    return this.asObject(org);
  }
}
