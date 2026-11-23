#!/usr/bin/env python3
# SPDX-License-Identifier: MIT
# Copyright (c) 2026 Rafael Cantillana - Robot-Edu.es
"""Compila solo el plugin usando una API ImageJ local, sin descargas ni instalación."""

import argparse
import os
from pathlib import Path
import subprocess
import tempfile
import zipfile

ROOT = Path(__file__).resolve().parents[1]


def compile_classes(api, classes):
    """ImageJ está en el classpath; sus clases nunca se copian al artefacto."""
    sources = sorted((ROOT / "src/main/java").rglob("*.java"))
    subprocess.run(
        [os.environ.get("JAVAC", "javac"), "--release", "11", "-encoding", "UTF-8",
         "-Xlint:all", "-d", str(classes), "-cp", str(api), *map(str, sources)],
        check=True,
    )


def collect_entries(classes, version):
    entries = {str(path.relative_to(classes)): path.read_bytes()
               for path in classes.rglob("*.class")}
    resources = ROOT / "src/main/resources"
    for path in resources.rglob("*"):
        if path.is_file():
            entries[str(path.relative_to(resources))] = path.read_bytes()
    for notice in ("LICENSE", "NOTICE"):
        entries["META-INF/" + notice] = (ROOT / notice).read_bytes()
    entries["META-INF/MANIFEST.MF"] = (
        "Manifest-Version: 1.0\r\n"
        "Implementation-Title: Microscopio Color para Fiji\r\n"
        "Implementation-Version: " + version + "\r\n"
        "Implementation-Vendor: Robot-Edu.es\r\n\r\n"
    ).encode("utf-8")
    return entries


def write_jar(entries, output):
    # Orden, fecha y permisos estables evitan diferencias por el directorio o la hora local.
    # Esta fecha forma parte del formato del artefacto; no es la fecha de cada compilación.
    with zipfile.ZipFile(output, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as jar:
        for name in sorted(entries):
            info = zipfile.ZipInfo(name, (2026, 11, 16, 0, 0, 0))
            info.compress_type = zipfile.ZIP_DEFLATED
            info.create_system = 3
            info.external_attr = 0o100644 << 16
            jar.writestr(info, entries[name])


def main():
    version = (ROOT / "VERSION").read_text().strip()
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--ij-jar", default=os.environ.get("IJ_JAR"))
    parser.add_argument("--output", default=str(ROOT / "dist" / f"Microscopio_Color-{version}.jar"))
    args = parser.parse_args()
    if not args.ij_jar or not Path(args.ij_jar).is_file():
        parser.error("Indique --ij-jar /ruta/al/ij.jar o IJ_JAR. No se descargan dependencias.")

    output = Path(args.output).resolve()
    output.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix="microscopio-build-") as temporary:
        classes = Path(temporary) / "classes"
        classes.mkdir()
        compile_classes(Path(args.ij_jar).resolve(), classes)
        entries = collect_entries(classes, version)
        # Reemplazo atómico: una compilación interrumpida no sustituye el JAR por uno parcial.
        with tempfile.NamedTemporaryFile(dir=output.parent, prefix=".plugin-", delete=False) as stream:
            staged = Path(stream.name)
        try:
            write_jar(entries, staged)
            staged.chmod(0o644)
            staged.replace(output)
        finally:
            staged.unlink(missing_ok=True)
    print(output)


if __name__ == "__main__":
    main()
