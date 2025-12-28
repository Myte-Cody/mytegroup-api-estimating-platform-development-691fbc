import {
  BadGatewayException,
  GatewayTimeoutException,
  Injectable,
  ServiceUnavailableException,
} from '@nestjs/common';
import {
  IngestionCandidateIssueCode,
  IngestionContactCandidate,
  IngestionContactsProfile,
} from '../../features/ingestion/ingestion.types';

type ParseContactsRowInput = {
  orgId?: string;
  profile: IngestionContactsProfile;
  cells: Record<string, any>;
};

type SuggestContactsMappingInput = {
  orgId?: string;
  profile: IngestionContactsProfile;
  headers: string[];
};

type EnrichContactsCandidateInput = {
  orgId?: string;
  profile: IngestionContactsProfile;
  candidate: any;
  deterministicSuggestions: any;
};

@Injectable()
export class AiService {
  isEnabled() {
    return !!process.env.OPENAI_API_KEY;
  }

  async suggestContactsMapping(input: SuggestContactsMappingInput): Promise<Record<string, string | null>> {
    if (!this.isEnabled()) throw new ServiceUnavailableException('AI is not enabled.');

    const model = process.env.OPENAI_MODEL || 'gpt-4o-mini';
    const headers = Array.isArray(input.headers) ? input.headers.map(String) : [];
    const headerSet = new Set(headers);

    const system = [
      'You are a data ingestion assistant.',
      'Given a list of spreadsheet headers, choose which header best maps to each canonical field.',
      'Return ONLY strict JSON (no markdown, no commentary).',
      'Values must be either one of the provided headers EXACTLY, or null.',
      'Do not invent headers.',
    ].join(' ');

    const canonicalFields = [
      'personType',
      'displayName',
      'emails',
      'primaryEmail',
      'phones',
      'primaryPhone',
      'orgLocationName',
      'reportsToDisplayName',
      'companyExternalId',
      'companyName',
      'companyLocationExternalId',
      'companyLocationName',
      'title',
      'departmentKey',
      'ironworkerNumber',
      'unionLocal',
      'skillKeys',
      'tagKeys',
      'certifications',
      'rating',
      'notes',
      'inviteRole',
    ];

    const user = {
      orgId: input.orgId,
      profile: input.profile,
      headers,
      outputSchema: {
        suggestions: Object.fromEntries(canonicalFields.map((k) => [k, 'string|null'])),
      },
      rules: [
        'Return { "suggestions": { ... } }.',
        'Use null when there is no good match.',
        'Prefer exact semantic matches (e.g., "Email Address" -> emails).',
      ],
    };

    const controller = new AbortController();
    const timeoutMs = Number(process.env.AI_HTTP_TIMEOUT_MS || 15000);
    const timeout = setTimeout(() => controller.abort(), timeoutMs);

    let res: Response;
    try {
      res = await fetch('https://api.openai.com/v1/chat/completions', {
        method: 'POST',
        signal: controller.signal,
        headers: {
          Authorization: `Bearer ${process.env.OPENAI_API_KEY}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          model,
          temperature: 0,
          max_tokens: 500,
          response_format: { type: 'json_object' },
          messages: [
            { role: 'system', content: system },
            { role: 'user', content: JSON.stringify(user) },
          ],
        }),
      });
    } catch (err: any) {
      if (err?.name === 'AbortError') throw new GatewayTimeoutException('AI request timed out.');
      throw new BadGatewayException('AI request failed.');
    } finally {
      clearTimeout(timeout);
    }

    if (!res.ok) throw new BadGatewayException(`AI request failed (${res.status}).`);

    const json = (await res.json()) as any;
    const content = json?.choices?.[0]?.message?.content;
    if (!content || typeof content !== 'string') throw new BadGatewayException('AI response missing content.');

    let parsed: any;
    try {
      parsed = JSON.parse(content);
    } catch {
      throw new BadGatewayException('AI response was not valid JSON.');
    }

    const suggestionsRaw = parsed?.suggestions && typeof parsed.suggestions === 'object' ? parsed.suggestions : {};

    const suggestions: Record<string, string | null> = {};
    for (const key of canonicalFields) {
      const value = suggestionsRaw[key];
      if (value === null || value === undefined || value === '') {
        suggestions[key] = null;
        continue;
      }
      const str = String(value);
      suggestions[key] = headerSet.has(str) ? str : null;
    }

    return suggestions;
  }

  async enrichContactsCandidate(input: EnrichContactsCandidateInput): Promise<any> {
    if (!this.isEnabled()) throw new ServiceUnavailableException('AI is not enabled.');

    const model = process.env.OPENAI_MODEL || 'gpt-4o-mini';

    const system = [
      'You are a data ingestion assistant.',
      'Goal: suggest values ONLY for missing fields on a single contact candidate.',
      'Return ONLY strict JSON (no markdown, no commentary).',
      'Never overwrite an existing non-empty field.',
      'Do not invent emails or phone numbers.',
    ].join(' ');

    const user = {
      orgId: input.orgId,
      profile: input.profile,
      candidate: input.candidate,
      deterministicSuggestions: input.deterministicSuggestions,
      outputSchema: {
        suggestions: {
          person: { displayName: 'string|null' },
          companyName: 'string|null',
        },
      },
      rules: [
        'Return { "suggestions": { "person": { ... }, "companyName": ... } }.',
        'Only propose values when the candidate field is empty.',
        'If unsure, return null.',
        'You may reformat a deterministic suggestion (e.g., better capitalization), but do not add new PII.',
      ],
    };

    const controller = new AbortController();
    const timeoutMs = Number(process.env.AI_HTTP_TIMEOUT_MS || 15000);
    const timeout = setTimeout(() => controller.abort(), timeoutMs);

    let res: Response;
    try {
      res = await fetch('https://api.openai.com/v1/chat/completions', {
        method: 'POST',
        signal: controller.signal,
        headers: {
          Authorization: `Bearer ${process.env.OPENAI_API_KEY}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          model,
          temperature: 0,
          max_tokens: 400,
          response_format: { type: 'json_object' },
          messages: [
            { role: 'system', content: system },
            { role: 'user', content: JSON.stringify(user) },
          ],
        }),
      });
    } catch (err: any) {
      if (err?.name === 'AbortError') throw new GatewayTimeoutException('AI request timed out.');
      throw new BadGatewayException('AI request failed.');
    } finally {
      clearTimeout(timeout);
    }

