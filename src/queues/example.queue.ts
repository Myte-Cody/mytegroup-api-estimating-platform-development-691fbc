import { createQueue, createQueueScheduler, createWorker } from './queue.factory'

const queueName = 'events'

export const eventsQueue = createQueue(queueName)
export const eventsQueueScheduler = createQueueScheduler(queueName)

export const enqueueEvent = async (payload: Record<string, unknown>) => {
  await eventsQueue.add('event', payload, { removeOnComplete: true, removeOnFail: 10 })
}

export const startEventsWorker = () =>
  createWorker(queueName, async (job) => {
    // Placeholder processor; extend with real work as needed
    // eslint-disable-next-line no-console
    console.log(`[worker] processing job ${job.id} in ${queueName}`, job.data)
  })
