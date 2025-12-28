import test from 'node:test'
import assert from 'node:assert/strict'
import { waitlistConfig } from '../src/features/waitlist/waitlist.config'

test('waitlist auto-invite cadence matches business-hours batching', () => {
  assert.equal(waitlistConfig.invite.delayHours, 36)
  assert.equal(waitlistConfig.invite.intervalMs, 10 * 1000)
  assert.equal(waitlistConfig.invite.batchLimit, 40)
  assert.equal(waitlistConfig.invite.autoInviteEnabled, true)
})
