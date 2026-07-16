import { Bell, ChevronRight } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { API_URL, api } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { formatDate } from './UI'

const wait = (milliseconds) => new Promise((resolve) => setTimeout(resolve, milliseconds))

function parseEventBlock(block) {
  let eventName = 'message'
  const data = []
  for (const line of block.split('\n')) {
    if (line.startsWith('event:')) eventName = line.slice(6).trim()
    if (line.startsWith('data:')) data.push(line.slice(5).trimStart())
  }
  if (!data.length) return null
  try {
    return { eventName, payload: JSON.parse(data.join('\n')) }
  } catch {
    return null
  }
}

export default function NotificationBell() {
  const { session } = useAuth()
  const navigate = useNavigate()
  const [open, setOpen] = useState(false)
  const [data, setData] = useState({ unreadCount: 0, items: [] })
  const ref = useRef(null)

  const load = () => api.get('/teacher/notifications').then(setData).catch(() => {})

  useEffect(() => {
    load()
    const close = (event) => { if (!ref.current?.contains(event.target)) setOpen(false) }
    document.addEventListener('pointerdown', close)
    return () => document.removeEventListener('pointerdown', close)
  }, [])

  useEffect(() => {
    if (!session?.accessToken) return undefined
    const controller = new AbortController()
    let active = true

    const handle = ({ eventName, payload }) => {
      if (eventName === 'notification') {
        setData((current) => ({
          unreadCount: current.unreadCount + 1,
          items: [payload, ...current.items.filter((item) => item.id !== payload.id)].slice(0, 50),
        }))
      }
      if (eventName === 'connected') {
        setData((current) => ({ ...current, unreadCount: payload.unreadCount }))
      }
    }

    const connect = async () => {
      while (active) {
        try {
          const response = await fetch(`${API_URL}/teacher/notifications/stream`, {
            headers: {
              Accept: 'text/event-stream',
              Authorization: `Bearer ${session.accessToken}`,
            },
            cache: 'no-store',
            signal: controller.signal,
          })
          if (!response.ok || !response.body) throw new Error('SSE no disponible')
          const reader = response.body.getReader()
          const decoder = new TextDecoder()
          let buffer = ''
          while (active) {
            const { value, done } = await reader.read()
            if (done) break
            buffer += decoder.decode(value, { stream: true }).replace(/\r\n/g, '\n')
            const blocks = buffer.split('\n\n')
            buffer = blocks.pop() || ''
            blocks.map(parseEventBlock).filter(Boolean).forEach(handle)
          }
        } catch (error) {
          if (error.name === 'AbortError') return
        }
        if (active) await wait(3000)
      }
    }

    connect()
    return () => {
      active = false
      controller.abort()
    }
  }, [session?.accessToken])

  const view = async (item) => {
    try {
      await api.put(`/teacher/notifications/${item.id}/read`)
    } catch {
      // La ruta mantiene el contexto aunque falle el refresco visual.
    }
    setData((current) => ({
      unreadCount: Math.max(0, current.unreadCount - (item.read ? 0 : 1)),
      items: current.items.map((notification) => notification.id === item.id ? { ...notification, read: true } : notification),
    }))
    setOpen(false)
    const query = new URLSearchParams({
      notificationId: item.id,
      topicId: item.topicId,
      ...(item.attemptId && { attemptId: item.attemptId }),
      ...(item.alertId && { alertId: item.alertId }),
    })
    navigate(`/docente/alumnos/${item.studentId}/avance?${query}`, { state: { notification: item } })
  }

  return <div className="notification-control" ref={ref}>
    <button className="icon-button" aria-label={`Notificaciones: ${data.unreadCount} sin leer`} aria-expanded={open} onClick={() => setOpen(!open)}>
      <Bell size={21}/>{data.unreadCount > 0 && <span className="notification-count">{data.unreadCount > 99 ? '99+' : data.unreadCount}</span>}
    </button>
    {open && <div className="notification-popover">
      <div className="popover-title"><strong>Notificaciones</strong><button className="text-button" onClick={() => { setOpen(false); navigate('/docente/notificaciones') }}>Ver todas</button></div>
      <div className="popover-list">
        {data.items.slice(0, 5).map((item) => <article className={`notification-item ${item.read ? '' : 'unread'}`} key={item.id}>
          <div><strong>{item.studentName}</strong><span>{item.summary}</span><small>{item.course} · {item.topic} · {formatDate(item.createdAt)}</small></div>
          <button className="mini-action" onClick={() => view(item)}>Ver avance <ChevronRight size={14}/></button>
        </article>)}
        {!data.items.length && <p className="popover-empty">No hay notificaciones recientes.</p>}
      </div>
    </div>}
  </div>
}
