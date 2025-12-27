import {
  BadRequestException,
  ForbiddenException,
  Injectable,
  Logger,
  NotFoundException,
} from '@nestjs/common';
import { InjectConnection, InjectModel } from '@nestjs/mongoose';
import { Connection, Model, Schema, createConnection } from 'mongoose';
import { AuditLogService } from '../../common/services/audit-log.service';
import { mongoConfig, tenantConfig } from '../../config/app.config';
import { Role } from '../../common/roles';
import { StartMigrationDto } from './dto/start-migration.dto';
import {
  CollectionProgress,
  MigrationDirection,
  MigrationStatus,
  TenantMigration,
} from './schemas/tenant-migration.schema';
import { Organization } from '../organizations/schemas/organization.schema';
import { UserSchema } from '../users/schemas/user.schema';
import { InviteSchema } from '../invites/schemas/invite.schema';
import { ContactSchema } from '../contacts/schemas/contact.schema';
import { ProjectSchema } from '../projects/schemas/project.schema';
import { OfficeSchema } from '../offices/schemas/office.schema';
import { EventLogSchema } from '../../common/events/event-log.schema';
import { AbortMigrationDto } from './dto/abort-migration.dto';
import { FinalizeMigrationDto } from './dto/finalize-migration.dto';

type Actor = { id?: string; role?: string };

type EntityConfig = {
  key: string;
  modelName: string;
  schema: Schema;
  filterField: string | string[];
};

const DEFAULT_CHUNK_SIZE = 100;
const MAX_CHUNK_SIZE = 5000;
const START_THROTTLE_MS = 30 * 1000;

@Injectable()
export class MigrationsService {
  private readonly logger = new Logger(MigrationsService.name);
  private readonly connectionCache = new Map<string, Promise<Connection>>();

  private readonly entities: EntityConfig[] = [
    { key: 'users', modelName: 'User', schema: UserSchema, filterField: 'orgId' },
    { key: 'invites', modelName: 'Invite', schema: InviteSchema, filterField: 'orgId' },
    { key: 'contacts', modelName: 'Contact', schema: ContactSchema, filterField: 'orgId' },
    { key: 'projects', modelName: 'Project', schema: ProjectSchema, filterField: 'orgId' },
    { key: 'offices', modelName: 'Office', schema: OfficeSchema, filterField: 'orgId' },
    { key: 'eventLogs', modelName: 'EventLog', schema: EventLogSchema, filterField: 'orgId' },
  ];

  constructor(
    @InjectConnection() private readonly sharedConnection: Connection,
    @InjectModel('TenantMigration') private readonly migrationModel: Model<TenantMigration>,
    @InjectModel('Organization') private readonly orgModel: Model<Organization>,
    private readonly audit: AuditLogService
  ) {}

  private ensurePlatformRole(actor: Actor) {
    if (actor.role !== Role.SuperAdmin) {
      throw new ForbiddenException('Migration tooling is restricted to platform admins');
    }
  }

  private resolveChunkSize(input?: number) {
    if (!input) return DEFAULT_CHUNK_SIZE;
    if (input > MAX_CHUNK_SIZE) return MAX_CHUNK_SIZE;
    if (input < 1) return DEFAULT_CHUNK_SIZE;
    return input;
  }

  private async getOrCreateConnection(uri: string, dbName?: string) {
    const key = `${uri}::${dbName || ''}`;
    if (this.connectionCache.has(key)) {
      return this.connectionCache.get(key) as Promise<Connection>;
    }
    const promise = createConnection(uri, { dbName, maxPoolSize: 5 })
      .asPromise()
      .catch((err) => {
        this.connectionCache.delete(key);
        throw err;
      });
    this.connectionCache.set(key, promise);
    return promise;
  }

  private buildFilter(config: EntityConfig, orgId: string) {
    if (Array.isArray(config.filterField)) {
      return { $or: config.filterField.map((field) => ({ [field]: orgId })) };
    }
    return { [config.filterField]: orgId };
  }

