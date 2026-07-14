import { Activity, Bell, BookOpen, ChartNoAxesCombined, CircleUserRound, ClipboardCheck, CloudOff, House, LogOut, Menu, NotebookPen, PanelLeftClose, Settings, ShieldAlert, Users, Wifi } from 'lucide-react'
import { useEffect, useState } from 'react'
import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { pendingCount } from '../offline/db'
import { enableAutomaticSync } from '../offline/sync'
import NotificationBell from './NotificationBell'

const studentNav = [
  ['/alumno', 'Inicio', House], ['/alumno/diagnostico', 'Diagnóstico', ClipboardCheck], ['/alumno/practica', 'Práctica adaptativa', BookOpen], ['/alumno/progreso', 'Mi progreso', ChartNoAxesCombined], ['/alumno/notificaciones', 'Notificaciones', Bell], ['/alumno/historial', 'Historial', Activity], ['/alumno/offline', 'Sin conexión y sincronización', CloudOff], ['/alumno/perfil', 'Mi perfil', CircleUserRound],
]
const teacherNav = [
  ['/docente', 'Panel principal', House], ['/docente/alumnos', 'Alumnos', Users], ['/docente/alertas', 'Alertas', ShieldAlert], ['/docente/notificaciones', 'Notificaciones', Bell], ['/docente/preguntas', 'Banco de preguntas', NotebookPen], ['/docente/actividades', 'Actividades', ClipboardCheck], ['/docente/reportes', 'Reportes', ChartNoAxesCombined], ['/docente/configuracion', 'Aula y perfil', Settings],
]

export default function AppShell() {
  const { user, logout } = useAuth(); const navigate = useNavigate(); const [mobile, setMobile] = useState(false); const [online, setOnline] = useState(navigator.onLine); const [pending, setPending] = useState(0); const [installPrompt, setInstallPrompt] = useState(null)
  const nav = user?.role === 'TEACHER' ? teacherNav : studentNav
  useEffect(() => { const state = () => setOnline(navigator.onLine); window.addEventListener('online', state); window.addEventListener('offline', state); const stopSync = enableAutomaticSync(); return () => { window.removeEventListener('online', state); window.removeEventListener('offline', state); stopSync() } }, [])
  useEffect(() => { const update = () => pendingCount().then(setPending).catch(() => setPending(0)); update(); window.addEventListener('tutor:queue-change', update); window.addEventListener('tutor:sync-complete', update); return () => { window.removeEventListener('tutor:queue-change', update); window.removeEventListener('tutor:sync-complete', update) } }, [])
  useEffect(() => { const handler = (event) => { event.preventDefault(); setInstallPrompt(event) }; window.addEventListener('beforeinstallprompt', handler); return () => window.removeEventListener('beforeinstallprompt', handler) }, [])
  const signOut = () => { logout(); navigate('/ingresar', { replace: true }) }
  return <div className="app-shell">
    <aside className={`sidebar ${mobile ? 'open' : ''}`}><div className="brand"><span className="brand-mark">TI</span><div><strong>Tutor Inteligente</strong><small>Aprende a tu ritmo</small></div><button className="sidebar-close" aria-label="Cerrar menú" onClick={() => setMobile(false)}><PanelLeftClose/></button></div>
      <nav aria-label="Navegación principal">{nav.map(([to, label, Icon]) => <NavLink key={to} to={to} end={to === '/alumno' || to === '/docente'} onClick={() => setMobile(false)}><Icon size={19}/><span>{label}</span>{to.endsWith('/offline') && pending > 0 && <b className="nav-badge">{pending}</b>}</NavLink>)}</nav>
      <div className="sidebar-user"><span className="avatar">{user?.fullName?.split(' ').map((word) => word[0]).slice(0,2).join('')}</span><div><strong>{user?.fullName}</strong><small>{user?.role === 'TEACHER' ? 'Docente' : `${user?.grade}.° secundaria`}</small></div><button aria-label="Cerrar sesión" onClick={signOut}><LogOut size={18}/></button></div>
    </aside>
    {mobile && <button className="sidebar-backdrop" aria-label="Cerrar menú" onClick={() => setMobile(false)}/>}<div className="app-area"><header className="topbar"><button className="menu-button" aria-label="Abrir menú" onClick={() => setMobile(true)}><Menu/></button><div className={`connection-pill ${online ? 'online' : 'offline'}`}>{online ? <Wifi size={16}/> : <CloudOff size={16}/>}<span>{online ? 'En línea' : 'Sin conexión'}</span>{pending > 0 && <b>{pending} pendiente{pending === 1 ? '' : 's'}</b>}</div><div className="topbar-actions">{installPrompt && <button className="install-button" onClick={async () => { await installPrompt.prompt(); setInstallPrompt(null) }}>Instalar app</button>}{user?.role === 'TEACHER' && <NotificationBell/>}</div></header><main id="main-content" className="page-content" tabIndex="-1"><Outlet/></main></div>
  </div>
}
