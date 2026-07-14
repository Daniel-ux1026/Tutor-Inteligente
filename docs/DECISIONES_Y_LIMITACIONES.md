# Decisiones y limitaciones reales

## Decisiones

- Se priorizó una implementación local demostrable y auditable sobre servicios de nube.
- Las notificaciones usan Server-Sent Events y reconexión nativa de `EventSource`.
- La recuperación de contraseña devuelve un código local solo con el perfil `local`; debe sustituirse por correo institucional antes de producción.
- Los datos curriculares son demostrativos y deben ser validados por un especialista de Matemática y Comunicación antes de usarse con estudiantes reales.

## Limitaciones

- El dataset de IA es sintético. Las métricas describen ese dataset, no eficacia pedagógica real.
- El proyecto no incluye certificados TLS ni un proveedor de correo; la demo local funciona sobre HTTP/localhost.
- La integración se verificó contra SQL Server 2019 Developer: Flyway aplicó cinco migraciones, Hibernate validó el esquema y el backend inició con salud `UP`. No se ejecutó el recorrido equivalente con el contenedor Docker en este equipo.
- No fue posible renderizar el DOCX original por ausencia de LibreOffice; se realizó revisión estructural y de imágenes embebidas.
- Las notificaciones push del sistema operativo no forman parte del alcance; las notificaciones son internas y en tiempo real mientras la aplicación está abierta.
- Para una adopción institucional faltan consentimiento, gobierno de datos, pruebas de carga, respaldo operacional y evaluación pedagógica con datos reales.
