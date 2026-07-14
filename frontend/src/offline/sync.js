import { api } from '../api/client'
import { addSyncHistory, pendingAttempts, removeConfirmed } from './db'

let running = false
export async function synchronizeQueue() {
  if (running || !navigator.onLine) return { skipped: true }
  const attempts = await pendingAttempts()
  if (!attempts.length) return { confirmed: 0 }
  running = true
  try {
    const clientBatchId = crypto.randomUUID()
    const result = await api.post('/student/attempts/sync', { clientBatchId, attempts })
    await removeConfirmed(result.confirmedClientAttemptIds)
    await addSyncHistory({ at: result.synchronizedAt, clientBatchId, confirmed: result.confirmedClientAttemptIds.length, duplicates: result.duplicatedClientAttemptIds.length, status: 'CONFIRMED' })
    window.dispatchEvent(new CustomEvent('tutor:sync-complete', { detail: result }))
    return { confirmed: result.confirmedClientAttemptIds.length }
  } catch (error) {
    await addSyncHistory({ at: new Date().toISOString(), confirmed: 0, status: 'FAILED', message: error.message })
    throw error
  } finally { running = false }
}

export function enableAutomaticSync() {
  const handler = () => synchronizeQueue().catch(() => {})
  window.addEventListener('online', handler); if (navigator.onLine) handler()
  return () => window.removeEventListener('online', handler)
}
