import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

export default function ProtectedRoute({ role }) {
  const { user } = useAuth()
  if (!user) return <Navigate to="/ingresar" replace />
  if (role && user.role !== role) return <Navigate to={user.role === 'TEACHER' ? '/docente' : '/alumno'} replace />
  return <Outlet />
}
