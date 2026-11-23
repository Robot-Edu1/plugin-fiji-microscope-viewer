# Pruebas y estado de validación

La compilación normal requiere únicamente recursos locales. Para compilar y probar:

```sh
make test IJ_JAR=/ruta/al/ij.jar
```

Se prueban modos discretos de color, descarte de GREY, tamaño de fotograma, timeout de procesos, carpeta XDG con espacios, canales RGB, JPEG sin escalado, nombres únicos, MP4 y la entrada ImageJ estándar. El vídeo de prueba recibe barras RGB lentamente; se comprueba que su duración sea próxima al tiempo de envío. No abre cámaras reales.

## Perfiles aislados e interfaz

Después de `make test` pueden ejecutarse:

```sh
IJ_JAR=/ruta/al/ij.jar python3 tools/test_multiuser.py
IJ_JAR=/ruta/al/ij.jar python3 tools/test_gui.py
```

La prueba multiusuario requiere bubblewrap y namespaces de usuario habilitados. La gráfica requiere DISPLAY, xdotool e ImageMagick. La captura se sustituye solo en el entorno temporal del test por FFmpeg lavfi. Los archivos de prueba están ignorados por Git y nunca deben presentarse como imágenes reales del microscopio.

## GitHub Actions

`ci.yml` prepara un runner Ubuntu 22.04 con FFmpeg y Java 11/21. `fetch_ci_api.py` descarga exclusivamente la API ImageJ 1.54p de Maven Central y verifica un SHA-256 fijado. Es una ayuda explícita de CI; ni el plugin ni el constructor offline la llaman. Las instalaciones APT de ese YAML afectan al runner temporal, no al portátil del alumnado.

Los workflows deben ejecutarse realmente en el repositorio de destino para acreditar CI remoto. Una ejecución local o una validación de YAML no equivale a un resultado verde en GitHub.

## Pendiente en LliureX

Comprobar el microscopio físico, imagen en color, permisos preexistentes, cámara ocupada, cierre y reconexión, cuentas reales, grabación prolongada y poco espacio con el Fiji y el Java elegidos por LliureX. Sin esos resultados no se afirma validación institucional ni compatibilidad universal.
