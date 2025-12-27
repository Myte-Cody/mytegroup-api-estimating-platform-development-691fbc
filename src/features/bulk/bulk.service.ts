import { BadRequestException, ForbiddenException, GoneException, Injectable } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model, Schema } from 'mongoose';
import * as bcrypt from 'bcryptjs';
import { Readable } from 'stream';
import { Express } from 'express';
import { Role } from '../../common/roles';
import { AuditLogService } from '../../common/services/audit-log.service';
import { TenantConnectionService } from '../../common/tenancy/tenant-connection.service';
import { STRONG_PASSWORD_REGEX } from '../auth/dto/password-rules';
import { Contact, ContactSchema } from '../contacts/schemas/contact.schema';
import { Office, OfficeSchema } from '../offices/schemas/office.schema';
import { Project, ProjectSchema } from '../projects/schemas/project.schema';
import { User, UserSchema } from '../users/schemas/user.schema';

export type BulkEntityType = 'users' | 'contacts' | 'projects' | 'offices';

type ActorContext = { orgId?: string; role?: string; roles?: string[]; id?: string };

type ImportOptions = {
  entityType: BulkEntityType;
  orgId?: string;
  actor: ActorContext;
  dryRun?: boolean;
  records?: any[];
  file?: Express.Multer.File;
  filename?: string;
  formatHint?: 'csv' | 'json';
};

type ExportOptions = {
  entityType: BulkEntityType;
  orgId?: string;
  actor: ActorContext;
  format?: 'csv' | 'json';
  includeArchived?: boolean;
};

type RowError = { row: number; field?: string; message: string };

export type ImportResult = {
  entityType: BulkEntityType;
  dryRun: boolean;
  processed: number;
  created: number;
  updated: number;
  errors: RowError[];
};

export type ExportResult =
  | { entityType: BulkEntityType; format: 'json'; count: number; data: any[] }
  | { entityType: BulkEntityType; format: 'csv'; count: number; filename: string; stream: Readable };

type EntityConfig = {
  modelName: string;
  schema: Schema;
  defaultModel: Model<any>;
  orgField: string;
  uniqueField: string;
  allowedFields: string[];
  requiredFields: string[];
  arrayFields?: string[];
  piiFields?: string[];
  complianceFields?: string[];
  exportFields: string[];
};

const MAX_ROWS = 1000;

@Injectable()
export class BulkService {
  constructor(
    private readonly audit: AuditLogService,
    private readonly tenants: TenantConnectionService,
    @InjectModel('User') private readonly userModel: Model<User>,
    @InjectModel('Contact') private readonly contactModel: Model<Contact>,
    @InjectModel('Project') private readonly projectModel: Model<Project>,
    @InjectModel('Office') private readonly officeModel: Model<Office>
  ) {}

  private readonly configs: Record<BulkEntityType, EntityConfig> = {
    users: {
      modelName: 'User',
      schema: UserSchema,
      defaultModel: this.userModel,
      orgField: 'orgId',
      uniqueField: 'email',
      allowedFields: [
        'username',
        'email',
        'password',
        'role',
        'roles',
        'isEmailVerified',
        'isOrgOwner',
        'archivedAt',
        'piiStripped',
        'legalHold',
      ],
      requiredFields: ['username', 'email'],
      arrayFields: ['roles'],
      piiFields: ['email'],
      complianceFields: ['archivedAt', 'piiStripped', 'legalHold'],
      exportFields: [
        'username',
        'email',
        'role',
        'roles',
        'isOrgOwner',
        'isEmailVerified',
        'archivedAt',
        'piiStripped',
        'legalHold',
      ],
    },
    contacts: {
      modelName: 'Contact',
      schema: ContactSchema,
      defaultModel: this.contactModel,
      orgField: 'orgId',
      uniqueField: 'email',
      allowedFields: [
        'name',
        'email',
        'phone',
        'company',
        'roles',
        'tags',
        'notes',
        'archivedAt',
        'piiStripped',
        'legalHold',
      ],
      requiredFields: ['name', 'email'],
      arrayFields: ['roles', 'tags'],
      piiFields: ['email', 'phone', 'company', 'notes'],
      complianceFields: ['archivedAt', 'piiStripped', 'legalHold'],
      exportFields: [
        'name',
        'email',
        'phone',
        'company',
        'roles',
        'tags',
        'notes',
        'archivedAt',
        'piiStripped',
        'legalHold',
      ],
    },
    projects: {
      modelName: 'Project',
      schema: ProjectSchema,
      defaultModel: this.projectModel,
      orgField: 'orgId',
      uniqueField: 'name',
      allowedFields: ['name', 'description', 'officeId', 'archivedAt', 'piiStripped', 'legalHold'],
      requiredFields: ['name'],
      arrayFields: [],
      piiFields: [],
      complianceFields: ['archivedAt', 'piiStripped', 'legalHold'],
      exportFields: ['name', 'description', 'officeId', 'archivedAt', 'piiStripped', 'legalHold'],
    },
    offices: {
      modelName: 'Office',
      schema: OfficeSchema,
      defaultModel: this.officeModel,
      orgField: 'orgId',
      uniqueField: 'name',
      allowedFields: ['name', 'address', 'archivedAt', 'piiStripped', 'legalHold'],
      requiredFields: ['name'],
      arrayFields: [],
      piiFields: [],
      complianceFields: ['archivedAt', 'piiStripped', 'legalHold'],
      exportFields: ['name', 'address', 'archivedAt', 'piiStripped', 'legalHold'],
    },
  };

