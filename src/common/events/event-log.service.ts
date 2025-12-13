import { ForbiddenException, Injectable, Logger, NotFoundException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { FilterQuery, Model, Types } from 'mongoose';
import { EventLog } from './event-log.schema';
import { loggingConfig } from '../../config/app.config';
import { ListEventsDto } from './dto/list-events.dto';
import { expandRoles, Role } from '../roles';

const MAX_PAGE_SIZE = 100;
const DEFAULT_LIMIT = 25;

@Injectable()
export class EventLogService {
  private readonly logger = new Logger(EventLogService.name);
  constructor(@InjectModel('EventLog') private readonly model: Model<EventLog>) {}

  async log(event: Partial<EventLog>) {
    const payload = {
      ...event,
      action: event.action || this.deriveAction(event.eventType),
      entityType: event.entityType || event.entity,
      actor: event.actor || event.userId,
      createdAt: event.createdAt || new Date(),
      metadata: loggingConfig.sanitizeMetadata(event.metadata),
      payload: loggingConfig.sanitizeMetadata(event.payload),
      archivedAt: event.archivedAt ?? null,
      piiStripped: event.piiStripped ?? false,
      legalHold: event.legalHold ?? false,
    };
    try {
      await this.model.create(payload);
    } catch (err) {
      this.logger.error(`Failed to persist event ${event.eventType || 'unknown'}`, err as Error);
    }
    if (loggingConfig.enableConsole) {
      this.logger.log(`EVENT: ${event.eventType || 'message'}`);
    }
  }

  async list(orgId: string | undefined, dto: ListEventsDto, role: Role = Role.User) {
    const effectiveRole = role || Role.User;
    this.assertReadAllowed(effectiveRole);
    const scopeOrgId = this.resolveOrgScope(orgId, dto.orgId, effectiveRole);
    if (!scopeOrgId) {
      throw new ForbiddenException('Organization context required');
    }

    const limit = Math.min(dto.limit ?? DEFAULT_LIMIT, MAX_PAGE_SIZE);
    const sortOrder = dto.sort === 'asc' ? 1 : -1;
    const baseFilters: FilterQuery<EventLog> = {};

    if (scopeOrgId) {
      baseFilters.orgId = scopeOrgId;
    }
    if (dto.entityType) {
      baseFilters.$and = (baseFilters.$and || []).concat({
        $or: [{ entityType: dto.entityType }, { entity: dto.entityType }],
      });
    }
    if (dto.entityId) baseFilters.entityId = dto.entityId;
    if (dto.actor) baseFilters.actor = dto.actor;
    if (dto.action) {
      baseFilters.$and = (baseFilters.$and || []).concat({
        $or: [{ action: dto.action }, { eventType: dto.action }],
      });
    }
    if (dto.eventType) baseFilters.eventType = dto.eventType;
    if (dto.createdAtGte || dto.createdAtLte) {
      baseFilters.createdAt = {};
      if (dto.createdAtGte) baseFilters.createdAt.$gte = dto.createdAtGte;
      if (dto.createdAtLte) baseFilters.createdAt.$lte = dto.createdAtLte;
    }
    if (dto.archived !== true) {
      baseFilters.archivedAt = null;
    }

    const total = await this.model.countDocuments(baseFilters);

    const filters: FilterQuery<EventLog> = { ...baseFilters };
    const cursorCond = this.buildCursorCondition(dto.cursor, sortOrder);
    if (cursorCond) {
      filters.$and = (filters.$and || []).concat(cursorCond);
    }

    const events = await this.model
      .find(filters)
      .sort({ createdAt: sortOrder, _id: sortOrder })
      .limit(limit)
      .lean();

    const data = events.map((ev) => this.redactIfNeeded(ev));
    const nextCursor = this.buildNextCursor(data, limit, sortOrder);

    return { data, total, nextCursor };
  }

  async findByIdScoped(id: string, orgId: string | undefined, role: Role = Role.User, requestedOrgId?: string) {
    const effectiveRole = role || Role.User;
    this.assertReadAllowed(effectiveRole);
    const scopeOrgId = this.resolveOrgScope(orgId, requestedOrgId, effectiveRole);
    if (!scopeOrgId) {
      throw new ForbiddenException('Organization context required');
    }
    const filters: FilterQuery<EventLog> = { _id: id, orgId: scopeOrgId };
    const event = await this.model.findOne(filters).lean();
    if (!event) throw new NotFoundException('Event not found');
    return this.redactIfNeeded(event);
  }

  async applyComplianceToEntityEvents(params: {
    orgId?: string;
    entity: string;
    entityId: string;
    piiStripped?: boolean;
    legalHold?: boolean;
  }) {
    const { orgId, entity, entityId, piiStripped, legalHold } = params;
    if (!orgId) return;
    if (!entity || !entityId) return;

    const updates: Record<string, any> = {};
    const set: Partial<EventLog> = {};
    if (piiStripped !== undefined) set.piiStripped = piiStripped;
    if (legalHold !== undefined) set.legalHold = legalHold;
    if (Object.keys(set).length) updates.$set = set;
    if (piiStripped === true) {
      updates.$unset = { payload: 1, metadata: 1 };
    }
    if (!Object.keys(updates).length) return;

    const filters: FilterQuery<EventLog> = {
      orgId,
      entityId,
      $or: [{ entity }, { entityType: entity }],
    };
    await this.model.updateMany(filters, updates);
  }

  private resolveOrgScope(sessionOrgId?: string, requestedOrgId?: string, role?: Role) {
    if (role === Role.SuperAdmin && requestedOrgId) return requestedOrgId;
    if (role === Role.SuperAdmin) return sessionOrgId;
    return sessionOrgId;
  }

  private deriveAction(eventType?: string) {
    if (!eventType) return undefined;
    const parts = eventType.split('.');
    return parts[parts.length - 1];
  }

  private buildCursorCondition(cursor: string | undefined, sortOrder: 1 | -1) {
    if (!cursor) return undefined;
    try {
      const decoded = JSON.parse(Buffer.from(cursor, 'base64').toString('utf-8')) as {
        createdAt: string;
        id: string;
      };
      const createdAt = new Date(decoded.createdAt);
      const objectId = new Types.ObjectId(decoded.id);
      const comparator = sortOrder === -1 ? '$lt' : '$gt';
      return {
        $or: [
          { createdAt: { [comparator]: createdAt } },
          { createdAt, _id: { [comparator]: objectId } },
        ],
      };
    } catch {
      return undefined;
    }
  }

  private buildNextCursor(events: any[], limit: number, sortOrder: 1 | -1) {
    if (!events.length || events.length < limit) return undefined;
    const last = events[events.length - 1];
    if (!last?._id || !last?.createdAt) return undefined;
    return Buffer.from(
      JSON.stringify({
        id: last._id.toString(),
        createdAt: new Date(last.createdAt).toISOString(),
        sort: sortOrder === -1 ? 'desc' : 'asc',
      })
    ).toString('base64');
  }

  private redactIfNeeded(event: any) {
    if (!event) return event;
    if (!event.piiStripped && !event.legalHold) return event;
    const { payload, metadata, ...rest } = event;
    return { ...rest, payload: undefined, metadata: undefined, redacted: true };
  }

  private assertReadAllowed(role: Role) {
    if (role === Role.OrgOwner) {
      throw new ForbiddenException('Insufficient role for audit logs');
    }
    const effectiveRoles = expandRoles(role ? [role] : []);
    const allowed = [Role.Admin, Role.Compliance, Role.Security, Role.SuperAdmin, Role.PlatformAdmin];
    const hasAccess = allowed.some((allowedRole) => effectiveRoles.includes(allowedRole));
    if (!hasAccess) {
      throw new ForbiddenException('Insufficient role for audit logs');
    }
  }
}
