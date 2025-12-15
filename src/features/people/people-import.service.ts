import { BadRequestException, Injectable } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { AuditLogService } from '../../common/services/audit-log.service';
import { TenantConnectionService } from '../../common/tenancy/tenant-connection.service';
import { Role } from '../../common/roles';
import { Contact, ContactSchema } from '../contacts/schemas/contact.schema';
import { ContactsService } from '../contacts/contacts.service';
import { InvitesService } from '../invites/invites.service';
import { UsersService } from '../users/users.service';
import { PeopleImportConfirmRowDto, PeopleImportRowDto } from './dto/people-import.dto';

type ActorContext = { userId?: string; orgId?: string; role?: Role };

type PreviewRowResult = {
  row: number;
  suggestedAction: 'create' | 'update' | 'error';
  contactId?: string;
  matchBy?: 'email' | 'ironworkerNumber';
  errors?: string[];
  warnings?: string[];
};

@Injectable()
export class PeopleImportService {
  constructor(
    private readonly audit: AuditLogService,
    private readonly tenants: TenantConnectionService,
    @InjectModel('Contact') private readonly contactModel: Model<Contact>,
    private readonly contacts: ContactsService,
    private readonly invites: InvitesService,
    private readonly users: UsersService
  ) {}

  private async contactTenantModel(orgId: string) {
    return this.tenants.getModelForOrg<Contact>(orgId, 'Contact', ContactSchema, this.contactModel);
  }

  private normalizePersonType(value?: string) {
    const normalized = (value || '').trim().toLowerCase();
    if (normalized === 'staff') return 'staff';
    if (normalized === 'ironworker') return 'ironworker';
    if (normalized === 'external') return 'external';
    return undefined;
  }

  private rowErrors(row: PeopleImportRowDto) {
    const errors: string[] = [];
    if (!row.name || !row.name.trim()) errors.push('name is required');
    const normalizedPersonType = this.normalizePersonType(row.personType);
    if (row.personType && !normalizedPersonType) {
      errors.push(`personType must be one of staff|ironworker|external`);
    }
    if (row.inviteRole && !row.email) {
      errors.push('email is required when inviteRole is provided');
    }
    return errors;
  }

  async preview(actor: ActorContext, rows: PeopleImportRowDto[]) {
    if (!actor.orgId) throw new BadRequestException('Missing organization context');
    const orgId = actor.orgId;

    if (!Array.isArray(rows) || rows.length === 0) {
      throw new BadRequestException('No rows supplied for import preview');
    }
    if (rows.length > 1000) {
      throw new BadRequestException('Row limit exceeded (max 1000)');
    }

    const model = await this.contactTenantModel(orgId);
    const emails = Array.from(
      new Set(
        rows
          .map((row) => (row.email || '').trim().toLowerCase())
          .filter(Boolean)
      )
    );
    const ironworkerNumbers = Array.from(
      new Set(
        rows
          .map((row) => (row.ironworkerNumber || '').trim())
          .filter(Boolean)
      )
    );

    const orFilters: Record<string, any>[] = [
      ...(emails.length ? [{ email: { $in: emails } }] : []),
      ...(ironworkerNumbers.length ? [{ ironworkerNumber: { $in: ironworkerNumbers } }] : []),
    ];

    const existing = orFilters.length
      ? await model
          .find({
            orgId,
            $or: orFilters,
          })
          .lean()
      : [];

    const emailMap = new Map<string, any>();
    const ironMap = new Map<string, any>();
    existing.forEach((contact) => {
      if (contact.email) emailMap.set(String(contact.email).toLowerCase(), contact);
      if (contact.ironworkerNumber) ironMap.set(String(contact.ironworkerNumber), contact);
    });

    const previewRows: PreviewRowResult[] = rows.map((row) => {
      const errors = this.rowErrors(row);
      const warnings: string[] = [];

      const emailKey = (row.email || '').trim().toLowerCase();
      const ironKey = (row.ironworkerNumber || '').trim();

      const matchedByEmail = emailKey ? emailMap.get(emailKey) : undefined;
      const matchedByIron = ironKey ? ironMap.get(ironKey) : undefined;

      if (matchedByEmail && matchedByIron && String(matchedByEmail._id) !== String(matchedByIron._id)) {
        errors.push('email and ironworkerNumber match different existing contacts');
      }

      const matched = matchedByEmail || matchedByIron;
      const matchBy = matchedByEmail ? 'email' : matchedByIron ? 'ironworkerNumber' : undefined;

      if (matched?.archivedAt) {
        warnings.push('matches an archived contact; update may fail unless unarchived first');
      }

      if (errors.length) {
        return { row: row.row, suggestedAction: 'error', contactId: matched?._id, matchBy, errors, warnings };
      }

      if (matched) {
        return { row: row.row, suggestedAction: 'update', contactId: matched._id, matchBy, warnings };
      }

      return { row: row.row, suggestedAction: 'create', warnings };
    });

    const summary = previewRows.reduce(
      (acc, r) => {
        acc.total += 1;
        if (r.suggestedAction === 'create') acc.creates += 1;
        else if (r.suggestedAction === 'update') acc.updates += 1;
        else acc.errors += 1;
        return acc;
      },
      { total: 0, creates: 0, updates: 0, errors: 0 }
    );

    await this.audit.logMutation({
      action: 'preview',
      entity: 'people_import',
      orgId,
      userId: actor.userId,
      metadata: {
        total: summary.total,
        creates: summary.creates,
        updates: summary.updates,
        errors: summary.errors,
      },
    });

    return { orgId, summary, rows: previewRows };
  }

