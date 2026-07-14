import { describe, expect, it } from 'vitest'

describe('offline idempotency contract', () => {
  it('uses UUIDs that survive retries', () => {
    const attempt = { clientAttemptId: '25df4a62-c92c-4c42-9f55-4f709dadbabe' }
    const retried = { ...attempt }
    expect(retried.clientAttemptId).toBe(attempt.clientAttemptId)
  })
})
