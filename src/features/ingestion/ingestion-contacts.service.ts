import { BadRequestException, Injectable, ServiceUnavailableException } from '@nestjs/common';
import { AiService } from '../../common/services/ai.service';
import { AuditLogService } from '../../common/services/audit-log.service';
import { IngestionContactsEnrichDto } from './dto/ingestion-contacts-enrich.dto';
import { IngestionContactsParseRowDto } from './dto/ingestion-contacts-parse-row.dto';
import { IngestionContactsSuggestMappingDto } from './dto/ingestion-contacts-suggest-mapping.dto';
import {
  IngestionCandidateIssueCode,
  IngestionContactCandidate,
  IngestionContactsProfile,
} from './ingestion.types';
import { normalizeHeaderKey, uniq } from './ingestion.utils';

type Actor = { userId?: string; orgId?: string; role?: string };

@Injectable()
export class IngestionContactsService {
  constructor(
    private readonly ai: AiService,
    private readonly audit: AuditLogService
  ) {}

  suggestMapping(actor: Actor, dto: IngestionContactsSuggestMappingDto) {
    if (dto.allowAiProcessing) {
      if (!this.ai.isEnabled()) throw new ServiceUnavailableException('AI is not enabled.');
      return this.aiSuggestMapping(actor, dto);
    }

    const headers = dto.headers || [];
    const normalized = headers.map((h) => ({ raw: h, key: normalizeHeaderKey(h) }));
    const pick = (candidates: string[]) => {
      const set = new Set(candidates.map(normalizeHeaderKey));
      const exact = normalized.find((h) => set.has(h.key));
      if (exact) return exact.raw;
      return normalized.find((h) => candidates.some((c) => h.key.includes(normalizeHeaderKey(c))))?.raw;
    };

    const profile = dto.profile ?? IngestionContactsProfile.Mixed;

    const suggestions = {
      personType:
        profile === IngestionContactsProfile.CompaniesLocations
          ? null
          : pick(['person_type', 'personType', 'type', 'contact_type']),
      displayName:
        profile === IngestionContactsProfile.CompaniesLocations
          ? null
          : pick(['display_name', 'displayName', 'name', 'full_name', 'fullname', 'contact_name']),
      emails:
        profile === IngestionContactsProfile.CompaniesLocations ? null : pick(['emails', 'email', 'email_address']),
      phones:
        profile === IngestionContactsProfile.CompaniesLocations ? null : pick(['phones', 'phone', 'phone_number']),
      companyName: pick(['company', 'company_name', 'vendor', 'supplier', 'business']),
      companyExternalId: pick(['company_external_id', 'companyExternalId']),
      companyLocationName: pick(['company_location', 'company_location_name', 'location', 'site', 'branch']),
      companyLocationExternalId: pick(['company_location_external_id', 'location_external_id']),
      ironworkerNumber:
        profile === IngestionContactsProfile.Ironworkers
          ? pick(['ironworker_number', 'ironworker', 'member_number', 'unionNumber'])
          : pick(['ironworker_number', 'ironworker', 'member_number', 'unionNumber']),
      orgLocationName:
        profile === IngestionContactsProfile.CompaniesLocations ? null : pick(['org_location', 'office', 'branch']),
      notes: pick(['notes', 'note', 'comments']),
    };

    return {
      orgId: actor.orgId,
      profile,
      suggestions,
      mode: 'deterministic' as const,
    };
  }

