/**
 * Manage waitlist-mail DLQ.
 * Usage:
 *   node scripts/queue-dlq.js stats
 *   node scripts/queue-dlq.js requeue   (move DLQ failed back to main queue)
 *   node scripts/queue-dlq.js purge     (delete DLQ failed jobs)
 */
const { Queue } = require('bullmq')

const action = process.argv[2] || 'stats'
const url = process.env.REDIS_URL || 'redis://localhost:6379'
const prefix = process.env.BULLMQ_PREFIX || 'myte'

async function main() {
  const queue = new Queue('waitlist-mail', { connection: { url }, prefix })
  const dlq = new Queue('waitlist-mail-dlq', { connection: { url }, prefix })

  if (action === 'stats') {
    const counts = await queue.getJobCounts('waiting', 'active', 'failed', 'delayed', 'completed')
    const dlqFailed = await dlq.getFailed(0, 20)
    console.log(JSON.stringify({
      counts,
      dlqFailed: dlqFailed.map((j) => ({ id: j.id, reason: j.failedReason })),
    }, null, 2))
  } else if (action === 'requeue') {
    const dlqFailed = await dlq.getFailed(0, 1000)
    for (const job of dlqFailed) {
      await queue.add('dlq-retry', job.data, {
        removeOnComplete: true,
        removeOnFail: true,
        attempts: 3,
        backoff: { type: 'exponential', delay: 2000 },
      })
      await job.remove()
    }
    console.log(`Requeued ${dlqFailed.length} job(s) from DLQ`)
  } else if (action === 'purge') {
    const dlqFailed = await dlq.getFailed(0, 1000)
    for (const job of dlqFailed) {
      await job.remove()
    }
    console.log(`Purged ${dlqFailed.length} job(s) from DLQ`)
  } else {
    console.log('Unknown action. Use stats|requeue|purge')
  }

  await queue.close()
  await dlq.close()
}

main().catch((err) => {
  console.error(err)
  process.exit(1)
})
