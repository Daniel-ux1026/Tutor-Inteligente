# Tutor Inteligente Adaptativo

Plataforma web educativa para estudiantes de 1.° a 5.° de secundaria del Perú. Enseña Matemática y Comunicación, recomienda la dificultad con un árbol de decisión explicable, funciona como PWA offline y permite que el docente supervise, filtre, intervenga y abra el avance exacto desde una notificación.

## Resumen del proyecto

Tutor Inteligente es un prototipo funcional avanzado orientado al acompañamiento personalizado de estudiantes de secundaria. El sistema integra diagnóstico, práctica adaptativa, seguimiento docente y trabajo sin conexión dentro de una misma plataforma.

| Área | Capacidades principales |
|---|---|
| Alumno | Diagnósticos de Matemática y Comunicación, ejercicios adaptativos, progreso, historial, actividades asignadas y notificaciones pedagógicas. |
| Docente | Panel de seguimiento, alumnos paginados y filtrados por curso, alertas, observaciones, banco de preguntas, generación/asignación de temas y acceso contextual a **Ver avance**. |
| Adaptación | Árbol de decisión explicable, métricas consultadas desde la API y reglas de respaldo cuando el servicio de IA no está disponible. |
| Offline | PWA, caché de ejercicios, cola de intentos en IndexedDB, historial de sincronización e idempotencia mediante UUID. |
| Datos y seguridad | SQL Server con migraciones Flyway, JWT, BCrypt, autorización por rol, bloqueo por intentos fallidos y validaciones en frontend/backend. |

### Estado actual

- 33 de 38 funcionalidades inventariadas están completamente implementadas; una es parcial, una está documentada sin módulo funcional y tres permanecen pendientes.
- La API contiene 38 operaciones distribuidas entre autenticación, alumno, docente e IA.
- El esquema final contiene 20 tablas y cinco migraciones Flyway.
- El banco inicial contiene 495 preguntas y 1 980 alternativas.
- La IA se entrenó con 620 registros sintéticos y alcanzó 90.32 % de exactitud en la partición de prueba.
- Es adecuado para demostración académica y pruebas locales; todavía no debe considerarse una plataforma lista para producción institucional.

## Arquitectura

```text
frontend/   React 18 + Vite + React Router + PWA + IndexedDB
backend/    Java 21 + Spring Boot 3.4 + Security/JWT + JPA + SSE + OpenAPI
ia/         Python + pandas/numpy/scikit-learn + FastAPI + joblib + notebook
database/   SQL Server 2019 + Flyway + 20 tablas + 495 ejercicios curriculares
docs/       auditoría, arquitectura, trazabilidad, pruebas y prototipo heredado
scripts/    verificación e iconos reproducibles
```

Consulta [ARQUITECTURA.md](docs/ARQUITECTURA.md) para los flujos adaptativo, offline y «Ver avance».

## Requisitos previos

- Windows 10/11.
- JDK 21.
- Maven 3.9+ para compilar o iniciar el backend desde una clonación nueva. El directorio `backend/target` no se publica en Git.
- Node.js 20+ y npm.
- Python 3.11+.
- Docker Desktop (recomendado) o SQL Server 2019 local.

## Inicio rápido

### Inicio con doble clic (SQL Server local configurado)

Haz doble clic en `INICIAR_TUTOR_INTELIGENTE.bat`. El archivo usa SQL Server local, crea la base `TutorInteligente` si todavía no existe, aplica las migraciones, levanta la IA, el backend y el frontend, espera a que respondan y abre automáticamente `http://127.0.0.1:5173`.

Si aún no existe `.env`, el lanzador solicitará la contraseña del usuario configurado en SQL Server durante el primer inicio. Esa contraseña se guarda únicamente en el archivo local `.env`, que está excluido de Git.

En el primer inicio, si faltan las dependencias del frontend o el entorno de IA, el mismo lanzador las instala automáticamente. Para esa preparación inicial se necesita conexión a Internet; después no vuelve a descargar nada.

Para apagar los tres servicios, haz doble clic en `DETENER_TUTOR_INTELIGENTE.bat`.

### Inicio desde PowerShell (opcional)

