# Cómo funciona el plugin

## Del menú a la imagen

`plugins.config` registra `Microscopio_Color`, que implementa `ij.plugin.PlugIn`. Esa clase solo delega en `MicroscopePlugin`: así se mantiene el nombre que espera ImageJ y el resto del proyecto queda en `es.robotedu.microscopio`.

`MicroscopePlugin` consulta las herramientas externas en un hilo de arranque y presenta el selector en AWT. `CameraDiscovery` lee `/sys/class/video4linux` y las capacidades publicadas por `v4l2-ctl`. Solo acepta 1280×720 discreto en los formatos de color conocidos. `CameraMode` conserva ese modo y construye una lista de argumentos FFmpeg; no ejecutamos órdenes mediante shell.

`CaptureSession` lee RGB24 del proceso FFmpeg. `RgbFrames` exige un fotograma completo: una desconexión no se confunde con una imagen válida. Cada fotograma ocupa 1280×720×3 bytes. La traducción a `int[]` conserva los canales rojo, verde y azul.

## Por qué hay varios hilos

Leer una cámara o finalizar un vídeo puede tardar. Esas operaciones se realizan fuera de AWT para no congelar los botones. El repintado usa una referencia al último fotograma y solo un evento pendiente: si la pantalla no sigue el ritmo se omiten repintados, sin acumular memoria.

Las acciones de fotografía y arranque del vídeo se serializan en un ejecutor. Al cerrar, se espera a las acciones pendientes antes de finalizar el grabador. El vídeo tiene una cola de dos fotogramas: si FFmpeg va lento se contabilizan descartes en vez de crecer indefinidamente. Los tiempos de llegada se pasan al codificador, pero deben validarse también bajo carga prolongada.

Los límites de espera evitan consultas o cierres bloqueados indefinidamente. Un corte de alimentación puede dejar un MP4 sin finalizar; esta implementación no promete recuperar archivos tras un cierre forzado del proceso o del sistema.

## Archivos y multiusuario

`UserStorage` obtiene la carpeta XDG en la sesión actual, con fallback dentro de `user.home`. `CREATE_NEW`, fecha con milisegundos e identificador adicional evitan sobrescrituras. Se guardan los registros con las capturas, no junto al JAR. La prueba de dos UID monta el código en solo lectura; no crea cuentas en el equipo anfitrión.

No hay configuración persistente propia del plugin. Se vuelve a elegir la cámara al abrir. `ACTIVE` impide dos capturas dentro de la misma JVM; no bloquea globalmente otros procesos. Un dispositivo ocupado se resuelve con los permisos y el controlador del sistema, sin alterar grupos ni udev.

## Límites de la señal

No se cambia la saturación de la cámara. No se convierte una imagen monocroma en color ni se usan tablas de pseudocolor. La resolución y la tasa proceden de V4L2; que una cámara anuncie un modo no certifica la resolución óptica de su sensor.

JPEG y H.264 son salidas con pérdida. La vista RGB24 y el archivo MP4 YUV420p no son formatos equivalentes. Si hace falta adquisición científica sin pérdida o fotografía de 5 MP, se necesita un cambio funcional y validación con hardware; no lo resuelve una ampliación de pantalla.
