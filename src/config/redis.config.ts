export const redisConfig = () => {
  const url = process.env.REDIS_URL || 'redis://localhost:6379'
  const sessionTtlHours = Number(process.env.SESSION_TTL_HOURS || 12)
  const bullPrefix = process.env.BULLMQ_PREFIX || 'myte'

  return {
    url,
    sessionTtlHours,
    bullPrefix,
  }
}