1. Abre PowerShell en la raíz del proyecto.
2. Si prefieres preparar las dependencias manualmente:

```powershell
cd frontend
npm install
cd ..\ia
python -m pip install -r requirements.txt
cd ..
```

3. El lanzador de doble clic crea `.env` la primera vez con la conexión local indicada y un secreto JWT aleatorio. Si prefieres configurarlos manualmente:

```powershell
Copy-Item .env.example .env
# Reemplaza el marcador de JWT_SECRET y, si corresponde, la conexión de SQL Server.
```

4. Inicia todo:

```powershell
.\iniciar-proyecto.ps1
```

También puedes hacer doble clic en `iniciar-proyecto.bat`. En esta entrega está preparado para usar SQL Server local y no intenta iniciar Docker.

Direcciones:

- Aplicación: `http://127.0.0.1:5173`
- Swagger: `http://127.0.0.1:8080/swagger-ui.html`
- API backend: `http://127.0.0.1:8080`
- FastAPI IA: `http://127.0.0.1:8001/docs`

Para detener los procesos:

```powershell
.\detener-proyecto.ps1
docker compose down
```

### Si ya tienes SQL Server 2019

El lanzador principal comprueba la conexión y crea `TutorInteligente` automáticamente. También puedes configurar `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER` y `DB_PASSWORD` en `.env`, y luego ejecutar:

```powershell
.\iniciar-proyecto.ps1 -SinDocker
```

Flyway crea las tablas, índices, restricciones, cursos, temas y preguntas al iniciar el backend.

## Inicio manual por servicio

Terminal 1 — IA:

```powershell
cd ia
python train_model.py
python -m uvicorn app.main:app --host 127.0.0.1 --port 8001
```

Terminal 2 — backend:

```powershell
cd backend
mvn spring-boot:run
```

Terminal 3 — frontend:

```powershell
cd frontend
npm run dev
```

## Credenciales de demostración

| Rol | Correo | Contraseña |
|---|---|---|
| Alumno | `alumno@demo.pe` | `1234` |
| Docente | `docente@demo.pe` | `1234` |

Las contraseñas se crean con BCrypt al inicializar; no están guardadas en texto plano en SQL ni en componentes React.

## Demostrar dos sesiones simultáneas

1. Abre `http://127.0.0.1:5173` en una ventana normal e ingresa como docente.
2. Abre la misma URL en una ventana privada o en otro navegador e ingresa como alumno.
3. En el alumno, entra a **Práctica adaptativa**, responde hasta completar cinco ejercicios o provocar dos errores consecutivos.
4. En el docente, observa el contador de la campana actualizarse mediante SSE.
5. Pulsa **Ver avance**. La URL será `/docente/alumnos/{id}/avance?...` y el tema, intento o alerta que originó el evento quedará resaltado.

Si el docente estaba desconectado, la notificación reaparece al iniciar sesión porque se persiste en `notifications`.

## Demostrar el modo offline

1. Con conexión y sesión de alumno, abre **Práctica adaptativa** y pulsa **Preparar sin conexión**.
2. En DevTools, pestaña Network, activa **Offline** (o desconecta la red).
3. Recarga una ruta ya visitada: el service worker entrega el shell.
4. Responde un ejercicio descargado. Se guarda con UUID en IndexedDB; el contador de pendientes aumenta.
5. Abre **Sin conexión y sincronización** para inspeccionar la cola.
6. Restablece la conexión. La sincronización automática envía el lote al backend.
7. El backend confirma cada UUID; el cliente elimina solo los confirmados y registra el evento en el historial.
8. Reintentar el mismo lote no duplica datos porque `attempts.client_attempt_id` y `sync_batches.client_batch_id` son únicos.

## Modelo de IA

`ia/train_model.py` genera exactamente 620 muestras con `random_state=42`, usa división estratificada 80/20 y entrena un `DecisionTreeClassifier` multiclase.

Artefactos reales incluidos:

- `ia/data/dataset_tutor_620.csv`
- `ia/models/decision_tree_tutor.joblib`
- `ia/reports/metrics.json`
- `ia/reports/confusion_matrix.png`
- `ia/reports/feature_importance.png`
- `ia/modelo_tutor_adaptativo.ipynb`

