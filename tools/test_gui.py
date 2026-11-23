#!/usr/bin/env python3
# SPDX-License-Identifier: MIT
# Copyright (c) 2026 Rafael Cantillana - Robot-Edu.es
"""Optional GUI smoke using only lavfi generated frames. Requires DISPLAY, xdotool, import."""
import os,pathlib,subprocess,tempfile,time,shutil
base=pathlib.Path(__file__).resolve().parents[1]
(base/'verificacion').mkdir(exist_ok=True)
version=(base/'VERSION').read_text().strip()
ij=pathlib.Path(os.environ['IJ_JAR']).resolve()
real_ffmpeg=shutil.which('ffmpeg')
with tempfile.TemporaryDirectory(prefix='microscopio-gui-test-') as temp:
    work=pathlib.Path(temp); bin_dir=work/'bin';bin_dir.mkdir()
    # This fixture substitutes the capture process only. Production files are unchanged.
    wrapper=bin_dir/'ffmpeg'
    wrapper.write_text('#!/usr/bin/python3\nimport os,sys\nreal='+repr(real_ffmpeg)+'\nargs=sys.argv[1:]\nif "v4l2" in args:\n args=["-nostdin","-hide_banner","-loglevel","error","-re","-f","lavfi","-i","testsrc=size=1280x720:rate=15","-t","35","-f","rawvideo","-pix_fmt","rgb24","pipe:1"]\nos.execv(real,[real,*args])\n')
    wrapper.chmod(0o755)
    cp=os.pathsep.join([str(base/'build/test-classes'),str(base/('dist/Microscopio_Color-'+version+'.jar')),str(ij)])
    env=dict(os.environ,PATH=str(bin_dir)+os.pathsep+os.environ['PATH'])
    with (base/'verificacion/04-interfaz.txt').open('w') as log:
        p=subprocess.Popen(['java','-cp',cp,'es.robotedu.microscopio.GuiSmoke',str(work/'result')],stdout=log,stderr=subprocess.STDOUT,env=env)
        try:
            limit=time.monotonic()+20
            while not (work/'result/ready').exists() and time.monotonic()<limit and p.poll() is None:time.sleep(.1)
            if not (work/'result/ready').exists():raise RuntimeError('Interfaz de prueba no preparada; revise 04-interfaz.txt')
            ids=subprocess.check_output(['xdotool','search','--onlyvisible','--name','^PRUEBA SINTETICA - Microscopio Color'],env=env,text=True).split()
            subprocess.run(['import','-window',ids[-1],str(base/'verificacion/04-interfaz-SINTETICA.png')],check=True,env=env)
            (work/'result/finish').write_text('finish')
            if p.wait(timeout=35)!=0:raise RuntimeError('Fallo en prueba gráfica')
            for video in (work/'result/capturas').glob('*.mp4'):
                subprocess.run(['ffprobe','-v','error','-show_entries','stream=width,height,pix_fmt','-of','json',str(video)],stdout=log,stderr=subprocess.STDOUT,check=True)
        finally:
            if p.poll() is None:p.terminate();p.wait(timeout=5)
print('OK captura real de interfaz con entrada SINTÉTICA; fotografía, vídeo y cierre probados.')
