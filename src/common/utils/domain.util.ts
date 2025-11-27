export const normalizeDomainFromEmail = (email: string): string | null => {
  const parts = (email || '').trim().toLowerCase().split('@')
  if (parts.length < 2) return null
  const domain = parts[parts.length - 1].trim()
  return domain || null
}
