import { ForbiddenException, Injectable, Logger, NotFoundException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { ClientSession, Model } from 'mongoose';
import { AuditLogService } from '../../common/services/audit-log.service';
import { Organization } from '../organizations/schemas/organization.schema';
import { Seat } from './schemas/seat.schema';

@Injectable()
export class SeatsService {
  private readonly logger = new Logger(SeatsService.name);

  constructor(
    @InjectModel('Seat') private readonly seatModel: Model<Seat>,
    @InjectModel('Organization') private readonly orgModel: Model<Organization>,
    private readonly audit: AuditLogService
  ) {}

  private async ensureOrgActive(orgId: string) {
    const org = await this.orgModel.findOne({ _id: orgId, archivedAt: null }).lean();
    if (!org) throw new NotFoundException('Organization not found or archived');
    if (org.legalHold) throw new ForbiddenException('Organization is under legal hold');
    return org;
  }

  async ensureOrgSeats(orgId: string, totalSeats: number) {
    const target = Number.isFinite(totalSeats) ? Math.max(0, Math.floor(totalSeats)) : 0;
    if (!orgId || target <= 0) return;
    await this.ensureOrgActive(orgId);

    const existing = await this.seatModel.find({ orgId }).select({ seatNumber: 1 }).lean();
    const existingNumbers = new Set<number>(existing.map((seat) => Number(seat.seatNumber)));

    const toCreate: Array<Partial<Seat>> = [];
    for (let i = 1; i <= target; i += 1) {
      if (!existingNumbers.has(i)) {
        toCreate.push({ orgId, seatNumber: i, status: 'vacant' });
      }
    }

    if (!toCreate.length) return;

    try {
      await this.seatModel.insertMany(toCreate, { ordered: false });
    } catch (err: any) {
      // Duplicate key errors can happen if two requests seed simultaneously.
      if (err?.code !== 11000) {
        this.logger.warn(`Failed to seed seats for org ${orgId}: ${err?.message || err}`);
        throw err;
      }
    }

    await this.audit.logMutation({
      action: 'seeded',
      entity: 'seats',
      orgId,
      metadata: { totalSeats: target, created: toCreate.length },
    });
  }

  async allocateSeat(
    orgId: string,
    userId: string,
    options?: { role?: string; projectId?: string | null; session?: ClientSession }
  ) {
    await this.ensureOrgActive(orgId);
    const now = new Date();
    const session = options?.session;
    const role = options?.role ? String(options.role) : 'user';
    const projectId = options?.projectId ? String(options.projectId) : null;

    const seat = await this.seatModel.findOneAndUpdate(
      { orgId, status: 'vacant' },
      {
        $set: {
          status: 'active',
          userId,
          role,
          projectId,
          activatedAt: now,
        },
        $push: {
          history: {
            userId,
            projectId,
            role,
            assignedAt: now,
            removedAt: null,
          },
        },
      },
      { new: true, session }
    );
    if (!seat) {
      throw new ForbiddenException('No available seats for this organization');
    }

    await this.audit.logMutation({
      action: 'allocated',
      entity: 'seat',
      entityId: seat.id,
      orgId,
      userId,
      metadata: { seatNumber: seat.seatNumber, role, projectId },
    }, session ? { session } : undefined);

    return seat.toObject ? seat.toObject() : seat;
  }

  async releaseSeatForUser(orgId: string, userId: string, session?: ClientSession) {
    if (!orgId || !userId) return null;
    await this.ensureOrgActive(orgId);
    const seatQuery = this.seatModel.findOne({ orgId, userId });
    if (session) {
      seatQuery.session(session);
    }
    const seat = await (typeof (seatQuery as any).exec === 'function' ? (seatQuery as any).exec() : seatQuery);
    if (!seat) return null;

    const now = new Date();
    const history = Array.isArray((seat as any).history) ? (seat as any).history : [];
    const last = [...history].reverse().find((entry) => entry?.userId === userId && !entry?.removedAt);
    if (last) {
      last.removedAt = now;
    }

    (seat as any).status = 'vacant';
    (seat as any).activatedAt = null;
    (seat as any).userId = null;
    (seat as any).projectId = null;
    (seat as any).role = null;
    (seat as any).history = history;
    await seat.save({ session });

    await this.audit.logMutation({
      action: 'released',
      entity: 'seat',
      entityId: seat.id,
      orgId,
      userId,
      metadata: { seatNumber: seat.seatNumber },
    }, session ? { session } : undefined);

    return seat.toObject ? seat.toObject() : seat;
  }

  async assignSeatToProject(
    orgId: string,
    seatId: string,
    projectId: string,
    role?: string,
    session?: ClientSession
  ) {
    await this.ensureOrgActive(orgId);
    const seatQuery = this.seatModel.findOne({ _id: seatId, orgId });
    if (session) seatQuery.session(session);
    const seat = await (typeof (seatQuery as any).exec === 'function' ? (seatQuery as any).exec() : seatQuery);
    if (!seat) throw new NotFoundException('Seat not found');
    if ((seat as any).status !== 'active' || !(seat as any).userId) {
      throw new ForbiddenException('Seat must be active and assigned before linking to a project');
    }

    const now = new Date();
    const history = Array.isArray((seat as any).history) ? (seat as any).history : [];
    const lastActive = [...history].reverse().find((entry) => entry?.projectId && !entry?.removedAt);
    if (lastActive && lastActive.projectId !== projectId) {
      lastActive.removedAt = now;
    }

    (seat as any).projectId = projectId;
    if (role) (seat as any).role = role;
    history.push({
      userId: (seat as any).userId,
      projectId,
      role: (seat as any).role || role || null,
      assignedAt: now,
      removedAt: null,
    });
    (seat as any).history = history;
    await seat.save({ session });

    await this.audit.logMutation(
      {
        action: 'project_assigned',
        entity: 'seat',
        entityId: seat.id,
        orgId,
        userId: (seat as any).userId,
        metadata: { seatNumber: (seat as any).seatNumber, projectId, role: (seat as any).role || role || null },
      },
      session ? { session } : undefined
    );

    return seat.toObject ? seat.toObject() : seat;
  }

  async clearSeatProject(orgId: string, seatId: string, projectId: string, session?: ClientSession) {
    await this.ensureOrgActive(orgId);
    const seatQuery = this.seatModel.findOne({ _id: seatId, orgId });
    if (session) seatQuery.session(session);
    const seat = await (typeof (seatQuery as any).exec === 'function' ? (seatQuery as any).exec() : seatQuery);
    if (!seat) throw new NotFoundException('Seat not found');

    const now = new Date();
    const history = Array.isArray((seat as any).history) ? (seat as any).history : [];
    const last = [...history].reverse().find((entry) => entry?.projectId === projectId && !entry?.removedAt);
    if (last) {
      last.removedAt = now;
    }

    if ((seat as any).projectId === projectId) {
      (seat as any).projectId = null;
    }
    (seat as any).history = history;
    await seat.save({ session });

    await this.audit.logMutation(
      {
        action: 'project_unassigned',
        entity: 'seat',
        entityId: seat.id,
        orgId,
        userId: (seat as any).userId,
        metadata: { seatNumber: (seat as any).seatNumber, projectId },
      },
      session ? { session } : undefined
    );

    return seat.toObject ? seat.toObject() : seat;
  }

  async summary(orgId: string) {
    await this.ensureOrgActive(orgId);
    const total = await this.seatModel.countDocuments({ orgId });
    const active = await this.seatModel.countDocuments({ orgId, status: 'active' });
    const vacant = Math.max(0, total - active);
    return { orgId, total, active, vacant };
  }

  async list(orgId: string) {
    await this.ensureOrgActive(orgId);
    return this.seatModel.find({ orgId }).sort({ seatNumber: 1 }).lean();
  }

  async findActiveSeatForUser(orgId: string, userId: string, session?: ClientSession) {
    await this.ensureOrgActive(orgId);
    const query = this.seatModel.findOne({ orgId, userId, status: 'active' });
    if (session) query.session(session);
    return query.lean();
  }
}
