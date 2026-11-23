// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Rafael Cantillana - Robot-Edu.es
package es.robotedu.microscopio;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;

public final class PluginTests {
  private static void check(boolean value, String name) {
    if (!value) throw new AssertionError(name);
    System.out.println("OK " + name);
  }

  public static void main(String[] args) throws Exception {
    String formats =
        "[0]: 'GREY'\n"
            + " Size: Discrete 1280x720\n"
            + " Interval: Discrete 0.033s (30.000 fps)\n"
            + "[1]: 'MJPG'\n"
            + " Size: Discrete 640x480\n"
            + " Interval: Discrete 0.033s (30.000 fps)\n"
            + " Size: Discrete 1280x720\n"
            + " Interval: Discrete 0.067s (15.000 fps)\n"
            + " Interval: Discrete 0.033s (30.000 fps)\n"
            + "[2]: 'YUYV'\n"
            + " Size: Discrete 1280x720\n"
            + " Interval: Discrete 0.100s (10.000 fps)\n"
            + " Size: Stepwise 16x16 - 4096x4096 with step 2/2\n"
            + " Interval: Discrete 0.033s (30.000 fps)\n";
    List<CameraMode> modes = CameraDiscovery.parse("/dev/video2", "USB Microscope", formats);
    check(
        modes.size() == 3 && modes.get(0).fps == 30 && modes.get(0).format.equals("mjpeg"),
        "modos de color 1280x720; GREY y tamaños no anunciados descartados");
    check(
        modes.get(0).command().contains("rgb24") && !modes.get(0).command().contains("scale"),
        "RGB24 sin interpolación");
    boolean rejected = false;
    try {
      new CameraMode("/tmp/fake", "X", "mjpeg", 30);
    } catch (IllegalArgumentException e) {
      rejected = true;
    }
    check(rejected, "ruta de dispositivo restringida");
    byte[] shortFrame = new byte[8];
    rejected = false;
    try {
      RgbFrames.read(new ByteArrayInputStream(new byte[7]), shortFrame);
    } catch (EOFException e) {
      rejected = true;
    }
    check(rejected, "fotograma incompleto rechazado");
    long before = System.nanoTime();
    boolean timedOut = false;
    try {
      ProcessTools.run(1, "/bin/sleep", "5");
    } catch (IOException ex) {
      timedOut = true;
    }
    check(
        timedOut && System.nanoTime() - before < 4_000_000_000L,
        "consulta bloqueada interrumpida por timeout");
    Path work = Paths.get(args[0]);
    Files.createDirectories(work);
    check(
        UserStorage.resolvePictures(work, "/tmp/Imatges usuari")
            .equals(Paths.get("/tmp/Imatges usuari/Microscopio")),
        "ruta XDG absoluta con espacios");
    check(
        UserStorage.resolvePictures(work, "relative").equals(work.resolve("Imágenes/Microscopio")),
        "fallback relativo al hogar actual");
    UserStorage storage = new UserStorage(work.resolve("Imágenes/Microscopio"));
    byte[] bytes = pattern();
    int[] pixels = RgbFrames.pixels(bytes);
    check(
        (pixels[0] & 0xffffff) == 0xff0000
            && (pixels[600] & 0xffffff) == 0x00ff00
            && (pixels[1000] & 0xffffff) == 0x0000ff,
        "canales rojo, verde y azul conservados");
    Path a = storage.jpeg(pixels), b = storage.jpeg(pixels);
    BufferedImage im = ImageIO.read(a.toFile());
    check(
        im.getWidth() == 1280 && im.getHeight() == 720 && !a.equals(b),
        "JPEG 1280x720 con nombres únicos");
    int red = im.getRGB(100, 100), green = im.getRGB(600, 100), blue = im.getRGB(1000, 100);
    check(
        ((red >> 16) & 255) > 240
            && ((green >> 8) & 255) > 240
            && (blue & 255) > 240
            && (red & 255) < 10,
        "JPEG preserva color de patrón RGB (con pérdida JPEG)");
    VideoRecorder video = new VideoRecorder(storage, 30, System.out::println);
    video.start();
    for (int i = 0; i < 12; i++) {
      video.offer(bytes);
      Thread.sleep(200);
    }
    video.finishRecording();
    video.join(25000);
    check(!video.isAlive() && video.failure == null, "MP4 finalizado sin bloqueo");
    String probe =
        ProcessTools.run(
            8,
            "ffprobe",
            "-v",
            "error",
            "-select_streams",
            "v:0",
            "-show_entries",
            "stream=width,height,pix_fmt",
            "-of",
            "csv=p=0",
            video.target.toString());
    check(
        probe.startsWith("1280,720,yuv420p"),
        "MP4 1280x720 reproducible; H.264 YUV420p documentado");
    double duration =
        Double.parseDouble(
            ProcessTools.run(
                8,
                "ffprobe",
                "-v",
                "error",
                "-show_entries",
                "format=duration",
                "-of",
                "default=noprint_wrappers=1:nokey=1",
                video.target.toString()));
    check(
        duration >= 1.8 && duration <= 3.5,
        "duración vídeo lento: " + duration + " s para ~2,4 s de envío");
    // Load the exact entry class from the built JAR without a camera or GUI.
    Object plugin = Class.forName("Microscopio_Color").getDeclaredConstructor().newInstance();
    check(plugin instanceof ij.plugin.PlugIn, "entrada estándar ImageJ PlugIn");
    ((ij.plugin.PlugIn) plugin).run("version");
    check(
        PluginTests.class.getClassLoader().getResource("plugins.config") != null,
        "registro de menú incluido");
    System.out.println("PENDIENTE: microscopio físico, LliureX limpio y aceptación SICE.");
  }

  static byte[] pattern() {
    byte[] p = new byte[CameraMode.FRAME_BYTES];
    for (int y = 0; y < 720; y++)
      for (int x = 0; x < 1280; x++)
        p[(y * 1280 + x) * 3 + (x < 426 ? 0 : x < 853 ? 1 : 2)] = (byte) 255;
    return p;
  }
}
