// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Rafael Cantillana - Robot-Edu.es
package es.robotedu.microscopio;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/**
 * Ejecuta herramientas del sistema con argumentos separados, sin intérprete de shell. Leemos su
 * salida mientras están en marcha para no bloquear una tubería llena. Las consultas tienen un
 * límite de espera para que una cámara no congele el diálogo.
 */
final class ProcessTools {
  private ProcessTools() {}

  static String run(long seconds, String... args) throws IOException {
    Process process = new ProcessBuilder(args).redirectErrorStream(true).start();
    ExecutorService reader =
        Executors.newSingleThreadExecutor(
            r -> {
              Thread t = new Thread(r, "microscope-command-output");
              t.setDaemon(true);
              return t;
            });
    Future<String> output =
        reader.submit(
            () -> {
              ByteArrayOutputStream bytes = new ByteArrayOutputStream();
              byte[] buffer = new byte[4096];
              int count;
              try (InputStream input = process.getInputStream()) {
                while ((count = input.read(buffer)) != -1) {
                  if (bytes.size() + count > 1024 * 1024)
                    throw new IOException("Respuesta de herramienta excesiva.");
                  bytes.write(buffer, 0, count);
                }
              }
              return bytes.toString(StandardCharsets.UTF_8.name());
            });
    try {
      if (!process.waitFor(seconds, TimeUnit.SECONDS))
        throw new IOException("Tiempo agotado: " + args[0]);
      String text = output.get(2, TimeUnit.SECONDS).trim();
      if (process.exitValue() != 0) throw new IOException(args[0] + ": " + text);
      return text;
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new IOException("Operación interrumpida.", ex);
    } catch (ExecutionException | TimeoutException ex) {
      throw new IOException("No se pudo consultar " + args[0], ex);
    } finally {
      process.destroyForcibly();
      reader.shutdownNow();
    }
  }

  static void stop(Process process) {
    if (process == null) return;
    process.destroy();
    try {
      if (!process.waitFor(2, TimeUnit.SECONDS)) process.destroyForcibly();
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      process.destroyForcibly();
    }
  }
}
