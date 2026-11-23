# Para el equipo de empaquetado de LliureX

Este origen contiene solo el plugin. Fiji, Java y dependencias externas se empaquetan según la política de LliureX. No hay DEB, EPI, udev ni instalador de dependencias del usuario final.

Compilar el tarball fuente con `make IJ_JAR=/ruta/al/ij.jar`. Colocar una única copia del JAR en la ruta que use el Fiji de la distribución. El artefacto no incluye `ij.jar`, Java ni bibliotecas nativas. Requiere `ffmpeg` con entrada V4L2 y libx264, `v4l2-ctl` y preferiblemente `xdg-user-dir`.

Las fuentes de cada versión serán `microscopio-color-fiji-X.Y.Z.tar.gz`; el JAR `Microscopio_Color-X.Y.Z.jar`. La Release pública tendrá SHA256SUMS.txt. El empaquetador debe construir desde fuentes y validar su resultado; no depende de una descarga `latest` realizada por el plugin.

`debian.watch.example` propone un formato 4 para herramientas de la base Ubuntu existente. Sustituir `OWNER` por la cuenta real después de publicar. Es una plantilla, no una detección ya comprobada contra un repositorio público. Se usa el tarball fuente de las Releases y no el JAR como origen del paquete Debian.

Antes de distribuir, revisar LICENSE y NOTICE, elegir las dependencias del sistema y probar el dispositivo real con dos cuentas. Robot-Edu no presta mantenimiento gratuito; LliureX o la comunidad pueden mantener una rama o fork bajo MIT. Cualquier trabajo encargado a Robot-Edu requiere acuerdo comercial previo.
