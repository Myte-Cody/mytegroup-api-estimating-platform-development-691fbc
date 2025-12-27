export const normalizeHeaderKey = (value: string) => {
  return (value || '')
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9]/g, '');
};

export const uniq = <T>(items: T[]) => Array.from(new Set(items));