Último entrenamiento verificado:

| Métrica | Resultado |
|---|---:|
| Exactitud | 90.32 % |
| Precisión ponderada | 90.79 % |
| Sensibilidad ponderada | 90.32 % |
| Puntuación F1 ponderada | 90.29 % |

La pantalla **Reportes** consulta `/api/ai/metrics`; no contiene métricas fijas. El dataset es sintético, por lo que estos resultados no prueban eficacia pedagógica real.

## API principal

- Auth: `/api/auth/login`, `/register`, `/refresh`, `/recovery`.
- Alumno: `/api/student/profile`, `/diagnostic`, `/diagnostics`, `/courses`, `/topics`, `/exercises/next`, `/exercises/offline-pack`, `/activities`, `/notifications`, `/attempts`, `/attempts/sync`, `/progress`, `/history`.
- Docente: `/api/teacher/dashboard`, `/catalog`, `/students`, `/students/{id}/progress`, `/observations`, `/alerts`, `/notifications`, `/questions`, `/activities`, `/topics/assign`, `/settings`.
- Tiempo real: `/api/teacher/notifications/stream` (SSE).
- IA: `/api/ai/recommendation`, `/api/ai/metrics`.

## Pruebas

Ejecuta todas las verificaciones:

```powershell
.\scripts\verificar.ps1
```

O individualmente:

```powershell
cd ia; python -m unittest discover -s tests -v
cd frontend; npm test; npm run build
cd backend; mvn test
```

Los escenarios y su evidencia están en [PRUEBAS_ACEPTACION.md](docs/PRUEBAS_ACEPTACION.md). La relación funcionalidad/pantalla/componente/endpoint/tabla/prueba está en [TRAZABILIDAD_FUNCIONAL.md](docs/TRAZABILIDAD_FUNCIONAL.md).

## Seguridad

- JWT de acceso de 30 minutos y refresh de 7 días.
- BCrypt con costo 12.
- Rutas protegidas por rol.
- Bloqueo de cinco minutos después de cinco fallos.
- Recuperación local con código de 15 minutos; sustituir por correo institucional en producción.
- Validación Bean Validation, CORS acotado y manejo global de excepciones.
- Secretos solo por entorno; `.env` está ignorado.

## Limitaciones reales

Lee [DECISIONES_Y_LIMITACIONES.md](docs/DECISIONES_Y_LIMITACIONES.md). Las principales son: dataset sintético, ausencia de TLS/correo en la demo local, contenido curricular pendiente de validación pedagógica y notificaciones internas (no push del sistema operativo).

## Qué falta para completar el proyecto

Para convertir el prototipo en una solución institucional completa se recomienda trabajar en este orden:

1. **Validación pedagógica y de datos:** revisar el banco con docentes especialistas, ejecutar pilotos con consentimiento informado y reentrenar/calibrar la IA usando datos reales anonimizados.
2. **Seguridad de producción:** desplegar con HTTPS, rotar secretos, usar una cuenta SQL de privilegios mínimos, endurecer cookies/tokens, añadir auditoría y retirar el código fijo de recuperación local.
3. **Recuperación y comunicaciones:** integrar correo institucional para recuperación de contraseña y, si se requiere, notificaciones push con preferencias y consentimiento del usuario.
4. **Módulos faltantes:** implementar administración de usuarios/cursos, reportes académicos exportables por periodo y cierre completo de actividades asignadas.
5. **Offline multidispositivo:** incorporar versionado y resolución de conflictos semánticos cuando un mismo progreso cambie en más de un dispositivo.
6. **Calidad:** ampliar las pruebas unitarias, de integración, seguridad, accesibilidad, carga y recorridos E2E en navegadores reales.
7. **Operación:** añadir CI/CD, ambientes separados, observabilidad, respaldos, restauración probada, políticas de retención y monitoreo de disponibilidad.
8. **Cumplimiento:** definir gobierno de datos, privacidad de menores, responsables institucionales, términos de uso y proceso de soporte.

El detalle técnico, las decisiones, las pruebas y la trazabilidad se mantienen en la carpeta [`docs/`](docs/).
