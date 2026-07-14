# Arquitectura final

```text
React PWA (5173)
  ├─ JWT / REST ───────────────┐
  ├─ SSE de notificaciones ────┤
  └─ IndexedDB offline/sync    │
                               ▼
                    Spring Boot (8080)
                      ├─ Security + JWT
                      ├─ servicios de dominio
                      ├─ JPA / Flyway ─────► SQL Server 2019 (1433)
                      └─ REST ─────────────► FastAPI IA (8001)
                                               └─ DecisionTree.joblib
```

## Flujo adaptativo

1. El frontend solicita el siguiente ejercicio con curso y tema.
2. El backend resume los últimos intentos del estudiante en seis variables.
3. FastAPI clasifica `basico`, `intermedio` o `avanzado` y devuelve una explicación.
4. Spring guarda la recomendación y elige una pregunta activa del nivel recomendado.
5. Al responder, se guarda el intento, se recalcula el progreso y se evalúan alertas.
6. Los eventos relevantes crean una notificación persistida y se emiten por SSE al docente asignado.

## Flujo offline e idempotencia

1. El service worker mantiene disponible el shell y las rutas visitadas.
2. Los ejercicios descargados se guardan en el store `exercise-cache`.
3. Cada respuesta offline recibe un UUID (`crypto.randomUUID`) y entra a `attempt-queue`.
4. Al volver la conexión, el cliente envía un lote a `/api/student/attempts/sync`.
5. La restricción única `attempts.client_attempt_id` evita duplicados incluso si el lote se reintenta.
6. El backend devuelve `confirmedClientAttemptIds`; el cliente elimina solo esos registros y añade un evento a `sync-history`.

## Flujo «Ver avance»

1. La notificación persiste `studentId`, `topicId`, `attemptId`, `alertId` y `contextJson`.
2. La campana y la página de notificaciones llaman `PUT /api/teacher/notifications/{id}/read`.
3. React navega a `/docente/alumnos/{studentId}/avance` con el contexto en query string y estado de navegación.
4. La página consulta al alumno exacto y aplica la clase de resaltado al tema, intento o alerta origen.
5. El contexto sobrevive a una recarga porque los identificadores viajan en la URL.
