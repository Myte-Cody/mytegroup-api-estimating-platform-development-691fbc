import { normalizeEmail, normalizePhoneE164 } from '../../common/utils/normalize.util';

type RowLike = {
  row: number;
  emails?: string[];
  primaryEmail?: string;
  phones?: string[];
  primaryPhone?: string;
  ironworkerNumber?: string;
};

const derivePrimaryEmail = (emails?: string[], primaryEmail?: string) => {
  const clean = (emails || []).map((e) => normalizeEmail(e)).filter(Boolean);
  const deduped = Array.from(new Set(clean));
  if (!deduped.length) return null;
  const desired = primaryEmail ? normalizeEmail(primaryEmail) : '';
  const chosen = desired && deduped.includes(desired) ? desired : deduped[0];
  return chosen || null;
};

const derivePrimaryPhoneE164 = (phones?: string[], primaryPhone?: string) => {
  const preferred = String(primaryPhone || '').trim();
  if (preferred) return normalizePhoneE164(preferred);
  const first = Array.isArray(phones) && phones.length ? String(phones[0] || '').trim() : '';
  if (!first) return null;
  return normalizePhoneE164(first);
};

const addError = (map: Map<number, string[]>, row: number, message: string) => {
  const existing = map.get(row) || [];
  if (existing.includes(message)) return;
  map.set(row, [...existing, message]);
};

const emitGroup = (errorsByRow: Map<number, string[]>, kind: string, rows: number[]) => {
  if (rows.length < 2) return;
  const sorted = [...rows].sort((a, b) => a - b);
  const list = sorted.join(', ');
  const message = `duplicate ${kind} within file (rows ${list})`;
  sorted.forEach((row) => addError(errorsByRow, row, message));
};

export const computeWithinFileDuplicateErrors = (rows: RowLike[]) => {
  const byEmail = new Map<string, number[]>();
  const byIron = new Map<string, number[]>();
  const byPhone = new Map<string, number[]>();

  rows.forEach((row) => {
    const email = derivePrimaryEmail(row.emails, row.primaryEmail);
    if (email) {
      const list = byEmail.get(email) || [];
      list.push(row.row);
      byEmail.set(email, list);
    }

    const iron = String(row.ironworkerNumber || '').trim();
    if (iron) {
      const list = byIron.get(iron) || [];
      list.push(row.row);
      byIron.set(iron, list);
    }

    const phone = derivePrimaryPhoneE164(row.phones, row.primaryPhone);
    if (phone) {
      const list = byPhone.get(phone) || [];
      list.push(row.row);
      byPhone.set(phone, list);
    }
  });

  const errorsByRow = new Map<number, string[]>();
  Array.from(byEmail.values()).forEach((rowsList) => emitGroup(errorsByRow, 'primaryEmail', rowsList));
  Array.from(byIron.values()).forEach((rowsList) => emitGroup(errorsByRow, 'ironworkerNumber', rowsList));
  Array.from(byPhone.values()).forEach((rowsList) => emitGroup(errorsByRow, 'primaryPhone', rowsList));
  return errorsByRow;
};

