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
 * Graba MP4 sin bloquear la vista. La cola acotada limita memoria; si el codificador no sigue el
 * ritmo se cuentan los descartes. El archivo usa H.264/YUV420p: la entrada RGB no implica vídeo RGB
 * sin compresión.
 */
final class VideoRecorder extends Thread {
  private final BlockingQueue<byte[]> queue = new ArrayBlockingQueue<>(2);
  private final Process process;
  private final OutputStream input;
  final Path target;
  private final UserStorage storage;
  private final Consumer<String> status;
  private final AtomicLong dropped = new AtomicLong();
  private volatile boolean finishing;
  volatile String failure;

  VideoRecorder(UserStorage storage, double fps, Consumer<String> status) throws IOException {
    super("microscope-video-writer");
    this.storage = storage;
    this.status = status;
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
                "-use_wallclock_as_timestamps",
                "1",
                "-f",
                "rawvideo",
                "-pixel_format",
                "rgb24",
                "-video_size",
                "1280x720",
                "-framerate",
                Double.toString(fps),
                "-i",
                "pipe:0",
                "-an",
                "-c:v",
                "libx264",
                "-preset",
                "veryfast",
                "-pix_fmt",
                "yuv420p",
                "-vsync",
                "vfr",
                "-movflags",
                "+faststart",
                target.toString())
            .redirectError(log.toFile())
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .start();
    input = process.getOutputStream();
  }

  void offer(byte[] frame) {
    if (!finishing && !queue.offer(frame.clone())) dropped.incrementAndGet();
  }

  void finishRecording() {
    finishing = true;
  }

  void abort() {
    finishing = true;
    process.destroyForcibly();
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
    try {
      long lastSpace = 0;
      while (!finishing || !queue.isEmpty()) {
        if (System.nanoTime() - lastSpace > 1_000_000_000L) {
          storage.checkSpace();
          lastSpace = System.nanoTime();
        }
        byte[] frame = queue.poll(200, TimeUnit.MILLISECONDS);
        if (frame != null) input.write(frame);
        if (!process.isAlive())
          throw new IOException(
              "El codificador se detuvo. Consulte " + target.getFileName() + ".log");
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
      finishing = true;
      try {
        input.close();
      } catch (IOException ignored) {
      }
      ProcessTools.stop(process);
      try {
        Files.writeString(
            Paths.get(target + ".txt"),
            "Microscopio Color 1.1.1\nFecha: "
                + OffsetDateTime.now()
                + "\n"
                + "Entrada RGB24: 1280x720\n"
                + "MP4 H.264 YUV420p (compresión con pérdida, sin audio)\n"
                + "Cuadros descartados: "
                + dropped.get()
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
