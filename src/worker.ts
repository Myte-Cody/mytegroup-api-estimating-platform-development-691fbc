import { eventsQueueScheduler, startEventsWorker } from './queues/example.queue'
import { costCodeImportScheduler, startCostCodeImportWorker } from './queues/cost-code-import.queue'

async function bootstrap() {
  // initialize scheduler
  await Promise.all([eventsQueueScheduler.waitUntilReady(), costCodeImportScheduler.waitUntilReady()])

  const worker = startEventsWorker()
  worker.on('completed', (job) => {
    // eslint-disable-next-line no-console
    console.log(`[worker] job completed ${job.id}`)
  })
  worker.on('failed', (job, err) => {
    // eslint-disable-next-line no-console
    console.error(`[worker] job failed ${job?.id}:`, err?.message || err)
  })

  const importWorker = startCostCodeImportWorker()
  importWorker.on('completed', (job) => {
    // eslint-disable-next-line no-console
    console.log(`[worker] cost code import completed ${job.id}`)
  })
  importWorker.on('failed', (job, err) => {
    // eslint-disable-next-line no-console
    console.error(`[worker] cost code import failed ${job?.id}:`, err?.message || err)
  })

  // eslint-disable-next-line no-console
  console.log('[worker] started')
}

bootstrap().catch((err) => {
  // eslint-disable-next-line no-console
  console.error('[worker] bootstrap error', err)
  process.exit(1)
})
