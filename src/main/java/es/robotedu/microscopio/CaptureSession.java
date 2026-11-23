// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Rafael Cantillana - Robot-Edu.es
package es.robotedu.microscopio;

import ij.ImagePlus;
import ij.gui.ImageWindow;
import ij.process.ColorProcessor;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.Path;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * Coordina una sesión de captura. La lectura USB vive fuera del hilo gráfico; la ventana solo se
 * modifica desde AWT. Los archivos pertenecen a la cuenta que abrió Fiji, nunca a la instalación
 * compartida.
 */
final class CaptureSession {
  private final CameraMode mode;
  private final UserStorage storage;
  private final Runnable ended;
  private final AtomicReference<int[]> latest = new AtomicReference<>();
  private final AtomicBoolean paintPending = new AtomicBoolean(),
      photoPending = new AtomicBoolean();
  private final ExecutorService actions =
      Executors.newSingleThreadExecutor(r -> new Thread(r, "microscope-actions"));
  private volatile Process camera;
  private volatile VideoRecorder video;
  private volatile boolean closing;
  private volatile long lastFrame;
  private ImagePlus image;
  private Label status;
  private Button photo, start, stop;
  private volatile String endMessage = "Cámara cerrada.";

  CaptureSession(CameraMode mode, UserStorage storage, Runnable ended) {
    this.mode = mode;
    this.storage = storage;
    this.ended = ended;
  }

  void start() {
    new Thread(this::capture, "microscope-acquisition").start();
  }