  private async getModel(connection: Connection, config: EntityConfig) {
    if (connection.models[config.modelName]) {
      return connection.models[config.modelName];
    }
    return connection.model(config.modelName, config.schema);
  }

  private async validateOrg(orgId: string) {
    const org = await this.orgModel.findById(orgId);
    if (!org) throw new NotFoundException('Organization not found');
    if (org.archivedAt) throw new BadRequestException('Cannot migrate an archived organization');
    return org;
  }

  private async validateTargetConnection(uri: string, dbName?: string) {
    const conn = await this.getOrCreateConnection(uri, dbName);
    await conn.db.admin().command({ ping: 1 });
    return conn;
  }

  private async recordProgress(
    migrationId: string,
    key: string,
    progress: CollectionProgress,
    status?: MigrationStatus
  ) {
    const update: Record<string, unknown> = {
      [`progress.${key}`]: progress,
      lastProgressAt: new Date(),
    };
    if (status) update.status = status;
    await this.migrationModel.findByIdAndUpdate(migrationId, update);
  }

  private async copyCollection(params: {
    migrationId: string;
    key: string;
    sourceConn: Connection;
    targetConn: Connection;
    config: EntityConfig;
    orgId: string;
    chunkSize: number;
    initialProgress?: CollectionProgress;
  }) {
    const { migrationId, key, sourceConn, targetConn, config, orgId, chunkSize, initialProgress } = params;
    const sourceModel = (await this.getModel(sourceConn, config)) as any;
    const targetModel = (await this.getModel(targetConn, config)) as any;
    const filter = this.buildFilter(config, orgId);
    const total = await sourceModel.countDocuments(filter);
    let copied = initialProgress?.copied || 0;
    let lastId = initialProgress?.lastId || null;

    await this.recordProgress(migrationId, key, { total, copied, lastId });

    while (true) {
      const query: Record<string, unknown> = { ...filter };
      if (lastId) query._id = { $gt: lastId };
      const batch = await sourceModel
        .find(query)
        .sort({ _id: 1 })
        .limit(chunkSize)
        .lean()
        .exec();
      if (!batch.length) break;
      const operations = batch.map((doc: any) =>
        targetModel.replaceOne({ _id: doc._id }, doc, { upsert: true }).exec()
      );
      await Promise.all(operations);
      copied += batch.length;
      lastId = (batch[batch.length - 1]._id as any)?.toString?.() || null;
      await this.recordProgress(migrationId, key, { total, copied, lastId });
    }
  }

  private ensureLegalHold(org: Organization, override?: boolean) {
    if (org.legalHold && !override) {
      throw new ForbiddenException('Organization is under legal hold; migration blocked without override');
    }
  }

  private async cleanupTargetData(migration: TenantMigration, orgId: string) {
    if (migration.dryRun) return;
    if (this.directionIsSharedToDedicated(migration.direction as MigrationDirection) && !migration.targetUri) {
      return;
    }
    const direction = migration.direction as MigrationDirection;
    try {
      const targetConn = this.directionIsSharedToDedicated(direction)
        ? await this.getOrCreateConnection(migration.targetUri as string, migration.targetDbName || undefined)
        : this.sharedConnection;
      for (const entity of this.entities) {
        const model = await this.getModel(targetConn, entity);
        await model.deleteMany(this.buildFilter(entity, orgId));
      }
    } catch (err) {
      this.logger.warn(`Rollback cleanup failed for org ${orgId}: ${(err as Error).message}`);
    }
  }

  private async ensureNoRecentStart(orgId: string) {
    const recent = await this.migrationModel
      .findOne({ orgId, startedAt: { $gte: new Date(Date.now() - START_THROTTLE_MS) } })
      .sort({ startedAt: -1 });
    if (recent && ['in_progress', 'ready_for_cutover'].includes(recent.status)) {
      throw new BadRequestException('Migration recently started; please wait before retrying');
    }
  }

  private directionIsSharedToDedicated(direction: MigrationDirection) {
    return direction === 'shared_to_dedicated';
  }