    if (!res.ok) throw new BadGatewayException(`AI request failed (${res.status}).`);

    const json = (await res.json()) as any;
    const content = json?.choices?.[0]?.message?.content;
    if (!content || typeof content !== 'string') throw new BadGatewayException('AI response missing content.');

    let parsed: any;
    try {
      parsed = JSON.parse(content);
    } catch {
      throw new BadGatewayException('AI response was not valid JSON.');
    }

    const candidatePerson = input.candidate?.person || {};
    const candidateCompanyName = typeof input.candidate?.companyName === 'string' ? input.candidate.companyName : '';

    const raw = parsed?.suggestions && typeof parsed.suggestions === 'object' ? parsed.suggestions : {};
    const personRaw = raw.person && typeof raw.person === 'object' ? raw.person : {};

    const suggestions: any = { person: {} as any };

    if (!candidatePerson?.displayName) {
      const displayName = typeof personRaw.displayName === 'string' ? personRaw.displayName.trim() : '';
      if (displayName) suggestions.person.displayName = displayName;
    }

    if (!candidateCompanyName) {
      const companyName = typeof raw.companyName === 'string' ? raw.companyName.trim() : '';
      if (companyName) suggestions.companyName = companyName;
    }

    return suggestions;
  }

  async parseContactsRow(input: ParseContactsRowInput): Promise<IngestionContactCandidate[]> {
    if (!this.isEnabled()) throw new ServiceUnavailableException('AI is not enabled.');

    const model = process.env.OPENAI_MODEL || 'gpt-4o-mini';

    const system = [
      'You are a data ingestion assistant.',
      'Goal: parse one spreadsheet row into 1..N contact candidates.',
      'Return ONLY strict JSON.',
      'Never include commentary.',
    ].join(' ');

    const user = {
      orgId: input.orgId,
      profile: input.profile,
      cells: input.cells,
      outputSchema: {
        candidates: [
          {
            splitIndex: 'number (0..n-1)',
            extractedFrom: 'string[]',
            method: '"ai"',
            confidence: 'number (0..1)',
            person: { displayName: 'string?', emails: 'string[]?', phones: 'string[]?' },
            issues: 'string[] (keep empty if none)',
          },
        ],
      },
      rules: [
        'If you infer more than 1 contact, split into multiple candidates.',
        'If a candidate lacks unique identifiers (email/phone), add issue "missing_identifier".',
        'If name is missing, add issue "missing_display_name".',
        'Do not invent emails or phone numbers.',
      ],
    };

    const controller = new AbortController();
    const timeoutMs = Number(process.env.AI_HTTP_TIMEOUT_MS || 15000);
    const timeout = setTimeout(() => controller.abort(), timeoutMs);

    let res: Response;
    try {
      res = await fetch('https://api.openai.com/v1/chat/completions', {
        method: 'POST',
        signal: controller.signal,
        headers: {
          Authorization: `Bearer ${process.env.OPENAI_API_KEY}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          model,
          temperature: 0,
          max_tokens: 800,
          response_format: { type: 'json_object' },
          messages: [
            { role: 'system', content: system },
            { role: 'user', content: JSON.stringify(user) },
          ],
        }),
      });
    } catch (err: any) {
      if (err?.name === 'AbortError') throw new GatewayTimeoutException('AI request timed out.');
      throw new BadGatewayException('AI request failed.');
    } finally {
      clearTimeout(timeout);
    }

    if (!res.ok) {
      throw new BadGatewayException(`AI request failed (${res.status}).`);
    }

    const json = (await res.json()) as any;
    const content = json?.choices?.[0]?.message?.content;
    if (!content || typeof content !== 'string') throw new BadGatewayException('AI response missing content.');

    let parsed: any;
    try {
      parsed = JSON.parse(content);
    } catch {
      throw new BadGatewayException('AI response was not valid JSON.');
    }

    const allowedIssues = new Set<string>(Object.values(IngestionCandidateIssueCode));
    const candidates = Array.isArray(parsed?.candidates) ? parsed.candidates : [];
    const maxCandidates = 20;
    return candidates.slice(0, maxCandidates).map((c: any, idx: number) => ({
      splitIndex: typeof c?.splitIndex === 'number' ? c.splitIndex : idx,
      extractedFrom: Array.isArray(c?.extractedFrom) ? c.extractedFrom.map(String) : ['ai'],
      method: 'ai' as const,
      confidence:
        typeof c?.confidence === 'number' ? Math.max(0, Math.min(1, c.confidence)) : 0.5,
      person: {
        displayName: typeof c?.person?.displayName === 'string' ? c.person.displayName : undefined,
        emails: Array.isArray(c?.person?.emails)
          ? c.person.emails.map((e: any) => String(e).trim().toLowerCase()).filter(Boolean)
          : undefined,
        phones: Array.isArray(c?.person?.phones)
          ? c.person.phones.map((p: any) => String(p).trim()).filter(Boolean)
          : undefined,
      },
      issues: Array.isArray(c?.issues)
        ? c.issues
            .map((i: any) => String(i))
            .filter((i: string) => allowedIssues.has(i)) as IngestionCandidateIssueCode[]
        : [],
    }));
  }

  async extractCostCodesFromWorkbook(input: {
    orgId?: string;
    workbook: { sheets: Array<{ name: string; rows: string[][] }> };
  }): Promise<Array<{ category: string; code: string; description: string }>> {
    if (!this.isEnabled()) throw new ServiceUnavailableException('AI is not enabled.');

    const model = process.env.OPENAI_MODEL || 'gpt-4o-mini';

    const system = [
      'You are a JSON extraction assistant.',
      'Given a workbook with sheet names and row arrays (including headers), extract cost codes.',
      'Return ONLY strict JSON (no markdown, no commentary).',
      'Output must be a JSON object with a "codes" array.',
    ].join(' ');

    const user = {
      orgId: input.orgId,
      workbook: input.workbook,
      outputSchema: {
        codes: [{ category: 'string', code: 'string', description: 'string' }],
      },
      rules: [
        'Extract rows that represent cost codes.',
        'Each entry must include category, code, description.',
        'If a value is missing, return an empty string for that field.',
      ],
    };

    const controller = new AbortController();
    const timeoutMs = Number(process.env.AI_HTTP_TIMEOUT_MS || 15000);
    const timeout = setTimeout(() => controller.abort(), timeoutMs);

    let res: Response;
    try {
      res = await fetch('https://api.openai.com/v1/chat/completions', {
        method: 'POST',
        signal: controller.signal,
        headers: {
          Authorization: `Bearer ${process.env.OPENAI_API_KEY}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          model,
          temperature: 0,
          max_tokens: 4000,
          response_format: { type: 'json_object' },
          messages: [
            { role: 'system', content: system },
            { role: 'user', content: JSON.stringify(user) },
          ],
        }),
      });
    } catch (err: any) {
      if (err?.name === 'AbortError') throw new GatewayTimeoutException('AI request timed out.');
      throw new BadGatewayException('AI request failed.');
    } finally {
      clearTimeout(timeout);
    }

    if (!res.ok) throw new BadGatewayException(`AI request failed (${res.status}).`);

    const json = (await res.json()) as any;
    const content = json?.choices?.[0]?.message?.content;
    if (!content || typeof content !== 'string') throw new BadGatewayException('AI response missing content.');

    let parsed: any;
    try {
      parsed = JSON.parse(content);
    } catch {
      throw new BadGatewayException('AI response was not valid JSON.');
    }

    const rawCodes = Array.isArray(parsed?.codes)
      ? parsed.codes
      : Array.isArray(parsed)
        ? parsed
        : Array.isArray(parsed?.data)
          ? parsed.data
          : [];

    return rawCodes.map((row: any) => ({
      category: typeof row?.category === 'string' ? row.category : '',
      code: typeof row?.code === 'string' ? row.code : '',
      description: typeof row?.description === 'string' ? row.description : '',
    }));
  }
}
