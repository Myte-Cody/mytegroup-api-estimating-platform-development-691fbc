import {
  BadRequestException,
  ConflictException,
  ForbiddenException,
  Injectable,
  NotFoundException,
  ServiceUnavailableException,
} from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import ExcelJS from 'exceljs';
import { Role, expandRoles } from '../../common/roles';
import { AuditLogService } from '../../common/services/audit-log.service';
import { AiService } from '../../common/services/ai.service';
import { TenantConnectionService } from '../../common/tenancy/tenant-connection.service';
import { Organization } from '../organizations/schemas/organization.schema';
import { BulkCostCodesDto } from './dto/bulk-cost-codes.dto';
import { CostCodeInputDto } from './dto/cost-code-input.dto';
import { CreateCostCodeDto } from './dto/create-cost-code.dto';
import { CostCodeImportCommitDto } from './dto/import-commit.dto';
import { ListCostCodesQueryDto } from './dto/list-cost-codes.dto';
import { SeedCostCodesDto } from './dto/seed-cost-codes.dto';
import { ToggleCostCodeDto } from './dto/toggle-cost-code.dto';
import { UpdateCostCodeDto } from './dto/update-cost-code.dto';
import { STEEL_FIELD_PACK } from './seed/steel-field-pack';
import { CostCode, CostCodeSchema } from './schemas/cost-code.schema';
import {
  CostCodeImportJob,
  CostCodeImportJobSchema,
  CostCodeImportStatus,
} from './schemas/cost-code-import-job.schema';
import { enqueueCostCodeImport } from '../../queues/cost-code-import.queue';

type ActorContext = { userId?: string; orgId?: string; role?: Role };

const ALLOWED_ROLES = [Role.SuperAdmin, Role.PlatformAdmin, Role.Admin, Role.OrgOwner, Role.OrgAdmin];
const MAX_IMPORT_ROWS = 1000;
const MAX_IMPORT_CODES = 5000;

@Injectable()
export class CostCodesService {
  constructor(
    private readonly audit: AuditLogService,
    private readonly tenants: TenantConnectionService,
    private readonly ai: AiService,
    @InjectModel('CostCode') private readonly costCodeModel: Model<CostCode>,
    @InjectModel('CostCodeImportJob') private readonly importJobModel: Model<CostCodeImportJob>,
    @InjectModel('Organization') private readonly orgModel: Model<Organization>
  ) {}

  private ensureRole(actor: ActorContext) {
    const role = actor.role || Role.User;
    const effective = expandRoles([role]);
    const ok = ALLOWED_ROLES.some((r) => effective.includes(r));
    if (!ok) throw new ForbiddenException('Insufficient role');
  }

  private ensureOrgScope(actor: ActorContext, orgId: string) {
    if (actor.role === Role.SuperAdmin || actor.role === Role.PlatformAdmin) return;
    if (!actor.orgId || actor.orgId !== orgId) {
      throw new ForbiddenException('Cannot access cost codes outside your organization');
    }
  }

  private async validateOrg(orgId: string) {
    const org = await this.orgModel.findOne({ _id: orgId, archivedAt: null });
    if (!org) throw new NotFoundException('Organization not found or archived');
    if (org.legalHold) {
      throw new ForbiddenException('Organization is under legal hold');
    }
  }

  private async costCodes(orgId: string) {
    return this.tenants.getModelForOrg<CostCode>(orgId, 'CostCode', CostCodeSchema, this.costCodeModel);
  }

  private async importJobs(orgId: string) {
    return this.tenants.getModelForOrg<CostCodeImportJob>(
      orgId,
      'CostCodeImportJob',
      CostCodeImportJobSchema,
      this.importJobModel
    );
  }

  private sanitizeText(value?: string | null) {
    if (value === undefined || value === null) return '';
    return String(value).trim();
  }

  private normalizeCostCode(input: Partial<CostCodeInputDto>, allowDefaultCategory = false) {
    const categoryRaw = this.sanitizeText(input.category);
    const category = categoryRaw || (allowDefaultCategory ? 'General' : '');
    const code = this.sanitizeText(input.code);
    const description = this.sanitizeText(input.description);
    return { category, code, description };
  }