  private configFor(entityType: BulkEntityType) {
    const config = this.configs[entityType];
    if (!config) throw new BadRequestException(`Unsupported entity type: ${entityType}`);
    return config;
  }

  private collectRoles(actor: ActorContext): Role[] {
    const roles = [
      actor.role,
      ...(Array.isArray(actor.roles) ? actor.roles : []),
    ].filter(Boolean) as Role[];
    return Array.from(new Set(roles));
  }

  private assertPrivileged(actorRoles: Role[]) {
    const allowed = [
      Role.SuperAdmin,
      Role.PlatformAdmin,
      Role.OrgOwner,
      Role.OrgAdmin,
      Role.Admin,
      Role.Compliance,
      Role.ComplianceOfficer,
    ];
    const hasAllowed = actorRoles.some((role) => allowed.includes(role));
    if (!hasAllowed) {
      throw new ForbiddenException('Insufficient role for bulk import/export');
    }
  }

  private canSetCompliance(actorRoles: Role[]) {
    return actorRoles.some((role) =>
      [
        Role.SuperAdmin,
        Role.PlatformAdmin,
        Role.Compliance,
        Role.ComplianceOfficer,
        Role.Admin,
        Role.OrgOwner,
        Role.OrgAdmin,
      ].includes(role)
    );
  }

  private resolveOrgId(requestedOrgId: string | undefined, actor: ActorContext) {
    const actorRoles = this.collectRoles(actor);
    const isSuper = actorRoles.includes(Role.SuperAdmin);
    if (requestedOrgId && isSuper) return requestedOrgId;
    if (actor.orgId) return actor.orgId;
    throw new ForbiddenException('Missing organization context');
  }

  private parseBoolean(val: any) {
    if (val === undefined || val === null) return undefined;
    if (typeof val === 'boolean') return val;
    if (typeof val === 'number') return val !== 0;
    if (typeof val === 'string') {
      const lowered = val.trim().toLowerCase();
      if (lowered === 'true' || lowered === '1' || lowered === 'yes') return true;
      if (lowered === 'false' || lowered === '0' || lowered === 'no') return false;
    }
    return undefined;
  }

  private parseDate(val: any): Date | null | undefined {
    if (val === undefined || val === null || val === '') return undefined;
    const date = val instanceof Date ? val : new Date(val);
    if (Number.isNaN(date.getTime())) return undefined;
    return date;
  }

  private normalizeArray(val: any): string[] | undefined {
    if (val === undefined || val === null) return undefined;
    if (Array.isArray(val)) {
      return val
        .map((v) => (typeof v === 'string' ? v.trim() : String(v)))
        .filter((v) => v.length > 0);
    }
    if (typeof val === 'string') {
      return val
        .split(/[;,]/)
        .map((v) => v.trim())
        .filter((v) => v.length > 0);
    }
    return undefined;
  }

  private normalizeValue(key: string, value: any) {
    if (typeof value === 'string') {
      const trimmed = value.trim();
      if (trimmed === '') return undefined;
      if (['email'].includes(key)) return trimmed.toLowerCase();
      return trimmed;
    }
    return value;
  }

  private detectFormat(file?: Express.Multer.File, formatHint?: 'csv' | 'json') {
    if (formatHint) return formatHint;
    if (file?.mimetype?.includes('csv') || file?.originalname?.toLowerCase().endsWith('.csv')) {
      return 'csv';
    }
    return 'json';
  }

