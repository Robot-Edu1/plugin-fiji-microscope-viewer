// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Rafael Cantillana - Robot-Edu.es
package es.robotedu.microscopio;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javax.imageio.*;
import javax.imageio.stream.ImageOutputStream;

/**
 * Resuelve la salida de la cuenta actual. Respetamos la carpeta localizada XDG y no escribimos
 * junto al JAR. Los nombres únicos evitan sobrescribir trabajos cuando se pulsa varias veces.
 */
final class UserStorage {
  static final long MIN_FREE = 50L * 1024 * 1024;
  final Path directory;

  UserStorage(Path directory) throws IOException {
    this.directory = directory.toAbsolutePath().normalize();
    Files.createDirectories(this.directory);
    if (!Files.isWritable(this.directory))
      throw new IOException("No se puede escribir en " + this.directory);
  }

  static Path resolvePictures(Path home, String xdg) {
    if (xdg != null && !xdg.isBlank()) {
      Path path = Paths.get(xdg.trim());
      if (path.isAbsolute()) return path.resolve("Microscopio");
    }
    for (String name : new String[] {"Imágenes", "Imatges", "Pictures"}) {
      Path path = home.resolve(name);
      if (Files.isDirectory(path)) return path.resolve("Microscopio");
    }
    return home.resolve("Imágenes").resolve("Microscopio");
  }

  static UserStorage current() throws IOException {
    String home = System.getProperty("user.home");
    if (home == null || !Paths.get(home).isAbsolute())
      throw new IOException("La cuenta no tiene carpeta personal válida.");
    String xdg = null;
    try {
      xdg = ProcessTools.run(3, "xdg-user-dir", "PICTURES");
    } catch (IOException ignored) {
      /* documented home-relative fallback */
    }
    return new UserStorage(resolvePictures(Paths.get(home), xdg));
  }

  Path next(String prefix, String suffix) {
    String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS"));
    return directory.resolve(
        prefix + "-" + stamp + "-" + UUID.randomUUID().toString().substring(0, 8) + suffix);
  }

  void checkSpace() throws IOException {
    if (Files.getFileStore(directory).getUsableSpace() < MIN_FREE)
      throw new IOException("Se necesitan al menos 50 MB libres en " + directory);
  }

  Path log(String prefix) throws IOException {
    return Files.createFile(next(prefix, ".log"));
  }

  Path jpeg(int[] rgb) throws IOException {
    checkSpace();
    Path target = next("foto-1280x720", ".jpg");
    BufferedImage image = new BufferedImage(1280, 720, BufferedImage.TYPE_INT_RGB);
    image.setRGB(0, 0, 1280, 720, rgb, 0, 1280);
    ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
    try (OutputStream stream = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW);
        ImageOutputStream output = ImageIO.createImageOutputStream(stream)) {
      ImageWriteParam param = writer.getDefaultWriteParam();
      param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
      param.setCompressionQuality(.95f);
      writer.setOutput(output);
      writer.write(null, new IIOImage(image, null, null), param);
    } finally {
      writer.dispose();
    }
    Files.writeString(
        Paths.get(target + ".txt"),
        "Microscopio Color 1.1.3\nCaptura RGB: 1280x720\nInterpolación: ninguna\nFecha: "
            + OffsetDateTime.now()
            + "\n",
        StandardOpenOption.CREATE_NEW);
    return target;
  }
}