  private normalizeCostCodes(inputs: Partial<CostCodeInputDto>[], allowDefaultCategory = false) {
    const seen = new Set<string>();
    const normalized: CostCodeInputDto[] = [];

    (inputs || []).forEach((raw) => {
      const { category, code, description } = this.normalizeCostCode(raw, allowDefaultCategory);
      if (!category || !code || !description) return;
      if (seen.has(code)) return;
      seen.add(code);
      normalized.push({ category, code, description });
    });

    if (normalized.length > MAX_IMPORT_CODES) {
      return normalized.slice(0, MAX_IMPORT_CODES);
    }
    return normalized;
  }

  private parseOptionalDate(value?: string) {
    if (!value) return null;
    const dt = new Date(value);
    if (Number.isNaN(dt.getTime())) return null;
    return dt;
  }

  private buildFilter(orgId: string, query: ListCostCodesQueryDto) {
    const filter: Record<string, any> = { orgId };
    if (query.active !== undefined) {
      filter.active = query.active;
    }
    if (query.category) {
      filter.category = query.category;
    }
    if (query.updatedSince) {
      const since = this.parseOptionalDate(query.updatedSince);
      if (since) filter.updatedAt = { $gte: since };
    }
    if (query.q) {
      const escaped = query.q.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
      const rx = new RegExp(escaped, 'i');
      filter.$or = [{ code: rx }, { description: rx }, { category: rx }];
    }
    return filter;
  }

  async list(actor: ActorContext, query: ListCostCodesQueryDto) {
    this.ensureRole(actor);
    if (!actor.orgId) throw new BadRequestException('Missing organization context');
    const orgId = actor.orgId;
    this.ensureOrgScope(actor, orgId);
    await this.validateOrg(orgId);

    const model = await this.costCodes(orgId);
    const filter = this.buildFilter(orgId, query);
    const sort = { code: 1, _id: 1 };

    if (query.noPagination) {
      return model.find(filter).sort(sort).lean();
    }

    const page = Math.max(1, query.page || 1);
    const limit = Math.min(Math.max(query.limit || 20, 1), 200);
    const skip = (page - 1) * limit;

    const [data, total] = await Promise.all([
      model.find(filter).sort(sort).skip(skip).limit(limit).lean(),
      model.countDocuments(filter),
    ]);

    return { data, total, page, limit };
  }

  async counts(actor: ActorContext, query: ListCostCodesQueryDto) {
    this.ensureRole(actor);
    if (!actor.orgId) throw new BadRequestException('Missing organization context');
    const orgId = actor.orgId;
    this.ensureOrgScope(actor, orgId);
    await this.validateOrg(orgId);

    const model = await this.costCodes(orgId);
    const filter = this.buildFilter(orgId, query);
    const total = await model.countDocuments(filter);
    const active = await model.countDocuments({ ...filter, active: true });
    const inactive = await model.countDocuments({ ...filter, active: false });
    const used = await model.countDocuments({ ...filter, isUsed: true });
    return { total, active, inactive, used };
  }

  async getById(actor: ActorContext, id: string) {
    this.ensureRole(actor);
    if (!actor.orgId) throw new BadRequestException('Missing organization context');
    const orgId = actor.orgId;
    this.ensureOrgScope(actor, orgId);
    await this.validateOrg(orgId);

    const model = await this.costCodes(orgId);
    const code = await model.findOne({ _id: id, orgId }).lean();
    if (!code) throw new NotFoundException('Cost code not found');
    return code;
  }

  async create(actor: ActorContext, dto: CreateCostCodeDto) {
    this.ensureRole(actor);
    if (!actor.orgId) throw new BadRequestException('Missing organization context');
    const orgId = actor.orgId;
    this.ensureOrgScope(actor, orgId);
    await this.validateOrg(orgId);

    const model = await this.costCodes(orgId);
    const { category, code, description } = this.normalizeCostCode(dto);
    if (!category || !code || !description) {
      throw new BadRequestException('category, code, and description are required');
    }

    try {
      const created = await model.create({
        orgId,
        category,
        code,
        description,
        active: false,
        isUsed: false,
      });

      await this.audit.logMutation({
        action: 'created',
        entity: 'cost_code',
        entityId: created.id,
        orgId,
        userId: actor.userId,
        metadata: { code },
      });

      return created.toObject ? created.toObject() : created;
    } catch (err: any) {
      if (err?.code === 11000) {
        throw new ConflictException('Duplicate cost code for this organization');
      }
      throw err;
    }
  }