  private parseCsv(content: string) {
    const rows: string[][] = [];
    const lines = content.split(/\r?\n/).filter((line) => line.trim().length > 0);
    for (const line of lines) {
      const values: string[] = [];
      let current = '';
      let inQuotes = false;
      for (let i = 0; i < line.length; i++) {
        const char = line[i];
        if (char === '"' && line[i + 1] === '"' && inQuotes) {
          current += '"';
          i++;
          continue;
        }
        if (char === '"') {
          inQuotes = !inQuotes;
          continue;
        }
        if (char === ',' && !inQuotes) {
          values.push(current);
          current = '';
          continue;
        }
        current += char;
      }
      values.push(current);
      rows.push(values.map((v) => v.trim()));
    }
    if (rows.length === 0) return [];
    const headers = rows[0];
    return rows.slice(1).map((cells) => {
      const row: Record<string, any> = {};
      headers.forEach((header, idx) => {
        row[header] = cells[idx] ?? '';
      });
      return row;
    });
  }

  private parsePayload(
    entityType: BulkEntityType,
    options: Pick<ImportOptions, 'file' | 'records' | 'formatHint'>
  ) {
    if (Array.isArray(options.records)) {
      return { records: options.records as Record<string, any>[], format: options.formatHint || 'json' };
    }
    const file = options.file;
    if (!file) throw new BadRequestException('No records supplied for import');
    const format = this.detectFormat(file, options.formatHint);
    const content = file.buffer?.toString('utf-8') ?? '';
    if (!content.trim()) throw new BadRequestException('Uploaded file is empty');
    if (format === 'csv') {
      return { records: this.parseCsv(content), format };
    }
    try {
      const parsed = JSON.parse(content);
      const records = Array.isArray(parsed) ? parsed : parsed.records || parsed.data;
      if (!Array.isArray(records)) throw new Error('Invalid JSON payload');
      return { records, format: 'json' as const };
    } catch (err) {
      throw new BadRequestException(`Failed to parse ${entityType} file: ${(err as Error).message}`);
    }
  }

  private async modelFor(entityType: BulkEntityType, orgId: string) {
    const config = this.configFor(entityType);
    return this.tenants.getModelForOrg(orgId, config.modelName, config.schema, config.defaultModel);
  }

  private validateColumns(config: EntityConfig, row: Record<string, any>, rowIndex: number, actorRoles: Role[]) {
    const errors: RowError[] = [];
    const sanitized: Record<string, any> = {};
    const unknown = Object.keys(row).filter((key) => !config.allowedFields.includes(key));
    if (unknown.length) {
      errors.push({ row: rowIndex, message: `Unexpected columns: ${unknown.join(', ')}` });
    }

    for (const [key, value] of Object.entries(row)) {
      if (!config.allowedFields.includes(key)) continue;
      if (config.arrayFields?.includes(key)) {
        sanitized[key] = this.normalizeArray(value);
        continue;
      }
      if (['archivedAt'].includes(key)) {
        const hasValue = value !== undefined && value !== null && value !== '';
        const dt = this.parseDate(hasValue ? value : undefined);
        if (hasValue && dt === undefined) {
          errors.push({ row: rowIndex, field: key, message: 'archivedAt must be a valid date' });
        } else if (hasValue) {
          sanitized[key] = dt ?? null;
        }
        continue;
      }
      if (['piiStripped', 'legalHold', 'isOrgOwner', 'isEmailVerified'].includes(key)) {
        sanitized[key] = this.parseBoolean(value);
        continue;
      }
      sanitized[key] = this.normalizeValue(key, value);
    }

    const hasCompliance = this.canSetCompliance(actorRoles);
    for (const field of config.complianceFields || []) {
      if (sanitized[field] !== undefined && !hasCompliance) {
        errors.push({ row: rowIndex, field, message: 'Insufficient role to set compliance fields' });
      }
    }

    return { errors, sanitized };
  }

  private normalizeRoles(role?: any, roles?: any): Role[] {
    const fromArray = this.normalizeArray(roles) || [];
    const merged = [...fromArray];
    if (role) merged.unshift(role as string);
    const mapped = merged
      .map((value) =>
        typeof value === 'string' ? ((value.trim().toLowerCase() as unknown) as Role) : ((value as unknown) as Role)
      )
      .filter(Boolean) as Role[];
    return Array.from(new Set(mapped));
  }

