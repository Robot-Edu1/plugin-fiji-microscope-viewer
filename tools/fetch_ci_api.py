#!/usr/bin/env python3
# SPDX-License-Identifier: MIT
"""Fetch only the pinned ImageJ API for CI. Not used by the plugin or offline build."""
import hashlib
import pathlib
import urllib.request

ROOT = pathlib.Path(__file__).resolve().parents[1]
URL = "https://repo.maven.apache.org/maven2/net/imagej/ij/1.54p/ij-1.54p.jar"
SHA256 = "2e1a09961dfb41cee66ddc821b2577a41a072566ce45a49bae69267099741e20"
target = ROOT / ".deps/ij-1.54p.jar"
if not target.is_file() or hashlib.sha256(target.read_bytes()).hexdigest() != SHA256:
    with urllib.request.urlopen(URL, timeout=45) as response:
        data = response.read(8 * 1024 * 1024)
    if hashlib.sha256(data).hexdigest() != SHA256:
        raise SystemExit("ImageJ API checksum mismatch; build stopped.")
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_bytes(data)
print(target)