  async update(actor: ActorContext, id: string, dto: UpdateCostCodeDto) {
    this.ensureRole(actor);
    if (!actor.orgId) throw new BadRequestException('Missing organization context');
    const orgId = actor.orgId;
    this.ensureOrgScope(actor, orgId);
    await this.validateOrg(orgId);

    const model = await this.costCodes(orgId);
    const code = await model.findOne({ _id: id, orgId });
    if (!code) throw new NotFoundException('Cost code not found');

    const updates = this.normalizeCostCode(dto);
    if (dto.category !== undefined) (code as any).category = updates.category;
    if (dto.code !== undefined) (code as any).code = updates.code;
    if (dto.description !== undefined) (code as any).description = updates.description;

    try {
      await code.save();
    } catch (err: any) {
      if (err?.code === 11000) {
        throw new ConflictException('Duplicate cost code for this organization');
      }
      throw err;
    }

    await this.audit.logMutation({
      action: 'updated',
      entity: 'cost_code',
      entityId: code.id,
      orgId,
      userId: actor.userId,
      metadata: { code: code.code },
    });

    return code.toObject ? code.toObject() : code;
  }

  async toggleActive(actor: ActorContext, id: string, dto: ToggleCostCodeDto) {
    this.ensureRole(actor);
    if (!actor.orgId) throw new BadRequestException('Missing organization context');
    const orgId = actor.orgId;
    this.ensureOrgScope(actor, orgId);
    await this.validateOrg(orgId);

    const model = await this.costCodes(orgId);
    const code = await model.findOne({ _id: id, orgId });
    if (!code) throw new NotFoundException('Cost code not found');

    (code as any).active = dto.active;
    (code as any).deactivatedAt = dto.active ? null : new Date();
    await code.save();

    await this.audit.logMutation({
      action: dto.active ? 'activated' : 'deactivated',
      entity: 'cost_code',
      entityId: code.id,
      orgId,
      userId: actor.userId,
      metadata: { code: code.code },
    });

    return code.toObject ? code.toObject() : code;
  }

  async remove(actor: ActorContext, id: string) {
    this.ensureRole(actor);
    if (!actor.orgId) throw new BadRequestException('Missing organization context');
    const orgId = actor.orgId;
    this.ensureOrgScope(actor, orgId);
    await this.validateOrg(orgId);

    const model = await this.costCodes(orgId);
    const code = await model.findOne({ _id: id, orgId });
    if (!code) throw new NotFoundException('Cost code not found');

    if ((code as any).isUsed) {
      (code as any).active = false;
      (code as any).deactivatedAt = new Date();
      await code.save();
      await this.audit.logMutation({
        action: 'deactivated',
        entity: 'cost_code',
        entityId: code.id,
        orgId,
        userId: actor.userId,
        metadata: { code: code.code, reason: 'is_used' },
      });
      return code.toObject ? code.toObject() : code;
    }

    await model.deleteOne({ _id: id, orgId });
    await this.audit.logMutation({
      action: 'deleted',
      entity: 'cost_code',
      entityId: code.id,
      orgId,
      userId: actor.userId,
      metadata: { code: code.code },
    });
    return { deleted: true };
  }

  async bulkUpsert(actor: ActorContext, dto: BulkCostCodesDto) {
    this.ensureRole(actor);
    if (!actor.orgId) throw new BadRequestException('Missing organization context');
    const orgId = actor.orgId;
    this.ensureOrgScope(actor, orgId);
    await this.validateOrg(orgId);

    const normalized = this.normalizeCostCodes(dto.codes || []);
    if (!normalized.length) {
      throw new BadRequestException('No valid cost codes supplied');
    }

    const model = await this.costCodes(orgId);
    const ops = normalized.map((item) => ({
      updateOne: {
        filter: { orgId, code: item.code },
        update: {
          $set: {
            category: item.category,
            description: item.description,
            active: true,
          },
          $unset: { deactivatedAt: '' },
        },
        upsert: true,
      },
    }));

    const result = await model.bulkWrite(ops, { ordered: false });

    await this.audit.logMutation({
      action: 'bulk_upsert',
      entity: 'cost_code',
      orgId,
      userId: actor.userId,
      metadata: {
        upserted: result.upsertedCount || 0,
        modified: result.modifiedCount || 0,
      },
    });

    return {
      upserted: result.upsertedCount || 0,
      modified: result.modifiedCount || 0,
    };
  }

