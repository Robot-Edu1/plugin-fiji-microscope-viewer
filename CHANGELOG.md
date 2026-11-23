# Historial de cambios

## 1.1.2 — 2026-11-23

- Corrección del sondeo inicial de FFmpeg al grabar RGB por tubería. Las primeras pruebas remotas detectaron un vídeo más corto que el tiempo de envío; se desactiva la inferencia inicial de tasa y se limita el sondeo porque el formato ya está definido. Se mantiene la prueba de duración, sin relajar su umbral.
- Actualizadas las acciones de GitHub a las revisiones v5 con Node.js 24.
- 1.1.1 permanece como etiqueta histórica: no se publicó una Release porque la comprobación Java 11 falló.


## 1.1.1 — 2026-11-23

Revisión de publicación del plugin independiente 1.1.0. No cambia la resolución ni añade compatibilidad física acreditada.

- Fuentes organizadas para GitHub y código Java con formato uniforme y comentarios de diseño.
- README, arquitectura, instrucciones de contribución y política de mantenimiento explícita.
- MIT canónica en LICENSE e identificación empresarial complementaria en NOTICE, ambos dentro del JAR.
- Construcción y pruebas automatizadas previstas para Java 11/21.
- Herramienta de Release con fuentes versionadas, JAR y sumas SHA-256, y ejemplo para debian/watch.
- Procedimiento de publicación y respuesta propuesta a LliureX sin prometer soporte gratuito.

## 1.1.0 — 2026-11-16

Entrega independiente facilitada por archivo ZIP para revisión de LliureX. No fue una Release pública en GitHub.

- Plugin Java para Fiji/ImageJ 1.x, captura RGB 1280×720, JPEG y MP4.
- Instalación compartida y salida por cuenta sin reglas udev ni cambios de grupos.
- Fuentes, compilación offline y pruebas sintéticas y de perfiles aislados.

Las versiones RC anteriores incluían otro planteamiento de despliegue; no forman parte del repositorio del plugin independiente.
