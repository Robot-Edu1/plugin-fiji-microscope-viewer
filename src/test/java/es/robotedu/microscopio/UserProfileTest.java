// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Rafael Cantillana - Robot-Edu.es
package es.robotedu.microscopio;

import java.nio.file.*;

public final class UserProfileTest {
  public static void main(String[] args) throws Exception {
    UserStorage storage = UserStorage.current();
    Path expected = Paths.get(args[0]);
    if (!storage.directory.equals(expected))
      throw new AssertionError("Carpeta personal incorrecta: " + storage.directory);
    Path photo = storage.jpeg(RgbFrames.pixels(PluginTests.pattern()));
    if (!Files.isRegularFile(photo)) throw new AssertionError("Sin fotografía");
    boolean denied = false;
    try {
      Files.writeString(Paths.get("/mnt/NO-DEBE-CREARSE"), "x");
    } catch (java.io.IOException ex) {
      denied = true;
    }
    if (!denied) throw new AssertionError("El plugin compartido no era solo lectura");
    System.out.println(
        "OK perfil independiente: "
            + storage.directory
            + "; JAR compartido solo lectura; JPEG guardado");
  }
}
