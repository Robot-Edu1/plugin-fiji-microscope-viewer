# Microscopio Color para Fiji

Repositorio: https://github.com/Robot-Edu1/plugin-fiji-microscope-viewer

Plugin de captura para microscopios USB en Linux, preparado para que LliureX incorpore el complemento a su propia distribución de Fiji. La versión de esta revisión es **1.1.1**; reorganiza y documenta el código entregado previamente como 1.1.0.

El proyecto abre una imagen de color a **1280×720**, con botones para zoom, fotografía JPEG y vídeo MP4. La salida se guarda con fecha y hora en la carpeta de imágenes de la cuenta que ejecuta Fiji, dentro de `Microscopio`. El programa no interpola imágenes para simular una resolución mayor.

**Mantenimiento:** Robot-Edu no asume mantenimiento gratuito, soporte continuado ni un plazo de respuesta. Los trabajos que se soliciten a Robot-Edu requieren presupuesto y aceptación previa. LliureX y la comunidad pueden corregir, adaptar, mantener y redistribuir el código conforme a MIT, sin contratar servicios. La participación comunitaria no garantiza que una incidencia vaya a resolverse. Véase [MAINTENANCE.md](MAINTENANCE.md).

## Qué incluye y qué necesita

Este repositorio contiene fuentes Java, pruebas, licencia y herramientas de construcción. **Fiji, ImageJ, Java, FFmpeg y v4l-utils no están incluidos.** Sus dependencias y el empaquetado del sistema los gestiona LliureX.

- Linux con cámara V4L2 que anuncie un modo discreto 1280×720 en MJPG, YUYV, NV12, RGB3 o BGR3.
- Fiji con API ImageJ 1.x y Java 11 o posterior. Las pruebas locales usan ImageJ 1.54p y Java 11/21; no se promete compatibilidad con versiones futuras sin validación.
- `ffmpeg` con V4L2 y libx264, `v4l2-ctl`; se recomienda `xdg-user-dir` para la carpeta localizada.
- Los permisos USB deben existir previamente. El plugin no cambia udev, grupos, permisos ni controles persistentes de cámara.

La vista es RGB24; el JPEG y el MP4 H.264 YUV420p usan compresión con pérdida. El vídeo no tiene audio. **1280×720 son 921.600 píxeles, no 5 MP.** Si el sensor envía gris, el plugin no inventa el color ausente.

## Compilar desde fuentes

Con JDK 11+, Python 3 y un `ij.jar` ya disponible:

```sh
make IJ_JAR=/ruta/al/ij.jar
```

Sin Make:

```sh
python3 tools/build.py --ij-jar /ruta/al/ij.jar
```

Resultado: `dist/Microscopio_Color-1.1.1.jar`. El constructor no descarga ni instala nada; utiliza ImageJ solo para compilar. No hay un instalador privilegiado ni una copia de Fiji en el resultado.

## Incorporarlo a Fiji y utilizarlo

El empaquetador coloca una única copia del JAR en la ruta de plugins del Fiji que administra. Esa ruta la decide LliureX; el código no presupone una instalación en un hogar concreto. Reiniciar Fiji y abrir **Plugins → Microscopio → Microscopio Color RGB (1280x720)**.

1. Conectar el microscopio con un cable de datos y elegirlo en el diálogo, evitando la webcam integrada.
2. Usar **Zoom + / Zoom −** para ajustar la vista; la captura conserva 1280×720.
3. Pulsar **Guardar foto JPEG**: carpeta y nombre se asignan automáticamente.
4. Pulsar **Iniciar vídeo MP4** y **Detener vídeo**. Esperar al mensaje de archivo guardado.
5. **Cerrar cámara** finaliza también el vídeo activo y libera el dispositivo.

Cada cuenta resuelve su carpeta con `xdg-user-dir PICTURES`. Si no existe, se buscan Imágenes, Imatges o Pictures en su hogar y, como último recurso, se crea `~/Imágenes/Microscopio`. El JAR compartido puede ser de solo lectura. El acceso simultáneo al mismo microscopio depende del controlador; multiusuario no significa compartir un flujo USB simultáneamente.

## Pruebas y versiones

```sh
make test IJ_JAR=/ruta/al/ij.jar
```

Los tests generan sus propias barras de color y **no abren una cámara física**. GitHub Actions ejecuta las comprobaciones con Java 11/21. Los resultados se consultan en https://github.com/Robot-Edu1/plugin-fiji-microscope-viewer/actions. [Pruebas y limitaciones](docs/PRUEBAS.md).

Las versiones se identifican mediante etiquetas `vX.Y.Z`. El flujo de publicación prepara un **borrador** de Release con JAR, fuentes `.tar.gz` y SHA-256; una persona revisa y publica el borrador. [Procedimiento de publicación](docs/PUBLICAR-GITHUB.md). El código público está en este repositorio; las versiones descargables se consultan en https://github.com/Robot-Edu1/plugin-fiji-microscope-viewer/releases.

## Para quien quiera entender o cambiar el código

- [Arquitectura y recorrido de una captura](docs/ARQUITECTURA.md).
- [Integración y ejemplo debian/watch para LliureX](docs/lliurex/INTEGRACION.md).
- [Cómo colaborar](CONTRIBUTING.md) y [política de mantenimiento](MAINTENANCE.md).
- [Historial de cambios](CHANGELOG.md).

Los comentarios explican decisiones: cómo se evita bloquear la pantalla, por qué la cola de vídeo es acotada y cómo se respeta la carpeta de cada cuenta. No se presentan pruebas sintéticas como validaciones físicas del microscopio.

## Licencia y contacto

Código propio bajo [MIT](LICENSE). Copyright © 2026 Rafael Cantillana — Robot-Edu.es. La identificación empresarial publicada figura en [NOTICE](NOTICE): Ingenio Triana S.L., CIF/NIF B-04984514. Contacto: info@robot-edu.es.

La publicación pública responde al flujo solicitado por LliureX. El texto MIT permite usar, modificar y redistribuir con sus avisos; no obliga por sí solo a alojar un repositorio público ni a prestar mantenimiento. Referencia: https://opensource.org/license/mit.