  async parseRow(actor: Actor, dto: IngestionContactsParseRowDto) {
    const cells = dto.cells ?? {};

    const maxCellKeys = 250;
    const keys = Object.keys(cells);
    if (keys.length > maxCellKeys) throw new BadRequestException(`cells has too many keys (max ${maxCellKeys}).`);

    const maxTotalChars = 50_000;
    const totalChars = Object.values(cells).reduce((sum, value) => {
      const str = typeof value === 'string' ? value : String(value ?? '');
      return sum + str.length;
    }, 0);
    if (totalChars > maxTotalChars) throw new BadRequestException(`cells payload too large (max ${maxTotalChars} chars).`);

    const allText = Object.values(cells)
      .map((v) => (typeof v === 'string' ? v : String(v ?? '')))
      .join('\n');

    const extractedEmails = this.extractEmails(allText);
    const extractedPhones = this.extractPhones(allText);
    const emails = uniq(extractedEmails);
    const phones = uniq(extractedPhones);

    const nameCell =
      cells['displayName'] ??
      cells['display_name'] ??
      cells['name'] ??
      cells['full_name'] ??
      cells['contact'] ??
      '';
    const displayName = typeof nameCell === 'string' ? nameCell.trim() : String(nameCell ?? '').trim();

    const candidates: IngestionContactCandidate[] = [];

    const splitByEmails = emails.length > 1;
    const baseIssues: IngestionCandidateIssueCode[] = [];

    if (!displayName) baseIssues.push(IngestionCandidateIssueCode.MissingDisplayName);

    if (emails.length === 0 && phones.length === 0) baseIssues.push(IngestionCandidateIssueCode.MissingIdentifier);

    if (splitByEmails) {
      emails.forEach((email, idx) => {
        const issues = [...baseIssues];
        const candidatePhones = idx === 0 ? phones : [];
        if (!email) issues.push(IngestionCandidateIssueCode.MissingIdentifier);
        if (!candidatePhones.length && !email) issues.push(IngestionCandidateIssueCode.MissingIdentifier);
        candidates.push({
          splitIndex: idx,
          method: 'deterministic',
          confidence: idx === 0 ? 0.9 : 0.75,
          extractedFrom: ['*'],
          person: {
            displayName: displayName || undefined,
            emails: [email],
            phones: candidatePhones.length ? candidatePhones : undefined,
          },
          issues: uniq(issues),
        });
      });
    } else {
      candidates.push({
        splitIndex: 0,
        method: 'deterministic',
        confidence: emails.length || phones.length ? 0.85 : 0.6,
        extractedFrom: ['*'],
        person: {
          displayName: displayName || undefined,
          emails: emails.length ? emails : undefined,
          phones: phones.length ? phones : undefined,
        },
        issues: uniq(baseIssues),
      });
    }

    const hasDuplicateIdentifiers = emails.length !== extractedEmails.length || phones.length !== extractedPhones.length;
    if (hasDuplicateIdentifiers) {
      for (const candidate of candidates) {
        candidate.issues = uniq([...candidate.issues, IngestionCandidateIssueCode.DuplicateIdentifier]);
      }
    }

    const allowAi = !!dto.allowAiProcessing;
    if (!allowAi) {
      return {
        orgId: actor.orgId,
        profile: dto.profile,
        mode: 'deterministic' as const,
        candidates,
      };
    }

    if (!this.ai.isEnabled()) throw new ServiceUnavailableException('AI is not enabled.');

    await this.audit.log({
      eventType: 'ingestion.ai_row_parse_requested',
      orgId: actor.orgId,
      userId: actor.userId,
      metadata: {
        profile: dto.profile,
        cellKeys: Object.keys(cells).length,
      },
    });

    const aiCandidates = await this.ai.parseContactsRow({
      orgId: actor.orgId,
      profile: dto.profile,
      cells: dto.cells,
    });

    return {
      orgId: actor.orgId,
      profile: dto.profile,
      mode: 'ai' as const,
      candidates: aiCandidates,
    };
  }

