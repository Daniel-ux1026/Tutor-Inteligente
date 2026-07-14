const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api'
let auth = { getSession: () => null, setSession: () => {}, logout: () => {} }

export function configureAuth(handlers) { auth = handlers }

async function request(path, options = {}, retried = false) {
  const session = auth.getSession()
  const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) }
  if (session?.accessToken) headers.Authorization = `Bearer ${session.accessToken}`
  let response
  try {
    response = await fetch(`${API_URL}${path}`, { ...options, headers })
  } catch (error) {
    const offline = new Error('No se pudo conectar con el servidor.')
    offline.code = 'NETWORK_ERROR'; offline.cause = error; throw offline
  }
  if (response.status === 401 && !retried && session?.refreshToken && !path.startsWith('/auth/')) {
    const refreshed = await fetch(`${API_URL}/auth/refresh`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ refreshToken: session.refreshToken }) })
    if (refreshed.ok) { const next = await refreshed.json(); auth.setSession(next); return request(path, options, true) }
    auth.logout()
  }
  const contentType = response.headers.get('content-type') || ''
  const body = contentType.includes('json') ? await response.json() : await response.text()
  if (!response.ok) { const error = new Error(body?.message || body?.detail || 'La operación no pudo completarse.'); error.status = response.status; error.details = body?.details; throw error }
  return body
}

export const api = {
  get: (path) => request(path),
  post: (path, body) => request(path, { method: 'POST', body: JSON.stringify(body) }),
  put: (path, body) => request(path, { method: 'PUT', body: body === undefined ? undefined : JSON.stringify(body) }),
  patch: (path, body) => request(path, { method: 'PATCH', body: JSON.stringify(body) }),
}

export { API_URL }
