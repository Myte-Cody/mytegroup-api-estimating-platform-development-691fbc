import { ConflictException, ForbiddenException, Injectable, NotFoundException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { AuditLogService } from '../../common/services/audit-log.service';
import { CreateProjectDto } from './dto/create-project.dto';
import { Project } from './schemas/project.schema';
import { Organization } from '../organizations/schemas/organization.schema';
import { Office } from '../offices/schemas/office.schema';

@Injectable()
export class ProjectsService {
  constructor(
    @InjectModel('Project') private readonly projectModel: Model<Project>,
    @InjectModel('Organization') private readonly orgModel: Model<Organization>,
    @InjectModel('Office') private readonly officeModel: Model<Office>,
    private readonly audit: AuditLogService
  ) {}

  private resolveOrgId(dtoOrgId: string | undefined, actorOrgId: string | undefined, isSuper: boolean) {
    if (dtoOrgId && isSuper) return dtoOrgId;
    if (actorOrgId) return actorOrgId;
    throw new ForbiddenException('Missing organization context');
  }

  async create(dto: CreateProjectDto, actor: { orgId?: string; role?: string }) {
    const isSuper = actor.role === 'superadmin';
    const organizationId = this.resolveOrgId(dto.organizationId, actor.orgId, isSuper);

    const org = await this.orgModel.findOne({ _id: organizationId, archivedAt: null });
    if (!org) throw new NotFoundException('Organization not found or archived');

    const existing = await this.projectModel.findOne({ organizationId, name: dto.name });
    if (existing) throw new ConflictException('Project name already exists for this organization');

    let officeId: string | undefined = dto.officeId;
    if (officeId) {
      const office = await this.officeModel.findOne({ _id: officeId, organizationId, archivedAt: null });
      if (!office) throw new NotFoundException('Office not found or archived for this organization');
    }

    const project = await this.projectModel.create({
      name: dto.name,
      description: dto.description,
      organizationId,
      officeId,
    });

    await this.audit.log({
      eventType: 'project.created',
      orgId: organizationId,
      entity: 'Project',
      entityId: project.id,
    });

    return project.toObject();
  }

  async list(orgId: string) {
    return this.projectModel.find({ organizationId: orgId, archivedAt: null });
  }

  async archive(id: string, actor: { orgId?: string; role?: string }) {
    const project = await this.projectModel.findById(id);
    if (!project) throw new NotFoundException('Project not found');
    const isSuper = actor.role === 'superadmin';
    if (!isSuper && actor.orgId && actor.orgId !== project.organizationId) {
      throw new ForbiddenException('Cannot archive project outside your organization');
    }
    if (project.archivedAt) return project.toObject();
    project.archivedAt = new Date();
    await project.save();
    await this.audit.log({
      eventType: 'project.archived',
      orgId: project.organizationId,
      entity: 'Project',
      entityId: project.id,
    });
    return project.toObject();
  }

  async unarchive(id: string, actor: { orgId?: string; role?: string }) {
    const project = await this.projectModel.findById(id);
    if (!project) throw new NotFoundException('Project not found');
    const isSuper = actor.role === 'superadmin';
    if (!isSuper && actor.orgId && actor.orgId !== project.organizationId) {
      throw new ForbiddenException('Cannot unarchive project outside your organization');
    }
    if (!project.archivedAt) return project.toObject();
    project.archivedAt = null;
    await project.save();
    await this.audit.log({
      eventType: 'project.unarchived',
      orgId: project.organizationId,
      entity: 'Project',
      entityId: project.id,
    });
    return project.toObject();
  }
}