  private void capture() {
    ScheduledExecutorService watchdog =
        Executors.newSingleThreadScheduledExecutor(
            r -> {
              Thread t = new Thread(r, "microscope-camera-watchdog");
              t.setDaemon(true);
              return t;
            });
    try {
      storage.checkSpace();
      Path log = storage.log("camara");
      if (closing) return;
      Process started = new ProcessBuilder(mode.command()).redirectError(log.toFile()).start();
      camera = started;
      if (closing) return;
      EventQueue.invokeAndWait(this::createWindow);
      lastFrame = System.nanoTime();
      watchdog.scheduleAtFixedRate(
          () -> {
            if (!closing && System.nanoTime() - lastFrame > 15_000_000_000L) {
              endMessage =
                  "Sin fotogramas durante 15 s. Revise el cable y el registro " + log.getFileName();
              close();
            }
          },
          1,
          1,
          TimeUnit.SECONDS);
      try (InputStream input = started.getInputStream()) {
        byte[] bytes = new byte[CameraMode.FRAME_BYTES];
        boolean firstFrame = true;
        while (!closing) {
          RgbFrames.read(input, bytes);
          lastFrame = System.nanoTime();
          latest.set(RgbFrames.pixels(bytes));
          paint();
          if (firstFrame) {
            status("RGB 1280×720 · " + mode.name + " · Listo para foto y vídeo.");
            firstFrame = false;
          }
          VideoRecorder recorder = video;
          if (recorder != null) recorder.offer(bytes);
        }
      } catch (IOException ex) {
        if (!closing)
          endMessage =
              "Captura interrumpida: " + ex.getMessage() + " Registro: " + log.getFileName();
      }
    } catch (Exception ex) {
      endMessage = "No se pudo abrir la cámara: " + ex.getMessage();
    } finally {
      closing = true;
      watchdog.shutdownNow();
      ProcessTools.stop(camera);
      // Un clic pendiente podría estar creando el grabador. Primero vaciamos la cola de
      // acciones y después finalizamos el vídeo para no dejar un proceso huérfano.
      actions.shutdown();
      try {
        if (!actions.awaitTermination(5, TimeUnit.SECONDS)) actions.shutdownNow();
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        actions.shutdownNow();
      }
      VideoRecorder recorder = video;
      if (recorder != null) {
        recorder.finishRecording();
        try {
          recorder.join(23000);
          if (recorder.isAlive()) {
            recorder.abort();
            recorder.join(3000);
          }
        } catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
          recorder.abort();
        }
        if (recorder.failure != null)
          endMessage += " Error finalizando el vídeo; consulte su registro.";
      }
      status(endMessage);
      EventQueue.invokeLater(
          () -> {
            if (photo != null) {
              photo.setEnabled(false);
              start.setEnabled(false);
              stop.setEnabled(false);
            }
          });
      ended.run();
    }
  }

  void createWindow() {
    image = new ImagePlus("Microscopio Color RGB · 1280×720", new ColorProcessor(1280, 720));
    ImageWindow window =
        new ImageWindow(image) {
          private static final long serialVersionUID = 1L;

          @Override
          public boolean close() {
            CaptureSession.this.close();
            image.changes = false;
            return super.close();
          }
        };
    window.setLayout(new BorderLayout());
    window.add(window.getCanvas(), BorderLayout.CENTER);
    Panel panel = new Panel(new BorderLayout()), buttons = new Panel(new GridLayout(2, 3, 5, 5));
    Button plus = new Button("Zoom +"),
        minus = new Button("Zoom −"),
        close = new Button("Cerrar cámara");
    photo = new Button("Guardar foto JPEG");
    start = new Button("Iniciar vídeo MP4");
    stop = new Button("Detener vídeo");
    plus.addActionListener(e -> zoom(true));
    minus.addActionListener(e -> zoom(false));
    close.addActionListener(e -> close());
    photo.addActionListener(e -> savePhoto());
    start.addActionListener(e -> beginVideo());
    stop.addActionListener(
        e -> {
          VideoRecorder recorder = video;
          if (recorder != null) {
            recorder.finishRecording();
            status("Finalizando vídeo…");
          }
        });
    for (Button button : new Button[] {plus, minus, photo, start, stop, close}) buttons.add(button);
    status = new Label("Esperando imagen RGB · " + storage.directory);
    panel.add(buttons, BorderLayout.CENTER);
    panel.add(status, BorderLayout.SOUTH);
    window.add(panel, BorderLayout.SOUTH);
    window.pack();
    window.setVisible(true);
    // Reducimos solo la vista si no cabe en pantalla. El fotograma y el JPEG siguen
    // teniendo 1280×720 píxeles; el zoom no añade detalle óptico.
    Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
    for (int n = 0;
        n < 4 && (window.getWidth() > screen.width - 40 || window.getHeight() > screen.height - 70);
        n++) {
      window.getCanvas().zoomOut(0, 0);
      window.pack();
    }
  }

  // Evitamos acumular eventos de repintado: si la pantalla va lenta, mostramos el último
  // fotograma disponible sin bloquear la adquisición ni guardar una cola ilimitada.
  private void paint() {
    if (paintPending.compareAndSet(false, true))
      EventQueue.invokeLater(
          () -> {
            try {
              int[] rgb = latest.get();
              if (rgb != null && image != null && image.getWindow() != null) {
                image.getProcessor().setPixels(rgb);
                image.updateAndDraw();
              }
            } finally {
              paintPending.set(false);
            }
          });
  }

  private void zoom(boolean in) {
    if (in) image.getCanvas().zoomIn(0, 0);
    else image.getCanvas().zoomOut(0, 0);
  }

  private void savePhoto() {
    int[] rgb = latest.get();
    if (closing || rgb == null) {
      status("Espere a que la cámara esté enviando imagen.");
      return;
    }
    if (!photoPending.compareAndSet(false, true)) return;
    try {
      actions.submit(
          () -> {
            try {
              status("Foto guardada: " + storage.jpeg(rgb).getFileName());
            } catch (IOException ex) {
              status("Error de fotografía: " + ex.getMessage());
            } finally {
              photoPending.set(false);
            }
          });
    } catch (RejectedExecutionException ex) {
      photoPending.set(false);
      status("La sesión está cerrándose.");
    }
  }

  private void beginVideo() {
    if (closing || latest.get() == null) {
      status("Espere a que la cámara esté enviando imagen.");
      return;
    }
    try {
      actions.submit(
          () -> {
            if (closing) return;
            if (video != null && video.isAlive()) {
              status("El vídeo ya está activo o se está finalizando.");
              return;
            }
            try {
              video = new VideoRecorder(storage, mode.fps, this::status);
              video.start();
              status("Grabando MP4. Pulse Detener vídeo para finalizar.");
            } catch (IOException ex) {
              status("No se pudo iniciar vídeo: " + ex.getMessage());
            }
          });
    } catch (RejectedExecutionException ex) {
      status("La sesión está cerrándose.");
    }
  }

  void close() {
    closing = true;
    Process process = camera;
    if (process != null) process.destroy();
    VideoRecorder recorder = video;
    if (recorder != null) recorder.finishRecording();
  }

  void status(String message) {
    EventQueue.invokeLater(
        () -> {
          if (status != null) status.setText(message);
          ij.IJ.showStatus(message);
        });
  }
}
