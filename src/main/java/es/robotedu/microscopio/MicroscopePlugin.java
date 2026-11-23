// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Rafael Cantillana - Robot-Edu.es
package es.robotedu.microscopio;

import ij.IJ;
import ij.gui.GenericDialog;
import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Abre el diálogo de cámara y entrega la captura a una sesión independiente. El bloqueo ACTIVE es
 * por proceso Fiji; no es un bloqueo global entre usuarios del equipo.
 */
public final class MicroscopePlugin {
  public static final String VERSION = "1.1.1";
  private static final AtomicBoolean ACTIVE = new AtomicBoolean();

  public void run(String argument) {
    if ("version".equals(argument)) {
      IJ.log("Microscopio Color " + VERSION);
      return;
    }
    if (GraphicsEnvironment.isHeadless()) {
      IJ.log("Microscopio Color requiere una sesión gráfica para adquirir imágenes.");
      return;
    }
    if (!ACTIVE.compareAndSet(false, true)) {
      IJ.error("Microscopio Color", "Ya hay una captura activa en esta sesión de Fiji.");
      return;
    }
    new Thread(
            () -> {
              boolean transferred = false;
              try {
                if (!System.getProperty("os.name", "")
                    .toLowerCase(java.util.Locale.ROOT)
                    .contains("linux"))
                  throw new IOException("Este plugin utiliza la captura V4L2 de Linux.");
                ProcessTools.run(5, "ffmpeg", "-version");
                List<CameraMode> modes = CameraDiscovery.discover();
                CameraMode[] selected = {null};
                EventQueue.invokeAndWait(
                    () -> {
                      GenericDialog dialog = new GenericDialog("Microscopio Color · RGB 1280×720");
                      String[] choices =
                          modes.stream().map(CameraMode::label).toArray(String[]::new);
                      dialog.addChoice("Cámara y formato", choices, choices[0]);
                      dialog.addMessage(
                          "Captura nativa 1280×720. Guardado en su carpeta de imágenes.\n"
                              + "Seleccione el microscopio, no la webcam integrada.\n"
                              + "Los permisos y dependencias los gestiona el sistema operativo.");
                      dialog.showDialog();
                      if (!dialog.wasCanceled())
                        selected[0] = modes.get(dialog.getNextChoiceIndex());
                    });
                if (selected[0] == null) return;
                UserStorage storage = UserStorage.current();
                CaptureSession session =
                    new CaptureSession(selected[0], storage, () -> ACTIVE.set(false));
                session.start();
                transferred = true;
              } catch (Exception ex) {
                String message = ex.getMessage() == null ? ex.toString() : ex.getMessage();
                EventQueue.invokeLater(() -> IJ.error("Microscopio Color", message));
              } finally {
                if (!transferred) ACTIVE.set(false);
              }
            },
            "microscope-startup")
        .start();
  }
}