  private ensureRoleValues(values: Role[], rowIndex: number) {
    const errors: RowError[] = [];
    const validRoles = new Set(Object.values(Role));
    for (const value of values) {
      if (!validRoles.has(value)) {
        errors.push({ row: rowIndex, field: 'roles', message: `Invalid role: ${value}` });
      }
    }
    return errors;
  }

  private async upsertUser(
    model: Model<User>,
    orgId: string,
    actorRoles: Role[],
    rowIndex: number,
    data: Record<string, any>,
    dryRun: boolean
  ) {
    const errors: RowError[] = [];
    if (!data.email) {
      errors.push({ row: rowIndex, field: 'email', message: 'email is required for upsert' });
      return { errors, created: 0, updated: 0 };
    }
    if (!STRONG_PASSWORD_REGEX.test(String(data.password || data.newPassword || data.passwordHash || ''))) {
      if (!data.password) {
        // allow updates without password when user already exists
      } else {
        errors.push({ row: rowIndex, field: 'password', message: 'Password does not meet policy' });
      }
    }

    const roles = this.normalizeRoles(data.role, data.roles);
    errors.push(...this.ensureRoleValues(roles, rowIndex));
    if (roles.includes(Role.SuperAdmin) && !actorRoles.includes(Role.SuperAdmin)) {
      errors.push({ row: rowIndex, field: 'roles', message: 'Only superadmins can assign superadmin role' });
      return { errors, created: 0, updated: 0 };
    }
    const existing = await model.findOne({ email: data.email });
    if (!existing && !data.password) {
      errors.push({ row: rowIndex, field: 'password', message: 'password is required for new users' });
      return { errors, created: 0, updated: 0 };
    }
    if (!existing && !data.username) {
      errors.push({ row: rowIndex, field: 'username', message: 'username is required for new users' });
      return { errors, created: 0, updated: 0 };
    }
    if (existing?.legalHold) {
      errors.push({ row: rowIndex, message: 'User is on legal hold and cannot be modified' });
      return { errors, created: 0, updated: 0 };
    }
    if (errors.length) {
      return { errors, created: 0, updated: 0 };
    }

    const roleToPersist = roles[0] || existing?.role || Role.User;
    const payload: any = {
      username: data.username ?? existing?.username,
      email: data.email,
      orgId,
      roles: roles.length ? roles : existing?.roles || [roleToPersist],
      role: roleToPersist,
      isOrgOwner: data.isOrgOwner ?? existing?.isOrgOwner ?? roles.includes(Role.OrgOwner),
      isEmailVerified: data.isEmailVerified ?? existing?.isEmailVerified ?? false,
      piiStripped: data.piiStripped ?? existing?.piiStripped ?? false,
      legalHold: data.legalHold ?? existing?.legalHold ?? false,
      archivedAt: data.archivedAt ?? existing?.archivedAt ?? null,
    };

    if (dryRun) {
      return { errors: [], created: existing ? 0 : 1, updated: existing ? 1 : 0 };
    }

    if (data.password) {
      payload.passwordHash = await bcrypt.hash(data.password, 10);
    }

    if (!existing) {
      await model.create(payload);
      return { errors: [], created: 1, updated: 0 };
    }
    await model.updateOne({ _id: existing._id }, payload);
    return { errors: [], created: 0, updated: 1 };
  }

  private async upsertContact(
    model: Model<Contact>,
    orgId: string,
    rowIndex: number,
    data: Record<string, any>,
    dryRun: boolean
  ) {
    const errors: RowError[] = [];
    if (!data.email) {
      errors.push({ row: rowIndex, field: 'email', message: 'email is required for contact upsert' });
      return { errors, created: 0, updated: 0 };
    }
    const existing = await model.findOne({ orgId, email: data.email });
    if (existing?.legalHold) {
      errors.push({ row: rowIndex, message: 'Contact is on legal hold and cannot be modified' });
      return { errors, created: 0, updated: 0 };
    }
    if (!existing && !data.name) {
      errors.push({ row: rowIndex, field: 'name', message: 'name is required for new contacts' });
      return { errors, created: 0, updated: 0 };
    }
    const roles = this.normalizeArray(data.roles) || existing?.roles || [];
    const tags = this.normalizeArray(data.tags) || existing?.tags || [];
    const payload: any = {
      orgId,
      name: data.name ?? existing?.name,
      email: data.email,
      phone: data.phone ?? existing?.phone ?? null,
      company: data.company ?? existing?.company ?? null,
      roles,
      tags,
      notes: data.notes ?? existing?.notes ?? null,
      archivedAt: data.archivedAt ?? existing?.archivedAt ?? null,
      piiStripped: data.piiStripped ?? existing?.piiStripped ?? false,
      legalHold: data.legalHold ?? existing?.legalHold ?? false,
    };
    if (dryRun) {
      return { errors, created: existing ? 0 : 1, updated: existing ? 1 : 0 };
    }
    if (!existing) {
      await model.create(payload);
      return { errors, created: 1, updated: 0 };
    }
    await model.updateOne({ _id: existing._id }, payload);
    return { errors, created: 0, updated: 1 };
  }

