import { ConflictException, ForbiddenException, Injectable, NotFoundException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { Role } from '../../common/roles';
import { AuditLogService } from '../../common/services/audit-log.service';
import { CreateProjectDto } from './dto/create-project.dto';
import { UpdateProjectDto } from './dto/update-project.dto';
import { Project } from './schemas/project.schema';
import { Organization } from '../organizations/schemas/organization.schema';
import { Office } from '../offices/schemas/office.schema';

type ActorContext = { userId?: string; orgId?: string; role?: Role };

@Injectable()
export class ProjectsService {
  constructor(
    @InjectModel('Project') private readonly projectModel: Model<Project>,
    @InjectModel('Organization') private readonly orgModel: Model<Organization>,
    @InjectModel('Office') private readonly officeModel: Model<Office>,
    private readonly audit: AuditLogService
  ) {}

  private ensureRole(actor: ActorContext, allowed: Role[]) {
    if (actor.role === Role.SuperAdmin) return;
    if (!actor.role) throw new ForbiddenException('Insufficient role');
    if (allowed.includes(Role.Admin) && actor.role === Role.OrgOwner) return;
    if (!allowed.includes(actor.role)) throw new ForbiddenException('Insufficient role');
  }

  private resolveOrgId(requestedOrgId: string | undefined, actor: ActorContext) {
    if (actor.role === Role.SuperAdmin && requestedOrgId) return requestedOrgId;
    if (actor.orgId) return actor.orgId;
    throw new ForbiddenException('Missing organization context');
  }

  private ensureOrgScope(entityOrgId: string, actor: ActorContext) {
    if (actor.role === Role.SuperAdmin) return;
    if (!actor.orgId || actor.orgId !== entityOrgId) {
      throw new ForbiddenException('Cannot access project outside your organization');
    }
  }

  private canViewArchived(actor: ActorContext) {
    return (
      actor.role === Role.SuperAdmin ||
      actor.role === Role.Admin ||
      actor.role === Role.Manager ||
      actor.role === Role.OrgOwner
    );
  }

  private ensureNotOnLegalHold(entity: { legalHold?: boolean } | null, action: string) {
    if (entity?.legalHold) {
      throw new ForbiddenException(`Cannot ${action} project while legal hold is active`);
    }
  }

  private toPlain(project: Project) {
    return project.toObject ? project.toObject() : project;
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
  }

  private async validateOffice(orgId: string, officeId?: string | null) {
    if (!officeId) return;
    const office = await this.officeModel.findOne({ _id: officeId, organizationId: orgId, archivedAt: null });
    if (!office) throw new NotFoundException('Office not found or archived for this organization');
  }

  async create(dto: CreateProjectDto, actor: ActorContext) {
    this.ensureRole(actor, [Role.Admin, Role.Manager, Role.OrgOwner]);
    const organizationId = this.resolveOrgId(dto.organizationId, actor);
    await this.validateOrg(organizationId);
    await this.validateOffice(organizationId, dto.officeId);

    const existing = await this.projectModel.findOne({ organizationId, name: dto.name });
    if (existing) throw new ConflictException('Project name already exists for this organization');

    const project = await this.projectModel.create({
      name: dto.name,
      description: dto.description,
      organizationId,
      officeId: dto.officeId || null,
    });

    await this.audit.log({
      eventType: 'project.created',
      orgId: organizationId,
      userId: actor.userId,
      entity: 'Project',
      entityId: project.id,
      metadata: { name: project.name, officeId: project.officeId || null },
    });

    return this.toPlain(project);
  }

  async list(actor: ActorContext, orgId: string, includeArchived = false) {
    this.ensureRole(actor, [Role.Admin, Role.Manager, Role.OrgOwner, Role.PM, Role.Viewer]);
    const organizationId = this.resolveOrgId(orgId, actor);
    if (includeArchived && !this.canViewArchived(actor)) {
      throw new ForbiddenException('Not allowed to include archived projects');
    }
    const filter: Record<string, any> = { organizationId };
    if (!includeArchived) filter.archivedAt = null;
    return this.projectModel.find(filter).lean();
  }

  async getById(id: string, actor: ActorContext, includeArchived = false) {
    this.ensureRole(actor, [Role.Admin, Role.Manager, Role.OrgOwner, Role.PM, Role.Viewer]);
    const project = await this.projectModel.findById(id);
    if (!project) throw new NotFoundException('Project not found');
    this.ensureOrgScope(project.organizationId, actor);
    if (project.archivedAt && !includeArchived) throw new NotFoundException('Project archived');
    if (project.archivedAt && includeArchived && !this.canViewArchived(actor)) {
      throw new ForbiddenException('Not allowed to view archived projects');
    }
    return this.toPlain(project);
  }

  async update(id: string, dto: UpdateProjectDto, actor: ActorContext) {
    this.ensureRole(actor, [Role.Admin, Role.Manager, Role.OrgOwner]);
    const project = await this.projectModel.findById(id);
    if (!project) throw new NotFoundException('Project not found');
    this.ensureOrgScope(project.organizationId, actor);
    if (project.archivedAt) throw new NotFoundException('Project archived');

    if (dto.name && dto.name !== project.name) {
      const existing = await this.projectModel.findOne({
        organizationId: project.organizationId,
        name: dto.name,
        _id: { $ne: id },
      });
      if (existing) throw new ConflictException('Project name already exists for this organization');
    }

    const before = this.toPlain(project);

    if (dto.officeId !== undefined) {
      await this.validateOffice(project.organizationId, dto.officeId);
      project.officeId = dto.officeId || null;
    }
    if (dto.name !== undefined) project.name = dto.name;
    if (dto.description !== undefined) project.description = dto.description;

    await project.save();
    const after = this.toPlain(project);

    await this.audit.log({
      eventType: 'project.updated',
      orgId: project.organizationId,
      userId: actor.userId,
      entity: 'Project',
      entityId: project.id,
      metadata: { changes: this.summarizeDiff(before, after, ['name', 'description', 'officeId']) },
    });

    return after;
  }

  async archive(id: string, actor: ActorContext) {
    this.ensureRole(actor, [Role.Admin, Role.Manager, Role.OrgOwner]);
    const project = await this.projectModel.findById(id);
    if (!project) throw new NotFoundException('Project not found');
    this.ensureOrgScope(project.organizationId, actor);
    this.ensureNotOnLegalHold(project, 'archive');

    if (!project.archivedAt) {
      project.archivedAt = new Date();
      await project.save();
    }

    await this.audit.log({
      eventType: 'project.archived',
      orgId: project.organizationId,
      userId: actor.userId,
      entity: 'Project',
      entityId: project.id,
      metadata: { archivedAt: project.archivedAt },
    });
    return this.toPlain(project);
  }

  async unarchive(id: string, actor: ActorContext) {
    this.ensureRole(actor, [Role.Admin, Role.Manager, Role.OrgOwner]);
    const project = await this.projectModel.findById(id);
    if (!project) throw new NotFoundException('Project not found');
    this.ensureOrgScope(project.organizationId, actor);
    this.ensureNotOnLegalHold(project, 'unarchive');

    if (project.archivedAt) {
      project.archivedAt = null;
      await project.save();
    }

    await this.audit.log({
      eventType: 'project.unarchived',
      orgId: project.organizationId,
      userId: actor.userId,
      entity: 'Project',
      entityId: project.id,
      metadata: { archivedAt: project.archivedAt },
    });
    return this.toPlain(project);
  }
}