  async start(dto: StartMigrationDto, actor: Actor) {
    this.ensurePlatformRole(actor);
    const org = await this.validateOrg(dto.orgId);
    this.ensureLegalHold(org, dto.overrideLegalHold);

    if (dto.direction === 'dedicated_to_shared' && (!org.useDedicatedDb || !org.databaseUri)) {
      throw new BadRequestException('Organization is not using a dedicated datastore');
    }

    const chunkSize = this.resolveChunkSize(dto.chunkSize);
    const dryRun = !!dto.dryRun;
    const resume = dto.resume !== false;
    const isSharedToDedicated = this.directionIsSharedToDedicated(dto.direction);

    const initialTargetUri = isSharedToDedicated ? dto.targetUri || org.databaseUri : dto.targetUri || mongoConfig.uri;
    const initialTargetDbName =
      dto.targetDbName ||
      (isSharedToDedicated
        ? org.databaseName || `${tenantConfig.dedicated.dbNamePrefix || 'MyteTenant_'}${org.id}`
        : mongoConfig.dbName);

    let migration = await this.migrationModel
      .findOne({
        orgId: dto.orgId,
        direction: dto.direction,
        status: { $in: ['in_progress', 'failed', 'ready_for_cutover'] },
      })
      .sort({ startedAt: -1 });

    if (!migration) {
      await this.ensureNoRecentStart(dto.orgId);
    }

    if (migration && !resume) {
      throw new BadRequestException('Migration already exists for this organization; enable resume to continue');
    }

    if (!migration) {
      migration = await this.migrationModel.create({
        orgId: dto.orgId,
        direction: dto.direction,
        status: 'in_progress',
        dryRun,
        allowLegalHoldOverride: !!dto.overrideLegalHold,
        chunkSize,
        actorUserId: actor.id,
        actorRole: actor.role,
        targetUri: initialTargetUri,
        targetDbName: initialTargetDbName,
      });
    } else {
      migration.status = 'in_progress';
      migration.dryRun = dryRun;
      migration.chunkSize = chunkSize;
      migration.resumeRequested = resume;
      migration.actorUserId = actor.id;
      migration.actorRole = actor.role;
      migration.allowLegalHoldOverride = !!dto.overrideLegalHold;
      if (dto.targetUri) migration.targetUri = dto.targetUri;
      if (dto.targetDbName) migration.targetDbName = dto.targetDbName;
      if (!migration.targetUri) migration.targetUri = initialTargetUri;
      if (!migration.targetDbName) migration.targetDbName = initialTargetDbName;
      migration.error = null;
      await migration.save();
    }

    const direction = migration.direction as MigrationDirection;

    const sourceConn = isSharedToDedicated
      ? this.sharedConnection
      : await this.getOrCreateConnection(org.databaseUri as string, org.databaseName || undefined);

    const targetUri = isSharedToDedicated ? migration.targetUri : migration.targetUri || mongoConfig.uri;
    const targetDbName = isSharedToDedicated ? migration.targetDbName : migration.targetDbName || mongoConfig.dbName;

    if (!targetUri) {
      throw new BadRequestException('Target URI is required for migration');
    }
    if (isSharedToDedicated && targetUri === mongoConfig.uri && targetDbName === mongoConfig.dbName) {
      throw new BadRequestException('Target datastore matches shared datastore; aborting');
    }

    await this.audit.log({
      eventType: 'migration.start',
      orgId: dto.orgId,
      userId: actor.id,
      metadata: {
        direction,
        dryRun,
        chunkSize,
        targetDbName,
        targetUriProvided: !!targetUri,
        resume,
      },
    });

    try {
      const targetConn = await this.validateTargetConnection(targetUri, targetDbName || undefined);

      migration.targetUri = targetUri;
      migration.targetDbName = targetDbName || null;
      migration.status = 'in_progress';
      migration.startedAt = migration.startedAt || new Date();
      await migration.save();

      for (const entity of this.entities) {
        if (dryRun) {
          const sourceModel = await this.getModel(sourceConn, entity);
          const total = await sourceModel.countDocuments(this.buildFilter(entity, dto.orgId));
          await this.recordProgress(migration.id, entity.key, { total, copied: total, lastId: null });
          continue;
        }
        await this.copyCollection({
          migrationId: migration.id,
          key: entity.key,
          sourceConn,
          targetConn,
          config: entity,
          orgId: dto.orgId,
          chunkSize,
          initialProgress: (migration.progress && (migration.progress as any)[entity.key]) || undefined,
        });
        await this.audit.log({
          eventType: 'migration.progress',
          orgId: dto.orgId,
          metadata: { entity: entity.key },
        });
      }

      migration.status = dryRun ? 'completed' : 'ready_for_cutover';
      migration.error = null;
      migration.completedAt = dryRun ? new Date() : null;
      migration.lastProgressAt = new Date();
      await migration.save();

      await this.audit.log({
        eventType: dryRun ? 'migration.dry_run.completed' : 'migration.ready_for_cutover',
        orgId: dto.orgId,
        metadata: { direction, dryRun, targetDbName },
      });
    } catch (err) {
      const message = (err as Error).message || 'Migration failed';
      this.logger.error(`Migration failed for org ${dto.orgId}`, err as Error);
      migration.status = 'failed';
      migration.error = message;
      migration.lastProgressAt = new Date();
      await migration.save();
      await this.audit.log({
        eventType: 'migration.failed',
        orgId: dto.orgId,
        metadata: { direction, error: message },
      });
      throw err;
    }

    const latest = await this.migrationModel.findById(migration.id).lean();
    return latest || migration.toObject();
  }

