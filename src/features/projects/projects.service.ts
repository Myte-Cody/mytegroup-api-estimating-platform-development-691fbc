import { BadRequestException, ConflictException, ForbiddenException, Injectable, NotFoundException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { expandRoles, Role } from '../../common/roles';
import { AuditLogService } from '../../common/services/audit-log.service';
import { TenantConnectionService } from '../../common/tenancy/tenant-connection.service';
import { CreateProjectDto } from './dto/create-project.dto';
import { UpdateProjectDto } from './dto/update-project.dto';
import { Project } from './schemas/project.schema';
import { CostCode, CostCodeSchema } from '../cost-codes/schemas/cost-code.schema';
import { Organization } from '../organizations/schemas/organization.schema';
import { Office, OfficeSchema } from '../offices/schemas/office.schema';
import { PersonsService } from '../persons/persons.service';
import { SeatsService } from '../seats/seats.service';

type ActorContext = { userId?: string; orgId?: string; role?: Role };

@Injectable()
export class ProjectsService {
  constructor(
    @InjectModel('Project') private readonly projectModel: Model<Project>,
    @InjectModel('Organization') private readonly orgModel: Model<Organization>,
    @InjectModel('Office') private readonly officeModel: Model<Office>,
    @InjectModel('CostCode') private readonly costCodeModel: Model<CostCode>,
    private readonly audit: AuditLogService,
    private readonly tenants: TenantConnectionService,
    private readonly persons: PersonsService,
    private readonly seats: SeatsService
  ) {}

  private async officeTenantModel(orgId: string) {
    return this.tenants.getModelForOrg<Office>(orgId, 'Office', OfficeSchema, this.officeModel);
  }

  private async costCodes(orgId: string) {
    return this.tenants.getModelForOrg<CostCode>(orgId, 'CostCode', CostCodeSchema, this.costCodeModel);
  }

  private parseOptionalDate(value?: string | null) {
    if (!value) return null;
    const dt = new Date(value);
    if (Number.isNaN(dt.getTime())) return null;
    return dt;
  }

  private parseOptionalNumber(value?: any) {
    if (value === undefined || value === null || value === '') return null;
    const num = Number(value);
    return Number.isFinite(num) ? num : null;
  }

  private ensureRole(actor: ActorContext, allowed: Role[]) {
    if (actor.role === Role.SuperAdmin) return;
    if (!actor.role) throw new ForbiddenException('Insufficient role');
    const effectiveRoles = expandRoles([actor.role]);
    const allowedEffective = allowed.some((role) => effectiveRoles.includes(role));
    if (!allowedEffective) {
      throw new ForbiddenException('Insufficient role');
    }
  }

  private resolveOrgId(requestedOrgId: string | undefined, actor: ActorContext) {
    if (actor.role === Role.SuperAdmin && requestedOrgId) return requestedOrgId;
    if (actor.orgId) return actor.orgId;
    throw new ForbiddenException('Missing organization context');
  }

  private ensureOrgScope(entityOrgId: string, actor: ActorContext) {
    if (actor.role === Role.SuperAdmin) return;
    if (!actor.orgId || actor.orgId !== entityOrgId) {
      throw new ForbiddenException('Cannot access project outside your organization');
    }
  }

  private canViewArchived(actor: ActorContext) {
    const effectiveRoles = expandRoles(actor.role ? [actor.role] : []);
    return [Role.SuperAdmin, Role.PlatformAdmin, Role.Admin, Role.Manager, Role.OrgOwner].some((role) =>
      effectiveRoles.includes(role)
    );
  }

  private ensureNotOnLegalHold(entity: { legalHold?: boolean } | null, action: string) {
    if (entity?.legalHold) {
      throw new ForbiddenException(`Cannot ${action} project while legal hold is active`);
    }
  }

  private toPlain(project: Project) {
    return project.toObject ? project.toObject() : project;
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

  private normalizeBudget(input?: Record<string, any>, existing?: Record<string, any>) {
    if (!input && existing) return existing;
    if (!input && !existing) return null;
    const base = existing || {};
    const hours =
      input && Object.prototype.hasOwnProperty.call(input, 'hours') ? this.parseOptionalNumber(input.hours) : base.hours ?? null;
    const labourRate =
      input && Object.prototype.hasOwnProperty.call(input, 'labourRate')
        ? this.parseOptionalNumber(input.labourRate)
        : base.labourRate ?? null;
    const currency =
      input && Object.prototype.hasOwnProperty.call(input, 'currency')
        ? (typeof input.currency === 'string' ? input.currency.trim() : null)
        : base.currency ?? 'USD';
    const amount = hours !== null && labourRate !== null ? hours * labourRate : base.amount ?? null;
    return { hours, labourRate, currency, amount };
  }

  private normalizeQuantities(input?: Record<string, any>, existing?: Record<string, any>) {
    if (!input && existing) return existing;
    if (!input && !existing) return null;
    const base = existing || {};

    const structuralRaw = input && Object.prototype.hasOwnProperty.call(input, 'structural') ? input.structural : base.structural;
    const miscRaw = input && Object.prototype.hasOwnProperty.call(input, 'miscMetals') ? input.miscMetals : base.miscMetals;
    const metalDeckRaw = input && Object.prototype.hasOwnProperty.call(input, 'metalDeck') ? input.metalDeck : base.metalDeck;
    const cltRaw = input && Object.prototype.hasOwnProperty.call(input, 'cltPanels') ? input.cltPanels : base.cltPanels;
    const glulamRaw = input && Object.prototype.hasOwnProperty.call(input, 'glulam') ? input.glulam : base.glulam;

    return {
      structural: {
        tonnage: this.parseOptionalNumber(structuralRaw?.tonnage),
        pieces: this.parseOptionalNumber(structuralRaw?.pieces),
      },
      miscMetals: {
        tonnage: this.parseOptionalNumber(miscRaw?.tonnage),
        pieces: this.parseOptionalNumber(miscRaw?.pieces),
      },
      metalDeck: {
        pieces: this.parseOptionalNumber(metalDeckRaw?.pieces),
        sqft: this.parseOptionalNumber(metalDeckRaw?.sqft),
      },
      cltPanels: {
        pieces: this.parseOptionalNumber(cltRaw?.pieces),
        sqft: this.parseOptionalNumber(cltRaw?.sqft),
      },
      glulam: {
        volumeM3: this.parseOptionalNumber(glulamRaw?.volumeM3),
        pieces: this.parseOptionalNumber(glulamRaw?.pieces),
      },
    };
  }

  private dedupeList(values?: string[]) {
    return Array.from(
      new Set(
        (values || [])
          .map((value) => String(value || '').trim())
          .filter(Boolean)
      )
    );
  }

  private async ensurePersonAssignable(
    actor: ActorContext,
    orgId: string,
    personId: string,
    allowedTypes: Array<'internal_staff' | 'internal_union'>,
    label: string
  ) {
    const person = await this.persons.getById(actor as any, orgId, personId, false);
    if (!person) throw new NotFoundException(`${label} not found`);
    if (!allowedTypes.includes(person.personType as any)) {
      throw new BadRequestException(`${label} must be ${allowedTypes.join(' or ')}`);
    }
    if (!person.userId) {
      throw new BadRequestException(`${label} must be linked to a user before staffing`);
    }
    return person;
  }

  private async resolveStaffing(
    actor: ActorContext,
    orgId: string,
    current: Record<string, any> | null,
    input: Record<string, any>
  ) {
    const next = {
      projectManagerPersonId:
        input && Object.prototype.hasOwnProperty.call(input, 'projectManagerPersonId')
          ? input.projectManagerPersonId || null
          : current?.projectManagerPersonId || null,
      superintendentPersonId:
        input && Object.prototype.hasOwnProperty.call(input, 'superintendentPersonId')
          ? input.superintendentPersonId || null
          : current?.superintendentPersonId || null,
      foremanPersonIds:
        input && Object.prototype.hasOwnProperty.call(input, 'foremanPersonIds')
          ? this.dedupeList(input.foremanPersonIds)
          : this.dedupeList(current?.foremanPersonIds || []),
    };

    const people: any = { projectManager: null, superintendent: null, foremen: [] };

    if (next.projectManagerPersonId) {
      people.projectManager = await this.ensurePersonAssignable(
        actor,
        orgId,
        next.projectManagerPersonId,
        ['internal_staff'],
        'Project manager'
      );
    }

    if (next.superintendentPersonId) {
      people.superintendent = await this.ensurePersonAssignable(
        actor,
        orgId,
        next.superintendentPersonId,
        ['internal_staff', 'internal_union'],
        'Superintendent'
      );
    }

    if (next.foremanPersonIds.length) {
      const foremen = [];
      for (const id of next.foremanPersonIds) {
        foremen.push(
          await this.ensurePersonAssignable(actor, orgId, id, ['internal_union'], 'Foreman')
        );
      }
      people.foremen = foremen;
    }

    return { next, people };
  }

  private async normalizeCostCodeBudgets(
    orgId: string,
    input: Array<Record<string, any>> | undefined,
    labourRate: number | null,
    fallbackActive = false
  ) {
    if (!Array.isArray(input)) {
      if (!fallbackActive) return null;
      const model = await this.costCodes(orgId);
      const active = await model.find({ orgId, active: true }).lean();
      return active.map((code) => ({
        costCodeId: String((code as any)._id),
        budgetedHours: 0,
        costBudget: 0,
      }));
    }

    const normalized = input
      .map((row) => ({
        costCodeId: row.costCodeId || row.costCodeID || row.id || row._id,
        budgetedHours: this.parseOptionalNumber(row.budgetedHours),
        costBudget: this.parseOptionalNumber(row.costBudget),
      }))
      .filter((row) => !!row.costCodeId);

    const ids = this.dedupeList(normalized.map((row) => row.costCodeId));
    if (ids.length) {
      const model = await this.costCodes(orgId);
      const existing = await model.find({ orgId, _id: { $in: ids } }).select('_id').lean();
      const existingIds = new Set(existing.map((code) => String((code as any)._id)));
      const missing = ids.filter((id) => !existingIds.has(String(id)));
      if (missing.length) {
        throw new BadRequestException('Some cost codes were not found for this organization');
      }
    }

    return normalized.map((row) => {
      const budgetedHours = row.budgetedHours ?? 0;
      const costBudget =
        row.costBudget !== null && row.costBudget !== undefined
          ? row.costBudget
          : labourRate !== null
            ? budgetedHours * labourRate
            : null;
      return {
        costCodeId: String(row.costCodeId),
        budgetedHours,
        costBudget,
      };
    });
  }

  private async syncStaffingAssignments(
    project: Project,
    actor: ActorContext,
    orgId: string,
    input: Record<string, any> | undefined,
    session?: any
  ) {
    if (input === undefined) return;
    const currentStaffing = (project as any).staffing || null;
    const { next, people } = await this.resolveStaffing(actor, orgId, currentStaffing, input);

    const projectId = String((project as any).id || (project as any)._id);
    const assignments = Array.isArray((project as any).seatAssignments) ? (project as any).seatAssignments : [];
    const activeAssignments = assignments.filter((entry: any) => !entry.removedAt);
    const desired = new Map<string, { role: Role; person: any }>();

    if (people.projectManager) {
      const id = String(people.projectManager.id || people.projectManager._id);
      desired.set(`${id}:${Role.PM}`, { role: Role.PM, person: people.projectManager });
    }
    if (people.superintendent) {
      const id = String(people.superintendent.id || people.superintendent._id);
      desired.set(`${id}:${Role.Superintendent}`, { role: Role.Superintendent, person: people.superintendent });
    }
    if (Array.isArray(people.foremen)) {
      people.foremen.forEach((foreman: any) => {
        const id = String(foreman.id || foreman._id);
        desired.set(`${id}:${Role.Foreman}`, { role: Role.Foreman, person: foreman });
      });
    }

    const now = new Date();
    const activeKeys = new Set<string>();

    for (const entry of activeAssignments) {
      const personId = entry.personId ? String(entry.personId) : '';
      const role = entry.role ? String(entry.role) : '';
      const key = personId && role ? `${personId}:${role}` : '';
      if (key && desired.has(key)) {
        activeKeys.add(key);
        continue;
      }

      entry.removedAt = now;
      if (entry.seatId) {
        await this.seats.clearSeatProject(orgId, String(entry.seatId), projectId, session);
      }
    }

    for (const [key, value] of desired.entries()) {
      if (activeKeys.has(key)) continue;
      const person = value.person;
      const personId = String(person.id || person._id);
      const userId = String(person.userId || '');
      if (!userId) {
        throw new BadRequestException('Assigned person must be linked to a user');
      }

      let seat = await this.seats.findActiveSeatForUser(orgId, userId, session);
      if (!seat) {
        seat = await this.seats.allocateSeat(orgId, userId, { role: value.role, session });
      }
      const seatId = String((seat as any).id || (seat as any)._id);
      const seatProjectId = (seat as any).projectId ? String((seat as any).projectId) : null;
      if (seatProjectId && seatProjectId !== projectId) {
        throw new BadRequestException('Seat is already assigned to another project');
      }

      await this.seats.assignSeatToProject(orgId, seatId, projectId, value.role, session);
      assignments.push({
        seatId,
        personId,
        role: value.role,
        assignedAt: now,
        removedAt: null,
      });
    }

    (project as any).staffing = next;
    (project as any).seatAssignments = assignments;
  }

  private async validateOrg(orgId: string) {
    const org = await this.orgModel.findOne({ _id: orgId, archivedAt: null });
    if (!org) throw new NotFoundException('Organization not found or archived');
    if (org.legalHold) {
      throw new ForbiddenException('Organization is under legal hold');
    }
    return org;
  }

  private async validateOffice(orgId: string, officeId?: string | null) {
    if (!officeId) return;
    const offices = await this.officeTenantModel(orgId);
    const office = await offices.findOne({ _id: officeId, orgId, archivedAt: null });
    if (!office) throw new NotFoundException('Office not found or archived for this organization');
  }

  async create(dto: CreateProjectDto, actor: ActorContext) {
    this.ensureRole(actor, [Role.Admin, Role.Manager, Role.OrgOwner]);
    const orgId = this.resolveOrgId(dto.orgId, actor);
    await this.validateOrg(orgId);
    await this.validateOffice(orgId, dto.officeId);

    const name = String(dto.name || '').trim();
    if (!name) throw new BadRequestException('Project name is required');

    const projectCode = dto.projectCode ? String(dto.projectCode).trim() : null;
    const status = dto.status ? String(dto.status).trim() : null;
    const location = dto.location ? String(dto.location).trim() : null;

    const existing = await this.projectModel.findOne({ orgId, name, archivedAt: null });
    if (existing) throw new ConflictException('Project name already exists for this organization');
    if (projectCode) {
      const codeCollision = await this.projectModel.findOne({ orgId, projectCode, archivedAt: null });
      if (codeCollision) throw new ConflictException('Project code already exists for this organization');
    }

    const budget = this.normalizeBudget(dto.budget, null);
    const quantities = this.normalizeQuantities(dto.quantities, null);
    const costCodeBudgets = await this.normalizeCostCodeBudgets(
      orgId,
      dto.costCodeBudgets,
      budget?.labourRate ?? null,
      true
    );

    const project = await this.projectModel.create({
      name,
      description: dto.description ? String(dto.description).trim() : undefined,
      orgId,
      officeId: dto.officeId || null,
      projectCode,
      status,
      location,
      bidDate: this.parseOptionalDate(dto.bidDate),
      awardDate: this.parseOptionalDate(dto.awardDate),
      fabricationStartDate: this.parseOptionalDate(dto.fabricationStartDate),
      fabricationEndDate: this.parseOptionalDate(dto.fabricationEndDate),
      erectionStartDate: this.parseOptionalDate(dto.erectionStartDate),
      erectionEndDate: this.parseOptionalDate(dto.erectionEndDate),
      completionDate: this.parseOptionalDate(dto.completionDate),
      budget,
      quantities,
      costCodeBudgets: costCodeBudgets || [],
    });

    if (dto.staffing !== undefined) {
      await this.syncStaffingAssignments(project, actor, orgId, dto.staffing);
      await project.save();
    }

    await this.audit.log({
      eventType: 'project.created',
      orgId,
      userId: actor.userId,
      entity: 'Project',
      entityId: project.id,
      metadata: {
        name: project.name,
        projectCode: project.projectCode || null,
        status: project.status || null,
        officeId: project.officeId || null,
      },
    });

    return this.toPlain(project);
  }

  async list(actor: ActorContext, orgId: string, includeArchived = false) {
    this.ensureRole(actor, [Role.Admin, Role.Manager, Role.OrgOwner, Role.PM, Role.Viewer]);
    const resolvedOrgId = this.resolveOrgId(orgId, actor);
    if (includeArchived && !this.canViewArchived(actor)) {
      throw new ForbiddenException('Not allowed to include archived projects');
    }
    const filter: Record<string, any> = { orgId: resolvedOrgId };
    if (!includeArchived) filter.archivedAt = null;
    return this.projectModel.find(filter).lean();
  }

  async getById(id: string, actor: ActorContext, includeArchived = false) {
    this.ensureRole(actor, [Role.Admin, Role.Manager, Role.OrgOwner, Role.PM, Role.Viewer]);
    const project = await this.projectModel.findById(id);
    if (!project) throw new NotFoundException('Project not found');
    this.ensureOrgScope(project.orgId, actor);
    if (project.archivedAt && !includeArchived) throw new NotFoundException('Project archived');
    if (project.archivedAt && includeArchived && !this.canViewArchived(actor)) {
      throw new ForbiddenException('Not allowed to view archived projects');
    }
    return this.toPlain(project);
  }

  async update(id: string, dto: UpdateProjectDto, actor: ActorContext) {
    this.ensureRole(actor, [Role.Admin, Role.Manager, Role.OrgOwner]);
    const project = await this.projectModel.findById(id);
    if (!project) throw new NotFoundException('Project not found');
    this.ensureOrgScope(project.orgId, actor);
    await this.validateOrg(project.orgId);
    if (project.archivedAt) throw new NotFoundException('Project archived');
    this.ensureNotOnLegalHold(project, 'update');

    if (dto.name && dto.name !== project.name) {
      const existing = await this.projectModel.findOne({
        orgId: project.orgId,
        name: dto.name,
        archivedAt: null,
        _id: { $ne: id },
      });
      if (existing) throw new ConflictException('Project name already exists for this organization');
    }

    const before = this.toPlain(project);

    if (dto.officeId !== undefined) {
      await this.validateOffice(project.orgId, dto.officeId);
      project.officeId = dto.officeId || null;
    }
    if (dto.name !== undefined) project.name = dto.name;
    if (dto.description !== undefined) project.description = dto.description;

    if (dto.projectCode !== undefined) {
      const nextProjectCode = dto.projectCode ? String(dto.projectCode).trim() : null;
      if (nextProjectCode && nextProjectCode !== project.projectCode) {
        const codeCollision = await this.projectModel.findOne({
          orgId: project.orgId,
          projectCode: nextProjectCode,
          archivedAt: null,
          _id: { $ne: id },
        });
        if (codeCollision) throw new ConflictException('Project code already exists for this organization');
      }
      project.projectCode = nextProjectCode;
    }
    if (dto.status !== undefined) project.status = dto.status ? String(dto.status).trim() : null;
    if (dto.location !== undefined) project.location = dto.location ? String(dto.location).trim() : null;
    if (dto.bidDate !== undefined) project.bidDate = this.parseOptionalDate(dto.bidDate);
    if (dto.awardDate !== undefined) project.awardDate = this.parseOptionalDate(dto.awardDate);
    if (dto.fabricationStartDate !== undefined) {
      project.fabricationStartDate = this.parseOptionalDate(dto.fabricationStartDate);
    }
    if (dto.fabricationEndDate !== undefined) {
      project.fabricationEndDate = this.parseOptionalDate(dto.fabricationEndDate);
    }
    if (dto.erectionStartDate !== undefined) project.erectionStartDate = this.parseOptionalDate(dto.erectionStartDate);
    if (dto.erectionEndDate !== undefined) project.erectionEndDate = this.parseOptionalDate(dto.erectionEndDate);
    if (dto.completionDate !== undefined) project.completionDate = this.parseOptionalDate(dto.completionDate);

    const nextBudget = dto.budget !== undefined ? this.normalizeBudget(dto.budget, project.budget) : project.budget;
    if (dto.budget !== undefined) project.budget = nextBudget;
    if (dto.quantities !== undefined) project.quantities = this.normalizeQuantities(dto.quantities, project.quantities);

    if (dto.costCodeBudgets !== undefined) {
      const normalized = await this.normalizeCostCodeBudgets(
        project.orgId,
        dto.costCodeBudgets,
        nextBudget?.labourRate ?? null,
        false
      );
      project.costCodeBudgets = normalized || [];
    }

    if (dto.staffing !== undefined) {
      await this.syncStaffingAssignments(project, actor, project.orgId, dto.staffing);
    }

    await project.save();
    const after = this.toPlain(project);

    await this.audit.log({
      eventType: 'project.updated',
      orgId: project.orgId,
      userId: actor.userId,
      entity: 'Project',
      entityId: project.id,
      metadata: {
        changes: this.summarizeDiff(before, after, [
          'name',
          'description',
          'officeId',
          'projectCode',
          'status',
          'location',
          'bidDate',
          'awardDate',
          'fabricationStartDate',
          'fabricationEndDate',
          'erectionStartDate',
          'erectionEndDate',
          'completionDate',
          'budget',
          'quantities',
          'staffing',
          'costCodeBudgets',
          'seatAssignments',
        ]),
      },
    });

    return after;
  }

  async archive(id: string, actor: ActorContext) {
    this.ensureRole(actor, [Role.Admin, Role.Manager, Role.OrgOwner]);
    const project = await this.projectModel.findById(id);
    if (!project) throw new NotFoundException('Project not found');
    this.ensureOrgScope(project.orgId, actor);
    await this.validateOrg(project.orgId);
    this.ensureNotOnLegalHold(project, 'archive');

    if (!project.archivedAt) {
      project.archivedAt = new Date();
      await project.save();
    }

    await this.audit.log({
      eventType: 'project.archived',
      orgId: project.orgId,
      userId: actor.userId,
      entity: 'Project',
      entityId: project.id,
      metadata: { archivedAt: project.archivedAt },
    });
    return this.toPlain(project);
  }

  async unarchive(id: string, actor: ActorContext) {
    this.ensureRole(actor, [Role.Admin, Role.Manager, Role.OrgOwner]);
    const project = await this.projectModel.findById(id);
    if (!project) throw new NotFoundException('Project not found');
    this.ensureOrgScope(project.orgId, actor);
    await this.validateOrg(project.orgId);
    this.ensureNotOnLegalHold(project, 'unarchive');

    if (project.archivedAt) {
      const collision = await this.projectModel.findOne({
        orgId: project.orgId,
        name: project.name,
        archivedAt: null,
        _id: { $ne: id },
      });
      if (collision) throw new ConflictException('Project name already exists for this organization');
      project.archivedAt = null;
      await project.save();
    }

    await this.audit.log({
      eventType: 'project.unarchived',
      orgId: project.orgId,
      userId: actor.userId,
      entity: 'Project',
      entityId: project.id,
      metadata: { archivedAt: project.archivedAt },
    });
    return this.toPlain(project);
  }
}
