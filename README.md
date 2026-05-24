# Tutor Inteligente para Colegios Públicos

Prototipo web funcional de un tutor adaptativo de matemáticas para estudiantes de 1.° a 5.° de secundaria.

## Cómo abrirlo

La app queda disponible en:

```text
http://127.0.0.1:4173/
```

Si necesitas levantar el servidor manualmente:

```powershell
node server.js
```

## Qué incluye

- Modo estudiante con selección de grado y tema.
- Login y registro por rol: alumno o docente.
- Cuentas demo:
  - Alumno: `alumno@demo.pe` / `1234`
  - Docente: `docente@demo.pe` / `1234`
- Recordar acceso, cierre de sesión y menú de perfil para docente.
- Recuperación de contraseña simulada con enlace y código 2FA.
- Bloqueo por 5 intentos fallidos y reintento después de 5 minutos.
- Temporizador de 5 minutos para completar el formulario de acceso.
- Banco base de 75 preguntas de matemática.
- Motor adaptativo por reglas:
  - 3 respuestas correctas suben la dificultad.
  - 2 errores bajan la dificultad o recomiendan repaso.
- Guardado local con IndexedDB.
- Simulación de trabajo sin internet.
- Sincronización simulada al volver a estar en línea.
- Modo docente para registrar preguntas.
- Reportes automáticos por alumno, tema crítico y rendimiento.
- PWA con `manifest.webmanifest` y `sw.js` para caché offline.

## Archivos principales

- `index.html`: estructura de la interfaz.
- `styles.css`: diseño responsivo.
- `app.js`: lógica adaptativa, IndexedDB, reportes y sincronización.
- `sw.js`: service worker para funcionamiento offline.
- `server.js`: servidor local para presentar la PWA.
