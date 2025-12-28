import { ForbiddenException, Injectable, NotFoundException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { AuditLogService } from '../../common/services/audit-log.service';
import { Role } from '../../common/roles';
import { Organization } from '../organizations/schemas/organization.schema';
import { ListNotificationsQueryDto } from './dto/list-notifications.dto';
import { Notification, NotificationSchema } from './schemas/notification.schema';
import { TenantConnectionService } from '../../common/tenancy/tenant-connection.service';

type ActorContext = { userId?: string; orgId?: string; role?: Role };

@Injectable()
export class NotificationsService {
  constructor(
    private readonly audit: AuditLogService,
    private readonly tenants: TenantConnectionService,
    @InjectModel('Notification') private readonly notificationModel: Model<Notification>,
    @InjectModel('Organization') private readonly orgModel: Model<Organization>
  ) {}

  private ensureActor(actor: ActorContext) {
    if (!actor.userId) throw new ForbiddenException('Missing user context');
    if (!actor.orgId && actor.role !== Role.SuperAdmin && actor.role !== Role.PlatformAdmin) {
      throw new ForbiddenException('Missing organization context');
    }
  }

  private async validateOrg(orgId: string) {
    const org = await this.orgModel.findOne({ _id: orgId, archivedAt: null });
    if (!org) throw new NotFoundException('Organization not found or archived');
    return org;
  }

  private async model(orgId: string) {
    return this.tenants.getModelForOrg<Notification>(
      orgId,
      'Notification',
      NotificationSchema,
      this.notificationModel
    );
  }

  async create(orgId: string, userId: string, type: string, payload?: Record<string, any>) {
    if (!orgId || !userId || !type) return null;
    await this.validateOrg(orgId);
    const model = await this.model(orgId);
    const created = await model.create({
      orgId,
      userId,
      type,
      payload: payload || {},
      read: false,
      readAt: null,
    });

    await this.audit.logMutation({
      action: 'created',
      entity: 'notification',
      orgId,
      userId,
      entityId: created.id,
      metadata: { type },
    });

    return created.toObject ? created.toObject() : created;
  }

  async list(actor: ActorContext, query: ListNotificationsQueryDto) {
    this.ensureActor(actor);
    const orgId = actor.orgId as string;
    await this.validateOrg(orgId);
    const model = await this.model(orgId);

    const filter: Record<string, any> = { orgId, userId: actor.userId };
    if (query.read !== undefined) filter.read = query.read;

    const page = Math.max(1, query.page || 1);
    const limit = Math.min(Math.max(query.limit || 25, 1), 100);
    const skip = (page - 1) * limit;

    const [data, total] = await Promise.all([
      model.find(filter).sort({ createdAt: -1 }).skip(skip).limit(limit).lean(),
      model.countDocuments(filter),
    ]);

    return { data, total, page, limit };
  }

  async markRead(actor: ActorContext, id: string) {
    this.ensureActor(actor);
    const orgId = actor.orgId as string;
    await this.validateOrg(orgId);
    const model = await this.model(orgId);
    const notification = await model.findOne({ _id: id, orgId, userId: actor.userId });
    if (!notification) throw new NotFoundException('Notification not found');
    if (!notification.read) {
      notification.read = true;
      notification.readAt = new Date();
      await notification.save();
    }

    await this.audit.logMutation({
      action: 'read',
      entity: 'notification',
      orgId,
      userId: actor.userId,
      entityId: notification.id,
      metadata: { type: notification.type },
    });

    return notification.toObject ? notification.toObject() : notification;
  }
}
