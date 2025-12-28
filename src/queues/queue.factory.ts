import { Queue, QueueEvents, Worker, type WorkerOptions } from 'bullmq'
import { redisConnectionOptions } from '../common/redis/redis.client'
import { redisConfig } from '../config/redis.config'

const { bullPrefix: prefix } = redisConfig()
const redisDisabled = process.env.REDIS_DISABLED === '1' || process.env.QUEUE_DISABLED === '1'

const disabledQueue = (name: string) =>
  ({
    name,
    async add() {
      throw new Error('Redis queue is disabled.')
    },
    async close() {},
  }) as unknown as Queue

const disabledQueueEvents = (name: string) =>
  ({
    name,
    async waitUntilReady() {},
    async close() {},
  }) as unknown as QueueEvents

const disabledWorker = <T = any>(name: string) => {
  const stub = {
    name,
    on() {
      return stub
    },
    async close() {},
  }
  return stub as unknown as Worker<T>
}

export const createQueue = (name: string) =>
  redisDisabled
    ? disabledQueue(name)
    : new Queue(name, {
        prefix,
        connection: redisConnectionOptions,
      })

export const createQueueScheduler = (name: string) =>
  redisDisabled
    ? disabledQueueEvents(name)
    : new QueueEvents(name, {
        prefix,
        connection: redisConnectionOptions,
      })

export const createWorker = <T = any>(
  name: string,
  processor: (job: any) => any,
  options: Partial<WorkerOptions> = {}
) =>
  redisDisabled
    ? disabledWorker<T>(name)
    : new Worker<T>(name, processor as any, {
        prefix,
        connection: redisConnectionOptions,
        ...(options || {}),
      })
