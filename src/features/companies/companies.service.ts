import { ConflictException, ForbiddenException, Injectable, NotFoundException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { Role, expandRoles } from '../../common/roles';
import { AuditLogService } from '../../common/services/audit-log.service';
import { TenantConnectionService } from '../../common/tenancy/tenant-connection.service';
import { normalizeKey, normalizeKeys, normalizeName } from '../../common/utils/normalize.util';
import { CompanyLocationSchema } from '../company-locations/schemas/company-location.schema';
import { Organization } from '../organizations/schemas/organization.schema';
import { OrgTaxonomyService } from '../org-taxonomy/org-taxonomy.service';
import { PersonSchema } from '../persons/schemas/person.schema';
import { CreateCompanyDto } from './dto/create-company.dto';
import { ListCompaniesQueryDto } from './dto/list-companies.dto';
import { UpdateCompanyDto } from './dto/update-company.dto';
import { Company, CompanySchema } from './schemas/company.schema';

type ActorContext = { userId?: string; orgId?: string; role?: Role };

@Injectable()
export class CompaniesService {
  constructor(
    @InjectModel('Company') private readonly companyModel: Model<Company>,
    @InjectModel('Organization') private readonly orgModel: Model<Organization>,
    private readonly audit: AuditLogService,
    private readonly tenants: TenantConnectionService,
    private readonly taxonomy: OrgTaxonomyService
  ) {}

  private async model(orgId: string) {
    return this.tenants.getModelForOrg<Company>(orgId, 'Company', CompanySchema, this.companyModel);
  }

  private ensureRole(actor: ActorContext, allowed: Role[]) {
    const role = actor.role || Role.User;
    const effective = expandRoles([role]);
    const ok = allowed.some((r) => effective.includes(r));
    if (!ok) throw new ForbiddenException('Insufficient role');
  }

  private ensureOrgScope(orgId: string, actor: ActorContext) {
    if (actor.role === Role.SuperAdmin || actor.role === Role.PlatformAdmin) return;
    if (!actor.orgId || actor.orgId !== orgId) {
      throw new ForbiddenException('Cannot access companies outside your organization');
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
      throw new ForbiddenException(`Cannot ${action} company while legal hold is active`);
    }
  }

  private async validateOrg(orgId: string) {
    const org = await this.orgModel.findOne({ _id: orgId, archivedAt: null });
    if (!org) throw new NotFoundException('Organization not found or archived');
    if (org.legalHold) {
      throw new ForbiddenException('Organization is under legal hold');
    }
  }

  private toPlain(company: Company) {
    return company.toObject ? company.toObject() : company;
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

  private async attachRollups(orgId: string, companies: Array<Record<string, any>>) {
    const companyIds = Array.from(
      new Set(
        companies
          .map((company) => String(company?._id || company?.id || '').trim())
          .filter((value) => !!value)
      )
    );
    if (!companyIds.length) return companies;

    const [personModel, locationModel] = await Promise.all([
      this.tenants.getModelForOrg<any>(orgId, 'Person', PersonSchema),
      this.tenants.getModelForOrg<any>(orgId, 'CompanyLocation', CompanyLocationSchema),
    ]);

    const [peopleAgg, locationsAgg] = await Promise.all([
      personModel.aggregate([
        { $match: { orgId, archivedAt: null, companyId: { $in: companyIds } } },
        { $group: { _id: '$companyId', count: { $sum: 1 } } },
      ]),
      locationModel.aggregate([
        { $match: { orgId, archivedAt: null, companyId: { $in: companyIds } } },
        { $group: { _id: '$companyId', count: { $sum: 1 } } },
      ]),
    ]);

    const peopleByCompany = new Map<string, number>();
    (Array.isArray(peopleAgg) ? peopleAgg : []).forEach((row: any) => {
      const key = String(row?._id || '').trim();
      if (!key) return;
      peopleByCompany.set(key, typeof row.count === 'number' ? row.count : Number(row.count) || 0);
    });

    const locationsByCompany = new Map<string, number>();
    (Array.isArray(locationsAgg) ? locationsAgg : []).forEach((row: any) => {
      const key = String(row?._id || '').trim();
      if (!key) return;
      locationsByCompany.set(key, typeof row.count === 'number' ? row.count : Number(row.count) || 0);
    });

    return companies.map((company) => {
      const companyId = String(company?._id || company?.id || '').trim();
      return {
        ...company,
        peopleCount: peopleByCompany.get(companyId) || 0,
        locationsCount: locationsByCompany.get(companyId) || 0,
      };
    });
  }

  async create(actor: ActorContext, orgId: string, dto: CreateCompanyDto) {
    this.ensureRole(actor, [Role.OrgOwner, Role.OrgAdmin, Role.Admin, Role.SuperAdmin, Role.PlatformAdmin]);
    this.ensureOrgScope(orgId, actor);
    await this.validateOrg(orgId);
    const model = await this.model(orgId);

    const normalizedName = normalizeName(dto.name);
    const existing = await model.findOne({ orgId, normalizedName, archivedAt: null });
    if (existing) throw new ConflictException('Company already exists for this organization');

    if (dto.externalId) {
      const externalCollision = await model.findOne({ orgId, externalId: dto.externalId, archivedAt: null });
      if (externalCollision) throw new ConflictException('Company externalId already exists for this organization');
    }

    const companyTypeKeys = normalizeKeys(dto.companyTypeKeys);
    const tagKeys = normalizeKeys(dto.tagKeys);
    if (companyTypeKeys.length) {
      await this.taxonomy.ensureKeysActive(actor as any, orgId, 'company_type', companyTypeKeys);
    }
    if (tagKeys.length) {
      await this.taxonomy.ensureKeysActive(actor as any, orgId, 'company_tag', tagKeys);
    }

    const company = await model.create({
      orgId,
      name: dto.name,
      normalizedName,
      externalId: dto.externalId ?? null,
      website: dto.website ?? null,
      mainEmail: dto.mainEmail ?? null,
      mainPhone: dto.mainPhone ?? null,
      companyTypeKeys,
      tagKeys,
      rating: typeof dto.rating === 'number' ? dto.rating : null,
      notes: dto.notes ?? null,
      archivedAt: null,
      piiStripped: false,
      legalHold: false,
    });

    await this.audit.log({
      eventType: 'company.created',
      orgId,
      userId: actor.userId,
      entity: 'Company',
      entityId: company.id,
      metadata: { name: company.name },
    });

    return this.toPlain(company);
  }

  async list(actor: ActorContext, orgId: string, query: ListCompaniesQueryDto) {
    this.ensureRole(actor, [Role.OrgOwner, Role.OrgAdmin, Role.Admin, Role.SuperAdmin, Role.PlatformAdmin]);
    this.ensureOrgScope(orgId, actor);
    await this.validateOrg(orgId);
    const model = await this.model(orgId);

    const includeArchived = !!query?.includeArchived;
    if (includeArchived && !this.canViewArchived(actor)) {
      throw new ForbiddenException('Not allowed to include archived companies');
    }

    const includeCounts = !!query?.includeCounts;

    const filter: any = { orgId };
    if (!includeArchived) filter.archivedAt = null;

    const search = query?.search?.trim();
    if (search) {
      const rx = new RegExp(search.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'), 'i');
      filter.$or = [{ name: rx }, { normalizedName: rx }];
    }

    const typeKey = query?.type ? normalizeKey(query.type) : '';
    if (typeKey) filter.companyTypeKeys = typeKey;
    const tagKey = query?.tag ? normalizeKey(query.tag) : '';
    if (tagKey) filter.tagKeys = tagKey;

    const wantsPagination = query.page !== undefined || query.limit !== undefined;
    const page = Math.max(1, query.page || 1);
    const limit = Math.min(Math.max(query.limit || 25, 1), 100);

    if (!wantsPagination) {
      const data = await model.find(filter).sort({ name: 1, _id: 1 }).lean();
      return includeCounts ? this.attachRollups(orgId, data as any) : data;
    }

    const skip = (page - 1) * limit;
    const [data, total] = await Promise.all([
      model.find(filter).sort({ name: 1, _id: 1 }).skip(skip).limit(limit).lean(),
      model.countDocuments(filter),
    ]);

    const rows = includeCounts ? await this.attachRollups(orgId, data as any) : data;
    return { data: rows as any, total, page, limit };
  }

  async getById(actor: ActorContext, orgId: string, id: string, includeArchived = false, includeCounts = false) {
    this.ensureRole(actor, [Role.OrgOwner, Role.OrgAdmin, Role.Admin, Role.SuperAdmin, Role.PlatformAdmin]);
    this.ensureOrgScope(orgId, actor);
    await this.validateOrg(orgId);
    if (includeArchived && !this.canViewArchived(actor)) {
      throw new ForbiddenException('Not allowed to view archived companies');
    }

    const model = await this.model(orgId);
    const company = await model.findOne({ _id: id, orgId });
    if (!company) throw new NotFoundException('Company not found');
    if (company.archivedAt && !includeArchived) throw new NotFoundException('Company archived');

    if (!includeCounts) return this.toPlain(company);

    const [personModel, locationModel] = await Promise.all([
      this.tenants.getModelForOrg<any>(orgId, 'Person', PersonSchema),
      this.tenants.getModelForOrg<any>(orgId, 'CompanyLocation', CompanyLocationSchema),
    ]);

    const [peopleCount, locationsCount] = await Promise.all([
      personModel.countDocuments({ orgId, companyId: id, archivedAt: null }),
      locationModel.countDocuments({ orgId, companyId: id, archivedAt: null }),
    ]);

    return { ...this.toPlain(company), peopleCount, locationsCount };
  }

  async update(actor: ActorContext, orgId: string, id: string, dto: UpdateCompanyDto) {
    this.ensureRole(actor, [Role.OrgOwner, Role.OrgAdmin, Role.Admin, Role.SuperAdmin, Role.PlatformAdmin]);
    this.ensureOrgScope(orgId, actor);
    await this.validateOrg(orgId);
    const model = await this.model(orgId);
    const company = await model.findOne({ _id: id, orgId });
    if (!company) throw new NotFoundException('Company not found');
    if (company.archivedAt) throw new NotFoundException('Company archived');
    this.ensureNotOnLegalHold(company, 'update');

    const before = this.toPlain(company);

    if (dto.name !== undefined) {
      const normalizedName = normalizeName(dto.name);
      const collision = await model.findOne({
        orgId,
        normalizedName,
        archivedAt: null,
        _id: { $ne: id },
      });
      if (collision) throw new ConflictException('Company already exists for this organization');
      company.name = dto.name;
      (company as any).normalizedName = normalizedName;
    }

    if (dto.externalId !== undefined) {
      const externalId = dto.externalId || null;
      if (externalId) {
        const collision = await model.findOne({
          orgId,
          externalId,
          archivedAt: null,
          _id: { $ne: id },
        });
        if (collision) throw new ConflictException('Company externalId already exists for this organization');
      }
      (company as any).externalId = externalId;
    }

    if (dto.website !== undefined) (company as any).website = dto.website || null;
    if (dto.mainEmail !== undefined) (company as any).mainEmail = dto.mainEmail || null;
    if (dto.mainPhone !== undefined) (company as any).mainPhone = dto.mainPhone || null;
    if (dto.companyTypeKeys !== undefined) {
      const companyTypeKeys = normalizeKeys(dto.companyTypeKeys);
      if (companyTypeKeys.length) {
        await this.taxonomy.ensureKeysActive(actor as any, orgId, 'company_type', companyTypeKeys);
      }
      (company as any).companyTypeKeys = companyTypeKeys;
    }
    if (dto.tagKeys !== undefined) {
      const tagKeys = normalizeKeys(dto.tagKeys);
      if (tagKeys.length) {
        await this.taxonomy.ensureKeysActive(actor as any, orgId, 'company_tag', tagKeys);
      }
      (company as any).tagKeys = tagKeys;
    }
    if (dto.rating !== undefined) (company as any).rating = typeof dto.rating === 'number' ? dto.rating : null;
    if (dto.notes !== undefined) (company as any).notes = dto.notes || null;

    await company.save();
    const after = this.toPlain(company);

    await this.audit.log({
      eventType: 'company.updated',
      orgId,
      userId: actor.userId,
      entity: 'Company',
      entityId: company.id,
      metadata: {
        changes: this.summarizeDiff(before, after, [
          'name',
          'normalizedName',
          'externalId',
          'website',
          'mainEmail',
          'mainPhone',
          'companyTypeKeys',
          'tagKeys',
          'rating',
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
    const company = await model.findOne({ _id: id, orgId });
    if (!company) throw new NotFoundException('Company not found');
    this.ensureNotOnLegalHold(company, 'archive');

    if (!company.archivedAt) {
      company.archivedAt = new Date();
      await company.save();
    }

    await this.audit.log({
      eventType: 'company.archived',
      orgId,
      userId: actor.userId,
      entity: 'Company',
      entityId: company.id,
      metadata: { archivedAt: company.archivedAt },
    });

    return this.toPlain(company);
  }

  async unarchive(actor: ActorContext, orgId: string, id: string) {
    this.ensureRole(actor, [Role.OrgOwner, Role.OrgAdmin, Role.Admin, Role.SuperAdmin, Role.PlatformAdmin]);
    this.ensureOrgScope(orgId, actor);
    await this.validateOrg(orgId);
    const model = await this.model(orgId);
    const company = await model.findOne({ _id: id, orgId });
    if (!company) throw new NotFoundException('Company not found');
    this.ensureNotOnLegalHold(company, 'unarchive');

    if (company.archivedAt) {
      const normalizedName = (company as any).normalizedName || normalizeName(company.name);
      const collision = await model.findOne({
        orgId,
        normalizedName,
        archivedAt: null,
        _id: { $ne: id },
      });
      if (collision) throw new ConflictException('Company already exists for this organization');

      const externalId = (company as any).externalId;
      if (externalId) {
        const externalCollision = await model.findOne({
          orgId,
          externalId,
          archivedAt: null,
          _id: { $ne: id },
        });
        if (externalCollision) {
          throw new ConflictException('Company externalId already exists for this organization');
        }
      }

      company.archivedAt = null;
      await company.save();
    }

    await this.audit.log({
      eventType: 'company.unarchived',
      orgId,
      userId: actor.userId,
      entity: 'Company',
      entityId: company.id,
      metadata: { archivedAt: company.archivedAt },
    });

    return this.toPlain(company);
  }
}