  private async upsertProject(
    model: Model<Project>,
    orgId: string,
    rowIndex: number,
    data: Record<string, any>,
    dryRun: boolean
  ) {
    const errors: RowError[] = [];
    if (!data.name) {
      errors.push({ row: rowIndex, field: 'name', message: 'name is required for projects' });
      return { errors, created: 0, updated: 0 };
    }
    const existing = await model.findOne({ orgId, name: data.name });
    if (existing?.legalHold) {
      errors.push({ row: rowIndex, message: 'Project is on legal hold and cannot be modified' });
      return { errors, created: 0, updated: 0 };
    }
    const payload: any = {
      orgId,
      name: data.name,
      description: data.description ?? existing?.description ?? null,
      officeId: data.officeId ?? existing?.officeId ?? null,
      archivedAt: data.archivedAt ?? existing?.archivedAt ?? null,
      piiStripped: data.piiStripped ?? existing?.piiStripped ?? false,
      legalHold: data.legalHold ?? existing?.legalHold ?? false,
    };
    if (dryRun) return { errors, created: existing ? 0 : 1, updated: existing ? 1 : 0 };
    if (!existing) {
      await model.create(payload);
      return { errors, created: 1, updated: 0 };
    }
    await model.updateOne({ _id: existing._id }, payload);
    return { errors, created: 0, updated: 1 };
  }

  private async upsertOffice(
    model: Model<Office>,
    orgId: string,
    rowIndex: number,
    data: Record<string, any>,
    dryRun: boolean
  ) {
    const errors: RowError[] = [];
    if (!data.name) {
      errors.push({ row: rowIndex, field: 'name', message: 'name is required for offices' });
      return { errors, created: 0, updated: 0 };
    }
    const existing = await model.findOne({ orgId, name: data.name });
    if (existing?.legalHold) {
      errors.push({ row: rowIndex, message: 'Office is on legal hold and cannot be modified' });
      return { errors, created: 0, updated: 0 };
    }
    const payload: any = {
      orgId,
      name: data.name,
      address: data.address ?? existing?.address ?? null,
      archivedAt: data.archivedAt ?? existing?.archivedAt ?? null,
      piiStripped: data.piiStripped ?? existing?.piiStripped ?? false,
      legalHold: data.legalHold ?? existing?.legalHold ?? false,
    };
    if (dryRun) return { errors, created: existing ? 0 : 1, updated: existing ? 1 : 0 };
    if (!existing) {
      await model.create(payload);
      return { errors, created: 1, updated: 0 };
    }
    await model.updateOne({ _id: existing._id }, payload);
    return { errors, created: 0, updated: 1 };
  }