  async enrich(actor: Actor, dto: IngestionContactsEnrichDto) {
    const candidate = dto.candidate || {};

    const maxCandidateChars = 50_000;
    const candidateChars = JSON.stringify(candidate).length;
    if (candidateChars > maxCandidateChars) {
      throw new BadRequestException(`candidate payload too large (max ${maxCandidateChars} chars).`);
    }

    const person = candidate.person || {};
    const firstEmail = Array.isArray(person.emails) ? String(person.emails[0] || '').trim().toLowerCase() : '';

    const deterministicSuggestions: any = { person: {} as any };

    if (!person.displayName && firstEmail) {
      const derived = this.deriveDisplayNameFromEmail(firstEmail);
      if (derived) deterministicSuggestions.person.displayName = derived;
    }

    if (!candidate.companyName && firstEmail) {
      const derivedCompany = this.deriveCompanyNameFromEmail(firstEmail);
      if (derivedCompany) deterministicSuggestions.companyName = derivedCompany;
    }

    const allowAi = !!dto.allowAiProcessing;
    if (!allowAi) {
      return {
        orgId: actor.orgId,
        profile: dto.profile,
        mode: 'deterministic' as const,
        suggestions: deterministicSuggestions,
      };
    }

    if (!this.ai.isEnabled()) throw new ServiceUnavailableException('AI is not enabled.');

    await this.audit.log({
      eventType: 'ingestion.ai_enrich_requested',
      orgId: actor.orgId,
      userId: actor.userId,
      metadata: {
        profile: dto.profile,
      },
    });

    const aiSuggestions = await this.ai.enrichContactsCandidate({
      orgId: actor.orgId,
      profile: dto.profile,
      candidate: dto.candidate,
      deterministicSuggestions,
    });

    return {
      orgId: actor.orgId,
      profile: dto.profile,
      mode: 'ai' as const,
      suggestions: aiSuggestions,
    };
  }

  private async aiSuggestMapping(actor: Actor, dto: IngestionContactsSuggestMappingDto) {
    await this.audit.log({
      eventType: 'ingestion.ai_mapping_requested',
      orgId: actor.orgId,
      userId: actor.userId,
      metadata: {
        profile: dto.profile ?? IngestionContactsProfile.Mixed,
        headersCount: (dto.headers || []).length,
      },
    });

    const res = await this.ai.suggestContactsMapping({
      orgId: actor.orgId,
      profile: dto.profile ?? IngestionContactsProfile.Mixed,
      headers: dto.headers || [],
    });

    return {
      orgId: actor.orgId,
      profile: dto.profile ?? IngestionContactsProfile.Mixed,
      suggestions: res,
      mode: 'ai' as const,
    };
  }

  private extractEmails(text: string) {
    const matches = text.match(/[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}/gi) || [];
    return matches.map((m) => m.trim().toLowerCase()).filter(Boolean);
  }

  private extractPhones(text: string) {
    const matches = text.match(/(\+?\d[\d\s().-]{6,}\d)/g) || [];
    return matches.map((m) => m.trim()).filter(Boolean);
  }

  private deriveDisplayNameFromEmail(email: string) {
    const local = email.split('@')[0] || '';
    if (!local) return null;
    if (['info', 'sales', 'support', 'admin', 'office'].includes(local.toLowerCase())) return null;
    const cleaned = local.replace(/[._-]+/g, ' ').replace(/\d+/g, ' ').trim();
    if (!cleaned) return null;
    return cleaned
      .split(/\s+/g)
      .filter(Boolean)
      .slice(0, 4)
      .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
      .join(' ');
  }

  private deriveCompanyNameFromEmail(email: string) {
    const domain = email.split('@')[1] || '';
    if (!domain) return null;
    const parts = domain.toLowerCase().split('.').filter(Boolean);
    const common = new Set(['gmail', 'yahoo', 'outlook', 'hotmail', 'aol', 'icloud', 'protonmail']);
    const sld = parts.length >= 2 ? parts[parts.length - 2] : parts[0];
    if (!sld || common.has(sld)) return null;
    const cleaned = sld.replace(/[^a-z0-9]/g, ' ').trim();
    if (!cleaned) return null;
    return cleaned
      .split(/\s+/g)
      .filter(Boolean)
      .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
      .join(' ');
  }
}
