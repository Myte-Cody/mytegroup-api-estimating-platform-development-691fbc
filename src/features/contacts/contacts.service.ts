import { Injectable, NotFoundException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { AuditLogService } from '../../common/services/audit-log.service';
import { TenantConnectionService } from '../../common/tenancy/tenant-connection.service';
import { CreateContactDto } from './dto/create-contact.dto';
import { UpdateContactDto } from './dto/update-contact.dto';
import { Contact, ContactSchema } from './schemas/contact.schema';

@Injectable()
export class ContactsService {
  constructor(
    @InjectModel('Contact') private readonly contactModel: Model<Contact>,
    private readonly audit: AuditLogService,
    private readonly tenants: TenantConnectionService
  ) {}

  private async model(orgId: string) {
    return this.tenants.getModelForOrg<Contact>(orgId, 'Contact', ContactSchema, this.contactModel);
  }

  async create(orgId: string, dto: CreateContactDto) {
    const model = await this.model(orgId);
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
      entity: 'Contact',
      entityId: contact.id,
    });
    return contact.toObject ? contact.toObject() : contact;
  }

  async list(orgId: string) {
    const model = await this.model(orgId);
    return model.find({ orgId, archivedAt: null }).lean();
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

  async update(orgId: string, contactId: string, dto: UpdateContactDto) {
    const model = await this.model(orgId);
    const updated = await model.findOneAndUpdate({ _id: contactId, orgId }, dto, { new: true });
    if (!updated) throw new NotFoundException('Contact not found');
    await this.audit.log({
      eventType: 'contact.updated',
      orgId,
      entity: 'Contact',
      entityId: contactId,
    });
    return updated.toObject ? updated.toObject() : updated;
  }

  async archive(orgId: string, contactId: string) {
    const model = await this.model(orgId);
    const updated = await model.findOneAndUpdate(
      { _id: contactId, orgId, archivedAt: null },
      { archivedAt: new Date() },
      { new: true }
    );
    if (!updated) throw new NotFoundException('Contact not found');
    await this.audit.log({
      eventType: 'contact.archived',
      orgId,
      entity: 'Contact',
      entityId: contactId,
    });
    return updated.toObject ? updated.toObject() : updated;
  }

  async unarchive(orgId: string, contactId: string) {
    const model = await this.model(orgId);
    const updated = await model.findOneAndUpdate(
      { _id: contactId, orgId, archivedAt: { $ne: null } },
      { archivedAt: null },
      { new: true }
    );
    if (!updated) throw new NotFoundException('Contact not found');
    await this.audit.log({
      eventType: 'contact.unarchived',
      orgId,
      entity: 'Contact',
      entityId: contactId,
    });
    return updated.toObject ? updated.toObject() : updated;
  }
}
