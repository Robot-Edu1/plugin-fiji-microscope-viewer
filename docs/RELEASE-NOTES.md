Microscopio Color 1.1.3 — plugin independiente para Fiji.

La primera versión pública incorpora una corrección detectada en las pruebas remotas: las marcas temporales de vídeo se toman en Java, antes de la cola del codificador. La salida de tasa constante repite la última imagen cuando faltan cuadros, para mantener la duración. Las repeticiones y descartes se registran; no equivalen a movimiento adquirido. Se comprueba también el arranque lento del codificador.

Incluye JAR propio, tarball fuente y SHA256SUMS.txt. Código y documentación bajo MIT. Sin Fiji, Java, dependencias del sistema ni instaladores privilegiados.

Captura RGB 1280×720, JPEG y MP4. No acredita 5 MP. Las pruebas de adquisición son sintéticas; sigue pendiente la validación física con el microscopio y el Fiji de LliureX.

Robot-Edu no asume mantenimiento gratuito ni soporte continuado. Cualquier trabajo solicitado requiere presupuesto y aceptación previa. LliureX y la comunidad pueden mantener, modificar y redistribuir bajo MIT sin contratar servicios.

Las etiquetas 1.1.1 y 1.1.2 conservan el historial de las revisiones de validación; no deben usarse como versiones distribuidas. Véanse CHANGELOG.md, MAINTENANCE.md y docs/PRUEBAS.md.
