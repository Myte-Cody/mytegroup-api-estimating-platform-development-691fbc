export function normalizeEmail(value: string) {
  return (value || '').trim().toLowerCase();
}

export function normalizePhoneE164(value: string) {
  const raw = String(value || '').trim();
  if (!raw) return null;

  const cleaned = raw.replace(/[\s().-]/g, '');

  if (cleaned.startsWith('+')) {
    const digits = cleaned.slice(1);
    if (!/^[1-9]\d{1,14}$/.test(digits)) return null;
    return `+${digits}`;
  }

  const digitsOnly = cleaned.replace(/\D/g, '');
  if (digitsOnly.length === 10) {
    return `+1${digitsOnly}`;
  }
  if (digitsOnly.length === 11 && digitsOnly.startsWith('1')) {
    return `+${digitsOnly}`;
  }
  return null;
}

export function normalizeName(value: string) {
  return (value || '')
    .trim()
    .toLowerCase()
    .replace(/\s+/g, ' ');
}

export function normalizeKey(value: string) {
  return (value || '')
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9_]+/g, '_')
    .replace(/^_+|_+$/g, '')
    .replace(/_+/g, '_');
}

export function normalizeKeys(values?: string[]) {
  const seen = new Set<string>();
  const output: string[] = [];
  (values || []).forEach((value) => {
    const key = normalizeKey(String(value || ''));
    if (!key) return;
    if (seen.has(key)) return;
    seen.add(key);
    output.push(key);
  });
  return output;
}
