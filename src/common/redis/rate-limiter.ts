import type Redis from 'ioredis'

export type RateLimitResult = { allowed: boolean; count: number; resetMs: number }

/**
 * Minimal Redis-backed sliding window limiter using INCR + PEXPIRE (NX).
 * Keeps limits global across instances and survives process restarts.
 */
export class RedisRateLimiter {
  constructor(private readonly client: Redis, private readonly prefix = 'ratelimit') {}

  private key(key: string) {
    return `${this.prefix}:${key}`
  }

  async allow(key: string, max: number, windowMs: number): Promise<RateLimitResult> {
    const namespaced = this.key(key)
    const now = Date.now()
    const results = await this.client
      .multi()
      .incr(namespaced)
      .pexpire(namespaced, windowMs, 'NX')
      .exec()

    // results shape: [[null, count], [null, expireResult]]
    const count = Number(results?.[0]?.[1] ?? 0)
    const ttl = await this.client.pttl(namespaced)
    const resetMs = ttl > 0 ? ttl : windowMs

    return {
      allowed: count <= max,
      count,
      resetMs,
    }
  }
}
