const DB_NAME = 'tutor-inteligente-offline'
const DB_VERSION = 1

function openDb() {
  return new Promise((resolve, reject) => {
    const req = indexedDB.open(DB_NAME, DB_VERSION)
    req.onupgradeneeded = () => {
      const db = req.result
      if (!db.objectStoreNames.contains('attempt-queue')) db.createObjectStore('attempt-queue', { keyPath: 'clientAttemptId' })
      if (!db.objectStoreNames.contains('exercise-cache')) { const store = db.createObjectStore('exercise-cache', { keyPath: 'id' }); store.createIndex('topicId', 'topicId') }
      if (!db.objectStoreNames.contains('sync-history')) db.createObjectStore('sync-history', { keyPath: 'id', autoIncrement: true })
      if (!db.objectStoreNames.contains('profile-cache')) db.createObjectStore('profile-cache', { keyPath: 'key' })
    }
    req.onsuccess = () => resolve(req.result); req.onerror = () => reject(req.error)
  })
}

async function transaction(storeName, mode, action) {
  const db = await openDb()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(storeName, mode); const store = tx.objectStore(storeName); const req = action(store)
    req.onsuccess = () => resolve(req.result); req.onerror = () => reject(req.error); tx.oncomplete = () => db.close()
  })
}

export const queueAttempt = async (attempt) => { await transaction('attempt-queue', 'readwrite', (s) => s.put(attempt)); window.dispatchEvent(new Event('tutor:queue-change')) }
export const pendingAttempts = () => transaction('attempt-queue', 'readonly', (s) => s.getAll())
export const pendingCount = () => transaction('attempt-queue', 'readonly', (s) => s.count())
export const cacheExercise = async (exercise) => { const result = await transaction('exercise-cache', 'readwrite', (s) => s.put({ ...exercise, cachedAt: new Date().toISOString() })); window.dispatchEvent(new Event('tutor:cache-change')); return result }
export async function cacheExercises(exercises) {
  const db = await openDb()
  await new Promise((resolve, reject) => {
    const tx = db.transaction('exercise-cache', 'readwrite'); const store = tx.objectStore('exercise-cache'); const cachedAt = new Date().toISOString()
    exercises.forEach((exercise) => store.put({ ...exercise, cachedAt })); tx.oncomplete = resolve; tx.onerror = () => reject(tx.error)
  })
  db.close(); window.dispatchEvent(new Event('tutor:cache-change'))
}
export const allCachedExercises = () => transaction('exercise-cache', 'readonly', (s) => s.getAll())
export const cachedExercises = (topicId) => new Promise(async (resolve, reject) => {
  const db = await openDb(); const tx = db.transaction('exercise-cache', 'readonly'); const req = tx.objectStore('exercise-cache').index('topicId').getAll(Number(topicId))
  req.onsuccess = () => { resolve(req.result); db.close() }; req.onerror = () => reject(req.error)
})
export const syncHistory = () => transaction('sync-history', 'readonly', (s) => s.getAll())
export const addSyncHistory = (entry) => transaction('sync-history', 'readwrite', (s) => s.add(entry))
export const cacheProfile = (profile) => transaction('profile-cache', 'readwrite', (s) => s.put({ key: 'student', profile, cachedAt: new Date().toISOString() }))
export const getCachedProfile = async () => (await transaction('profile-cache', 'readonly', (s) => s.get('student')))?.profile

export async function removeConfirmed(ids) {
  const db = await openDb()
  await new Promise((resolve, reject) => {
    const tx = db.transaction('attempt-queue', 'readwrite'); const store = tx.objectStore('attempt-queue'); ids.forEach((id) => store.delete(id))
    tx.oncomplete = resolve; tx.onerror = () => reject(tx.error)
  })
  db.close(); window.dispatchEvent(new Event('tutor:queue-change'))
}
