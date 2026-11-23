// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Rafael Cantillana - Robot-Edu.es
package es.robotedu.microscopio;

import java.util.*;

/** Modo nativo anunciado por la cámara y orden FFmpeg correspondiente. */
final class CameraMode {
  static final int WIDTH = 1280, HEIGHT = 720, FRAME_BYTES = WIDTH * HEIGHT * 3;
  final String device, name, format;
  final double fps;

  CameraMode(String device, String name, String format, double fps) {
    if (!device.matches("/dev/video[0-9]+")
        || !Arrays.asList("mjpeg", "yuyv422", "nv12", "rgb24", "bgr24").contains(format)
        || !Double.isFinite(fps)
        || fps <= 0
        || fps > 1000) throw new IllegalArgumentException("Modo de cámara no válido.");
    this.device = device;
    this.name = name;
    this.format = format;
    this.fps = fps;
  }

  String label() {
    return name
        + " ("
        + device
        + ") · "
        + format
        + " · "
        + String.format(Locale.ROOT, "%.3f fps", fps);
  }

  List<String> command() {
    return Arrays.asList(
        "ffmpeg",
        "-nostdin",
        "-hide_banner",
        "-loglevel",
        "warning",
        "-f",
        "v4l2",
        "-input_format",
        format,
        "-video_size",
        "1280x720",
        "-framerate",
        Double.toString(fps),
        "-i",
        device,
        "-an",
        "-f",
        "rawvideo",
        "-pix_fmt",
        "rgb24",
        "pipe:1");
  }
}
