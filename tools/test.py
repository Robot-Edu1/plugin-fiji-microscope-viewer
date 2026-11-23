#!/usr/bin/env python3
# SPDX-License-Identifier: MIT
# Copyright (c) 2026 Rafael Cantillana - Robot-Edu.es
"""Pruebas sintéticas: no abre cámaras. Requiere ffmpeg y ffprobe del sistema."""

import argparse
import shutil
import tempfile
import os
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[1]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--slow-encoder", action="store_true", help="Retrasar FFmpeg 1,2 s para comprobar independencia temporal")
    args = parser.parse_args()
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
    command = ["java", "-Djava.awt.headless=true", "-ea", "-cp", str(classes) + os.pathsep + classpath,
               "es.robotedu.microscopio.PluginTests", str(ROOT / "build/test-output")]
    # Solo se sustituye el ejecutable en el entorno del test; el plugin no conoce esta opción.
    with tempfile.TemporaryDirectory(prefix="microscope-test-encoder-") as temporary:
        env = dict(os.environ)
        if args.slow_encoder:
            real = shutil.which("ffmpeg")
            if not real:
                raise SystemExit("FFmpeg no disponible para la prueba.")
            wrapper = Path(temporary) / "ffmpeg"
            wrapper.write_text("#!/usr/bin/python3\nimport os,sys,time\ntime.sleep(1.2)\nreal="
                               + repr(real) + "\nos.execv(real,[real,*sys.argv[1:]])\n")
            wrapper.chmod(0o755)
            env["PATH"] = temporary + os.pathsep + env["PATH"]
            print("PRUEBA: arranque de FFmpeg retrasado 1,2 s", flush=True)
        subprocess.run(command, check=True, timeout=90, env=env)


if __name__ == "__main__":
    main()
