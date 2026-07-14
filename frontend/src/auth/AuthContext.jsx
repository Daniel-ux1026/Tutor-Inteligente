import { createContext, useContext, useEffect, useMemo, useState } from 'react'
import { api, configureAuth } from '../api/client'

const AuthContext = createContext(null)
const STORAGE_KEY = 'tutor.auth.session'
const readSession = () => { try { return JSON.parse(sessionStorage.getItem(STORAGE_KEY)) } catch { return null } }

export function AuthProvider({ children }) {
  const [session, setSessionState] = useState(readSession)
  const setSession = (next) => { const normalized = { ...next, user: next.user || session?.user }; sessionStorage.setItem(STORAGE_KEY, JSON.stringify(normalized)); setSessionState(normalized) }
  const logout = () => { sessionStorage.removeItem(STORAGE_KEY); setSessionState(null) }
  useEffect(() => configureAuth({ getSession: readSession, setSession, logout }), [session])
  const value = useMemo(() => ({
    session, user: session?.user,
    login: async (credentials) => { const next = await api.post('/auth/login', credentials); setSession(next); return next },
    register: async (data) => { const next = await api.post('/auth/register', data); setSession(next); return next },
    recovery: (data) => api.post('/auth/recovery', data),
    logout,
  }), [session])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export const useAuth = () => useContext(AuthContext)
