import {
  ConflictException,
  ForbiddenException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { expandRoles, Role } from '../../common/roles';
import { AuditLogService } from '../../common/services/audit-log.service';
import { Organization } from '../organizations/schemas/organization.schema';
import { Project } from '../projects/schemas/project.schema';
import { CreateEstimateDto } from './dto/create-estimate.dto';
import { UpdateEstimateDto } from './dto/update-estimate.dto';
import { Estimate, EstimateLineItem, EstimateStatus } from './schemas/estimate.schema';

type ActorContext = { userId?: string; orgId?: string; role?: Role };

@Injectable()
export class EstimatesService {
  constructor(
    @InjectModel('Estimate') private readonly estimateModel: Model<Estimate>,
    @InjectModel('Project') private readonly projectModel: Model<Project>,
    @InjectModel('Organization') private readonly orgModel: Model<Organization>,
    private readonly audit: AuditLogService
  ) {}

  private toPlain<T extends { toObject?: () => any }>(doc: T) {
    return doc?.toObject ? doc.toObject() : (doc as any);
  }

  private isPrivileged(actor: ActorContext) {
    return actor.role === Role.SuperAdmin || actor.role === Role.PlatformAdmin;
  }

  private ensureRole(actor: ActorContext, allowed: Role[]) {
    const role = actor.role || Role.User;
    const effectiveRoles = expandRoles([role]);
    const allowedEffective = allowed.some((required) => effectiveRoles.includes(required));
    if (!allowedEffective) {
      throw new ForbiddenException('Insufficient role');
    }
  }

  private ensureOrgScope(entityOrgId: string, actor: ActorContext) {
    if (this.isPrivileged(actor)) return;
    if (!actor.orgId || actor.orgId !== entityOrgId) {
      throw new ForbiddenException('Cannot access estimate outside your organization');
    }
  }

  private ensureNotOnLegalHold(record: { legalHold?: boolean } | null, action: string) {
    if (record?.legalHold) {
      throw new ForbiddenException(`Cannot ${action} estimate while legal hold is active`);
    }
  }

  private ensureProjectWritable(project: any, action: string) {
    if (!project) return;
    if (project.archivedAt) {
      throw new ForbiddenException(`Cannot ${action} estimate for archived project`);
    }
    if (project.legalHold) {
      throw new ForbiddenException(`Cannot ${action} estimate while project legal hold is active`);
    }
  }

  private async loadProjectOrThrow(projectId: string, actor: ActorContext) {
    const project = await this.projectModel.findById(projectId).lean();
    if (!project) throw new NotFoundException('Project not found');
    const orgId = (project as any).orgId;
    if (!orgId) throw new ForbiddenException('Project is missing organization scope');
    this.ensureOrgScope(orgId, actor);
    return project;
  }

  private async loadOrgOrThrow(orgId: string) {
    const org = await this.orgModel.findOne({ _id: orgId, archivedAt: null }).lean();
    if (!org) throw new NotFoundException('Organization not found or archived');
    if (org.legalHold) {
      throw new ForbiddenException('Organization is under legal hold');
    }
    return org;
  }

  private normalizeLineItems(items?: EstimateLineItem[]) {
    const raw = Array.isArray(items) ? items : [];
    const normalized = raw.map((item) => {
      const quantity = typeof item.quantity === 'number' ? item.quantity : Number(item.quantity || 0);
      const unitCost = typeof item.unitCost === 'number' ? item.unitCost : Number(item.unitCost || 0);
      const total = Number.isFinite(quantity) && Number.isFinite(unitCost) ? quantity * unitCost : 0;
      return {
        code: item.code || undefined,
        description: item.description || undefined,
        quantity: Number.isFinite(quantity) ? quantity : 0,
        unit: item.unit || undefined,
        unitCost: Number.isFinite(unitCost) ? unitCost : 0,
        total: Number.isFinite(total) ? total : 0,
      };
    });

    const totalAmount = normalized.reduce((sum, item) => sum + (item.total || 0), 0);
    return { lineItems: normalized, totalAmount: Number.isFinite(totalAmount) ? totalAmount : undefined };
  }

  private canViewArchived(actor: ActorContext) {
    const effectiveRoles = expandRoles(actor.role ? [actor.role] : []);
    return [Role.SuperAdmin, Role.PlatformAdmin, Role.Admin, Role.OrgOwner].some((role) =>
      effectiveRoles.includes(role)
    );
  }

  private summarizeDiff(before: Record<string, any>, after: Record<string, any>, fields: string[]) {
    const changes: Record<string, { from: any; to: any }> = {};
    fields.forEach((field) => {
      if (before[field] === after[field]) return;
      changes[field] = { from: before[field], to: after[field] };
    });
    return Object.keys(changes).length ? changes : undefined;
  }

  async create(projectId: string, dto: CreateEstimateDto, actor: ActorContext) {
    this.ensureRole(actor, [
      Role.Estimator,
      Role.PM,
      Role.Admin,
      Role.OrgOwner,
      Role.SuperAdmin,
      Role.PlatformAdmin,
    ]);
    const project = await this.loadProjectOrThrow(projectId, actor);
    const projectOrgId = (project as any).orgId;
    await this.loadOrgOrThrow(projectOrgId);
    this.ensureProjectWritable(project, 'create');

    const existing = await this.estimateModel.findOne({
      projectId,
      orgId: projectOrgId,
      name: dto.name,
      archivedAt: null,
    });
    if (existing) throw new ConflictException('Estimate name already exists for this project');

    const { lineItems, totalAmount } = this.normalizeLineItems(dto.lineItems as any);

    const estimate = await this.estimateModel.create({
      projectId,
      orgId: projectOrgId,
      createdByUserId: actor.userId,
      name: dto.name,
      description: dto.description,
      notes: dto.notes,
      status: 'draft' as EstimateStatus,
      revision: 1,
      lineItems,
      totalAmount,
    });

    await this.audit.log({
      eventType: 'estimate.created',
      orgId: projectOrgId,
      userId: actor.userId,
      entity: 'Estimate',
      entityId: estimate.id,
      metadata: { projectId, name: estimate.name, totalAmount: estimate.totalAmount || 0 },
    });

    return this.toPlain(estimate);
  }

  async list(projectId: string, actor: ActorContext, includeArchived = false) {
    this.ensureRole(actor, [
      Role.Estimator,
      Role.PM,
      Role.Admin,
      Role.OrgOwner,
      Role.SuperAdmin,
      Role.PlatformAdmin,
    ]);
    const project = await this.loadProjectOrThrow(projectId, actor);
    if (includeArchived && !this.canViewArchived(actor)) {
      throw new ForbiddenException('Not allowed to include archived estimates');
    }
    const projectOrgId = (project as any).orgId;
    const filter: Record<string, any> = {
      projectId,
      orgId: projectOrgId,
    };
    if (!includeArchived) filter.archivedAt = null;
    return this.estimateModel.find(filter).lean();
  }

  async getById(projectId: string, estimateId: string, actor: ActorContext, includeArchived = false) {
    this.ensureRole(actor, [
      Role.Estimator,
      Role.PM,
      Role.Admin,
      Role.OrgOwner,
      Role.SuperAdmin,
      Role.PlatformAdmin,
    ]);
    const estimate = await this.estimateModel.findById(estimateId);
    if (!estimate) throw new NotFoundException('Estimate not found');
    if (estimate.projectId !== projectId) throw new NotFoundException('Estimate not found');
    this.ensureOrgScope((estimate as any).orgId, actor);
    if (estimate.archivedAt && !includeArchived) throw new NotFoundException('Estimate archived');
    if (estimate.archivedAt && includeArchived && !this.canViewArchived(actor)) {
      throw new ForbiddenException('Not allowed to view archived estimate');
    }
    return this.toPlain(estimate);
  }

  async update(projectId: string, estimateId: string, dto: UpdateEstimateDto, actor: ActorContext) {
    this.ensureRole(actor, [
      Role.Estimator,
      Role.PM,
      Role.Admin,
      Role.OrgOwner,
      Role.SuperAdmin,
      Role.PlatformAdmin,
    ]);
    const estimate = await this.estimateModel.findById(estimateId);
    if (!estimate) throw new NotFoundException('Estimate not found');
    if (estimate.projectId !== projectId) throw new NotFoundException('Estimate not found');
    const estimateOrgId = (estimate as any).orgId;
    this.ensureOrgScope(estimateOrgId, actor);
    if (estimate.archivedAt) throw new NotFoundException('Estimate archived');
    this.ensureNotOnLegalHold(estimate, 'update');

    const project = await this.loadProjectOrThrow(projectId, actor);
    await this.loadOrgOrThrow((project as any).orgId);
    this.ensureProjectWritable(project, 'update');

    if (dto.name && dto.name !== estimate.name) {
      const collision = await this.estimateModel.findOne({
        projectId,
        orgId: estimateOrgId,
        name: dto.name,
        archivedAt: null,
        _id: { $ne: estimateId },
      });
      if (collision) throw new ConflictException('Estimate name already exists for this project');
    }

    const before = this.toPlain(estimate);

    if (dto.name !== undefined) estimate.name = dto.name;
    if (dto.description !== undefined) estimate.description = dto.description;
    if (dto.notes !== undefined) estimate.notes = dto.notes;
    if (dto.status !== undefined) estimate.status = dto.status as EstimateStatus;

    if (dto.lineItems !== undefined) {
      const { lineItems, totalAmount } = this.normalizeLineItems(dto.lineItems as any);
      estimate.lineItems = lineItems as any;
      estimate.totalAmount = totalAmount;
      estimate.revision = (estimate.revision || 1) + 1;
    }

    await estimate.save();
    const after = this.toPlain(estimate);

    await this.audit.log({
      eventType: 'estimate.updated',
      orgId: estimateOrgId,
      userId: actor.userId,
      entity: 'Estimate',
      entityId: estimate.id,
      metadata: { projectId, changes: this.summarizeDiff(before, after, ['name', 'description', 'notes', 'status', 'totalAmount']) },
    });

    return after;
  }

  async archive(projectId: string, estimateId: string, actor: ActorContext) {
    this.ensureRole(actor, [
      Role.Estimator,
      Role.PM,
      Role.Admin,
      Role.OrgOwner,
      Role.SuperAdmin,
      Role.PlatformAdmin,
    ]);
    const estimate = await this.estimateModel.findById(estimateId);
    if (!estimate) throw new NotFoundException('Estimate not found');
    if (estimate.projectId !== projectId) throw new NotFoundException('Estimate not found');
    const estimateOrgId = (estimate as any).orgId;
    this.ensureOrgScope(estimateOrgId, actor);
    this.ensureNotOnLegalHold(estimate, 'archive');

    const project = await this.loadProjectOrThrow(projectId, actor);
    await this.loadOrgOrThrow((project as any).orgId);
    this.ensureProjectWritable(project, 'archive');

    if (!estimate.archivedAt) {
      estimate.archivedAt = new Date();
      estimate.status = 'archived';
      await estimate.save();
    }

    await this.audit.log({
      eventType: 'estimate.archived',
      orgId: estimateOrgId,
      userId: actor.userId,
      entity: 'Estimate',
      entityId: estimate.id,
      metadata: { projectId, archivedAt: estimate.archivedAt },
    });

    return this.toPlain(estimate);
  }

  async unarchive(projectId: string, estimateId: string, actor: ActorContext) {
    this.ensureRole(actor, [
      Role.Estimator,
      Role.PM,
      Role.Admin,
      Role.OrgOwner,
      Role.SuperAdmin,
      Role.PlatformAdmin,
    ]);
    const estimate = await this.estimateModel.findById(estimateId);
    if (!estimate) throw new NotFoundException('Estimate not found');
    if (estimate.projectId !== projectId) throw new NotFoundException('Estimate not found');
    const estimateOrgId = (estimate as any).orgId;
    this.ensureOrgScope(estimateOrgId, actor);
    this.ensureNotOnLegalHold(estimate, 'unarchive');

    const project = await this.loadProjectOrThrow(projectId, actor);
    await this.loadOrgOrThrow((project as any).orgId);
    this.ensureProjectWritable(project, 'unarchive');

    if (estimate.archivedAt) {
      estimate.archivedAt = null;
      if (estimate.status === 'archived') estimate.status = 'draft';
      await estimate.save();
    }

    await this.audit.log({
      eventType: 'estimate.unarchived',
      orgId: estimateOrgId,
      userId: actor.userId,
      entity: 'Estimate',
      entityId: estimate.id,
      metadata: { projectId, archivedAt: estimate.archivedAt },
    });

    return this.toPlain(estimate);
  }
}
