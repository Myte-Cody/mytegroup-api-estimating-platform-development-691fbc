/**
 * Enqueue test jobs for waitlist-mail queue:
 *  - one normal job
 *  - one forced failure (lands in DLQ)
 *
 * Usage:
 *   REDIS_URL=redis://localhost:6379 BULLMQ_PREFIX=myte node scripts/queue-test.js
 */
const { Queue } = require('bullmq')

const url = process.env.REDIS_URL || 'redis://localhost:6379'
const prefix = process.env.BULLMQ_PREFIX || 'myte'

async function main() {
  const queue = new Queue('waitlist-mail', { connection: { url }, prefix })

  await queue.add(
    'verify',
    { type: 'verify', email: 'ops-test@myte.com', code: '000000', name: 'Ops Test' },
    { removeOnComplete: true, removeOnFail: true, attempts: 3, backoff: { type: 'exponential', delay: 2000 } }
  )
  await queue.add(
    'verify',
    { type: 'verify', email: 'ops-fail@myte.com', code: 'FAILME', name: 'Ops Fail', forceFail: true },
    { removeOnComplete: true, removeOnFail: true, attempts: 3, backoff: { type: 'exponential', delay: 2000 } }
  )
  console.log('Enqueued test jobs (one normal, one forced fail)')
  await queue.close()
}

main().catch((err) => {
  console.error(err)
  process.exit(1)
})
