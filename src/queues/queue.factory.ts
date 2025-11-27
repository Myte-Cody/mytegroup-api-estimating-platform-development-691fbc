import { Queue, QueueEvents, Worker, type WorkerOptions } from 'bullmq'
import { redisConnectionOptions } from '../common/redis/redis.client'
import { redisConfig } from '../config/redis.config'

const { bullPrefix: prefix } = redisConfig()

export const createQueue = (name: string) =>
  new Queue(name, {
    prefix,
    connection: redisConnectionOptions,
  })

export const createQueueScheduler = (name: string) =>
  new QueueEvents(name, {
    prefix,
    connection: redisConnectionOptions,
  })

export const createWorker = <T = any>(
  name: string,
  processor: (job: any) => any,
  options: Partial<WorkerOptions> = {}
) =>
  new Worker<T>(name, processor as any, {
    prefix,
    connection: redisConnectionOptions,
    ...(options || {}),
  })