  async analyzeImport(actor: ActorContext) {
    this.ensureRole(actor);
    if (!actor.orgId) throw new BadRequestException('Missing organization context');
    const orgId = actor.orgId;
    this.ensureOrgScope(actor, orgId);
    await this.validateOrg(orgId);
    return { guessedMapping: {} };
  }

  async buildTemplate(actor: ActorContext) {
    this.ensureRole(actor);
    if (!actor.orgId) throw new BadRequestException('Missing organization context');
    const orgId = actor.orgId;
    this.ensureOrgScope(actor, orgId);
    await this.validateOrg(orgId);

    const workbook = new ExcelJS.Workbook();
    const sheet = workbook.addWorksheet('Cost Codes');
    sheet.addRow(['Category', 'Code', 'Description']);
    sheet.addRow(['Logistics', '1010', 'Mobilize crew and equipment to site']);
    sheet.addRow(['Erection', '4010', 'Set base plates & grout verification']);
    sheet.addRow(['Welding', '6010', 'Field weld column splices']);
    const buffer = await workbook.xlsx.writeBuffer();
    return Buffer.isBuffer(buffer) ? buffer : Buffer.from(buffer as ArrayBuffer);
  }

  private async parseWorkbook(orgId: string, buffer: Buffer) {
    if (!this.ai.isEnabled()) {
      throw new ServiceUnavailableException('AI is not enabled.');
    }

    const workbook = new ExcelJS.Workbook();
    await workbook.xlsx.load(buffer);

    const sheets = workbook.worksheets.map((sheet) => {
      const rows: string[][] = [];
      const maxRows = Math.min(sheet.rowCount || 0, MAX_IMPORT_ROWS);
      for (let i = 1; i <= maxRows; i += 1) {
        const row = sheet.getRow(i);
        const values = row.values as any[];
        rows.push(values.slice(1).map((val) => this.sanitizeText(val)));
      }
      return { name: sheet.name, rows };
    });

    const workbookJson = { sheets };
    const extracted = await this.ai.extractCostCodesFromWorkbook({ orgId, workbook: workbookJson });
    const normalized = this.normalizeCostCodes(extracted || [], true);
    if (!normalized.length) {
      throw new BadRequestException('No cost codes detected in the import file');
    }
    return normalized;
  }

  async startImport(actor: ActorContext, file?: { buffer: Buffer }) {
    this.ensureRole(actor);
    if (!actor.orgId) throw new BadRequestException('Missing organization context');
    const orgId = actor.orgId;
    this.ensureOrgScope(actor, orgId);
    await this.validateOrg(orgId);

    if (!this.ai.isEnabled()) {
      throw new ServiceUnavailableException('AI is not enabled.');
    }

    if (!file || !file.buffer) {
      throw new BadRequestException('No file provided');
    }

    const jobs = await this.importJobs(orgId);
    const job = await jobs.create({
      orgId,
      status: 'queued',
      fileName: (file as any).originalname || null,
      fileMime: (file as any).mimetype || null,
      fileSize: (file as any).size || null,
      fileBase64: file.buffer.toString('base64'),
    });

    const forceInline = process.env.COST_CODE_IMPORT_INLINE !== '0';

    if (forceInline) {
      try {
        const preview = await this.parseWorkbook(orgId, file.buffer);
        (job as any).preview = preview;
        (job as any).status = 'preview';
        (job as any).fileBase64 = null;
        await job.save();
      } catch (err: any) {
        (job as any).status = 'failed';
        (job as any).errorMessage = err?.message || 'Import failed';
        (job as any).fileBase64 = null;
        await job.save();
        throw err;
      }
    } else {
      try {
        await enqueueCostCodeImport({ orgId, jobId: job.id });
      } catch (err: any) {
        (job as any).status = 'failed';
        (job as any).errorMessage = err?.message || 'Import queue unavailable';
        (job as any).fileBase64 = null;
        await job.save();
        throw new ServiceUnavailableException('Import queue unavailable');
      }
    }

    await this.audit.logMutation({
      action: forceInline ? 'preview' : 'queued',
      entity: 'cost_code_import',
      entityId: job.id,
      orgId,
      userId: actor.userId,
      metadata: { count: (job as any).preview?.length || 0, mode: forceInline ? 'inline' : 'queue' },
    });

    return { jobId: job.id };
  }

