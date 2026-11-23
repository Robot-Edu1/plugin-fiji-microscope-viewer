// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Rafael Cantillana - Robot-Edu.es
package es.robotedu.microscopio;

import java.io.*;
import java.nio.file.*;
import java.time.OffsetDateTime;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Graba MP4 sin bloquear la adquisición. Las marcas de tiempo se toman en Java, antes de la cola:
 * el tiempo del vídeo no depende de que FFmpeg arranque rápido o lea la tubería sin retraso. Se usa
 * una salida de tasa constante, repitiendo la última imagen cuando faltan fotogramas. No se inventa
 * movimiento: los descartes y las repeticiones se anotan en la ficha lateral.
 */
final class VideoRecorder extends Thread {
  private static final class Frame {
    final byte[] rgb;
    final long time;

    Frame(byte[] rgb, long time) {
      this.rgb = rgb;
      this.time = time;
    }
  }

  private final BlockingQueue<Frame> queue = new ArrayBlockingQueue<>(2);
  private final Process process;
  private final OutputStream input;
  final Path target;
  private final UserStorage storage;
  private final Consumer<String> status;
  private final AtomicLong dropped = new AtomicLong();
  private final double outputFps;
  private volatile boolean finishing;
  private volatile long stoppedAt;
  private long written, repeated;
  volatile String failure;

  VideoRecorder(UserStorage storage, double fps, Consumer<String> status) throws IOException {
    super("microscope-video-writer");
    this.storage = storage;
    this.status = status;
    // Una cámara que anuncie más de 30 fps no obliga a codificar cientos de cuadros por segundo.
    outputFps = Math.min(30.0, fps);
    if (!Double.isFinite(outputFps) || outputFps <= 0)
      throw new IOException("Tasa de vídeo inválida.");
    storage.checkSpace();
    target = storage.next("video-1280x720", ".mp4");
    Path log = Files.createFile(Paths.get(target + ".log"));
    process =
        new ProcessBuilder(
                "ffmpeg",
                "-nostdin",
                "-n",
                "-hide_banner",
                "-loglevel",
                "warning",
                "-f",
                "rawvideo",
                "-pixel_format",
                "rgb24",
                "-video_size",
                "1280x720",
                "-framerate",
                Double.toString(outputFps),
                "-i",
                "pipe:0",
                "-an",
                "-c:v",
                "libx264",
                "-preset",
                "veryfast",
                "-pix_fmt",
                "yuv420p",
                "-movflags",
                "+faststart",
                target.toString())
            .redirectError(log.toFile())
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .start();
    input = process.getOutputStream();
  }

  synchronized void offer(byte[] rgb) {
    if (!finishing && !queue.offer(new Frame(rgb.clone(), System.nanoTime())))
      dropped.incrementAndGet();
  }

  synchronized void finishRecording() {
    if (!finishing) {
      stoppedAt = System.nanoTime();
      finishing = true;
    }
  }

  void abort() {
    finishRecording();
    process.destroyForcibly();
  }

  private void write(byte[] frame) throws IOException {
    if (!process.isAlive())
      throw new IOException("El codificador se detuvo. Consulte el registro del vídeo.");
    input.write(frame);
    written++;
    // La comprobación periódica incluye también los cuadros repetidos al finalizar.
    if (written % 10 == 0) storage.checkSpace();
  }

  @Override
  public void run() {
    Thread watchdog =
        new Thread(
            () -> {
              while (isAlive()) {
                try {
                  Thread.sleep(500);
                  if (finishing) {
                    join(20000);
                    if (isAlive()) process.destroyForcibly();
                    return;
                  }
                } catch (InterruptedException ex) {
                  return;
                }
              }
            },
            "microscope-video-watchdog");
    watchdog.setDaemon(true);
    watchdog.start();
    long first = 0;
    byte[] previous = null;
    try {
      while (!finishing || !queue.isEmpty()) {
        Frame frame = queue.poll(200, TimeUnit.MILLISECONDS);
        if (frame == null) continue;
        if (previous == null) {
          first = frame.time;
          previous = frame.rgb;
        }
        long index = Math.round((frame.time - first) * outputFps / 1_000_000_000.0);
        if (index < written)
          continue; // Más entradas que cuadros de salida: conservar la línea de tiempo.
        while (written < index) {
          write(previous);
          repeated++;
        }
        write(frame.rgb);
        previous = frame.rgb;
      }
      if (previous == null) throw new IOException("La grabación no recibió ningún fotograma.");
      // La duración termina cuando se pidió detener, no cuando el codificador vacía su cola.
      long end = Math.max(written, Math.round((stoppedAt - first) * outputFps / 1_000_000_000.0));
      while (written < end) {
        write(previous);
        repeated++;
      }
      input.close();
      if (!process.waitFor(15, TimeUnit.SECONDS))
        throw new IOException("Tiempo de finalización del vídeo agotado.");
      if (process.exitValue() != 0 || !Files.isRegularFile(target) || Files.size(target) == 0)
        throw new IOException("No se pudo finalizar MP4. Consulte el registro del vídeo.");
      status.accept("Vídeo guardado: " + target.getFileName());
    } catch (Exception ex) {
      failure = ex.getMessage();
      status.accept("Error de vídeo: " + failure);
    } finally {
      finishRecording();
      try {
        input.close();
      } catch (IOException ignored) {
      }
      ProcessTools.stop(process);
      try {
        Files.writeString(
            Paths.get(target + ".txt"),
            "Microscopio Color 1.1.3\nFecha: "
                + OffsetDateTime.now()
                + "\nEntrada RGB24: 1280x720\nMP4 H.264 YUV420p (con pérdida, sin audio)"
                + "\nTasa de salida: "
                + outputFps
                + "\nCuadros descartados en la cola: "
                + dropped.get()
                + "\nCuadros repetidos para conservar duración: "
                + repeated
                + "\nCuadros escritos: "
                + written
                + "\nEstado: "
                + (failure == null ? "finalizado" : "ERROR: " + failure)
                + "\n",
            StandardOpenOption.CREATE_NEW);
      } catch (IOException ex) {
        status.accept("No se pudo escribir la ficha del vídeo: " + ex.getMessage());
      }
    }
  }
}