  async status(orgId: string) {
    const migration = await this.migrationModel.findOne({ orgId }).sort({ startedAt: -1 }).lean();
    if (!migration) throw new NotFoundException('No migration found for organization');
    return migration;
  }

  async abort(dto: AbortMigrationDto, actor: Actor) {
    this.ensurePlatformRole(actor);
    const migration = await this.migrationModel.findOne({ _id: dto.migrationId, orgId: dto.orgId });
    if (!migration) throw new NotFoundException('Migration not found');
    if (migration.status === 'completed') {
      throw new BadRequestException('Completed migrations cannot be aborted');
    }
    await this.cleanupTargetData(migration, dto.orgId);
    migration.status = 'aborted';
    migration.error = dto.reason || 'Aborted by operator';
    migration.completedAt = new Date();
    await migration.save();

    await this.audit.log({
      eventType: 'migration.aborted',
      orgId: dto.orgId,
      userId: actor.id,
      metadata: { reason: dto.reason, migrationId: dto.migrationId },
    });

    return migration.toObject();
  }

  async finalize(dto: FinalizeMigrationDto, actor: Actor) {
    this.ensurePlatformRole(actor);
    const migration = await this.migrationModel.findOne({ _id: dto.migrationId, orgId: dto.orgId });
    if (!migration) throw new NotFoundException('Migration not found');
    if (migration.dryRun) throw new BadRequestException('Cannot finalize a dry-run migration');
    if (migration.status !== 'ready_for_cutover') {
      throw new BadRequestException('Migration is not ready for cutover');
    }

    const org = await this.validateOrg(dto.orgId);
    const direction = migration.direction as MigrationDirection;
    if (direction === 'shared_to_dedicated') {
      org.useDedicatedDb = true;
      org.databaseUri = migration.targetUri || org.databaseUri;
      org.databaseName = migration.targetDbName || org.databaseName;
      org.dataResidency = 'dedicated';
    } else {
      org.useDedicatedDb = false;
      org.dataResidency = 'shared';
    }
    org.lastMigratedAt = new Date();
    await org.save();

    migration.status = 'completed';
    migration.completedAt = new Date();
    migration.lastProgressAt = new Date();
    await migration.save();

    await this.audit.log({
      eventType: 'migration.finalized',
      orgId: dto.orgId,
      userId: actor.id,
      metadata: {
        direction,
        targetDbName: migration.targetDbName,
      },
    });

    return migration.toObject();
  }
}
