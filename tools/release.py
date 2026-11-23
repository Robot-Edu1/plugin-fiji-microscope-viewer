#!/usr/bin/env python3
# SPDX-License-Identifier: MIT
"""Build versioned assets from a clean, tagged Git tree. Never publishes by itself."""
import argparse
import gzip
import hashlib
import io
import pathlib
import re
import subprocess
import tarfile

ROOT = pathlib.Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("--tag", required=True, help="Existing tag matching VERSION, e.g. v1.1.2")
args = parser.parse_args()
version = (ROOT / "VERSION").read_text().strip()
if not re.fullmatch(r"[0-9]+\.[0-9]+\.[0-9]+", version) or args.tag != "v" + version:
    parser.error("The release tag must exactly match v + VERSION.")
def git(*args):
    return subprocess.check_output(["git", "-C", str(ROOT), *args])
if git("status", "--porcelain").strip():
    raise SystemExit("Commit the reviewed changes before building a release.")
head = git("rev-parse", "HEAD").strip()
if git("rev-parse", args.tag + "^{commit}").strip() != head:
    raise SystemExit("The tag must point to the current commit.")
# Build and test the exact committed source. Tests only use synthetic frames.
subprocess.run(["make", "test"], cwd=ROOT, check=True)
jar = ROOT / "dist" / ("Microscopio_Color-" + version + ".jar")
# Refuse stale hard-coded version strings inside source.
source = (ROOT / "src/main/java/es/robotedu/microscopio/MicroscopePlugin.java").read_text()
if not re.search(r'VERSION\s*=\s*"' + re.escape(version) + r'"', source):
    raise SystemExit("Update the plugin Java VERSION constant before tagging.")
if git("status", "--porcelain").strip():
    raise SystemExit("Build changed tracked source; release stopped.")
out = ROOT / "release"
out.mkdir(exist_ok=True)
jar_target = out / jar.name
jar_target.write_bytes(jar.read_bytes())
epoch = int(git("show", "-s", "--format=%ct", "HEAD"))
prefix = "microscopio-color-fiji-" + version
# Archive only committed source, never generated images, private logs or the .git directory.
archive = out / (prefix + ".tar.gz")
with archive.open("wb") as output:
    with gzip.GzipFile(filename="", mode="wb", fileobj=output, mtime=0) as compressed:
        with tarfile.open(fileobj=compressed, mode="w|") as tar:
            for raw in sorted(git("ls-files", "-z").split(b"\0")):
                if not raw:
                    continue
                name = raw.decode()
                data = git("show", "HEAD:" + name)
                info = tarfile.TarInfo(prefix + "/" + name)
                info.size = len(data)
                info.mtime = epoch
                info.mode = 0o644
                tar.addfile(info, io.BytesIO(data))
assets = [jar_target, archive]
(out / "SHA256SUMS.txt").write_text("".join(
    hashlib.sha256(path.read_bytes()).hexdigest() + "  " + path.name + "\n"
    for path in assets
))
print("Release assets ready in " + str(out))
