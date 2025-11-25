import { ConflictException, ForbiddenException, Injectable, NotFoundException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { AuditLogService } from '../../common/services/audit-log.service';
import { CreateOfficeDto } from './dto/create-office.dto';
import { Office } from './schemas/office.schema';
import { Organization } from '../organizations/schemas/organization.schema';

@Injectable()
export class OfficesService {
  constructor(
    @InjectModel('Office') private readonly officeModel: Model<Office>,
    @InjectModel('Organization') private readonly orgModel: Model<Organization>,
    private readonly audit: AuditLogService
  ) {}

  private resolveOrgId(dtoOrgId: string | undefined, actorOrgId: string | undefined, isSuper: boolean) {
    if (dtoOrgId && isSuper) return dtoOrgId;
    if (actorOrgId) return actorOrgId;
    throw new ForbiddenException('Missing organization context');
  }

  async create(dto: CreateOfficeDto, actor: { orgId?: string; role?: string }) {
    const isSuper = actor.role === 'superadmin';
    const organizationId = this.resolveOrgId(dto.organizationId, actor.orgId, isSuper);

    const org = await this.orgModel.findOne({ _id: organizationId, archivedAt: null });
    if (!org) throw new NotFoundException('Organization not found or archived');

    const existing = await this.officeModel.findOne({ organizationId, name: dto.name });
    if (existing) throw new ConflictException('Office name already exists for this organization');

    const office = await this.officeModel.create({
      name: dto.name,
      organizationId,
      address: dto.address,
    });

    await this.audit.log({
      eventType: 'office.created',
      orgId: organizationId,
      entity: 'Office',
      entityId: office.id,
    });

    return office.toObject();
  }

  async list(orgId: string) {
    return this.officeModel.find({ organizationId: orgId, archivedAt: null });
  }

  async archive(id: string, actor: { orgId?: string; role?: string }) {
    const office = await this.officeModel.findById(id);
    if (!office) throw new NotFoundException('Office not found');
    const isSuper = actor.role === 'superadmin';
    if (!isSuper && actor.orgId && actor.orgId !== office.organizationId) {
      throw new ForbiddenException('Cannot archive office outside your organization');
    }
    if (office.archivedAt) return office.toObject();
    office.archivedAt = new Date();
    await office.save();
    await this.audit.log({
      eventType: 'office.archived',
      orgId: office.organizationId,
      entity: 'Office',
      entityId: office.id,
    });
    return office.toObject();
  }

  async unarchive(id: string, actor: { orgId?: string; role?: string }) {
    const office = await this.officeModel.findById(id);
    if (!office) throw new NotFoundException('Office not found');
    const isSuper = actor.role === 'superadmin';
    if (!isSuper && actor.orgId && actor.orgId !== office.organizationId) {
      throw new ForbiddenException('Cannot unarchive office outside your organization');
    }
    if (!office.archivedAt) return office.toObject();
    office.archivedAt = null;
    await office.save();
    await this.audit.log({
      eventType: 'office.unarchived',
      orgId: office.organizationId,
      entity: 'Office',
      entityId: office.id,
    });
    return office.toObject();
  }
}
