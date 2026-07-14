# Pruebas de aceptación

## Verificaciones automatizadas ejecutadas

| Área | Comando | Resultado |
|---|---|---|
| IA | `python -m unittest discover -s tests -v` | 2/2 pruebas aprobadas |
| IA | `python train_model.py` | 620 filas, 496 entrenamiento, 124 prueba; artefactos exportados |
| IA API | `GET /health`, `GET /metrics`, `POST /predict` | Respuestas HTTP reales aprobadas; modelo listo y recomendación explicada |
| Frontend | `vitest run` | 1/1 prueba aprobada |
| Frontend | `vite build` | Build de producción aprobado; 1,601 módulos transformados |
| Frontend HTTP | `GET /`, `/manifest.webmanifest`, `/sw.js` | HTTP 200; título, raíz React, manifest y service worker disponibles |
| Backend | `mvn package` | 2/2 pruebas aprobadas; 58 archivos Java compilados y JAR generado |
| Integración SQL Server 2019 | Flyway + arranque real | 5 migraciones, 20 tablas, 495 preguntas, 1980 alternativas y usuarios de demostración |
| Backend HTTP + autenticación | `/actuator/health` + `/api/auth/login` | Estado `UP` y JWT de alumno emitido correctamente |
| Recomendación docente → alumno | `POST /teacher/students/{id}/observations` + `GET /student/notifications` | La recomendación apareció con el mismo identificador y texto; el dato temporal de prueba se eliminó |

## Matriz de escenarios

| ID | Escenario | Resultado esperado / evidencia |
|---|---|---|
| A-01 | Alumno inicia sesión | Spring Security valida BCrypt y entrega JWT con rol `STUDENT`. |
| A-02 | Realiza diagnóstico | Resultado y respuestas se persisten; se inicializa progreso por tema del curso. |
| A-03 | Responde y cambia nivel | FastAPI predice con seis variables; intento, recomendación y progreso se guardan. |
| A-04 | Pierde conexión y recarga | El trabajador de servicio entrega la interfaz y `exercise-cache` aporta contenido descargado. |
| A-05 | Responde sin conexión y reconecta | El identificador único entra a IndexedDB, el lote se confirma y solo las respuestas confirmadas se eliminan. |
| A-06 | Reintenta sincronización | Restricciones únicas devuelven duplicado confirmado sin insertar un segundo intento. |
| A-07 | Docente filtra | Ocho filtros actualizan `/teacher/students` inmediatamente; el curso procede del catálogo docente. |
| A-08 | Docente recibe notificación | `NotificationService` persiste y emite evento SSE al docente asignado. |
| A-09 | Pulsa «Ver avance» | Marca leída y navega al `studentId` exacto con contexto en query string. |
| A-10 | Contexto resaltado | Clases `context-highlight` se aplican a tema, intento o alerta coincidente. |
| A-11 | Bajo rendimiento | Se genera por <60 %, 2 errores, baja de nivel, >120 s o repetición frecuente. |
| A-12 | Registra observación | `teacher_observations` persiste autor, alumno, texto y fecha. |
| A-13 | Agrega/edita pregunta | Valida cuatro alternativas y una correcta; se usa en la práctica por tema/grado/nivel. |
| A-14 | Desactiva pregunta | El selector de ejercicios usa exclusivamente `active=true`. |
| A-15 | PWA instalable | Manifest local con iconos 192/512, `display=standalone` y service worker registrado. |
| A-16 | Descarga paquete sin conexión | `/student/exercises/offline-pack` entrega nueve ejercicios por tema para el grado del alumno. |
| A-17 | Abre la biblioteca local | Los ejercicios guardados se agrupan por tema y abren la Práctica Adaptativa en modo sin conexión. |
| A-18 | Genera tema y asigna | Valida curso, alumno y datos; crea 30 ejercicios iniciales y una actividad en una sola transacción. |
| A-19 | Consulta rendimiento | El 70 % se presenta como 7 respuestas correctas de 10 intentos, sin duplicar el porcentaje. |
| A-20 | Filtra alertas | Estado, riesgo, tipo, curso y alumno se aplican en el backend. |
| A-21 | Pagina la tabla de alumnos | Muestra cinco filas por página y controles anterior, números de página y siguiente al final de la tabla. |
| A-22 | Envía una observación al alumno | La observación persistida aparece en el módulo Notificaciones del alumno autenticado. |
| A-23 | Cambia de diagnóstico | Dos pestañas sustituyen el desplegable y conservan la tarjeta lateral de avance. |
| A-24 | Busca un tema en el banco | El buscador consulta el catálogo desde dos caracteres y no despliega todos los temas. |
| A-25 | Consulta el panel principal docente | El panorama pedagógico es informativo y no contiene botones de redireccionamiento. |

## Prueba manual recomendada

Ejecuta la secuencia de dos navegadores y la demostración sin conexión descritas en el README. Comprueba consola, Red, Aplicación → Trabajadores de servicio e IndexedDB. No actives el modo sin conexión antes de pulsar **Preparar sin conexión**.

## Restricción de la verificación en este entorno

La conexión del navegador de prueba integrado no pudo inicializarse por un conflicto interno de su entorno (`Cannot redefine property: process`). La compilación React y los endpoints HTTP sí quedaron verificados, pero el recorrido visual automatizado completo debe ejecutarse en el navegador local siguiendo la guía anterior. No se declara ese recorrido como aprobado sin evidencia.
