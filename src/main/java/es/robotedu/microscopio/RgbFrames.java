// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Rafael Cantillana - Robot-Edu.es
package es.robotedu.microscopio;

import java.io.*;

/** Trata cada fotograma como una unidad completa; una lectura parcial no se muestra como imagen. */
final class RgbFrames {
  static void read(InputStream input, byte[] frame) throws IOException {
    int offset = 0;
    while (offset < frame.length) {
      int n = input.read(frame, offset, frame.length - offset);
      if (n < 0) throw new EOFException("La cámara dejó de enviar un fotograma completo.");
      if (n == 0) {
        int b = input.read();
        if (b < 0) throw new EOFException();
        frame[offset++] = (byte) b;
      } else offset += n;
    }
  }

  static int[] pixels(byte[] frame) {
    if (frame.length != CameraMode.FRAME_BYTES)
      throw new IllegalArgumentException("Tamaño de fotograma incorrecto.");
    int[] rgb = new int[1280 * 720];
    for (int i = 0, p = 0; i < rgb.length; i++, p += 3)
      rgb[i] =
          0xff000000
              | ((frame[p] & 255) << 16)
              | ((frame[p + 1] & 255) << 8)
              | (frame[p + 2] & 255);
    return rgb;
  }
}
