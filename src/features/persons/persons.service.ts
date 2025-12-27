import { BadRequestException, ConflictException, ForbiddenException, Injectable, NotFoundException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { expandRoles, Role } from '../../common/roles';
import { AuditLogService } from '../../common/services/audit-log.service';
import { TenantConnectionService } from '../../common/tenancy/tenant-connection.service';
import { normalizeEmail, normalizeKey, normalizeKeys, normalizePhoneE164 } from '../../common/utils/normalize.util';
import { Company, CompanySchema } from '../companies/schemas/company.schema';
import { CompanyLocation, CompanyLocationSchema } from '../company-locations/schemas/company-location.schema';
import { Organization } from '../organizations/schemas/organization.schema';
import { OrgTaxonomyService } from '../org-taxonomy/org-taxonomy.service';
import { Office, OfficeSchema } from '../offices/schemas/office.schema';
import { CreatePersonDto } from './dto/create-person.dto';
import { ListPersonsQueryDto } from './dto/list-persons.dto';
import { UpdatePersonDto } from './dto/update-person.dto';
import { Person, PersonEmail, PersonPhone, PersonSchema, PersonType } from './schemas/person.schema';

type ActorContext = { userId?: string; orgId?: string; role?: Role };

@Injectable()
export class PersonsService {
  constructor(
    private readonly audit: AuditLogService,
    private readonly tenants: TenantConnectionService,
    private readonly taxonomy: OrgTaxonomyService,
    @InjectModel('Person') private readonly personModel: Model<Person>,
    @InjectModel('Company') private readonly companyModel: Model<Company>,
    @InjectModel('CompanyLocation') private readonly companyLocationModel: Model<CompanyLocation>,
    @InjectModel('Office') private readonly officeModel: Model<Office>,
    @InjectModel('Organization') private readonly orgModel: Model<Organization>
  ) {}

  private ensureRole(actor: ActorContext, allowed: Role[]) {
    const role = actor.role || Role.User;
    const effective = expandRoles([role]);
    const ok = allowed.some((r) => effective.includes(r));
    if (!ok) throw new ForbiddenException('Insufficient role');
  }

  private ensureOrgScope(orgId: string, actor: ActorContext) {
    if (actor.role === Role.SuperAdmin || actor.role === Role.PlatformAdmin) return;
    if (!actor.orgId || actor.orgId !== orgId) {
      throw new ForbiddenException('Cannot access people outside your organization');
    }
  }

  private canViewArchived(actor: ActorContext) {
    const effective = expandRoles(actor.role ? [actor.role] : []);
    return [Role.SuperAdmin, Role.PlatformAdmin, Role.OrgOwner, Role.OrgAdmin, Role.Admin].some((r) =>
      effective.includes(r)
    );
  }

  private ensureNotOnLegalHold(entity: { legalHold?: boolean } | null, action: string) {
    if (entity?.legalHold) {
      throw new ForbiddenException(`Cannot ${action} person while legal hold is active`);
    }
  }

  private async validateOrg(orgId: string) {
    const org = await this.orgModel.findOne({ _id: orgId, archivedAt: null });
    if (!org) throw new NotFoundException('Organization not found or archived');
    if (org.legalHold) {
      throw new ForbiddenException('Organization is under legal hold');
    }
  }

  private async model(orgId: string) {
    return this.tenants.getModelForOrg<Person>(orgId, 'Person', PersonSchema, this.personModel);
  }

  private async companies(orgId: string) {
    return this.tenants.getModelForOrg<Company>(orgId, 'Company', CompanySchema, this.companyModel);
  }

  private async companyLocations(orgId: string) {
    return this.tenants.getModelForOrg<CompanyLocation>(
      orgId,
      'CompanyLocation',
      CompanyLocationSchema,
      this.companyLocationModel
    );
  }

  private async offices(orgId: string) {
    return this.tenants.getModelForOrg<Office>(orgId, 'Office', OfficeSchema, this.officeModel);
  }

  private toPlain(person: Person) {
    return person.toObject ? person.toObject() : person;
  }

  private parseOptionalDate(value?: string) {
    if (!value) return null;
    const dt = new Date(value);
    if (Number.isNaN(dt.getTime())) return null;
    return dt;
  }

  private normalizeEmails(emails: string[] | undefined, primaryEmail: string | undefined) {
    const normalized = (emails || [])
      .map((email) => (email || '').trim())
      .filter(Boolean)
      .map((email) => ({ value: email, normalized: normalizeEmail(email) }));

    const deduped = new Map<string, { value: string; normalized: string }>();
    normalized.forEach((email) => {
      if (!deduped.has(email.normalized)) {
        deduped.set(email.normalized, email);
      }
    });

    const ordered = Array.from(deduped.values());
    if (!ordered.length) return { emails: [] as PersonEmail[], primaryEmail: null as string | null };

    const desiredPrimary = primaryEmail ? normalizeEmail(primaryEmail) : null;
    const primaryNormalized = desiredPrimary && deduped.has(desiredPrimary) ? desiredPrimary : ordered[0].normalized;

    const emailObjects: PersonEmail[] = ordered.map((email) => ({
      value: email.value,
      normalized: email.normalized,
      label: null,
      isPrimary: email.normalized === primaryNormalized,
      verifiedAt: null,
    }));

    return { emails: emailObjects, primaryEmail: primaryNormalized };
  }

  private normalizePhones(phones: string[] | undefined, primaryPhone: string | undefined) {
    const normalized = (phones || [])
      .map((phone) => (phone || '').trim())
      .filter(Boolean)
      .map((value) => ({ value, e164: normalizePhoneE164(value) }))
      .filter((item) => !!item.e164) as Array<{ value: string; e164: string }>;

    const deduped = new Map<string, { value: string; e164: string }>();
    normalized.forEach((phone) => {
      if (!deduped.has(phone.e164)) {
        deduped.set(phone.e164, phone);
      }
    });

    const ordered = Array.from(deduped.values());
    if (!ordered.length) return { phones: [] as PersonPhone[], primaryPhoneE164: null as string | null };

    const desiredPrimary = primaryPhone ? normalizePhoneE164(primaryPhone) : null;
    const primaryE164 = desiredPrimary && deduped.has(desiredPrimary) ? desiredPrimary : ordered[0].e164;

    const phoneObjects: PersonPhone[] = ordered.map((phone) => ({
      value: phone.value,
      e164: phone.e164,
      label: null,
      isPrimary: phone.e164 === primaryE164,
    }));

    return { phones: phoneObjects, primaryPhoneE164: primaryE164 };
  }

  private async ensureCompanyActive(orgId: string, companyId: string) {
    const companies = await this.companies(orgId);
    const company = await companies.findOne({ _id: companyId, orgId, archivedAt: null }).lean();
    if (!company) throw new NotFoundException('Company not found or archived');
  }

  private async ensureCompanyLocationActive(orgId: string, companyId: string, companyLocationId: string) {
    const companyLocations = await this.companyLocations(orgId);
    const location = await companyLocations.findOne({ _id: companyLocationId, orgId, archivedAt: null }).lean();
    if (!location) throw new NotFoundException('Company location not found or archived');
    if (String((location as any).companyId) !== String(companyId)) {
      throw new BadRequestException('companyLocationId does not belong to companyId');
    }
  }

  private async ensureOrgLocationActive(orgId: string, orgLocationId: string) {
    const offices = await this.offices(orgId);
    const office = await offices.findOne({ _id: orgLocationId, orgId, archivedAt: null }).lean();
    if (!office) throw new NotFoundException('Org location not found or archived');
  }

  private async ensureReportsToActive(orgId: string, personId: string | null, reportsToPersonId: string) {
    if (personId && reportsToPersonId === personId) {
      throw new BadRequestException('reportsToPersonId cannot reference self');
    }
    const people = await this.model(orgId);
    const manager = await people.findOne({ _id: reportsToPersonId, orgId, archivedAt: null }).lean();
    if (!manager) throw new NotFoundException('reportsToPersonId person not found or archived');
  }

  private async ensureUniqueness(
    orgId: string,
    model: Model<Person>,
    personId: string | null,
    fields: { primaryEmail?: string | null; primaryPhoneE164?: string | null; ironworkerNumber?: string | null }
  ) {
    if (fields.primaryEmail) {
      const collision = await model.findOne({
        orgId,
        primaryEmail: fields.primaryEmail,
        archivedAt: null,
        ...(personId ? { _id: { $ne: personId } } : {}),
      });
      if (collision) throw new ConflictException(`Person primaryEmail already exists for this organization`);
    }

    if (fields.primaryPhoneE164) {
      const collision = await model.findOne({
        orgId,
        primaryPhoneE164: fields.primaryPhoneE164,
        archivedAt: null,
        ...(personId ? { _id: { $ne: personId } } : {}),
      });
      if (collision) throw new ConflictException(`Person primaryPhone already exists for this organization`);
    }

    if (fields.ironworkerNumber) {
      const collision = await model.findOne({
        orgId,
        ironworkerNumber: fields.ironworkerNumber,
        archivedAt: null,
        ...(personId ? { _id: { $ne: personId } } : {}),
      });
      if (collision) throw new ConflictException(`Ironworker number already exists for this organization`);
    }
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

  async create(actor: ActorContext, orgId: string, dto: CreatePersonDto) {
    this.ensureRole(actor, [Role.OrgOwner, Role.OrgAdmin, Role.Admin, Role.SuperAdmin, Role.PlatformAdmin]);
    this.ensureOrgScope(orgId, actor);
    await this.validateOrg(orgId);
    if (dto.companyId) {
      await this.ensureCompanyActive(orgId, dto.companyId);
    }
    if (dto.companyLocationId) {
      if (!dto.companyId) throw new BadRequestException('companyId is required when companyLocationId is provided');
      await this.ensureCompanyLocationActive(orgId, dto.companyId, dto.companyLocationId);
    }
    if (dto.orgLocationId) {
      await this.ensureOrgLocationActive(orgId, dto.orgLocationId);
    }
    if (dto.reportsToPersonId) {
      await this.ensureReportsToActive(orgId, null, dto.reportsToPersonId);
    }

    const people = await this.model(orgId);
    const { emails, primaryEmail } = this.normalizeEmails(dto.emails, dto.primaryEmail);
    const { phones, primaryPhoneE164 } = this.normalizePhones(dto.phones, dto.primaryPhone);

    if (dto.primaryEmail && primaryEmail !== normalizeEmail(dto.primaryEmail)) {
      throw new BadRequestException('primaryEmail must be one of the emails[] values');
    }
    if (dto.primaryPhone && primaryPhoneE164 !== normalizePhoneE164(dto.primaryPhone)) {
      throw new BadRequestException('primaryPhone must be one of the phones[] values');
    }

    if ((dto.phones || []).length && !(phones || []).length) {
      throw new BadRequestException('phones include invalid phone values; use E.164 (+15551234567) or 10-digit numbers');
    }

    await this.ensureUniqueness(orgId, people, null, {
      primaryEmail,
      primaryPhoneE164,
      ironworkerNumber: dto.ironworkerNumber || null,
    });

    const tagKeys = normalizeKeys(dto.tagKeys);
    const skillKeys = normalizeKeys(dto.skillKeys);
    const departmentKey = dto.departmentKey ? normalizeKey(dto.departmentKey) : '';
    if (tagKeys.length) {
      await this.taxonomy.ensureKeysActive(actor as any, orgId, 'person_tag', tagKeys);
    }
    if (skillKeys.length) {
      await this.taxonomy.ensureKeysActive(actor as any, orgId, 'person_skill', skillKeys);
    }
    if (departmentKey) {
      await this.taxonomy.ensureKeysActive(actor as any, orgId, 'department', [departmentKey]);
    }

    const created = await people.create({
      orgId,
      personType: dto.personType as PersonType,
      displayName: dto.displayName,
      firstName: dto.firstName || null,
      lastName: dto.lastName || null,
      dateOfBirth: this.parseOptionalDate(dto.dateOfBirth) || null,
      emails,
      phones,
      primaryEmail,
      primaryPhoneE164,
      tagKeys,
      skillKeys,
      departmentKey: departmentKey || null,
      orgLocationId: dto.orgLocationId || null,
      reportsToPersonId: dto.reportsToPersonId || null,
      ironworkerNumber: dto.ironworkerNumber || null,
      unionLocal: dto.unionLocal || null,
      skillFreeText: dto.skillFreeText || [],
      certifications: Array.isArray(dto.certifications)
        ? dto.certifications
            .filter((item) => item && typeof item.name === 'string' && item.name.trim())
            .map((item) => ({
              name: item.name.trim(),
              issuedAt: this.parseOptionalDate(item.issuedAt) || null,
              expiresAt: this.parseOptionalDate(item.expiresAt) || null,
              documentUrl: item.documentUrl?.trim() || null,
              notes: item.notes?.trim() || null,
            }))
        : [],
      rating: typeof dto.rating === 'number' ? dto.rating : null,
      notes: dto.notes || null,
      companyId: dto.companyId || null,
      companyLocationId: dto.companyLocationId || null,
      title: dto.title || null,
      userId: null,
      archivedAt: null,
      piiStripped: false,
      legalHold: false,
    });

    await this.audit.log({
      eventType: 'person.created',
      orgId,
      userId: actor.userId,
      entity: 'Person',
      entityId: created.id,
      metadata: { personType: created.personType },
    });

    return this.toPlain(created);
  }

  async list(actor: ActorContext, orgId: string, query: ListPersonsQueryDto) {
    this.ensureRole(actor, [Role.OrgOwner, Role.OrgAdmin, Role.Admin, Role.SuperAdmin, Role.PlatformAdmin]);
    this.ensureOrgScope(orgId, actor);
    await this.validateOrg(orgId);

    const includeArchived = !!query.includeArchived;
    if (includeArchived && !this.canViewArchived(actor)) {
      throw new ForbiddenException('Not allowed to include archived people');
    }

    const filter: Record<string, any> = { orgId };
    if (!includeArchived) filter.archivedAt = null;
    if (query.personType) filter.personType = query.personType;
    if (query.companyId) filter.companyId = query.companyId;
    if (query.companyLocationId) filter.companyLocationId = query.companyLocationId;
    if (query.orgLocationId) filter.orgLocationId = query.orgLocationId;

    const departmentKey = query.departmentKey ? normalizeKey(query.departmentKey) : '';
    if (departmentKey) filter.departmentKey = departmentKey;

    const skillKey = query.skillKey ? normalizeKey(query.skillKey) : '';
    if (skillKey) filter.skillKeys = skillKey;

    const tagKey = query.tag ? normalizeKey(query.tag) : '';
    if (tagKey) filter.tagKeys = tagKey;
    if (query.search) {
      const term = query.search.trim();
      const escaped = term.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
      const rx = new RegExp(escaped, 'i');
      const normalizedEmail = normalizeEmail(term);
      const normalizedPhone = normalizePhoneE164(term);

      filter.$or = [
        { displayName: rx },
        { firstName: rx },
        { lastName: rx },
        { primaryEmail: normalizedEmail },
        { 'emails.normalized': normalizedEmail },
        ...(normalizedPhone ? [{ primaryPhoneE164: normalizedPhone }, { 'phones.e164': normalizedPhone }] : []),
        { ironworkerNumber: term },
      ];
    }

    const people = await this.model(orgId);

    const wantsPagination = query.page !== undefined || query.limit !== undefined;
    const page = Math.max(1, query.page || 1);
    const limit = Math.min(Math.max(query.limit || 25, 1), 100);

    if (!wantsPagination) {
      return people.find(filter).sort({ displayName: 1, _id: 1 }).lean();
    }

    const skip = (page - 1) * limit;
    const [data, total] = await Promise.all([
      people.find(filter).sort({ displayName: 1, _id: 1 }).skip(skip).limit(limit).lean(),
      people.countDocuments(filter),
    ]);

    return { data, total, page, limit };
  }

  async getById(actor: ActorContext, orgId: string, id: string, includeArchived = false) {
    this.ensureRole(actor, [Role.OrgOwner, Role.OrgAdmin, Role.Admin, Role.SuperAdmin, Role.PlatformAdmin]);
    this.ensureOrgScope(orgId, actor);
    await this.validateOrg(orgId);
    const people = await this.model(orgId);
    const person = await people.findOne({ _id: id, orgId });
    if (!person) throw new NotFoundException('Person not found');
    if (person.archivedAt && !includeArchived) throw new NotFoundException('Person archived');
    if (person.archivedAt && includeArchived && !this.canViewArchived(actor)) {
      throw new ForbiddenException('Not allowed to view archived people');
    }
    return this.toPlain(person);
  }

  async findByPrimaryEmail(actor: ActorContext, orgId: string, email: string) {
    this.ensureRole(actor, [Role.OrgOwner, Role.OrgAdmin, Role.Admin, Role.SuperAdmin, Role.PlatformAdmin]);
    this.ensureOrgScope(orgId, actor);
    await this.validateOrg(orgId);
    const people = await this.model(orgId);
    return people.findOne({ orgId, primaryEmail: normalizeEmail(email), archivedAt: null }).lean();
  }

  async update(actor: ActorContext, orgId: string, id: string, dto: UpdatePersonDto) {
    this.ensureRole(actor, [Role.OrgOwner, Role.OrgAdmin, Role.Admin, Role.SuperAdmin, Role.PlatformAdmin]);
    this.ensureOrgScope(orgId, actor);
    await this.validateOrg(orgId);

    const people = await this.model(orgId);
    const person = await people.findOne({ _id: id, orgId });
    if (!person) throw new NotFoundException('Person not found');
    if (person.archivedAt) throw new NotFoundException('Person archived');
    this.ensureNotOnLegalHold(person, 'update');

    const before = this.toPlain(person);

    const nextCompanyId =
      dto.companyId !== undefined ? (dto.companyId || null) : ((person as any).companyId as string | null);

    if (dto.companyId !== undefined && dto.companyId) {
      await this.ensureCompanyActive(orgId, dto.companyId);
    }
    if (dto.companyLocationId !== undefined && dto.companyLocationId) {
      if (!nextCompanyId) throw new BadRequestException('companyId is required when companyLocationId is provided');
      await this.ensureCompanyLocationActive(orgId, nextCompanyId, dto.companyLocationId);
    }
    if (dto.orgLocationId !== undefined && dto.orgLocationId) {
      await this.ensureOrgLocationActive(orgId, dto.orgLocationId);
    }
    if (dto.reportsToPersonId !== undefined && dto.reportsToPersonId) {
      await this.ensureReportsToActive(orgId, id, dto.reportsToPersonId);
    }

    let nextPrimaryEmail = (person as any).primaryEmail || null;
    let nextPrimaryPhoneE164 = (person as any).primaryPhoneE164 || null;

    if (dto.emails !== undefined || dto.primaryEmail !== undefined) {
      const currentEmails = (person as any).emails || [];
      const currentPrimary = (person as any).primaryEmail || null;
      const { emails, primaryEmail } = this.normalizeEmails(
        dto.emails !== undefined ? dto.emails : currentEmails.map((e: any) => e.value),
        dto.primaryEmail !== undefined ? dto.primaryEmail : currentPrimary
      );
      if (dto.primaryEmail !== undefined && dto.primaryEmail && primaryEmail !== normalizeEmail(dto.primaryEmail)) {
        throw new BadRequestException('primaryEmail must be one of the emails[] values');
      }
      (person as any).emails = emails;
      (person as any).primaryEmail = primaryEmail;
      nextPrimaryEmail = primaryEmail;
    }

    if (dto.phones !== undefined || dto.primaryPhone !== undefined) {
      const currentPhones = (person as any).phones || [];
      const currentPrimary = (person as any).primaryPhoneE164 || null;
      const { phones, primaryPhoneE164 } = this.normalizePhones(
        dto.phones !== undefined ? dto.phones : currentPhones.map((p: any) => p.value),
        dto.primaryPhone !== undefined ? dto.primaryPhone : currentPrimary
      );
      if (dto.primaryPhone !== undefined && dto.primaryPhone && primaryPhoneE164 !== normalizePhoneE164(dto.primaryPhone)) {
        throw new BadRequestException('primaryPhone must be one of the phones[] values');
      }
      if ((dto.phones || []).length && !(phones || []).length) {
        throw new BadRequestException('phones include invalid phone values; use E.164 (+15551234567) or 10-digit numbers');
      }
      (person as any).phones = phones;
      (person as any).primaryPhoneE164 = primaryPhoneE164;
      nextPrimaryPhoneE164 = primaryPhoneE164;
    }

    await this.ensureUniqueness(orgId, people, id, {
      primaryEmail: nextPrimaryEmail,
      primaryPhoneE164: nextPrimaryPhoneE164,
      ironworkerNumber:
        dto.ironworkerNumber !== undefined ? dto.ironworkerNumber || null : (person as any).ironworkerNumber,
    });

    if (dto.personType !== undefined) (person as any).personType = dto.personType as PersonType;
    if (dto.displayName !== undefined) (person as any).displayName = dto.displayName;
    if (dto.firstName !== undefined) (person as any).firstName = dto.firstName || null;
    if (dto.lastName !== undefined) (person as any).lastName = dto.lastName || null;
    if (dto.dateOfBirth !== undefined) (person as any).dateOfBirth = this.parseOptionalDate(dto.dateOfBirth) || null;
    if (dto.tagKeys !== undefined) {
      const tagKeys = normalizeKeys(dto.tagKeys);
      if (tagKeys.length) {
        await this.taxonomy.ensureKeysActive(actor as any, orgId, 'person_tag', tagKeys);
      }
      (person as any).tagKeys = tagKeys;
    }
    if (dto.skillKeys !== undefined) {
      const skillKeys = normalizeKeys(dto.skillKeys);
      if (skillKeys.length) {
        await this.taxonomy.ensureKeysActive(actor as any, orgId, 'person_skill', skillKeys);
      }
      (person as any).skillKeys = skillKeys;
    }
    if (dto.departmentKey !== undefined) {
      const departmentKey = dto.departmentKey ? normalizeKey(dto.departmentKey) : '';
      if (departmentKey) {
        await this.taxonomy.ensureKeysActive(actor as any, orgId, 'department', [departmentKey]);
      }
      (person as any).departmentKey = departmentKey || null;
    }
    if (dto.orgLocationId !== undefined) (person as any).orgLocationId = dto.orgLocationId || null;
    if (dto.reportsToPersonId !== undefined) (person as any).reportsToPersonId = dto.reportsToPersonId || null;
    if (dto.ironworkerNumber !== undefined) (person as any).ironworkerNumber = dto.ironworkerNumber || null;
    if (dto.unionLocal !== undefined) (person as any).unionLocal = dto.unionLocal || null;
    if (dto.skillFreeText !== undefined) (person as any).skillFreeText = dto.skillFreeText || [];
    if (dto.certifications !== undefined) {
      (person as any).certifications = Array.isArray(dto.certifications)
        ? dto.certifications
            .filter((item) => item && typeof item.name === 'string' && item.name.trim())
            .map((item) => ({
              name: item.name.trim(),
              issuedAt: this.parseOptionalDate(item.issuedAt) || null,
              expiresAt: this.parseOptionalDate(item.expiresAt) || null,
              documentUrl: item.documentUrl?.trim() || null,
              notes: item.notes?.trim() || null,
            }))
        : [];
    }
    if (dto.rating !== undefined) (person as any).rating = typeof dto.rating === 'number' ? dto.rating : null;
    if (dto.notes !== undefined) (person as any).notes = dto.notes || null;
    if (dto.companyId !== undefined) (person as any).companyId = dto.companyId || null;
    if (dto.companyLocationId !== undefined) {
      (person as any).companyLocationId = dto.companyLocationId || null;
    } else if (dto.companyId !== undefined && (person as any).companyLocationId) {
      (person as any).companyLocationId = null;
    }
    if (dto.title !== undefined) (person as any).title = dto.title || null;

    await person.save();
    const after = this.toPlain(person);

    await this.audit.log({
      eventType: 'person.updated',
      orgId,
      userId: actor.userId,
      entity: 'Person',
      entityId: person.id,
      metadata: {
        changes: this.summarizeDiff(before, after, [
          'personType',
          'displayName',
          'firstName',
          'lastName',
          'dateOfBirth',
          'emails',
          'primaryEmail',
          'phones',
          'primaryPhoneE164',
          'tagKeys',
          'skillKeys',
          'departmentKey',
          'orgLocationId',
          'reportsToPersonId',
          'ironworkerNumber',
          'unionLocal',
          'skillFreeText',
          'certifications',
          'rating',
          'notes',
          'companyId',
          'companyLocationId',
          'title',
        ]),
      },
    });

    return after;
  }

  async archive(actor: ActorContext, orgId: string, id: string) {
    this.ensureRole(actor, [Role.OrgOwner, Role.OrgAdmin, Role.Admin, Role.SuperAdmin, Role.PlatformAdmin]);
    this.ensureOrgScope(orgId, actor);
    await this.validateOrg(orgId);
    const people = await this.model(orgId);
    const person = await people.findOne({ _id: id, orgId });
    if (!person) throw new NotFoundException('Person not found');
    this.ensureNotOnLegalHold(person, 'archive');

    if (!person.archivedAt) {
      person.archivedAt = new Date();
      await person.save();
    }

    await this.audit.log({
      eventType: 'person.archived',
      orgId,
      userId: actor.userId,
      entity: 'Person',
      entityId: person.id,
      metadata: { archivedAt: person.archivedAt },
    });

    return this.toPlain(person);
  }

  async unarchive(actor: ActorContext, orgId: string, id: string) {
    this.ensureRole(actor, [Role.OrgOwner, Role.OrgAdmin, Role.Admin, Role.SuperAdmin, Role.PlatformAdmin]);
    this.ensureOrgScope(orgId, actor);
    await this.validateOrg(orgId);
    const people = await this.model(orgId);
    const person = await people.findOne({ _id: id, orgId });
    if (!person) throw new NotFoundException('Person not found');
    this.ensureNotOnLegalHold(person, 'unarchive');

    if (person.archivedAt) {
      await this.ensureUniqueness(orgId, people, id, {
        primaryEmail: (person as any).primaryEmail || null,
        primaryPhoneE164: (person as any).primaryPhoneE164 || null,
        ironworkerNumber: (person as any).ironworkerNumber || null,
      });

      person.archivedAt = null;
      await person.save();
    }

    await this.audit.log({
      eventType: 'person.unarchived',
      orgId,
      userId: actor.userId,
      entity: 'Person',
      entityId: person.id,
      metadata: { archivedAt: person.archivedAt },
    });

    return this.toPlain(person);
  }

  async linkUser(orgId: string, personId: string, userId: string) {
    const people = await this.model(orgId);
    await people.updateOne({ _id: personId, orgId }, { $set: { userId } });
  }
}
