import { BadRequestException, ForbiddenException, Injectable, NotFoundException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { Role } from '../../common/roles';
import { AuditLogService } from '../../common/services/audit-log.service';
import { TenantConnectionService } from '../../common/tenancy/tenant-connection.service';
import { normalizeEmail, normalizeKeys, normalizeName } from '../../common/utils/normalize.util';
import { OrgTaxonomyService } from '../org-taxonomy/org-taxonomy.service';
import { Organization } from '../organizations/schemas/organization.schema';
import { Company, CompanySchema } from './schemas/company.schema';
import { CompanyLocation, CompanyLocationSchema } from '../company-locations/schemas/company-location.schema';
import { CompaniesImportConfirmRowDto, CompaniesImportRowDto } from './dto/companies-import.dto';

type ActorContext = { userId?: string; orgId?: string; role?: Role };

type PreviewRow = {
  row: number;
  suggestedAction: 'upsert' | 'error';
  companyAction?: 'create' | 'update';
  companyId?: string;
  locationAction?: 'create' | 'update' | 'none';
  locationId?: string;
  errors?: string[];
  warnings?: string[];
};

@Injectable()
export class CompaniesImportService {
  constructor(
    private readonly audit: AuditLogService,
    private readonly tenants: TenantConnectionService,
    private readonly taxonomy: OrgTaxonomyService,
    @InjectModel('Organization') private readonly orgModel: Model<Organization>
  ) {}

  private async validateOrg(orgId: string) {
    const org = await this.orgModel.findOne({ _id: orgId, archivedAt: null });
    if (!org) throw new NotFoundException('Organization not found or archived');
    if (org.legalHold) {
      throw new ForbiddenException('Organization is under legal hold');
    }
  }

  private async companies(orgId: string) {
    return this.tenants.getModelForOrg<Company>(orgId, 'Company', CompanySchema);
  }

  private async locations(orgId: string) {
    return this.tenants.getModelForOrg<CompanyLocation>(orgId, 'CompanyLocation', CompanyLocationSchema);
  }

  async preview(actor: ActorContext, rows: CompaniesImportRowDto[]) {
    if (!actor.orgId) throw new BadRequestException('Missing organization context');
    const orgId = actor.orgId;
    await this.validateOrg(orgId);

    if (!Array.isArray(rows) || rows.length === 0) {
      throw new BadRequestException('No rows supplied for import preview');
    }
    if (rows.length > 1000) {
      throw new BadRequestException('Row limit exceeded (max 1000)');
    }

    const companyExternalIds = Array.from(
      new Set(rows.map((r) => (r.companyExternalId || '').trim()).filter(Boolean))
    );
    const companyNames = Array.from(
      new Set(rows.map((r) => normalizeName(r.companyName || '')).filter(Boolean))
    );

    const companyModel = await this.companies(orgId);
    const companyOr: any[] = [];
    if (companyExternalIds.length) companyOr.push({ externalId: { $in: companyExternalIds } });
    if (companyNames.length) companyOr.push({ normalizedName: { $in: companyNames } });

    const existingCompanies = companyOr.length
      ? await companyModel.find({ orgId, archivedAt: null, $or: companyOr }).lean()
      : [];

    const companyByExternal = new Map<string, any>();
    const companyByName = new Map<string, any>();
    existingCompanies.forEach((c: any) => {
      if (c.externalId) companyByExternal.set(String(c.externalId), c);
      if (c.normalizedName) companyByName.set(String(c.normalizedName), c);
    });

    const locationCacheByExternal = new Map<string, any>();
    const locationCacheByName = new Map<string, any>();
    const locationModel = await this.locations(orgId);

    const previewRows: PreviewRow[] = [];

    for (const row of rows) {
      const errors: string[] = [];
      const warnings: string[] = [];

      const companyName = (row.companyName || '').trim();
      if (!companyName) errors.push('companyName is required');

      const normalizedCompanyName = companyName ? normalizeName(companyName) : '';
      const companyExternalId = (row.companyExternalId || '').trim();

      const matchedByExternal = companyExternalId ? companyByExternal.get(companyExternalId) : null;
      const matchedByName = normalizedCompanyName ? companyByName.get(normalizedCompanyName) : null;
      if (matchedByExternal && matchedByName && String(matchedByExternal._id) !== String(matchedByName._id)) {
        warnings.push('company_external_id and company_name match different existing companies; external_id wins');
      }

      const company = matchedByExternal || matchedByName;
      const companyId = company?._id ? String(company._id) : undefined;
      const companyAction = company ? 'update' : 'create';

      const locationName = (row.locationName || '').trim();
      const locationExternalId = (row.locationExternalId || '').trim();
      let locationAction: PreviewRow['locationAction'] = 'none';
      let locationId: string | undefined;

      if (locationName || locationExternalId) {
        if (!companyId && companyAction === 'create' && !companyName) {
          errors.push('location provided but company could not be resolved');
        }

        if (companyId) {
          const normalizedLocationName = locationName ? normalizeName(locationName) : '';
          const keyExternal = locationExternalId ? `${companyId}:${locationExternalId}` : '';
          const keyName = normalizedLocationName ? `${companyId}:${normalizedLocationName}` : '';

          let existingLoc: any = null;
          if (keyExternal && locationCacheByExternal.has(keyExternal)) {
            existingLoc = locationCacheByExternal.get(keyExternal);
          } else if (keyName && locationCacheByName.has(keyName)) {
            existingLoc = locationCacheByName.get(keyName);
          } else {
            if (locationExternalId) {
              existingLoc = await locationModel
                .findOne({ orgId, companyId, externalId: locationExternalId, archivedAt: null })
                .lean();
            }
            if (!existingLoc && normalizedLocationName) {
              existingLoc = await locationModel
                .findOne({ orgId, companyId, normalizedName: normalizedLocationName, archivedAt: null })
                .lean();
            }
            if (existingLoc?._id) {
              if (keyExternal) locationCacheByExternal.set(keyExternal, existingLoc);
              if (keyName) locationCacheByName.set(keyName, existingLoc);
            }
          }

          if (existingLoc?._id) {
            locationAction = 'update';
            locationId = String(existingLoc._id);
          } else {
            if (locationExternalId && !locationName) {
              warnings.push(
                'location_external_id provided without location_name; importer can only link if the location already exists'
              );
            }
            locationAction = 'create';
          }
        }
      }

      previewRows.push({
        row: row.row,
        suggestedAction: errors.length ? 'error' : 'upsert',
        companyAction,
        companyId,
        locationAction,
        locationId,
        errors: errors.length ? errors : undefined,
        warnings: warnings.length ? warnings : undefined,
      });
    }

    const summary = {
      total: rows.length,
      creates: previewRows.filter((r) => r.suggestedAction !== 'error' && r.companyAction === 'create').length,
      updates: previewRows.filter((r) => r.suggestedAction !== 'error' && r.companyAction === 'update').length,
      errors: previewRows.filter((r) => r.suggestedAction === 'error').length,
    };

    return { orgId, summary, rows: previewRows };
  }

  async confirm(actor: ActorContext, rows: CompaniesImportConfirmRowDto[]) {
    if (!actor.orgId) throw new BadRequestException('Missing organization context');
    const orgId = actor.orgId;
    await this.validateOrg(orgId);

    if (!Array.isArray(rows) || rows.length === 0) {
      throw new BadRequestException('No rows supplied for import confirm');
    }
    if (rows.length > 1000) {
      throw new BadRequestException('Row limit exceeded (max 1000)');
    }

    const companyModel = await this.companies(orgId);
    const locationModel = await this.locations(orgId);

    const results: Array<{ row: number; status: 'ok' | 'skipped' | 'error'; message?: string }> = [];
    let created = 0;
    let updated = 0;
    let skipped = 0;
    let errors = 0;
    let locationsCreated = 0;
    let locationsUpdated = 0;

    for (const row of rows) {
      if (row.action === 'skip') {
        skipped += 1;
        results.push({ row: row.row, status: 'skipped' });
        continue;
      }

      try {
        const companyName = (row.companyName || '').trim();
        if (!companyName) throw new BadRequestException('companyName is required');
        const normalizedCompanyName = normalizeName(companyName);
        const externalId = (row.companyExternalId || '').trim() || null;

        let company: any = null;
        if (externalId) {
          company = await companyModel.findOne({ orgId, externalId, archivedAt: null });
        }
        if (!company) {
          company = await companyModel.findOne({ orgId, normalizedName: normalizedCompanyName, archivedAt: null });
        }

        const companyTypeKeys = normalizeKeys(row.companyTypeKeys);
        const companyTagKeys = normalizeKeys(row.companyTagKeys);
        if (companyTypeKeys.length) {
          await this.taxonomy.ensureKeysActive(actor as any, orgId, 'company_type', companyTypeKeys);
        }
        if (companyTagKeys.length) {
          await this.taxonomy.ensureKeysActive(actor as any, orgId, 'company_tag', companyTagKeys);
        }

        const website = row.website?.trim() ? row.website.trim() : undefined;
        const mainEmail = row.mainEmail?.trim() ? normalizeEmail(row.mainEmail) : undefined;
        const mainPhone = row.mainPhone?.trim() ? row.mainPhone.trim() : undefined;
        const notes = row.notes?.trim() ? row.notes.trim() : undefined;

        if (!company) {
          company = await companyModel.create({
            orgId,
            name: companyName,
            normalizedName: normalizedCompanyName,
            externalId,
            website: website ?? null,
            mainEmail: mainEmail ?? null,
            mainPhone: mainPhone ?? null,
            companyTypeKeys,
            tagKeys: companyTagKeys,
            rating: null,
            notes: notes ?? null,
            archivedAt: null,
            piiStripped: false,
            legalHold: false,
          });
          created += 1;
        } else {
          let dirty = false;
          if (companyName && company.name !== companyName) {
            (company as any).name = companyName;
            (company as any).normalizedName = normalizedCompanyName;
            dirty = true;
          }
          if (externalId !== undefined && (company as any).externalId !== externalId) {
            (company as any).externalId = externalId;
            dirty = true;
          }
          if (website !== undefined) {
            (company as any).website = website || null;
            dirty = true;
          }
          if (mainEmail !== undefined) {
            (company as any).mainEmail = mainEmail || null;
            dirty = true;
          }
          if (mainPhone !== undefined) {
            (company as any).mainPhone = mainPhone || null;
            dirty = true;
          }
          if (companyTypeKeys.length) {
            (company as any).companyTypeKeys = companyTypeKeys;
            dirty = true;
          }
          if (companyTagKeys.length) {
            (company as any).tagKeys = companyTagKeys;
            dirty = true;
          }
          if (notes !== undefined) {
            (company as any).notes = notes || null;
            dirty = true;
          }
          if (dirty) {
            await company.save();
          }
          updated += 1;
        }

        const companyId = String((company as any)._id || company.id);

        const locationName = (row.locationName || '').trim();
        const locationExternalId = (row.locationExternalId || '').trim() || null;
        if (locationName || locationExternalId) {
          const normalizedLocationName = locationName ? normalizeName(locationName) : '';
          let location: any = null;
          if (locationExternalId) {
            location = await locationModel.findOne({ orgId, companyId, externalId: locationExternalId, archivedAt: null });
          }
          if (!location && normalizedLocationName) {
            location = await locationModel.findOne({ orgId, companyId, normalizedName: normalizedLocationName, archivedAt: null });
          }

          const locationTagKeys = normalizeKeys(row.locationTagKeys);
          if (locationTagKeys.length) {
            await this.taxonomy.ensureKeysActive(actor as any, orgId, 'company_location_tag', locationTagKeys);
          }

          const locationEmail = row.locationEmail?.trim() ? normalizeEmail(row.locationEmail) : undefined;
          const locationPhone = row.locationPhone?.trim() ? row.locationPhone.trim() : undefined;
          const locationAddressLine1 = row.locationAddressLine1?.trim() ? row.locationAddressLine1.trim() : undefined;
          const locationCity = row.locationCity?.trim() ? row.locationCity.trim() : undefined;
          const locationRegion = row.locationRegion?.trim() ? row.locationRegion.trim() : undefined;
          const locationPostal = row.locationPostal?.trim() ? row.locationPostal.trim() : undefined;
          const locationCountry = row.locationCountry?.trim() ? row.locationCountry.trim() : undefined;
          const locationNotes = row.locationNotes?.trim() ? row.locationNotes.trim() : undefined;

          if (!location) {
            if (!locationName) {
              throw new BadRequestException('locationName is required to create a new location');
            }
            await locationModel.create({
              orgId,
              companyId,
              name: locationName,
              normalizedName: normalizedLocationName,
              externalId: locationExternalId,
              timezone: null,
              email: locationEmail ?? null,
              phone: locationPhone ?? null,
              addressLine1: locationAddressLine1 ?? null,
              addressLine2: null,
              city: locationCity ?? null,
              region: locationRegion ?? null,
              postal: locationPostal ?? null,
              country: locationCountry ?? null,
              tagKeys: locationTagKeys,
              notes: locationNotes ?? null,
              archivedAt: null,
              piiStripped: false,
              legalHold: false,
            });
            locationsCreated += 1;
          } else {
            let dirty = false;
            if (locationName && (location as any).name !== locationName) {
              (location as any).name = locationName;
              (location as any).normalizedName = normalizedLocationName;
              dirty = true;
            }
            if (locationExternalId !== undefined && (location as any).externalId !== locationExternalId) {
              (location as any).externalId = locationExternalId;
              dirty = true;
            }
            if (locationEmail !== undefined) {
              (location as any).email = locationEmail || null;
              dirty = true;
            }
            if (locationPhone !== undefined) {
              (location as any).phone = locationPhone || null;
              dirty = true;
            }
            if (locationAddressLine1 !== undefined) {
              (location as any).addressLine1 = locationAddressLine1 || null;
              dirty = true;
            }
            if (locationCity !== undefined) {
              (location as any).city = locationCity || null;
              dirty = true;
            }
            if (locationRegion !== undefined) {
              (location as any).region = locationRegion || null;
              dirty = true;
            }
            if (locationPostal !== undefined) {
              (location as any).postal = locationPostal || null;
              dirty = true;
            }
            if (locationCountry !== undefined) {
              (location as any).country = locationCountry || null;
              dirty = true;
            }
            if (locationTagKeys.length) {
              (location as any).tagKeys = locationTagKeys;
              dirty = true;
            }
            if (locationNotes !== undefined) {
              (location as any).notes = locationNotes || null;
              dirty = true;
            }
            if (dirty) await location.save();
            locationsUpdated += 1;
          }
        }

        results.push({ row: row.row, status: 'ok' });
      } catch (err: any) {
        errors += 1;
        results.push({ row: row.row, status: 'error', message: err?.message || 'import_failed' });
      }
    }

    await this.audit.logMutation({
      action: 'confirm_v1',
      entity: 'companies_import_v1',
      orgId,
      userId: actor.userId,
      metadata: {
        processed: rows.length,
        created,
        updated,
        skipped,
        locationsCreated,
        locationsUpdated,
        errors,
      },
    });

    return {
      orgId,
      processed: rows.length,
      created,
      updated,
      skipped,
      locationsCreated,
      locationsUpdated,
      errors,
      results,
    };
  }
}
