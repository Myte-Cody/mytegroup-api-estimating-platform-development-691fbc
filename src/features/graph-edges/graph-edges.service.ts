import { BadRequestException, ConflictException, ForbiddenException, Injectable, NotFoundException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { expandRoles, Role } from '../../common/roles';
import { AuditLogService } from '../../common/services/audit-log.service';
import { TenantConnectionService } from '../../common/tenancy/tenant-connection.service';
import { normalizeKey } from '../../common/utils/normalize.util';
import { Company, CompanySchema } from '../companies/schemas/company.schema';
import { CompanyLocation, CompanyLocationSchema } from '../company-locations/schemas/company-location.schema';
import { Organization } from '../organizations/schemas/organization.schema';
import { Office, OfficeSchema } from '../offices/schemas/office.schema';
import { Person, PersonSchema } from '../persons/schemas/person.schema';
import { OrgTaxonomyService } from '../org-taxonomy/org-taxonomy.service';
import { CreateGraphEdgeDto } from './dto/create-graph-edge.dto';
import { ListGraphEdgesQueryDto } from './dto/list-graph-edges.dto';
import { GraphEdge, GraphEdgeSchema, GraphNodeType } from './schemas/graph-edge.schema';

type ActorContext = { userId?: string; orgId?: string; role?: Role };

const ALLOWED_NODE_TYPES: GraphNodeType[] = ['person', 'org_location', 'company', 'company_location'];

const EDGE_RULES: Array<{
  edgeTypeKey: string;
  from: GraphNodeType[];
  to: GraphNodeType[];
}> = [
  { edgeTypeKey: 'depends_on', from: ['org_location'], to: ['org_location'] },
  { edgeTypeKey: 'supports', from: ['org_location'], to: ['org_location'] },
  { edgeTypeKey: 'works_with', from: ['person'], to: ['person'] },
  { edgeTypeKey: 'reports_to', from: ['person'], to: ['person'] },
  { edgeTypeKey: 'primary_for', from: ['person'], to: ['company', 'company_location'] },
];

@Injectable()
export class GraphEdgesService {
  constructor(
    private readonly audit: AuditLogService,
    private readonly tenants: TenantConnectionService,
    private readonly taxonomy: OrgTaxonomyService,
    @InjectModel('GraphEdge') private readonly graphEdgeModel: Model<GraphEdge>,
    @InjectModel('Person') private readonly personModel: Model<Person>,
    @InjectModel('Company') private readonly companyModel: Model<Company>,
    @InjectModel('CompanyLocation') private readonly companyLocationModel: Model<CompanyLocation>,
    @InjectModel('Office') private readonly officeModel: Model<Office>,
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
      throw new ForbiddenException('Cannot access edges outside your organization');
    }
  }

  private ensureNotOnLegalHold(entity: { legalHold?: boolean } | null, action: string) {
    if (entity?.legalHold) {
      throw new ForbiddenException(`Cannot ${action} edge while legal hold is active`);
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
    return this.tenants.getModelForOrg<GraphEdge>(orgId, 'GraphEdge', GraphEdgeSchema, this.graphEdgeModel);
  }

  private normalizeNodeType(value: string) {
    const nodeType = String(value || '').trim().toLowerCase() as GraphNodeType;
    if (!ALLOWED_NODE_TYPES.includes(nodeType)) {
      throw new BadRequestException(`Unsupported nodeType: ${value}`);
    }
    return nodeType;
  }

  private validateEdgeTypePair(edgeTypeKey: string, fromNodeType: GraphNodeType, toNodeType: GraphNodeType) {
    const normalizedEdgeTypeKey = normalizeKey(edgeTypeKey);
    const rule = EDGE_RULES.find((r) => r.edgeTypeKey === normalizedEdgeTypeKey);
    if (!rule) {
      throw new BadRequestException(
        `edgeTypeKey ${normalizedEdgeTypeKey} is not allowed in v1 (allowed: ${EDGE_RULES.map((r) => r.edgeTypeKey).join(', ')})`
      );
    }
    if (!rule.from.includes(fromNodeType) || !rule.to.includes(toNodeType)) {
      throw new BadRequestException(
        `edgeTypeKey ${normalizedEdgeTypeKey} must be ${rule.from.join('|')} -> ${rule.to.join('|')}`
      );
    }
    return normalizedEdgeTypeKey;
  }

  private parseOptionalDate(value?: string) {
    if (!value) return null;
    const dt = new Date(value);
    if (Number.isNaN(dt.getTime())) return null;
    return dt;
  }

  private async ensureEdgeTypeAllowed(actor: ActorContext, orgId: string, edgeTypeKey: string) {
    const normalized = normalizeKey(edgeTypeKey);
    const taxonomy = await this.taxonomy.get(actor as any, orgId, 'edge_type');
    const values = (taxonomy as any)?.values || [];
    const activeKeys = new Set(values.filter((v: any) => !v.archivedAt).map((v: any) => v.key));
    if (!activeKeys.has(normalized)) {
      throw new BadRequestException(`edgeTypeKey ${normalized} is not active in org taxonomy`);
    }
    return normalized;
  }

  private async ensureNodeExists(orgId: string, nodeType: GraphNodeType, nodeId: string) {
    if (nodeType === 'person') {
      const people = await this.tenants.getModelForOrg<Person>(orgId, 'Person', PersonSchema, this.personModel);
      const person = await people.findOne({ _id: nodeId, orgId, archivedAt: null }).lean();
      if (!person) throw new NotFoundException(`Person ${nodeId} not found or archived`);
      return;
    }
    if (nodeType === 'company') {
      const companies = await this.tenants.getModelForOrg<Company>(orgId, 'Company', CompanySchema, this.companyModel);
      const company = await companies.findOne({ _id: nodeId, orgId, archivedAt: null }).lean();
      if (!company) throw new NotFoundException(`Company ${nodeId} not found or archived`);
      return;
    }
    if (nodeType === 'company_location') {
      const locations = await this.tenants.getModelForOrg<CompanyLocation>(
        orgId,
        'CompanyLocation',
        CompanyLocationSchema,
        this.companyLocationModel
      );
      const location = await locations.findOne({ _id: nodeId, orgId, archivedAt: null }).lean();
      if (!location) throw new NotFoundException(`Company location ${nodeId} not found or archived`);
      return;
    }
    if (nodeType === 'org_location') {
      const offices = await this.tenants.getModelForOrg<Office>(orgId, 'Office', OfficeSchema, this.officeModel);
      const office = await offices.findOne({ _id: nodeId, orgId, archivedAt: null }).lean();
      if (!office) throw new NotFoundException(`Org location ${nodeId} not found or archived`);
      return;
    }
    throw new BadRequestException(`Unsupported nodeType: ${nodeType}`);
  }

  private toPlain(record: GraphEdge) {
    return record.toObject ? record.toObject() : record;
  }

  async create(actor: ActorContext, orgId: string, dto: CreateGraphEdgeDto) {
    this.ensureRole(actor, [Role.OrgOwner, Role.OrgAdmin, Role.Admin, Role.SuperAdmin, Role.PlatformAdmin]);
    this.ensureOrgScope(orgId, actor);
    await this.validateOrg(orgId);

    const fromNodeType = this.normalizeNodeType(dto.fromNodeType);
    const toNodeType = this.normalizeNodeType(dto.toNodeType);
    const edgeTypeKey = await this.ensureEdgeTypeAllowed(actor, orgId, dto.edgeTypeKey);
    this.validateEdgeTypePair(edgeTypeKey, fromNodeType, toNodeType);

    if (fromNodeType === toNodeType && dto.fromNodeId === dto.toNodeId) {
      throw new BadRequestException('Self-edges are not allowed');
    }

    if (dto.metadata !== undefined && (dto.metadata === null || typeof dto.metadata !== 'object' || Array.isArray(dto.metadata))) {
      throw new BadRequestException('metadata must be an object');
    }

    await this.ensureNodeExists(orgId, fromNodeType, dto.fromNodeId);
    await this.ensureNodeExists(orgId, toNodeType, dto.toNodeId);

    const model = await this.model(orgId);
    const existing = await model.findOne({
      orgId,
      edgeTypeKey,
      fromNodeType,
      fromNodeId: dto.fromNodeId,
      toNodeType,
      toNodeId: dto.toNodeId,
      archivedAt: null,
    });
    if (existing) throw new ConflictException('Edge already exists');

    const created = await model.create({
      orgId,
      edgeTypeKey,
      fromNodeType,
      fromNodeId: dto.fromNodeId,
      toNodeType,
      toNodeId: dto.toNodeId,
      metadata: dto.metadata || {},
      effectiveFrom: this.parseOptionalDate(dto.effectiveFrom) || null,
      effectiveTo: this.parseOptionalDate(dto.effectiveTo) || null,
      archivedAt: null,
      piiStripped: false,
      legalHold: false,
    });

    await this.audit.log({
      eventType: 'graph_edge.created',
      orgId,
      userId: actor.userId,
      entity: 'GraphEdge',
      entityId: created.id,
      metadata: { edgeTypeKey, fromNodeType, toNodeType },
    });

    return this.toPlain(created);
  }

  async list(actor: ActorContext, orgId: string, query: ListGraphEdgesQueryDto) {
    this.ensureRole(actor, [Role.OrgOwner, Role.OrgAdmin, Role.Admin, Role.SuperAdmin, Role.PlatformAdmin]);
    this.ensureOrgScope(orgId, actor);
    await this.validateOrg(orgId);

    const filter: Record<string, any> = { orgId };
    const includeArchived = !!query.includeArchived;
    if (!includeArchived) filter.archivedAt = null;
    if (query.edgeTypeKey) filter.edgeTypeKey = normalizeKey(query.edgeTypeKey);
    if (query.fromNodeType) filter.fromNodeType = this.normalizeNodeType(query.fromNodeType);
    if (query.fromNodeId) filter.fromNodeId = query.fromNodeId;
    if (query.toNodeType) filter.toNodeType = this.normalizeNodeType(query.toNodeType);
    if (query.toNodeId) filter.toNodeId = query.toNodeId;

    const model = await this.model(orgId);

    const wantsPagination = query.page !== undefined || query.limit !== undefined;
    const page = Math.max(1, query.page || 1);
    const limit = Math.min(Math.max(query.limit || 25, 1), 100);

    if (!wantsPagination) {
      return model.find(filter).sort({ createdAt: -1, _id: -1 }).lean();
    }

    const skip = (page - 1) * limit;
    const [data, total] = await Promise.all([
      model.find(filter).sort({ createdAt: -1, _id: -1 }).skip(skip).limit(limit).lean(),
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
    if (!record) throw new NotFoundException('Edge not found');
    if (record.archivedAt && !includeArchived) throw new NotFoundException('Edge archived');
    return this.toPlain(record);
  }

  async archive(actor: ActorContext, orgId: string, id: string) {
    this.ensureRole(actor, [Role.OrgOwner, Role.OrgAdmin, Role.Admin, Role.SuperAdmin, Role.PlatformAdmin]);
    this.ensureOrgScope(orgId, actor);
    await this.validateOrg(orgId);
    const model = await this.model(orgId);
    const record = await model.findOne({ _id: id, orgId });
    if (!record) throw new NotFoundException('Edge not found');
    this.ensureNotOnLegalHold(record, 'archive');

    if (!record.archivedAt) {
      record.archivedAt = new Date();
      await record.save();
    }

    await this.audit.log({
      eventType: 'graph_edge.archived',
      orgId,
      userId: actor.userId,
      entity: 'GraphEdge',
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
    if (!record) throw new NotFoundException('Edge not found');
    this.ensureNotOnLegalHold(record, 'unarchive');

    if (record.archivedAt) {
      const collision = await model.findOne({
        orgId,
        edgeTypeKey: (record as any).edgeTypeKey,
        fromNodeType: (record as any).fromNodeType,
        fromNodeId: (record as any).fromNodeId,
        toNodeType: (record as any).toNodeType,
        toNodeId: (record as any).toNodeId,
        archivedAt: null,
        _id: { $ne: id },
      });
      if (collision) throw new ConflictException('Edge already exists');
      record.archivedAt = null;
      await record.save();
    }

    await this.audit.log({
      eventType: 'graph_edge.unarchived',
      orgId,
      userId: actor.userId,
      entity: 'GraphEdge',
      entityId: record.id,
      metadata: { archivedAt: record.archivedAt },
    });

    return this.toPlain(record);
  }
}