  async import(entityType: BulkEntityType, options: ImportOptions): Promise<ImportResult> {
    if (entityType === 'contacts') {
      throw new GoneException('Bulk contacts import is deprecated; use /people/import/v1/* and /companies/import/v1/*');
    }
    const config = this.configFor(entityType);
    const actorRoles = this.collectRoles(options.actor);
    this.assertPrivileged(actorRoles);
    const orgId = this.resolveOrgId(options.orgId, options.actor);

    const { records, format } = this.parsePayload(entityType, {
      file: options.file,
      records: options.records,
      formatHint: options.formatHint,
    });

    if (!records.length) throw new BadRequestException('No rows found in payload');
    if (records.length > MAX_ROWS) {
      throw new BadRequestException(`Row limit exceeded. Maximum ${MAX_ROWS} rows per request.`);
    }

    const model = await this.modelFor(entityType, orgId);
    let created = 0;
    let updated = 0;
    const errors: RowError[] = [];

    for (let idx = 0; idx < records.length; idx++) {
      const rowIndex = idx + 1;
      const row = records[idx] || {};
      const { errors: columnErrors, sanitized } = this.validateColumns(config, row, rowIndex, actorRoles);
      if (columnErrors.length) {
        errors.push(...columnErrors);
        continue;
      }
      const payload = { ...sanitized };
      delete payload[config.orgField];

      let result: { errors: RowError[]; created: number; updated: number } = {
        errors: [],
        created: 0,
        updated: 0,
      };

      if (entityType === 'users') {
        result = await this.upsertUser(model as Model<User>, orgId, actorRoles, rowIndex, payload, !!options.dryRun);
      } else if (entityType === 'projects') {
        result = await this.upsertProject(model as Model<Project>, orgId, rowIndex, payload, !!options.dryRun);
      } else if (entityType === 'offices') {
        result = await this.upsertOffice(model as Model<Office>, orgId, rowIndex, payload, !!options.dryRun);
      }

      errors.push(...result.errors);
      created += result.created;
      updated += result.updated;
    }

    await this.audit.logMutation({
      action: 'import',
      entity: 'bulk',
      orgId,
      userId: options.actor.id,
      metadata: {
        entityType,
        dryRun: !!options.dryRun,
        processed: records.length,
        created,
        updated,
        errorCount: errors.length,
        filename: options.filename,
        format,
      },
    });

    return {
      entityType,
      dryRun: !!options.dryRun,
      processed: records.length,
      created,
      updated,
      errors,
    };
  }

  private sanitizeForExport(config: EntityConfig, record: any) {
    const cleaned: Record<string, any> = {};
    for (const field of config.exportFields) {
      cleaned[field] = record[field] ?? null;
    }
    if (record.legalHold) {
      const complianceFields = new Set<string>([
        ...(config.complianceFields || []),
        'archivedAt',
        'piiStripped',
        'legalHold',
      ]);
      for (const field of config.exportFields) {
        if (!complianceFields.has(field)) {
          cleaned[field] = null;
        }
      }
      cleaned.redacted = true;
    }
    if (record.piiStripped) {
      for (const field of config.piiFields || []) {
        cleaned[field] = null;
      }
      cleaned.redacted = true;
    }
    if ('archivedAt' in cleaned && cleaned.archivedAt instanceof Date) {
      cleaned.archivedAt = cleaned.archivedAt.toISOString();
    }
    if (Array.isArray(cleaned.roles)) {
      cleaned.roles = Array.from(new Set(cleaned.roles));
    }
    if (Array.isArray(cleaned.tags)) {
      cleaned.tags = Array.from(new Set(cleaned.tags));
    }
    return cleaned;
  }

  private toCsv(fields: string[], rows: any[]) {
    const header = fields.join(',');
    const body = rows
      .map((row) =>
        fields
          .map((field) => {
            const value = row[field];
            if (Array.isArray(value)) return `"${value.join(';')}"`;
            if (value === null || value === undefined) return '';
            const str = typeof value === 'string' ? value : value.toString();
            if (str.includes('"') || str.includes(',') || str.includes('\n')) {
              return `"${str.replace(/"/g, '""')}"`;
            }
            return str;
          })
          .join(',')
      )
      .join('\n');
    return `${header}\n${body}`;
  }

  async export(entityType: BulkEntityType, options: ExportOptions): Promise<ExportResult> {
    if (entityType === 'contacts') {
      throw new GoneException('Bulk contacts export is deprecated; use /persons and /companies APIs');
    }
    const config = this.configFor(entityType);
    const actorRoles = this.collectRoles(options.actor);
    this.assertPrivileged(actorRoles);
    const orgId = this.resolveOrgId(options.orgId, options.actor);
    const model = await this.modelFor(entityType, orgId);
    const filter: any = { [config.orgField]: orgId };
    if (!options.includeArchived) {
      filter.archivedAt = null;
    }
    const docs = await model.find(filter).lean();
    const sanitized = docs.map((doc: any) => this.sanitizeForExport(config, doc));

    const format = options.format || 'json';
    const filename = `${entityType}-export-${Date.now()}.${format}`;

    await this.audit.logMutation({
      action: 'export',
      entity: 'bulk',
      orgId,
      userId: options.actor.id,
      metadata: {
        entityType,
        format,
        includeArchived: !!options.includeArchived,
        count: sanitized.length,
        filename,
      },
    });

    if (format === 'csv') {
      const csv = this.toCsv(config.exportFields, sanitized);
      const stream = Readable.from([csv]);
      return { entityType, format: 'csv', count: sanitized.length, filename, stream };
    }
    return { entityType, format: 'json', count: sanitized.length, data: sanitized };
  }
}
