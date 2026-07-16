# Decisiones y limitaciones reales

## Decisiones

- Se priorizó una implementación local demostrable y auditable sobre servicios de nube.
- Las notificaciones usan Server-Sent Events y reconexión nativa de `EventSource`.
- La recuperación de contraseña devuelve un código local solo con el perfil `local`; debe sustituirse por correo institucional antes de producción.
- Los datos curriculares son demostrativos y deben ser validados por un especialista de Matemática y Comunicación antes de usarse con estudiantes reales.
- La eficacia observada se calcula exclusivamente con intentos de cuentas no demostrativas; la validación técnica del clasificador se presenta en un bloque independiente.

## Limitaciones

- El conjunto inicial del clasificador fue generado de forma controlada y sirve para validar el motor técnico. No se mezcla con la evidencia pedagógica almacenada en SQL Server.
- El proyecto no incluye certificados TLS ni un proveedor de correo; la demo local funciona sobre HTTP/localhost.
- La integración se verificó contra SQL Server 2019 Developer: Flyway aplicó seis migraciones, Hibernate validó el esquema y el backend inició con salud `UP`. No se ejecutó el recorrido equivalente con el contenedor Docker en este equipo.
- No fue posible renderizar el DOCX original por ausencia de LibreOffice; se realizó revisión estructural y de imágenes embebidas.
- Las notificaciones push del sistema operativo no forman parte del alcance; las notificaciones son internas y en tiempo real mientras la aplicación está abierta.
- Para una adopción institucional faltan consentimiento, gobierno de datos, pruebas de carga, respaldo operacional y evaluación pedagógica con datos reales.
- Una mejora pre/post observada en la plataforma no demuestra causalidad por sí sola; una afirmación institucional de impacto requiere un piloto aprobado, seguimiento longitudinal y análisis pedagógico.
