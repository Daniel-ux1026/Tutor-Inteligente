# Auditoría del proyecto heredado

Fecha: 13 de julio de 2026

## Fuentes revisadas

- `APF3_Tutor_Inteligente_Avance3.docx`: 114 párrafos, 18 tablas, 16 objetos gráficos y una sección.
- `Tutor-Inteligente-main.zip`: prototipo de 10 archivos principales, sin gestor de dependencias ni backend persistente.
- Copia inalterada del prototipo: `docs/legacy-prototype/`.

La revisión estructural del DOCX confirmó que el segundo incremento prioriza el algoritmo adaptativo, el trabajo sin conexión, la sincronización, los filtros docentes, el progreso y las alertas. El documento declara 620 muestras simuladas y una arquitectura objetivo React + Spring Boot + SQL Server/MySQL + servicio Python. Se revisaron el texto, las tablas y las imágenes embebidas de las cinco pantallas principales.

## Qué se conserva

- El propósito educativo, las seis funcionalidades prioritarias del segundo incremento y el enfoque Scrumban.
- Las cuentas de demostración y el bloqueo temporal después de cinco intentos.
- La taxonomía de Matemática: Números y operaciones, Álgebra, Geometría, Funciones y Estadística.
- Las fórmulas que generan las 75 preguntas originales (cinco grados × cinco temas × tres niveles).
- La lógica de selección de nivel como respaldo explicable cuando el servicio de IA no está disponible.
- Los conceptos de caché PWA, trabajo offline, IndexedDB, estado de conexión y sincronización automática.
- La paleta turquesa/azul y la lectura visual por tarjetas observada en el prototipo y en el APF3.
- El prototipo original completo se conserva como evidencia histórica dentro de `docs/legacy-prototype/`.

## Qué se migra

| Elemento heredado | Destino profesional |
|---|---|
| HTML monolítico | React + React Router, páginas por rol y componentes reutilizables |
| Estado global mutable | Contexto de autenticación y servicios de dominio |
| IndexedDB con cuatro stores | IndexedDB versionado con caché, cola idempotente e historial de sincronización |
| Auth local con hash no criptográfico | Spring Security, JWT, BCrypt y autorización por rol |
| 75 preguntas generadas en JavaScript | Semillas SQL Server y banco administrable con 495 ejercicios de Matemática y Comunicación |
| Reglas adaptativas 3/2 | Árbol de decisión real con seis variables y respaldo por reglas |
| Sincronización simulada | `POST /api/student/attempts/sync` con UUID único y confirmación por registro |
| Reporte calculado en navegador | Consultas y agregados persistentes del backend |
| Alertas visuales | Alertas SQL Server con estado, riesgo y recomendación pedagógica |
| Sin notificaciones | Persistencia + SSE + campana + página completa + deep-link contextual |

## Problemas encontrados y correcciones

- Las contraseñas y cuentas vivían en IndexedDB; ahora la autoridad es SQL Server y las contraseñas se codifican con BCrypt.
- La sincronización solo cambiaba `synced=true`; ahora el backend aplica idempotencia por `clientAttemptId`, devuelve confirmaciones y el cliente limpia únicamente los UUID confirmados.
- No existía API, JPA ni control de acceso; se incorporan capas Controller/Service/Repository/DTO/Entity y manejo global de errores.
- El modelo de IA y sus métricas no estaban en el repositorio; se añade entrenamiento reproducible, notebook, artefactos calculados y FastAPI.
- La aplicación solo cubría Matemática; se agrega Comunicación para los cinco grados y tres dificultades.
- El panel docente tenía filtros parciales; se cubren grado, sección, curso, nivel, progreso, alerta y fechas.
- Los botones del perfil docente eran decorativos; la nueva navegación conecta cada opción a una ruta funcional.
- El manifest dependía de un único SVG; la nueva PWA usa iconos PNG locales de 192 y 512 px y un service worker con fallback de navegación.
- No había trazabilidad de notificación a alumno; el contexto persistido contiene alumno, intento, tema y alerta para abrir y resaltar el avance correcto.

## Decisiones técnicas

- SQL Server 2019 es la base principal. H2 solo se usa en pruebas automatizadas del backend.
- El token de acceso se mantiene en memoria y `sessionStorage`; los datos académicos autoritativos nunca se guardan allí.
- IndexedDB se limita a caché offline, cola pendiente e historial de sincronización.
- El backend intenta consultar FastAPI y usa reglas explicables solo como degradación controlada; la respuesta indica la fuente (`MODEL` o `FALLBACK`).
- SSE se eligió sobre WebSocket porque el flujo es unidireccional (servidor → docente), simple de reconectar y suficiente para la campana.
- El dataset de IA sigue siendo simulado, como declara el APF3. Las métricas se recalculan al entrenar y nunca se fijan en la interfaz.