  async confirm(actor: ActorContext, rows: PeopleImportConfirmRowDto[]) {
    if (!actor.orgId) throw new BadRequestException('Missing organization context');
    const orgId = actor.orgId;

    if (!Array.isArray(rows) || rows.length === 0) {
      throw new BadRequestException('No rows supplied for import confirm');
    }
    if (rows.length > 1000) {
      throw new BadRequestException('Row limit exceeded (max 1000)');
    }

    const results: Array<{ row: number; status: 'ok' | 'skipped' | 'error'; message?: string }> = [];
    let created = 0;
    let updated = 0;
    let skipped = 0;
    let invitesCreated = 0;
    let errors = 0;

    for (const row of rows) {
      if (row.action === 'skip') {
        skipped += 1;
        results.push({ row: row.row, status: 'skipped' });
        continue;
      }

      const validationErrors = this.rowErrors(row);
      if (validationErrors.length) {
        errors += 1;
        results.push({ row: row.row, status: 'error', message: validationErrors.join('; ') });
        continue;
      }

      const contactPayload: any = {
        name: row.name,
        personType: this.normalizePersonType(row.personType) || 'external',
        email: row.email || undefined,
        phone: row.phone || undefined,
        company: row.company || undefined,
        ironworkerNumber: row.ironworkerNumber || undefined,
        unionLocal: row.unionLocal || undefined,
        dateOfBirth: row.dateOfBirth || undefined,
        skills: row.skills || undefined,
        certifications: row.certifications || undefined,
        notes: row.notes || undefined,
      };

      try {
        if (row.action === 'create') {
          const createdContact = await this.contacts.create(actor, orgId, contactPayload);
          created += 1;

          if (row.inviteRole) {
            if (!row.email) throw new BadRequestException('email is required to create an invite');
            const existingUser = await this.users.findAnyByEmail(row.email);
            if (existingUser) {
              results.push({ row: row.row, status: 'ok', message: 'User already exists; invite skipped.' });
              continue;
            }
            await this.invites.create(orgId, actor.userId as string, actor.role as Role, {
              email: row.email,
              role: row.inviteRole,
              expiresInHours: 72,
              contactId: (createdContact as any)._id || (createdContact as any).id,
            });
            invitesCreated += 1;
          }

          results.push({ row: row.row, status: 'ok' });
          continue;
        }

        if (row.action === 'update') {
          const contactId = row.contactId;
          if (!contactId) {
            throw new BadRequestException('contactId is required for update rows');
          }
          const updatedContact = await this.contacts.update(actor, orgId, contactId, contactPayload);
          updated += 1;

          if (row.inviteRole) {
            if (!row.email) throw new BadRequestException('email is required to create an invite');
            const existingUser = await this.users.findAnyByEmail(row.email);
            if (existingUser) {
              results.push({ row: row.row, status: 'ok', message: 'User already exists; invite skipped.' });
              continue;
            }
            await this.invites.create(orgId, actor.userId as string, actor.role as Role, {
              email: row.email,
              role: row.inviteRole,
              expiresInHours: 72,
              contactId: (updatedContact as any)._id || (updatedContact as any).id || contactId,
            });
            invitesCreated += 1;
          }

          results.push({ row: row.row, status: 'ok' });
          continue;
        }

        skipped += 1;
        results.push({ row: row.row, status: 'skipped' });
      } catch (err: any) {
        errors += 1;
        results.push({ row: row.row, status: 'error', message: err?.message || 'import_failed' });
      }
    }

    await this.audit.logMutation({
      action: 'confirm',
      entity: 'people_import',
      orgId,
      userId: actor.userId,
      metadata: {
        processed: rows.length,
        created,
        updated,
        skipped,
        invitesCreated,
        errors,
      },
    });

    return {
      orgId,
      processed: rows.length,
      created,
      updated,
      skipped,
      invitesCreated,
      errors,
      results,
    };
  }
}
