# Trazabilidad funcional

| Funcionalidad | Pantalla | Componente / servicio | Endpoint | Tabla principal | Prueba / evidencia |
|---|---|---|---|---|---|
| Algoritmo adaptativo | Alumno → Práctica adaptativa; Docente → Reportes | `PracticePage`, `StudentService`, `AiClient`, FastAPI `/predict` | `GET /student/exercises/next`, `POST /student/attempts`, `POST /ai/recommendation` | `attempts`, `progress`, `ai_recommendations`, `questions`, `alternatives` | `ia/tests/test_model.py`, `backend/AiClientTest`, flujo A-03 |
| Trabajo sin conexión | Alumno → cualquier ruta visitada; Sin conexión y sincronización | `sw.js`, `AppShell`, `PracticePage` | Recursos estáticos; paquetes obtenidos por `/exercises/offline-pack` | IndexedDB `exercise-cache`; SQL `questions` | compilación Vite; flujo A-04 |
| Sincronización | Alumno → Sin conexión y sincronización | `db.js`, `sync.js`, `StudentService.sync` | `POST /student/attempts/sync` | IndexedDB `attempt-queue` y `sync-history`; SQL `attempts`, `sync_batches` | `frontend/sync.test.js`; restricciones únicas; flujo A-05 |
| Filtros docentes | Docente → Alumnos | `StudentsPage`, `TeacherService.students` | `GET /teacher/students` con 8 criterios | `students`, `enrollments`, `progress`, `topics`, `courses`, `alerts` | cambio reactivo de consulta; flujo A-07 |
| Paginación de alumnos | Docente → Alumnos | `StudentsPage` | Usa el resultado filtrado de `GET /teacher/students` | Sin tabla adicional | cinco filas por página, controles anterior/número/siguiente; flujo A-21 |
| Progreso | Alumno → Mi progreso; Docente → Avance individual | `StudentProgressPage`, `StudentService.progressDto`, `TeacherService.studentProgress` | `GET /student/progress`, `GET /teacher/students/{id}/progress` | `progress`, `attempts`, `ai_recommendations` | agregados calculados; flujo A-09 |
| Alertas | Docente → Alertas y Avance individual | `AlertsPage`, `StudentService.evaluateAlert`, `TeacherService.updateAlert` | `GET/PATCH /teacher/alerts` | `alerts`, `notifications`, `progress` | umbrales en servicio; resolución persistente; flujo A-10 |
| Notificaciones especiales | Campana, Notificaciones, Avance individual | `NotificationBell`, `NotificationService`, SSE | `GET /notifications`, `PUT /{id}/read`, `GET /stream` | `notifications`, `alerts`, `attempts` | ruta contextual con `studentId`, `topicId`, `attemptId`, `alertId`; flujo A-NOT |
| Recomendaciones del docente | Docente → Avance individual; Alumno → Notificaciones | `StudentProgressPage`, `StudentNotificationsPage`, `StudentService.notifications` | `POST /teacher/students/{id}/observations`, `GET /student/notifications` | `teacher_observations` | integración autenticada docente → alumno; flujo A-22 |
| Diagnóstico con pestañas | Alumno → Diagnóstico | `DiagnosticPage`, `ClipboardSummary` | `GET /student/courses`, `GET/POST /student/diagnostics` | `diagnostic_results` | dos pestañas, cuestionario y tarjeta de avance; flujo A-23 |
| Buscador de temas | Docente → Banco de preguntas | `QuestionsPage` | `GET /teacher/catalog` | `courses`, `topics` | resultados bajo demanda desde dos caracteres; flujo A-24 |
| Evidencia de aprendizaje | Docente → Reportes | `ReportsPage`, `LearningEffectivenessService` | `GET /teacher/reports/learning-effectiveness` | `users`, `students`, `attempts`, `questions`, `topics` | exclusión de cuentas demo, comparación pareada y umbral de muestra; flujo A-26 |

## Criterios implementados

- La recomendación usa las seis variables requeridas y devuelve nivel, explicación, versión y fuente.
- Cada intento offline conserva el mismo UUID en todos sus reintentos.
- Los filtros se aplican en el backend; el navegador no inventa resultados.
- El enlace contextual vive en la URL, por lo que sobrevive una recarga.
- Alertas y notificaciones continúan disponibles tras cerrar sesión.
- La eficacia observada se calcula con intentos persistidos y nunca con los registros usados para validar técnicamente el clasificador.