  async importStatus(actor: ActorContext, jobId: string) {
    this.ensureRole(actor);
    if (!actor.orgId) throw new BadRequestException('Missing organization context');
    const orgId = actor.orgId;
    this.ensureOrgScope(actor, orgId);
    await this.validateOrg(orgId);

    const jobs = await this.importJobs(orgId);
    const job = await jobs.findOne({ _id: jobId, orgId }).lean();
    if (!job) throw new NotFoundException('Import job not found');

    const result: any = { status: job.status as CostCodeImportStatus };
    if (job.status === 'preview') {
      result.preview = job.preview || [];
    }
    if (job.status === 'failed') {
      result.errorMessage = job.errorMessage || 'Import failed';
    }
    return result;
  }

  async commitImport(actor: ActorContext, jobId: string, dto: CostCodeImportCommitDto) {
    this.ensureRole(actor);
    if (!actor.orgId) throw new BadRequestException('Missing organization context');
    const orgId = actor.orgId;
    this.ensureOrgScope(actor, orgId);
    await this.validateOrg(orgId);

    const jobs = await this.importJobs(orgId);
    const job = await jobs.findOne({ _id: jobId, orgId });
    if (!job) throw new NotFoundException('Import job not found');
    if ((job as any).status !== 'preview') {
      throw new BadRequestException(`Cannot commit import in status '${(job as any).status}'.`);
    }

    const model = await this.costCodes(orgId);
    const usedCount = await model.countDocuments({ orgId, isUsed: true });
    if (usedCount > 0) {
      throw new BadRequestException(
        `Import aborted: ${usedCount} cost code${usedCount > 1 ? 's are' : ' is'} already in use.`
      );
    }

    const overrideCodes = Array.isArray(dto?.codes) ? dto.codes : undefined;
    const normalized = this.normalizeCostCodes(overrideCodes || (job as any).preview || [], true);
    if (!normalized.length) {
      throw new BadRequestException('No valid cost codes to import');
    }

    await model.deleteMany({ orgId });
    const toInsert = normalized.map((code) => ({
      orgId,
      category: code.category,
      code: code.code,
      description: code.description,
      active: false,
      isUsed: false,
      importJobId: job.id,
      deactivatedAt: null,
    }));
    const inserted = await model.insertMany(toInsert);

    (job as any).status = 'done';
    await job.save();

    await this.audit.logMutation({
      action: 'commit',
      entity: 'cost_code_import',
      entityId: job.id,
      orgId,
      userId: actor.userId,
      metadata: { inserted: inserted.length },
    });

    return { inserted: inserted.length, updated: 0, muted: 0 };
  }

  async seedPack(actor: ActorContext, dto: SeedCostCodesDto) {
    this.ensureRole(actor);
    if (!actor.orgId) throw new BadRequestException('Missing organization context');
    const orgId = actor.orgId;
    this.ensureOrgScope(actor, orgId);
    await this.validateOrg(orgId);

    const pack = (dto?.pack || 'steel-field').trim();
    if (pack !== 'steel-field') {
      throw new BadRequestException('Unknown seed pack.');
    }

    const model = await this.costCodes(orgId);
    const usedCount = await model.countDocuments({ orgId, isUsed: true });
    if (usedCount > 0) {
      throw new BadRequestException(
        `Seed aborted: ${usedCount} cost code${usedCount > 1 ? 's are' : ' is'} already in use.`
      );
    }

    const shouldReplace = dto?.replace !== false;
    if (shouldReplace) {
      await model.deleteMany({ orgId });
    } else {
      const existingCount = await model.countDocuments({ orgId });
      if (existingCount > 0) {
        throw new BadRequestException('Seed aborted: cost codes already exist for this organization.');
      }
    }

    const toInsert = STEEL_FIELD_PACK.map((code) => ({
      orgId,
      category: code.category,
      code: code.code,
      description: code.description,
      active: code.active ?? false,
      isUsed: code.isUsed ?? false,
      deactivatedAt: null,
      importJobId: null,
    }));

    const inserted = await model.insertMany(toInsert);

    await this.audit.logMutation({
      action: 'seeded',
      entity: 'cost_code',
      orgId,
      userId: actor.userId,
      metadata: { pack, inserted: inserted.length, replace: shouldReplace },
    });

    return { inserted: inserted.length };
  }
}
