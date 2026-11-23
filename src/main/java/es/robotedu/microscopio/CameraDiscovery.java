// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Rafael Cantillana - Robot-Edu.es
package es.robotedu.microscopio;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/**
 * Consulta las capacidades anunciadas por V4L2 sin cambiar permisos ni controles de cámara. Solo
 * ofrece tamaños discretos comprobados: no supone que cualquier cámara USB admita 720p.
 */
final class CameraDiscovery {
  private static final Pattern FORMAT = Pattern.compile("\\[\\d+\\]: '([^']+)'");
  private static final Pattern SIZE = Pattern.compile("Size: Discrete (\\d+)x(\\d+)");
  private static final Pattern FPS = Pattern.compile("\\(([0-9.]+) fps\\)");
  private static final Map<String, String> FORMATS =
      Map.of("MJPG", "mjpeg", "YUYV", "yuyv422", "NV12", "nv12", "RGB3", "rgb24", "BGR3", "bgr24");

  static List<CameraMode> parse(String device, String name, String text) {
    List<CameraMode> modes = new ArrayList<>();
    String format = null;
    boolean sizeOK = false;
    for (String line : text.split("\\R")) {
      Matcher f = FORMAT.matcher(line), s = SIZE.matcher(line), fps = FPS.matcher(line);
      if (f.find()) {
        format = FORMATS.get(f.group(1));
        sizeOK = false;
      } else if (line.contains("Size:")) {
        sizeOK =
            s.find() && Integer.parseInt(s.group(1)) == 1280 && Integer.parseInt(s.group(2)) == 720;
      } else if (format != null && sizeOK && fps.find()) {
        double value = Double.parseDouble(fps.group(1));
        if (Double.isFinite(value) && value > 0 && value <= 1000)
          modes.add(new CameraMode(device, name, format, value));
      }
    }
    // Preferimos MJPEG por el ancho de banda USB. La tasa siempre procede del dispositivo;
    // si admite hasta 30 fps elegimos la mayor, sin fabricar una capacidad inexistente.
    modes.sort(
        Comparator.comparing(
                (CameraMode m) -> !m.name.toLowerCase(Locale.ROOT).contains("microscop"))
            .thenComparing(m -> !m.format.equals("mjpeg"))
            .thenComparing(m -> m.fps > 30)
            .thenComparingDouble(m -> m.fps > 30 ? m.fps : -m.fps));
    return modes;
  }

  static List<CameraMode> discover() throws IOException {
    ProcessTools.run(5, "v4l2-ctl", "--version");
    List<CameraMode> modes = new ArrayList<>();
    List<String> errors = new ArrayList<>();
    Path sys = Paths.get("/sys/class/video4linux");
    if (Files.isDirectory(sys))
      try (DirectoryStream<Path> entries = Files.newDirectoryStream(sys, "video*")) {
        for (Path entry : entries) {
          String device = "/dev/" + entry.getFileName();
          if (!device.matches("/dev/video[0-9]+")) continue;
          try {
            String name = Files.readString(entry.resolve("name")).trim();
            List<CameraMode> found =
                parse(
                    device,
                    name,
                    ProcessTools.run(8, "v4l2-ctl", "--device", device, "--list-formats-ext"));
            if (!Files.isReadable(Paths.get(device)) || !Files.isWritable(Paths.get(device)))
              errors.add(device + ": sin permiso de acceso.");
            else modes.addAll(found);
          } catch (IOException ex) {
            errors.add(device + ": " + ex.getMessage());
          }
        }
      }
    modes.sort(
        Comparator.comparing(
                (CameraMode m) -> !m.name.toLowerCase(Locale.ROOT).contains("microscop"))
            .thenComparing(m -> m.device)
            .thenComparing(m -> !m.format.equals("mjpeg"))
            .thenComparing(m -> m.fps > 30)
            .thenComparingDouble(m -> m.fps > 30 ? m.fps : -m.fps));
    if (modes.isEmpty())
      throw new IOException(
          "No hay una cámara accesible que anuncie un modo de color discreto 1280×720.\n"
              + "Conecte el microscopio, cierre otras aplicaciones de cámara y consulte al"
              + " administrador si faltan permisos.\n"
              + String.join("\n", errors));
    return modes;
  }
}
