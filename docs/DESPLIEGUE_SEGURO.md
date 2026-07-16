# Despliegue seguro

## Plataforma elegida

La solución completa no cabe en Vercel sin reescribir el backend: Vercel funciona bien para el frontend Vite, pero el proyecto también necesita un proceso Spring Boot persistente, un servicio FastAPI, eventos SSE y SQL Server.

Para mantener la arquitectura se usa Railway con cuatro servicios dentro del mismo proyecto:

```text
Internet
   |
   +-- frontend (HTTPS público, React + Nginx)
   |
   +-- backend (HTTPS público, Spring Boot)
          |
          +-- ia.railway.internal:8001 (privado)
          |
          +-- sqlserver.railway.internal:1433 (privado + volumen)
```

Railway proporciona TLS para los dominios públicos y red privada cifrada entre servicios. El servicio IA y SQL Server no deben recibir dominios públicos ni TCP Proxy.

## Configuración por servicio

Cada servicio utiliza el mismo repositorio y una ruta de configuración distinta:

| Servicio | Archivo de configuración | Dockerfile | Puerto | Salud |
|---|---|---|---:|---|
| `frontend` | `/frontend/railway.json` | `/frontend/Dockerfile` | 8080 | `/health` |
| `backend` | `/backend/railway.json` | `/backend/Dockerfile` | 8080 | `/actuator/health` |
| `ia` | `/ia/railway.json` | `/ia/Dockerfile` | 8001 | `/health` |
| `sqlserver` | `/database/railway.json` | `/database/Dockerfile` | 1433 | interno |

En Railway, configura `Config File Path` con el archivo correspondiente. Adjunta un volumen al servicio `sqlserver` en `/var/opt/mssql`.

## Variables

### Variables compartidas secretas

Generar valores aleatorios diferentes y sellarlos en Railway:

- `MSSQL_SA_PASSWORD`: mínimo 16 caracteres, con mayúsculas, minúsculas, números y símbolos.
- `JWT_SECRET`: mínimo 32 bytes aleatorios; se recomienda 64.
- `AI_SERVICE_API_KEY`: mínimo 32 bytes aleatorios.
- `TEACHER_INVITATION_CODE`: código privado para autorizar el alta de docentes.

Nunca copiar estas variables al repositorio ni a una variable `VITE_*`.

### `sqlserver`

```text
ACCEPT_EULA=Y
MSSQL_PID=Developer
MSSQL_SA_PASSWORD=<secreto sellado>
DB_NAME=TutorInteligente
```

`Developer` es válido para demostración y desarrollo, no para explotación comercial. Un despliegue institucional debe usar una licencia válida o Azure SQL.

### `ia`

```text
PORT=8001
AI_SERVICE_API_KEY=<mismo secreto sellado del backend>
AI_DOCS_ENABLED=false
```

No crear dominio público para este servicio.

### `backend`

```text
PORT=8080
DB_HOST=sqlserver.railway.internal
DB_PORT=1433
DB_NAME=TutorInteligente
DB_USER=sa
DB_PASSWORD=<referencia a MSSQL_SA_PASSWORD>
DB_ENCRYPT=true
DB_TRUST_SERVER_CERTIFICATE=true
JWT_SECRET=<secreto sellado>
AI_SERVICE_URL=http://ia.railway.internal:8001
AI_SERVICE_API_KEY=<secreto sellado>
CORS_ORIGINS=https://<dominio-publico-frontend>
DEMO_DATA=false
TEACHER_INVITATION_CODE=<secreto sellado>
RECOVERY_ENABLED=false
RECOVERY_EXPOSE_LOCAL_CODE=false
SWAGGER_ENABLED=false
```

`DB_TRUST_SERVER_CERTIFICATE=true` se limita al contenedor privado de demostración, que usa certificado interno. Con Azure SQL o un certificado emitido por una autoridad, usar `false`.

### `frontend`

Estas variables se utilizan durante la compilación:

```text
VITE_API_URL=https://<dominio-publico-backend>/api
VITE_DEMO_MODE=false
```

Después de cambiar una variable `VITE_*`, es necesario volver a desplegar el frontend.

## Controles incorporados

- Contenedores de frontend, backend e IA ejecutados sin privilegios.
- SQL Server e IA accesibles solo mediante red privada.
- API IA protegida con una clave interna constante.
- CORS limitado al dominio exacto del frontend.
- Registro docente protegido por invitación.
- Datos y credenciales demo desactivados en producción.
- Recuperación con código local y Swagger desactivados.
- Contraseñas nuevas de mínimo ocho caracteres y BCrypt con costo 12.
- JWT de acceso corto y refresh separado.
- Tokens del frontend en `sessionStorage`, no en `localStorage`.
- SSE autenticado mediante cabecera `Authorization`, sin token en la URL.
- Source maps de producción desactivados.
- CSP, HSTS, protección contra `iframe`, `nosniff`, política de referente y permisos del navegador.
- Migraciones Flyway automáticas y health checks.

## Verificación posterior

1. Confirmar que `https://<frontend>/health` devuelve `UP`.
2. Confirmar que `https://<backend>/actuator/health` devuelve `UP`.
3. Verificar que IA y SQL Server no tienen dominio público.
4. Registrar un docente con el código de invitación y un alumno sin el código.
5. Comprobar diagnóstico, práctica, progreso, observación docente y notificación del alumno.
6. Confirmar que una llamada directa a `/predict` sin `X-Internal-API-Key` es rechazada.
7. Revisar cabeceras HTTPS y que Swagger no esté disponible.
8. Probar respaldo y restauración del volumen antes de almacenar información importante.
