import { ConflictException, ForbiddenException, Injectable, NotFoundException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { expandRoles, Role } from '../../common/roles';
import { AuditLogService } from '../../common/services/audit-log.service';
import { TenantConnectionService } from '../../common/tenancy/tenant-connection.service';
import { normalizeKey, normalizeKeys, normalizeName } from '../../common/utils/normalize.util';
import { Organization } from '../organizations/schemas/organization.schema';
import { Company, CompanySchema } from '../companies/schemas/company.schema';
import { OrgTaxonomyService } from '../org-taxonomy/org-taxonomy.service';
import { CreateCompanyLocationDto } from './dto/create-company-location.dto';
import { ListCompanyLocationsQueryDto } from './dto/list-company-locations.dto';
import { UpdateCompanyLocationDto } from './dto/update-company-location.dto';
import { CompanyLocation, CompanyLocationSchema } from './schemas/company-location.schema';

type ActorContext = { userId?: string; orgId?: string; role?: Role };

@Injectable()
export class CompanyLocationsService {
  constructor(
    private readonly audit: AuditLogService,
    private readonly tenants: TenantConnectionService,
    private readonly taxonomy: OrgTaxonomyService,
    @InjectModel('CompanyLocation') private readonly companyLocationModel: Model<CompanyLocation>,
    @InjectModel('Company') private readonly companyModel: Model<Company>,
    @InjectModel('Organization') private readonly orgModel: Model<Organization>
  ) {}

  private ensureRole(actor: ActorContext, allowed: Role[]) {
    const role = actor.role || Role.User;
    const effective = expandRoles([role]);
    const ok = allowed.some((r) => effective.includes(r));
    if (!ok) throw new ForbiddenException('Insufficient role');
  }

  private ensureOrgScope(orgId: string, actor: ActorContext) {
    if (actor.role === Role.SuperAdmin || actor.role === Role.PlatformAdmin) return;
    if (!actor.orgId || actor.orgId !== orgId) {
      throw new ForbiddenException('Cannot access company locations outside your organization');
    }
  }

  private canViewArchived(actor: ActorContext) {
    const effective = expandRoles(actor.role ? [actor.role] : []);
    return [Role.SuperAdmin, Role.PlatformAdmin, Role.OrgOwner, Role.OrgAdmin, Role.Admin].some((r) =>
      effective.includes(r)
    );
  }

  private ensureNotOnLegalHold(entity: { legalHold?: boolean } | null, action: string) {
    if (entity?.legalHold) {
      throw new ForbiddenException(`Cannot ${action} company location while legal hold is active`);
    }
  }

  private async validateOrg(orgId: string) {
    const org = await this.orgModel.findOne({ _id: orgId, archivedAt: null });
    if (!org) throw new NotFoundException('Organization not found or archived');
    if (org.legalHold) {
      throw new ForbiddenException('Organization is under legal hold');
    }
  }

  private async model(orgId: string) {
    return this.tenants.getModelForOrg<CompanyLocation>(
      orgId,
      'CompanyLocation',
      CompanyLocationSchema,
      this.companyLocationModel
    );
  }

  private async companies(orgId: string) {
    return this.tenants.getModelForOrg<Company>(orgId, 'Company', CompanySchema, this.companyModel);
  }

  private toPlain(record: CompanyLocation) {
    return record.toObject ? record.toObject() : record;
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

  private async ensureCompanyExists(orgId: string, companyId: string) {
    const model = await this.companies(orgId);
    const company = await model.findOne({ _id: companyId, orgId, archivedAt: null }).lean();
    if (!company) throw new NotFoundException('Company not found or archived');
  }

  async create(actor: ActorContext, orgId: string, dto: CreateCompanyLocationDto) {
    this.ensureRole(actor, [Role.OrgOwner, Role.OrgAdmin, Role.Admin, Role.SuperAdmin, Role.PlatformAdmin]);
    this.ensureOrgScope(orgId, actor);
    await this.validateOrg(orgId);
    await this.ensureCompanyExists(orgId, dto.companyId);

    const model = await this.model(orgId);
    const normalizedName = normalizeName(dto.name);
    const existing = await model.findOne({ orgId, companyId: dto.companyId, normalizedName, archivedAt: null });
    if (existing) throw new ConflictException('Company location already exists for this company');

    if (dto.externalId) {
      const externalCollision = await model.findOne({
        orgId,
        companyId: dto.companyId,
        externalId: dto.externalId,
        archivedAt: null,
      });
      if (externalCollision) throw new ConflictException('Company location externalId already exists for this company');
    }

    const tagKeys = normalizeKeys(dto.tagKeys);
    if (tagKeys.length) {
      await this.taxonomy.ensureKeysActive(actor as any, orgId, 'company_location_tag', tagKeys);
    }

    const created = await model.create({
      orgId,
      companyId: dto.companyId,
      name: dto.name,
      normalizedName,
      externalId: dto.externalId ?? null,
      timezone: dto.timezone ?? null,
      email: dto.email ?? null,
      phone: dto.phone ?? null,
      addressLine1: dto.addressLine1 ?? null,
      addressLine2: dto.addressLine2 ?? null,
      city: dto.city ?? null,
      region: dto.region ?? null,
      postal: dto.postal ?? null,
      country: dto.country ?? null,
      tagKeys,
      notes: dto.notes ?? null,
      archivedAt: null,
      piiStripped: false,
      legalHold: false,
    });

    await this.audit.log({
      eventType: 'company_location.created',
      orgId,
      userId: actor.userId,
      entity: 'CompanyLocation',
      entityId: created.id,
      metadata: { companyId: dto.companyId, name: created.name },
    });

    return this.toPlain(created);
  }

  async list(actor: ActorContext, orgId: string, query: ListCompanyLocationsQueryDto) {
    this.ensureRole(actor, [Role.OrgOwner, Role.OrgAdmin, Role.Admin, Role.SuperAdmin, Role.PlatformAdmin]);
    this.ensureOrgScope(orgId, actor);
    await this.validateOrg(orgId);
    const includeArchived = !!query.includeArchived;
    if (includeArchived && !this.canViewArchived(actor)) {
      throw new ForbiddenException('Not allowed to include archived company locations');
    }

    const filter: Record<string, any> = { orgId };
    if (!includeArchived) filter.archivedAt = null;
    if (query.companyId) filter.companyId = query.companyId;
    const tagKey = query.tag ? normalizeKey(query.tag) : '';
    if (tagKey) filter.tagKeys = tagKey;
    const search = query.search?.trim();
    if (search) {
      const escaped = search.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
      const rx = new RegExp(escaped, 'i');
      const normalizedSearch = normalizeName(search);
      const normalizedRx = new RegExp(normalizedSearch.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'), 'i');
      filter.$or = [{ name: rx }, { normalizedName: normalizedRx }];
    }

    const model = await this.model(orgId);

    const wantsPagination = query.page !== undefined || query.limit !== undefined;
    const page = Math.max(1, query.page || 1);
    const limit = Math.min(Math.max(query.limit || 25, 1), 100);

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

  async getById(actor: ActorContext, orgId: string, id: string, includeArchived = false) {
    this.ensureRole(actor, [Role.OrgOwner, Role.OrgAdmin, Role.Admin, Role.SuperAdmin, Role.PlatformAdmin]);
    this.ensureOrgScope(orgId, actor);
    await this.validateOrg(orgId);
    const model = await this.model(orgId);
    const record = await model.findOne({ _id: id, orgId });
    if (!record) throw new NotFoundException('Company location not found');
    if (record.archivedAt && !includeArchived) throw new NotFoundException('Company location archived');
    if (record.archivedAt && includeArchived && !this.canViewArchived(actor)) {
      throw new ForbiddenException('Not allowed to view archived company locations');
    }
    return this.toPlain(record);
  }

  async update(actor: ActorContext, orgId: string, id: string, dto: UpdateCompanyLocationDto) {
    this.ensureRole(actor, [Role.OrgOwner, Role.OrgAdmin, Role.Admin, Role.SuperAdmin, Role.PlatformAdmin]);
    this.ensureOrgScope(orgId, actor);
    await this.validateOrg(orgId);
    const model = await this.model(orgId);
    const record = await model.findOne({ _id: id, orgId });
    if (!record) throw new NotFoundException('Company location not found');
    if (record.archivedAt) throw new NotFoundException('Company location archived');
    this.ensureNotOnLegalHold(record, 'update');

    const before = this.toPlain(record);

    if (dto.name !== undefined) {
      const normalizedName = normalizeName(dto.name);
      const collision = await model.findOne({
        orgId,
        companyId: (record as any).companyId,
        normalizedName,
        archivedAt: null,
        _id: { $ne: id },
      });
      if (collision) throw new ConflictException('Company location already exists for this company');
      (record as any).name = dto.name;
      (record as any).normalizedName = normalizedName;
    }

    if (dto.externalId !== undefined) {
      const externalId = dto.externalId || null;
      if (externalId) {
        const collision = await model.findOne({
          orgId,
          companyId: (record as any).companyId,
          externalId,
          archivedAt: null,
          _id: { $ne: id },
        });
        if (collision) throw new ConflictException('Company location externalId already exists for this company');
      }
      (record as any).externalId = externalId;
    }

    if (dto.timezone !== undefined) (record as any).timezone = dto.timezone || null;
    if (dto.email !== undefined) (record as any).email = dto.email || null;
    if (dto.phone !== undefined) (record as any).phone = dto.phone || null;
    if (dto.addressLine1 !== undefined) (record as any).addressLine1 = dto.addressLine1 || null;
    if (dto.addressLine2 !== undefined) (record as any).addressLine2 = dto.addressLine2 || null;
    if (dto.city !== undefined) (record as any).city = dto.city || null;
    if (dto.region !== undefined) (record as any).region = dto.region || null;
    if (dto.postal !== undefined) (record as any).postal = dto.postal || null;
    if (dto.country !== undefined) (record as any).country = dto.country || null;
    if (dto.tagKeys !== undefined) {
      const tagKeys = normalizeKeys(dto.tagKeys);
      if (tagKeys.length) {
        await this.taxonomy.ensureKeysActive(actor as any, orgId, 'company_location_tag', tagKeys);
      }
      (record as any).tagKeys = tagKeys;
    }
    if (dto.notes !== undefined) (record as any).notes = dto.notes || null;

    await record.save();
    const after = this.toPlain(record);

    await this.audit.log({
      eventType: 'company_location.updated',
      orgId,
      userId: actor.userId,
      entity: 'CompanyLocation',
      entityId: record.id,
      metadata: {
        changes: this.summarizeDiff(before, after, [
          'name',
          'normalizedName',
          'externalId',
          'timezone',
          'email',
          'phone',
          'addressLine1',
          'addressLine2',
          'city',
          'region',
          'postal',
          'country',
          'tagKeys',
          'notes',
        ]),
      },
    });

    return after;
  }

  async archive(actor: ActorContext, orgId: string, id: string) {
    this.ensureRole(actor, [Role.OrgOwner, Role.OrgAdmin, Role.Admin, Role.SuperAdmin, Role.PlatformAdmin]);
    this.ensureOrgScope(orgId, actor);
    await this.validateOrg(orgId);
    const model = await this.model(orgId);
    const record = await model.findOne({ _id: id, orgId });
    if (!record) throw new NotFoundException('Company location not found');
    this.ensureNotOnLegalHold(record, 'archive');

    if (!record.archivedAt) {
      record.archivedAt = new Date();
      await record.save();
    }

    await this.audit.log({
      eventType: 'company_location.archived',
      orgId,
      userId: actor.userId,
      entity: 'CompanyLocation',
      entityId: record.id,
      metadata: { archivedAt: record.archivedAt },
    });

    return this.toPlain(record);
  }

  async unarchive(actor: ActorContext, orgId: string, id: string) {
    this.ensureRole(actor, [Role.OrgOwner, Role.OrgAdmin, Role.Admin, Role.SuperAdmin, Role.PlatformAdmin]);
    this.ensureOrgScope(orgId, actor);
    await this.validateOrg(orgId);
    const model = await this.model(orgId);
    const record = await model.findOne({ _id: id, orgId });
    if (!record) throw new NotFoundException('Company location not found');
    this.ensureNotOnLegalHold(record, 'unarchive');

    if (record.archivedAt) {
      const normalizedName = (record as any).normalizedName || normalizeName(record.name);
      const collision = await model.findOne({
        orgId,
        companyId: (record as any).companyId,
        normalizedName,
        archivedAt: null,
        _id: { $ne: id },
      });
      if (collision) throw new ConflictException('Company location already exists for this company');

      const externalId = (record as any).externalId;
      if (externalId) {
        const externalCollision = await model.findOne({
          orgId,
          companyId: (record as any).companyId,
          externalId,
          archivedAt: null,
          _id: { $ne: id },
        });
        if (externalCollision) {
          throw new ConflictException('Company location externalId already exists for this company');
        }
      }

      record.archivedAt = null;
      await record.save();
    }

    await this.audit.log({
      eventType: 'company_location.unarchived',
      orgId,
      userId: actor.userId,
      entity: 'CompanyLocation',
      entityId: record.id,
      metadata: { archivedAt: record.archivedAt },
    });

    return this.toPlain(record);
  }
}
