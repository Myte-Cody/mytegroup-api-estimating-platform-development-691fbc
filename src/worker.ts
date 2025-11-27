import { eventsQueueScheduler, startEventsWorker } from './queues/example.queue'

async function bootstrap() {
  // initialize scheduler
  await eventsQueueScheduler.waitUntilReady()

  const worker = startEventsWorker()
  worker.on('completed', (job) => {
    // eslint-disable-next-line no-console
    console.log(`[worker] job completed ${job.id}`)
  })
  worker.on('failed', (job, err) => {
    // eslint-disable-next-line no-console
    console.error(`[worker] job failed ${job?.id}:`, err?.message || err)
  })

  // eslint-disable-next-line no-console
  console.log('[worker] started')
}

bootstrap().catch((err) => {
  // eslint-disable-next-line no-console
  console.error('[worker] bootstrap error', err)
  process.exit(1)
})
