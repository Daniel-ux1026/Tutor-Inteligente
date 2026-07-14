import { Navigate, Route, Routes } from 'react-router-dom'
import { useAuth } from './auth/AuthContext'
import AppShell from './components/AppShell'
import ProtectedRoute from './components/ProtectedRoute'
import { LoginPage, RegisterPage } from './pages/AuthPages'
import { DiagnosticPage, OfflinePage, PracticePage, StudentDashboard, StudentHistoryPage, StudentNotificationsPage, StudentProfilePage, StudentProgressPage } from './pages/student/StudentPages'
import StudentTeacherProgressPage from './pages/teacher/StudentProgressPage'
import { ActivitiesPage, AlertsPage, NotificationsPage, QuestionsPage, ReportsPage, StudentsPage, TeacherDashboard, TeacherSettingsPage } from './pages/teacher/TeacherPages'

function HomeRedirect(){const{user}=useAuth();return <Navigate to={!user?'/ingresar':user.role==='TEACHER'?'/docente':'/alumno'} replace/>}

export default function App(){return <Routes><Route path="/" element={<HomeRedirect/>}/><Route path="/ingresar" element={<LoginPage/>}/><Route path="/registro" element={<RegisterPage/>}/><Route element={<ProtectedRoute role="STUDENT"/>}><Route element={<AppShell/>}><Route path="/alumno" element={<StudentDashboard/>}/><Route path="/alumno/diagnostico" element={<DiagnosticPage/>}/><Route path="/alumno/practica" element={<PracticePage/>}/><Route path="/alumno/progreso" element={<StudentProgressPage/>}/><Route path="/alumno/notificaciones" element={<StudentNotificationsPage/>}/><Route path="/alumno/historial" element={<StudentHistoryPage/>}/><Route path="/alumno/offline" element={<OfflinePage/>}/><Route path="/alumno/perfil" element={<StudentProfilePage/>}/></Route></Route><Route element={<ProtectedRoute role="TEACHER"/>}><Route element={<AppShell/>}><Route path="/docente" element={<TeacherDashboard/>}/><Route path="/docente/alumnos" element={<StudentsPage/>}/><Route path="/docente/alumnos/:id/avance" element={<StudentTeacherProgressPage/>}/><Route path="/docente/alertas" element={<AlertsPage/>}/><Route path="/docente/notificaciones" element={<NotificationsPage/>}/><Route path="/docente/preguntas" element={<QuestionsPage/>}/><Route path="/docente/actividades" element={<ActivitiesPage/>}/><Route path="/docente/reportes" element={<ReportsPage/>}/><Route path="/docente/configuracion" element={<TeacherSettingsPage/>}/></Route></Route><Route path="*" element={<HomeRedirect/>}/></Routes>}
