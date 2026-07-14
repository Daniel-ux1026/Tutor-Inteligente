import { AlertTriangle, LoaderCircle } from 'lucide-react'

export function PageHeader({ eyebrow, title, description, actions }) {
  return <header className="page-header"><div><p className="eyebrow">{eyebrow}</p><h1>{title}</h1>{description && <p className="page-description">{description}</p>}</div>{actions && <div className="header-actions">{actions}</div>}</header>
}
export function MetricCard({ label, value, hint, tone = 'teal', icon: Icon }) {
  return <article className={`metric-card tone-${tone}`}>{Icon && <span className="metric-icon"><Icon size={20}/></span>}<span>{label}</span><strong>{value}</strong>{hint && <small>{hint}</small>}</article>
}
export function ProgressBar({ value = 0, label, detail }) {
  const safe = Math.max(0, Math.min(100, Number(value) || 0))
  return <div className="progress-row"><div className="progress-label"><span>{label}</span><strong>{detail ?? `${safe}%`}</strong></div><div className="progress-track" role="progressbar" aria-label={label} aria-valuemin="0" aria-valuemax="100" aria-valuenow={safe}><span style={{ width: `${safe}%` }}/></div></div>
}
export function StatusMessage({ tone = 'info', children }) { return children ? <div className={`status-message ${tone}`} role="status">{tone === 'error' && <AlertTriangle size={18}/>}<span>{children}</span></div> : null }
export function Loading({ label = 'Cargando información…' }) { return <div className="loading" role="status"><LoaderCircle className="spin"/><span>{label}</span></div> }
export function EmptyState({ title = 'Aún no hay información', description }) { return <div className="empty-state"><strong>{title}</strong>{description && <p>{description}</p>}</div> }
export const levelLabel = (value) => ({ basico: 'Básico', intermedio: 'Intermedio', avanzado: 'Avanzado' }[value] || value)
export const statusLabel = (value) => ({ PENDING: 'Pendiente', REVIEWED: 'Revisada', RESOLVED: 'Resuelta', ASSIGNED: 'Asignada', SYNCHRONIZED: 'Sincronizado', CONFIRMED: 'Confirmada', FAILED: 'No completada', AT_RISK: 'En riesgo', IN_PROGRESS: 'En progreso', COMPLETED: 'Completado' }[value] || value)
export const riskLabel = (value) => ({ LOW: 'Bajo', MEDIUM: 'Medio', HIGH: 'Alto' }[value] || value)
export const alertTypeLabel = (value) => ({ LOW_PERFORMANCE: 'Bajo rendimiento', CONSECUTIVE_ERRORS: 'Errores consecutivos', EXCESSIVE_TIME: 'Tiempo excesivo', LEVEL_DECREASE: 'Descenso de nivel', TOPIC_REPETITION: 'Repetición de tema' }[value] || value)
export const notificationTypeLabel = (value) => ({ ACTIVITY_COMPLETED: 'Actividad completada', LEVEL_CHANGED: 'Cambio de nivel', SYNC_COMPLETED: 'Sincronización completada', LOW_PERFORMANCE: 'Bajo rendimiento' }[value] || value)
export const formatDate = (value) => value ? new Intl.DateTimeFormat('es-PE', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value)) : 'Sin actividad'
