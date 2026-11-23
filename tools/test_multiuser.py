#!/usr/bin/env python3
# SPDX-License-Identifier: MIT
# Copyright (c) 2026 Rafael Cantillana - Robot-Edu.es
"""Optional isolated-profile checks. Requires bubblewrap; no host accounts modified."""
import os,pathlib,subprocess,json
base=pathlib.Path(__file__).resolve().parents[1]
(base/'verificacion').mkdir(exist_ok=True)
version=(base/'VERSION').read_text().strip()
ij=pathlib.Path(os.environ['IJ_JAR']).resolve()
results=[]
for uid,folder in [(1001,'Imatges'),(1002,'Mis imágenes')]:
    script='''set -eu
mkdir -p /tmp/alumno/.config
printf 'XDG_PICTURES_DIR="$HOME/%s"\n' "$1" > /tmp/alumno/.config/user-dirs.dirs
id
java -Djava.awt.headless=true -Duser.home=/tmp/alumno -cp /mnt/build/test-classes:/mnt/dist/Microscopio_Color-$2.jar:/tmp/ij.jar es.robotedu.microscopio.UserProfileTest "/tmp/alumno/$1/Microscopio"
'''
    cmd=['bwrap','--unshare-user','--uid',str(uid),'--gid',str(uid),'--ro-bind','/','/','--ro-bind',str(base),'/mnt','--unshare-net','--unshare-pid','--dev','/dev','--proc','/proc','--tmpfs','/tmp','--ro-bind',str(ij),'/tmp/ij.jar','--setenv','HOME','/tmp/alumno','--setenv','XDG_CONFIG_HOME','/tmp/alumno/.config','--chdir','/tmp','/bin/bash','-c',script,'test-profile',folder,version]
    p=subprocess.run(cmd,capture_output=True,text=True,timeout=30)
    print(p.stdout, end='');print(p.stderr,end='')
    results.append({'uid':uid,'exit':p.returncode,'output':p.stdout,'error':p.stderr})
    if p.returncode:raise SystemExit(p.returncode)
(base/'verificacion/03-multiusuario.json').write_text(json.dumps(results,ensure_ascii=False,indent=2)+'\n')
print('OK 2 UID aislados; no se han creado usuarios ni alterado grupos del sistema anfitrión.')
