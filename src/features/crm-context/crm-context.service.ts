import { ForbiddenException, Injectable, NotFoundException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { expandRoles, Role } from '../../common/roles';
import { TenantConnectionService } from '../../common/tenancy/tenant-connection.service';
import { CompanyLocationSchema } from '../company-locations/schemas/company-location.schema';
import { CompanySchema } from '../companies/schemas/company.schema';
import { GraphEdgeSchema } from '../graph-edges/schemas/graph-edge.schema';
import { OfficeSchema } from '../offices/schemas/office.schema';
import { Organization } from '../organizations/schemas/organization.schema';
import { PersonSchema } from '../persons/schemas/person.schema';
import { ListCrmContextDocumentsQueryDto, CrmContextEntityType } from './dto/list-crm-context-documents.dto';

type ActorContext = { userId?: string; orgId?: string; role?: Role };

export type CrmContextDocument = {
  docId: string;
  orgId: string;
  entityType: CrmContextEntityType;
  entityId: string;
  title: string;
  text: string;
  metadata: Record<string, any>;
  createdAt?: string | null;
  updatedAt?: string | null;
  archivedAt?: string | null;
  piiStripped?: boolean;
  legalHold?: boolean;
};

type ListResponse<T> = { data: T[]; total: number; page: number; limit: number };

@Injectable()
export class CrmContextService {
  constructor(
    @InjectModel('Organization') private readonly orgModel: Model<Organization>,
    @InjectModel('Company') private readonly companyModel: Model<any>,
    @InjectModel('CompanyLocation') private readonly companyLocationModel: Model<any>,
    @InjectModel('Person') private readonly personModel: Model<any>,
    @InjectModel('Office') private readonly officeModel: Model<any>,
    @InjectModel('GraphEdge') private readonly graphEdgeModel: Model<any>,
    private readonly tenants: TenantConnectionService
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
      throw new ForbiddenException('Cannot access CRM context outside your organization');
    }
  }

  private async validateOrgExists(orgId: string) {
    const org = await this.orgModel.findOne({ _id: orgId, archivedAt: null });
    if (!org) throw new NotFoundException('Organization not found or archived');
  }

  private async resolveModel(orgId: string, entityType: CrmContextEntityType) {
    if (entityType === 'company') {
      return this.tenants.getModelForOrg<any>(orgId, 'Company', CompanySchema, this.companyModel);
    }
    if (entityType === 'company_location') {
      return this.tenants.getModelForOrg<any>(orgId, 'CompanyLocation', CompanyLocationSchema, this.companyLocationModel);
    }
    if (entityType === 'person') {
      return this.tenants.getModelForOrg<any>(orgId, 'Person', PersonSchema, this.personModel);
    }
    if (entityType === 'org_location') {
      return this.tenants.getModelForOrg<any>(orgId, 'Office', OfficeSchema, this.officeModel);
    }
    return this.tenants.getModelForOrg<any>(orgId, 'GraphEdge', GraphEdgeSchema, this.graphEdgeModel);
  }

  private iso(value: any) {
    if (!value) return null;
    const date = value instanceof Date ? value : new Date(value);
    if (Number.isNaN(date.getTime())) return null;
    return date.toISOString();
  }

  private joinValues(values?: Array<string | null | undefined>) {
    return (values || []).map((v) => (v || '').trim()).filter(Boolean).join(', ');
  }

  private buildDocument(orgId: string, entityType: CrmContextEntityType, record: any): CrmContextDocument {
    const entityId = String(record?.id || record?._id || '').trim();
    const archivedAt = this.iso(record?.archivedAt);

    const base = {
      docId: `crm:${entityType}:${orgId}:${entityId}`,
      orgId,
      entityType,
      entityId,
      createdAt: this.iso(record?.createdAt),
      updatedAt: this.iso(record?.updatedAt),
      archivedAt,
      piiStripped: !!record?.piiStripped,
      legalHold: !!record?.legalHold,
    };

    if (entityType === 'company') {
      const title = record?.name || `Company ${entityId}`;
      const textLines = [
        `Company: ${record?.name || ''}`.trim(),
        record?.website ? `Website: ${record.website}` : '',
        record?.mainEmail ? `Main email: ${record.mainEmail}` : '',
        record?.mainPhone ? `Main phone: ${record.mainPhone}` : '',
        typeof record?.rating === 'number' ? `Rating: ${record.rating}` : '',
        record?.companyTypeKeys?.length ? `Types: ${this.joinValues(record.companyTypeKeys)}` : '',
        record?.tagKeys?.length ? `Tags: ${this.joinValues(record.tagKeys)}` : '',
        record?.notes ? `Notes: ${record.notes}` : '',
      ].filter(Boolean);
      return {
        ...base,
        title,
        text: textLines.join('\n'),
        metadata: {
          name: record?.name ?? null,
          externalId: record?.externalId ?? null,
          companyTypeKeys: record?.companyTypeKeys || [],
          tagKeys: record?.tagKeys || [],
        },
      };
    }

    if (entityType === 'company_location') {
      const title = record?.name || `Company location ${entityId}`;
      const textLines = [
        `Company location: ${record?.name || ''}`.trim(),
        record?.companyId ? `CompanyId: ${record.companyId}` : '',
        record?.timezone ? `Timezone: ${record.timezone}` : '',
        record?.email ? `Email: ${record.email}` : '',
        record?.phone ? `Phone: ${record.phone}` : '',
        record?.addressLine1 ? `Address: ${record.addressLine1}` : '',
        record?.city ? `City: ${record.city}` : '',
        record?.region ? `Region: ${record.region}` : '',
        record?.postal ? `Postal: ${record.postal}` : '',
        record?.country ? `Country: ${record.country}` : '',
        record?.tagKeys?.length ? `Tags: ${this.joinValues(record.tagKeys)}` : '',
        record?.notes ? `Notes: ${record.notes}` : '',
      ].filter(Boolean);
      return {
        ...base,
        title,
        text: textLines.join('\n'),
        metadata: {
          name: record?.name ?? null,
          companyId: record?.companyId ?? null,
          externalId: record?.externalId ?? null,
          tagKeys: record?.tagKeys || [],
        },
      };
    }

    if (entityType === 'org_location') {
      const title = record?.name || `Org location ${entityId}`;
      const textLines = [
        `Org location: ${record?.name || ''}`.trim(),
        record?.description ? `Description: ${record.description}` : '',
        record?.timezone ? `Timezone: ${record.timezone}` : '',
        record?.orgLocationTypeKey ? `Type: ${record.orgLocationTypeKey}` : '',
        record?.tagKeys?.length ? `Tags: ${this.joinValues(record.tagKeys)}` : '',
        record?.parentOrgLocationId ? `ParentOrgLocationId: ${record.parentOrgLocationId}` : '',
        typeof record?.sortOrder === 'number' ? `SortOrder: ${record.sortOrder}` : '',
        record?.address ? `Address: ${record.address}` : '',
      ].filter(Boolean);
      return {
        ...base,
        title,
        text: textLines.join('\n'),
        metadata: {
          name: record?.name ?? null,
          orgLocationTypeKey: record?.orgLocationTypeKey ?? null,
          tagKeys: record?.tagKeys || [],
          parentOrgLocationId: record?.parentOrgLocationId ?? null,
          sortOrder: record?.sortOrder ?? null,
        },
      };
    }

    if (entityType === 'person') {
      const title = record?.displayName || `Person ${entityId}`;
      const email = record?.primaryEmail || '';
      const phone = record?.primaryPhoneE164 || '';
      const textLines = [
        `Person: ${record?.displayName || ''}`.trim(),
        record?.personType ? `Person type: ${record.personType}` : '',
        record?.departmentKey ? `Department: ${record.departmentKey}` : '',
        record?.skillKeys?.length ? `Skills: ${this.joinValues(record.skillKeys)}` : '',
        record?.tagKeys?.length ? `Tags: ${this.joinValues(record.tagKeys)}` : '',
        record?.orgLocationId ? `OrgLocationId: ${record.orgLocationId}` : '',
        record?.reportsToPersonId ? `ReportsToPersonId: ${record.reportsToPersonId}` : '',
        record?.companyId ? `CompanyId: ${record.companyId}` : '',
        record?.companyLocationId ? `CompanyLocationId: ${record.companyLocationId}` : '',
        record?.title ? `Title: ${record.title}` : '',
        email ? `Primary email: ${email}` : '',
        phone ? `Primary phone: ${phone}` : '',
        record?.ironworkerNumber ? `Ironworker #: ${record.ironworkerNumber}` : '',
        record?.unionLocal ? `Union local: ${record.unionLocal}` : '',
        record?.notes ? `Notes: ${record.notes}` : '',
      ].filter(Boolean);
      return {
        ...base,
        title,
        text: textLines.join('\n'),
        metadata: {
          displayName: record?.displayName ?? null,
          personType: record?.personType ?? null,
          primaryEmail: record?.primaryEmail ?? null,
          primaryPhoneE164: record?.primaryPhoneE164 ?? null,
          tagKeys: record?.tagKeys || [],
          skillKeys: record?.skillKeys || [],
          departmentKey: record?.departmentKey ?? null,
          orgLocationId: record?.orgLocationId ?? null,
          companyId: record?.companyId ?? null,
          companyLocationId: record?.companyLocationId ?? null,
          reportsToPersonId: record?.reportsToPersonId ?? null,
        },
      };
    }

    const title = `${record?.edgeTypeKey || 'edge'} ${entityId}`.trim();
    const textLines = [
      `Graph edge: ${record?.edgeTypeKey || ''}`.trim(),
      record?.fromNodeType && record?.fromNodeId ? `From: ${record.fromNodeType}:${record.fromNodeId}` : '',
      record?.toNodeType && record?.toNodeId ? `To: ${record.toNodeType}:${record.toNodeId}` : '',
      record?.metadata ? `Metadata: ${JSON.stringify(record.metadata)}` : '',
      record?.effectiveFrom ? `Effective from: ${this.iso(record.effectiveFrom)}` : '',
      record?.effectiveTo ? `Effective to: ${this.iso(record.effectiveTo)}` : '',
    ].filter(Boolean);
    return {
      ...base,
      title,
      text: textLines.join('\n'),
      metadata: {
        edgeTypeKey: record?.edgeTypeKey ?? null,
        fromNodeType: record?.fromNodeType ?? null,
        fromNodeId: record?.fromNodeId ?? null,
        toNodeType: record?.toNodeType ?? null,
        toNodeId: record?.toNodeId ?? null,
      },
    };
  }

  async listDocuments(
    actor: ActorContext,
    orgId: string,
    query: ListCrmContextDocumentsQueryDto
  ): Promise<ListResponse<CrmContextDocument>> {
    this.ensureRole(actor, [Role.OrgOwner, Role.OrgAdmin, Role.Admin, Role.SuperAdmin, Role.PlatformAdmin]);
    this.ensureOrgScope(orgId, actor);
    await this.validateOrgExists(orgId);

    const model = await this.resolveModel(orgId, query.entityType);
    const includeArchived = !!query.includeArchived;
    const page = Math.max(1, query.page || 1);
    const limit = Math.min(Math.max(query.limit || 100, 1), 250);
    const skip = (page - 1) * limit;

    const filter: any = { orgId };
    if (!includeArchived) filter.archivedAt = null;

    const [rows, total] = await Promise.all([
      model.find(filter).sort({ updatedAt: -1, _id: 1 }).skip(skip).limit(limit).lean(),
      model.countDocuments(filter),
    ]);

    const data = (Array.isArray(rows) ? rows : []).map((row) => this.buildDocument(orgId, query.entityType, row));
    return { data, total, page, limit };
  }
}
