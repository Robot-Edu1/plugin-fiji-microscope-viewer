#!/usr/bin/env python3
# SPDX-License-Identifier: MIT
# Copyright (c) 2026 Rafael Cantillana - Robot-Edu.es
"""Pruebas sintéticas: no abre cámaras. Requiere ffmpeg y ffprobe del sistema."""

import os
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[1]


def main():
    api = os.environ.get("IJ_JAR", "")
    if not Path(api).is_file():
        raise SystemExit("Defina IJ_JAR con un ImageJ existente.")
    version = (ROOT / "VERSION").read_text().strip()
    jar = ROOT / f"dist/Microscopio_Color-{version}.jar"
    classes = ROOT / "build/test-classes"
    classes.mkdir(parents=True, exist_ok=True)
    classpath = os.pathsep.join([str(jar), str(Path(api).resolve())])
    sources = sorted((ROOT / "src/test/java").rglob("*.java"))
    subprocess.run(
        ["javac", "--release", "11", "-encoding", "UTF-8", "-Xlint:all", "-cp", classpath,
         "-d", str(classes), *map(str, sources)],
        check=True,
    )
    subprocess.run(
        ["java", "-Djava.awt.headless=true", "-ea", "-cp", str(classes) + os.pathsep + classpath,
         "es.robotedu.microscopio.PluginTests", str(ROOT / "build/test-output")],
        check=True,
        timeout=90,
    )


if __name__ == "__main__":
    main()
