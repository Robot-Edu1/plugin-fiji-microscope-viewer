// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Rafael Cantillana - Robot-Edu.es
import es.robotedu.microscopio.MicroscopePlugin;
import ij.plugin.PlugIn;

/** Standard ImageJ 1.x entry point, also usable in Fiji's ImageJ legacy layer. */
public final class Microscopio_Color implements PlugIn {
  @Override
  public void run(String argument) {
    new MicroscopePlugin().run(argument);
  }
}
