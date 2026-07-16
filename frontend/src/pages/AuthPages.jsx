import { ArrowRight, BookOpenCheck, BrainCircuit, CloudOff, GraduationCap, KeyRound, LockKeyhole, Mail, UserRound } from 'lucide-react'
import { useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { StatusMessage } from '../components/UI'

const DEMO_MODE = import.meta.env.DEV || import.meta.env.VITE_DEMO_MODE === 'true'

function AuthLayout({ children, title, description }) {
  return <div className="auth-page">
    <section className="auth-visual">
      <div className="auth-brand"><span className="brand-mark">TI</span><strong>Tutor Inteligente</strong></div>
      <div className="auth-pitch">
        <p className="eyebrow light">Aprendizaje que se adapta</p>
        <h1>Avanza incluso cuando internet no acompaña.</h1>
        <p>Práctica explicable de Matemática y Comunicación, seguimiento docente y continuidad offline.</p>
        <div className="auth-benefits">
          <span><BrainCircuit/>Nivel recomendado por IA</span>
          <span><CloudOff/>Respuestas sin conexión</span>
          <span><GraduationCap/>Intervención docente oportuna</span>
        </div>
      </div>
    </section>
    <section className="auth-card">
      <div className="auth-card-head"><BookOpenCheck/><div><h2>{title}</h2><p>{description}</p></div></div>
      {children}
    </section>
  </div>
}

export function LoginPage() {
  const { user, login, recovery } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState(() => DEMO_MODE
    ? { email: 'alumno@demo.pe', password: '1234' }
    : { email: '', password: '' })
  const [message, setMessage] = useState('')
  const [tone, setTone] = useState('error')
  const [loading, setLoading] = useState(false)
  const [recovering, setRecovering] = useState(false)

  if (user) return <Navigate to={user.role === 'TEACHER' ? '/docente' : '/alumno'} replace />

  const submit = async (event) => {
    event.preventDefault()
    setLoading(true)
    setMessage('')
    try {
      const result = await login(form)
      navigate(result.user.role === 'TEACHER' ? '/docente' : '/alumno')
    } catch (error) {
      setTone('error')
      setMessage(error.message)
    } finally {
      setLoading(false)
    }
  }

  const recover = async () => {
    if (!form.email) {
      setTone('error')
      return setMessage('Ingresa tu correo primero.')
    }
    setRecovering(true)
    try {
      const result = await recovery({ email: form.email })
      setTone('success')
      setMessage(result.demoCode ? `${result.message} Código local: ${result.demoCode}` : result.message)
    } catch (error) {
      setTone('error')
      setMessage(error.message)
    } finally {
      setRecovering(false)
    }
  }

  return <AuthLayout title="Bienvenido de nuevo" description="Ingresa con tu cuenta de alumno o docente.">
    <form className="stack-form" onSubmit={submit}>
      <label>Correo institucional<div className="input-icon"><Mail/><input type="email" required autoComplete="email" value={form.email} onChange={(event) => setForm({ ...form, email: event.target.value })}/></div></label>
      <label>Contraseña<div className="input-icon"><LockKeyhole/><input type="password" required minLength="4" autoComplete="current-password" value={form.password} onChange={(event) => setForm({ ...form, password: event.target.value })}/></div></label>
      <StatusMessage tone={tone}>{message}</StatusMessage>
      <button className="primary-button" disabled={loading}>{loading ? 'Verificando…' : <>Ingresar <ArrowRight size={18}/></>}</button>
      <button type="button" className="text-button centered" disabled={recovering} onClick={recover}>{recovering ? 'Procesando…' : 'Olvidé mi contraseña'}</button>
      <p className="auth-switch">¿Aún no tienes cuenta? <Link to="/registro">Regístrate</Link></p>
    </form>
    {DEMO_MODE && <div className="demo-credentials">
      <strong>Cuentas de demostración</strong>
      <button onClick={() => setForm({ email: 'alumno@demo.pe', password: '1234' })}>Alumno · alumno@demo.pe</button>
      <button onClick={() => setForm({ email: 'docente@demo.pe', password: '1234' })}>Docente · docente@demo.pe</button>
      <small>Contraseña: 1234</small>
    </div>}
  </AuthLayout>
}

export function RegisterPage() {
  const { user, register } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({
    fullName: '',
    email: '',
    password: '',
    role: 'STUDENT',
    grade: 1,
    section: 'A',
    teacherInvitationCode: '',
  })
  const [message, setMessage] = useState('')
  const [loading, setLoading] = useState(false)

  if (user) return <Navigate to={user.role === 'TEACHER' ? '/docente' : '/alumno'} replace />

  const submit = async (event) => {
    event.preventDefault()
    setLoading(true)
    setMessage('')
    try {
      const result = await register({
        ...form,
        grade: form.role === 'STUDENT' ? Number(form.grade) : null,
        section: form.role === 'STUDENT' ? form.section : null,
        teacherInvitationCode: form.role === 'TEACHER' ? form.teacherInvitationCode : null,
      })
      navigate(result.user.role === 'TEACHER' ? '/docente' : '/alumno')
    } catch (error) {
      setMessage(error.message)
    } finally {
      setLoading(false)
    }
  }

  return <AuthLayout title="Crea tu cuenta" description="Configura el acceso según tu rol.">
    <form className="stack-form" onSubmit={submit}>
      <div className="segmented">
        <button type="button" className={form.role === 'STUDENT' ? 'active' : ''} onClick={() => setForm({ ...form, role: 'STUDENT' })}>Alumno</button>
        <button type="button" className={form.role === 'TEACHER' ? 'active' : ''} onClick={() => setForm({ ...form, role: 'TEACHER' })}>Docente</button>
      </div>
      <label>Nombre completo<div className="input-icon"><UserRound/><input required maxLength="160" autoComplete="name" value={form.fullName} onChange={(event) => setForm({ ...form, fullName: event.target.value })}/></div></label>
      <label>Correo<div className="input-icon"><Mail/><input type="email" required autoComplete="email" value={form.email} onChange={(event) => setForm({ ...form, email: event.target.value })}/></div></label>
      <label>Contraseña<div className="input-icon"><LockKeyhole/><input type="password" minLength="8" maxLength="72" required autoComplete="new-password" value={form.password} onChange={(event) => setForm({ ...form, password: event.target.value })}/></div><small>Usa al menos 8 caracteres.</small></label>
      {form.role === 'STUDENT'
        ? <div className="form-grid">
            <label>Grado<select value={form.grade} onChange={(event) => setForm({ ...form, grade: event.target.value })}>{[1, 2, 3, 4, 5].map((number) => <option key={number} value={number}>{number}.° secundaria</option>)}</select></label>
            <label>Sección<input maxLength="10" value={form.section} onChange={(event) => setForm({ ...form, section: event.target.value.toUpperCase() })}/></label>
          </div>
        : <label>Código de invitación docente<div className="input-icon"><KeyRound/><input type="password" required maxLength="128" autoComplete="off" value={form.teacherInvitationCode} onChange={(event) => setForm({ ...form, teacherInvitationCode: event.target.value })}/></div><small>Solicítalo al responsable del sistema.</small></label>}
      <StatusMessage tone="error">{message}</StatusMessage>
      <button className="primary-button" disabled={loading}>{loading ? 'Creando cuenta…' : 'Crear cuenta'}</button>
      <p className="auth-switch">¿Ya tienes cuenta? <Link to="/ingresar">Ingresa aquí</Link></p>
    </form>
  </AuthLayout>
}
