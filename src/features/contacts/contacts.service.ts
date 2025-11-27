import { ConflictException, ForbiddenException, Injectable, NotFoundException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { Role } from '../../common/roles';
import { AuditLogService } from '../../common/services/audit-log.service';
import { TenantConnectionService } from '../../common/tenancy/tenant-connection.service';
import { CreateContactDto } from './dto/create-contact.dto';
import { UpdateContactDto } from './dto/update-contact.dto';
import { Contact, ContactSchema } from './schemas/contact.schema';

type ActorContext = { userId?: string; orgId?: string; role?: Role };

@Injectable()
export class ContactsService {
  constructor(
    @InjectModel('Contact') private readonly contactModel: Model<Contact>,
    private readonly audit: AuditLogService,
    private readonly tenants: TenantConnectionService
  ) {}

  private ensureRole(actor: ActorContext, allowed: Role[]) {
    if (actor.role === Role.SuperAdmin) return;
    if (!actor.role) throw new ForbiddenException('Insufficient role');
    if (allowed.includes(Role.Admin) && actor.role === Role.OrgOwner) return;
    if (!allowed.includes(actor.role)) throw new ForbiddenException('Insufficient role');
  }

  private ensureOrgScope(orgId: string, actor: ActorContext) {
    if (actor.role === Role.SuperAdmin) return;
    if (!actor.orgId || actor.orgId !== orgId) {
      throw new ForbiddenException('Cannot access contacts outside your organization');
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

  private async model(orgId: string) {
    return this.tenants.getModelForOrg<Contact>(orgId, 'Contact', ContactSchema, this.contactModel);
  }

  private toPlain(contact: Contact) {
    return contact.toObject ? contact.toObject() : contact;
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

  private ensureNotOnLegalHold(entity: { legalHold?: boolean } | null, action: string) {
    if (entity?.legalHold) {
      throw new ForbiddenException(`Cannot ${action} contact while legal hold is active`);
    }
  }

  async create(actor: ActorContext, orgId: string, dto: CreateContactDto) {
    this.ensureRole(actor, [Role.Admin, Role.Manager, Role.PM, Role.OrgOwner]);
    this.ensureOrgScope(orgId, actor);
    const model = await this.model(orgId);

    if (dto.email) {
      const existing = await model.findOne({ orgId, email: dto.email });
      if (existing) throw new ConflictException('Contact email already exists for this organization');
    }

    const contact = await model.create({
      orgId,
      name: dto.name,
      email: dto.email || null,
      phone: dto.phone || null,
      company: dto.company || null,
      roles: dto.roles || [],
      tags: dto.tags || [],
      notes: dto.notes || null,
    });
    await this.audit.log({
      eventType: 'contact.created',
      orgId,
      userId: actor.userId,
      entity: 'Contact',
      entityId: contact.id,
      metadata: { name: contact.name, email: contact.email },
    });
    return this.toPlain(contact);
  }

  async list(actor: ActorContext, orgId: string, includeArchived = false) {
    this.ensureRole(actor, [Role.Admin, Role.Manager, Role.OrgOwner, Role.PM]);
    this.ensureOrgScope(orgId, actor);
    if (includeArchived && !this.canViewArchived(actor)) {
      throw new ForbiddenException('Not allowed to include archived contacts');
    }
    const model = await this.model(orgId);
    const filter: Record<string, any> = { orgId };
    if (!includeArchived) filter.archivedAt = null;
    return model.find(filter).lean();
  }

  async getById(actor: ActorContext, id: string, orgId?: string, includeArchived = false) {
    this.ensureRole(actor, [Role.Admin, Role.Manager, Role.OrgOwner, Role.PM]);
    if (!orgId && actor.role !== Role.SuperAdmin) {
      throw new ForbiddenException('Organization context is required');
    }
    const resolvedOrg = orgId || actor.orgId;
    if (!resolvedOrg) throw new ForbiddenException('Organization context is required');
    this.ensureOrgScope(resolvedOrg, actor);
    const model = await this.model(resolvedOrg);
    const contact = await model.findOne({ _id: id, orgId: resolvedOrg });
    if (!contact) throw new NotFoundException('Contact not found');
    if (contact.archivedAt && !includeArchived) throw new NotFoundException('Contact archived');
    if (contact.archivedAt && includeArchived && !this.canViewArchived(actor)) {
      throw new ForbiddenException('Not allowed to view archived contacts');
    }
    return this.toPlain(contact);
  }

  async linkInvitedUser(orgId: string, contactId: string, userId: string) {
    const model = await this.model(orgId);
    const updated = await model.findOneAndUpdate(
      { _id: contactId, orgId },
      { invitedUserId: userId, inviteStatus: 'accepted', invitedAt: new Date() },
      { new: true }
    );
    if (updated) {
      await this.audit.log({
        eventType: 'contact.invite_linked',
        orgId,
        entity: 'Contact',
        entityId: contactId,
        metadata: { invitedUserId: userId },
      });
    }
    return updated?.toObject ? updated.toObject() : updated;
  }

  async update(actor: ActorContext, orgId: string, contactId: string, dto: UpdateContactDto) {
    this.ensureRole(actor, [Role.Admin, Role.Manager, Role.PM, Role.OrgOwner]);
    this.ensureOrgScope(orgId, actor);
    const model = await this.model(orgId);
    const contact = await model.findOne({ _id: contactId, orgId });
    if (!contact) throw new NotFoundException('Contact not found');
    if (contact.archivedAt) throw new NotFoundException('Contact archived');

    if (dto.email && dto.email !== contact.email) {
      const existing = await model.findOne({ orgId, email: dto.email, _id: { $ne: contactId } });
      if (existing) throw new ConflictException('Contact email already exists for this organization');
    }

    const before = this.toPlain(contact);
    if (dto.name !== undefined) contact.name = dto.name;
    if (dto.email !== undefined) contact.email = dto.email;
    if (dto.phone !== undefined) contact.phone = dto.phone;
    if (dto.company !== undefined) contact.company = dto.company;
    if (dto.roles !== undefined) contact.roles = dto.roles;
    if (dto.tags !== undefined) contact.tags = dto.tags;
    if (dto.notes !== undefined) contact.notes = dto.notes;
    if (dto.invitedUserId !== undefined) contact.invitedUserId = dto.invitedUserId;
    if (dto.inviteStatus !== undefined) contact.inviteStatus = dto.inviteStatus;
    if (dto.invitedAt !== undefined) contact.invitedAt = dto.invitedAt ? new Date(dto.invitedAt) : null;

    await contact.save();
    const after = this.toPlain(contact);
    await this.audit.log({
      eventType: 'contact.updated',
      orgId,
      userId: actor.userId,
      entity: 'Contact',
      entityId: contactId,
      metadata: {
        changes: this.summarizeDiff(before, after, [
          'name',
          'email',
          'phone',
          'company',
          'roles',
          'tags',
          'notes',
          'invitedUserId',
          'inviteStatus',
          'invitedAt',
        ]),
      },
    });
    return after;
  }

  async archive(actor: ActorContext, orgId: string, contactId: string) {
    this.ensureRole(actor, [Role.Admin, Role.Manager, Role.OrgOwner]);
    this.ensureOrgScope(orgId, actor);
    const model = await this.model(orgId);
    const contact = await model.findOne({ _id: contactId, orgId });
    if (!contact) throw new NotFoundException('Contact not found');
    this.ensureNotOnLegalHold(contact, 'archive');

    if (!contact.archivedAt) {
      contact.archivedAt = new Date();
      await contact.save();
    }

    await this.audit.log({
      eventType: 'contact.archived',
      orgId,
      userId: actor.userId,
      entity: 'Contact',
      entityId: contactId,
      metadata: { archivedAt: contact.archivedAt },
    });
    return this.toPlain(contact);
  }

  async unarchive(actor: ActorContext, orgId: string, contactId: string) {
    this.ensureRole(actor, [Role.Admin, Role.Manager, Role.OrgOwner]);
    this.ensureOrgScope(orgId, actor);
    const model = await this.model(orgId);
    const contact = await model.findOne({ _id: contactId, orgId });
    if (!contact) throw new NotFoundException('Contact not found');
    this.ensureNotOnLegalHold(contact, 'unarchive');

    if (contact.archivedAt) {
      contact.archivedAt = null;
      await contact.save();
    }

    await this.audit.log({
      eventType: 'contact.unarchived',
      orgId,
      userId: actor.userId,
      entity: 'Contact',
      entityId: contactId,
      metadata: { archivedAt: contact.archivedAt },
    });
    return this.toPlain(contact);
  }
}
