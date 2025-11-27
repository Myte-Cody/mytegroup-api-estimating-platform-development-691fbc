import { ConflictException, ForbiddenException, Injectable, NotFoundException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { Role } from '../../common/roles';
import { AuditLogService } from '../../common/services/audit-log.service';
import { CreateOfficeDto } from './dto/create-office.dto';
import { UpdateOfficeDto } from './dto/update-office.dto';
import { Office } from './schemas/office.schema';
import { Organization } from '../organizations/schemas/organization.schema';

type ActorContext = { userId?: string; orgId?: string; role?: Role };

@Injectable()
export class OfficesService {
  constructor(
    @InjectModel('Office') private readonly officeModel: Model<Office>,
    @InjectModel('Organization') private readonly orgModel: Model<Organization>,
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
      throw new ForbiddenException('Cannot access office outside your organization');
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
  }

  async create(dto: CreateOfficeDto, actor: ActorContext) {
    this.ensureRole(actor, [Role.Admin, Role.Manager, Role.OrgOwner]);
    const organizationId = this.resolveOrgId(dto.organizationId, actor);
    await this.validateOrg(organizationId);
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
      userId: actor.userId,
      entity: 'Office',
      entityId: office.id,
      metadata: { name: office.name },
    });

    return this.toPlain(office);
  }

  async list(actor: ActorContext, orgId: string, includeArchived = false) {
    this.ensureRole(actor, [Role.Admin, Role.Manager, Role.OrgOwner, Role.PM]);
    const organizationId = this.resolveOrgId(orgId, actor);
    if (includeArchived && !this.canViewArchived(actor)) {
      throw new ForbiddenException('Not allowed to include archived offices');
    }
    const filter: Record<string, any> = { organizationId };
    if (!includeArchived) filter.archivedAt = null;
    return this.officeModel.find(filter).lean();
  }

  async getById(id: string, actor: ActorContext, includeArchived = false) {
    this.ensureRole(actor, [Role.Admin, Role.Manager, Role.OrgOwner, Role.PM]);
    const office = await this.officeModel.findById(id);
    if (!office) throw new NotFoundException('Office not found');
    this.ensureOrgScope(office.organizationId, actor);
    if (office.archivedAt && !includeArchived) throw new NotFoundException('Office archived');
    if (office.archivedAt && includeArchived && !this.canViewArchived(actor)) {
      throw new ForbiddenException('Not allowed to view archived offices');
    }
    return this.toPlain(office);
  }

  async update(id: string, dto: UpdateOfficeDto, actor: ActorContext) {
    this.ensureRole(actor, [Role.Admin, Role.Manager, Role.OrgOwner]);
    const office = await this.officeModel.findById(id);
    if (!office) throw new NotFoundException('Office not found');
    this.ensureOrgScope(office.organizationId, actor);
    if (office.archivedAt) throw new NotFoundException('Office archived');

    if (dto.name && dto.name !== office.name) {
      const existing = await this.officeModel.findOne({
        organizationId: office.organizationId,
        name: dto.name,
        _id: { $ne: id },
      });
      if (existing) throw new ConflictException('Office name already exists for this organization');
    }

    const before = this.toPlain(office);
    if (dto.name !== undefined) office.name = dto.name;
    if (dto.address !== undefined) office.address = dto.address;

    await office.save();
    const after = this.toPlain(office);

    await this.audit.log({
      eventType: 'office.updated',
      orgId: office.organizationId,
      userId: actor.userId,
      entity: 'Office',
      entityId: office.id,
      metadata: { changes: this.summarizeDiff(before, after, ['name', 'address']) },
    });

    return after;
  }

  async archive(id: string, actor: ActorContext) {
    this.ensureRole(actor, [Role.Admin, Role.Manager, Role.OrgOwner]);
    const office = await this.officeModel.findById(id);
    if (!office) throw new NotFoundException('Office not found');
    this.ensureOrgScope(office.organizationId, actor);
    this.ensureNotOnLegalHold(office, 'archive');

    if (!office.archivedAt) {
      office.archivedAt = new Date();
      await office.save();
    }

    await this.audit.log({
      eventType: 'office.archived',
      orgId: office.organizationId,
      userId: actor.userId,
      entity: 'Office',
      entityId: office.id,
      metadata: { archivedAt: office.archivedAt },
    });
    return this.toPlain(office);
  }

  async unarchive(id: string, actor: ActorContext) {
    this.ensureRole(actor, [Role.Admin, Role.Manager, Role.OrgOwner]);
    const office = await this.officeModel.findById(id);
    if (!office) throw new NotFoundException('Office not found');
    this.ensureOrgScope(office.organizationId, actor);
    this.ensureNotOnLegalHold(office, 'unarchive');

    if (office.archivedAt) {
      office.archivedAt = null;
      await office.save();
    }

    await this.audit.log({
      eventType: 'office.unarchived',
      orgId: office.organizationId,
      userId: actor.userId,
      entity: 'Office',
      entityId: office.id,
      metadata: { archivedAt: office.archivedAt },
    });
    return this.toPlain(office);
  }
}
