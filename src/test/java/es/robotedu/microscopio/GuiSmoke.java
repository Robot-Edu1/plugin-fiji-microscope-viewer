// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Rafael Cantillana - Robot-Edu.es
package es.robotedu.microscopio;

import ij.ImagePlus;
import java.awt.EventQueue;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.concurrent.*;

public final class GuiSmoke {
  public static void main(String[] args) throws Exception {
    Path out = Paths.get(args[0]);
    Files.createDirectories(out);
    CountDownLatch ended = new CountDownLatch(1);
    CaptureSession session =
        new CaptureSession(
            new CameraMode("/dev/video9999", "PRUEBA SINTETICA", "mjpeg", 15),
            new UserStorage(out.resolve("capturas")),
            ended::countDown);
    session.start();
    Field imageField = CaptureSession.class.getDeclaredField("image");
    imageField.setAccessible(true);
    Field latestField = CaptureSession.class.getDeclaredField("latest");
    latestField.setAccessible(true);
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(12);
    java.util.concurrent.atomic.AtomicReference<?> latest =
        (java.util.concurrent.atomic.AtomicReference<?>) latestField.get(session);
    while (latest.get() == null && System.nanoTime() < deadline) Thread.sleep(100);
    if (latest.get() == null) throw new AssertionError("No llegaron fotogramas sintéticos.");
    ImagePlus image = (ImagePlus) imageField.get(session);
    EventQueue.invokeAndWait(
        () -> image.setTitle("PRUEBA SINTETICA - Microscopio Color 1.1.2 - RGB 1280x720"));
    Method photo = CaptureSession.class.getDeclaredMethod("savePhoto"),
        video = CaptureSession.class.getDeclaredMethod("beginVideo");
    photo.setAccessible(true);
    video.setAccessible(true);
    EventQueue.invokeAndWait(
        () -> {
          try {
            photo.invoke(session);
            video.invoke(session);
          } catch (Exception ex) {
            throw new RuntimeException(ex);
          }
        });
    Thread.sleep(2500);
    Files.writeString(
        out.resolve("ready"),
        "Interfaz y controles reales con entrada sintética. No cámara física.");
    deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
    while (!Files.exists(out.resolve("finish")) && System.nanoTime() < deadline) Thread.sleep(100);
    session.close();
    if (!ended.await(28, TimeUnit.SECONDS)) throw new AssertionError("La sesión no terminó.");
    EventQueue.invokeAndWait(() -> image.getWindow().close());
    try (java.util.stream.Stream<Path> files = Files.list(out.resolve("capturas"))) {
      if (files.filter(p -> p.toString().endsWith(".jpg")).count() != 1)
        throw new AssertionError("JPEG del control de foto no guardado");
    }
    try (java.util.stream.Stream<Path> files = Files.list(out.resolve("capturas"))) {
      if (files.filter(p -> p.toString().endsWith(".mp4")).count() != 1)
        throw new AssertionError("MP4 del control de vídeo no guardado");
    }
    System.out.println(
        "OK interfaz de captura, foto, vídeo y cierre limpio con fotogramas sintéticos. No cámara"
            + " física.");
    System.exit(0);
  }
}
