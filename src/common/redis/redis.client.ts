import Redis from 'ioredis'
import { redisConfig } from '../../config/redis.config'
import { createClient, RedisClientType } from 'redis'

const redisUrl = redisConfig().url

export const createRedisClient = (label = 'default') => {
  const client = new Redis(redisUrl, {
    maxRetriesPerRequest: null,
    lazyConnect: true,
    showFriendlyErrorStack: process.env.NODE_ENV !== 'production',
  })

  client.on('error', (err) => {
    // eslint-disable-next-line no-console
    console.error(`[redis:${label}] error`, err?.message || err)
  })

  client.on('connect', () => {
    // eslint-disable-next-line no-console
    console.log(`[redis:${label}] connected`)
  })

  return client
}

export const redisConnectionOptions = {
  url: redisUrl,
  maxRetriesPerRequest: null as number | null,
}

/**
 * Dedicated Redis client for express-session store (uses node-redis in legacy mode
 * because connect-redis expects the v3-style API).
 */
export const createLegacySessionClient = async (label = 'session-store'): Promise<RedisClientType> => {
  const client = createClient({
    url: redisUrl,
  })

  client.on('error', (err) => {
    // eslint-disable-next-line no-console
    console.error(`[redis:${label}] error`, err?.message || err)
  })

  await client.connect()

  // Adapter to translate connect-redis expiration shape into node-redis v4 options
  const adapter: any = {
    set: async (key: string, value: string, opts?: any) => {
      if (opts?.expiration?.type === 'EX' && opts?.expiration?.value) {
        return client.set(key, value, { EX: opts.expiration.value })
      }
      return client.set(key, value)
    },
    get: (key: string) => client.get(key),
    del: (keys: string | string[]) => client.del(keys as any),
    expire: (key: string, ttl: number) => client.expire(key, ttl),
    quit: () => client.quit(),
    on: (...args: any[]) => client.on.apply(client, args as any),
  }

  return adapter as unknown as RedisClientType
}
