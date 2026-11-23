# Publicar el repositorio y las versiones

Estado de esta preparación: proyecto local listo para revisión. Todavía no existe una URL pública confirmada ni una ejecución remota de los workflows. No enviar el correo de publicación hasta tener los enlaces y comprobarlos sin iniciar sesión.

Nombre sugerido: `microscopio-color-fiji`. Versión inicial de esta revisión: `1.1.1`. La versión 1.1.0 anterior fue una entrega ZIP; no se reutiliza su número para cambiar archivos ya enviados.

## Primera publicación desde este ordenador

1. Entrar en la cuenta u organización de GitHub que deba alojar el proyecto.
2. Crear un repositorio **Public** llamado `microscopio-color-fiji`. Crearlo vacío: este proyecto ya contiene README y licencia.
3. Copiar la URL HTTPS o SSH que muestra GitHub. La autenticación se hace con la cuenta autorizada; no guardar tokens en el código ni en correos.
4. Abrir terminal en la carpeta `MICROSCOPIO-COLOR-FIJI-GITHUB` y ejecutar, sustituyendo OWNER por la cuenta real:

```sh
git remote add origin https://github.com/OWNER/microscopio-color-fiji.git
git push -u origin main
```

El repositorio local ya contiene el primer commit y una etiqueta local de esta preparación. El envío requiere credenciales de una cuenta con acceso al repositorio. Si se parte del ZIP sin `.git`, inicializar primero:

```sh
git init -b main
git add .
git -c user.name="Rafael Cantillana" -c user.email="info@robot-edu.es" commit -m "Preparar plugin independiente para publicación"
git tag -a v1.1.1 -m "Microscopio Color 1.1.1"
```

Antes de etiquetar o subir, revisar que se aprueba el contenido. Un tag no se debe mover para cambiar una versión distribuida; emitir otra versión.

## Publicar el primer lanzamiento

1. Comprobar que el workflow **Build and test** de main haya pasado con Java 11 y Java 21.
2. Enviar la etiqueta:

```sh
git push origin v1.1.1
```

3. El workflow **Prepare tagged release** vuelve a probar y crea un **borrador** con JAR, fuentes `.tar.gz` y SHA-256. Requiere GitHub Actions habilitado y permiso de escritura de contenido para ese job.
4. Abrir Releases, revisar el borrador, su changelog y sus archivos. Publicarlo solo cuando se decida distribuirlo. No usar la etiqueta «estable validado en LliureX» sin esas pruebas físicas.
5. Abrir sin sesión la URL del repositorio y la de la Release. Comprobar que las fuentes y los tres archivos se descargan.
6. Sustituir OWNER en el ejemplo de watch y los enlaces pendientes en el borrador de correo. No quedan enlaces de cuenta inventados en el proyecto.

Los borradores no son visibles para el público ni sirven todavía para `uscan`. Una vez publicada la Release, la API pública permite detectar las versiones y descargar el tarball fuente; el equipo de LliureX debe comprobar su plantilla watch con el repositorio real.

## Si no funciona GitHub Actions

La compilación sigue siendo local y sin red si se aporta `IJ_JAR`. Desde un commit limpio con el tag correcto:

```sh
IJ_JAR=/ruta/al/ij.jar python3 tools/release.py --tag v1.1.1
```

La herramienta ejecuta los tests y genera `release/`. En GitHub, crear una Release desde ese mismo tag, adjuntar JAR, tarball y SHA256SUMS.txt, revisar y publicar. La publicación manual es una alternativa; no cambia el procedimiento de versionado ni exige reenviar ZIP por correo.

## Siguientes versiones

Modificar fuentes y tests; actualizar VERSION, la constante Java VERSION, los textos de versión del código y las fixtures si corresponde. Actualizar CHANGELOG.md y docs/RELEASE-NOTES.md. Comprobar el cambio con `make test`, revisar el diff, crear un commit y una etiqueta nueva, y repetir la revisión de Release.

No se promete mantenimiento periódico. Cualquier persona con permisos del repositorio o un fork puede aplicar el flujo. Robot-Edu solo atenderá trabajos encargados mediante acuerdo previo según MAINTENANCE.md.

## ImageJ Update Site

No se ha creado uno en esta entrega. La integración solicitada usa el empaquetado de LliureX; mantener un segundo canal requeriría coordinar actualizaciones y responsables. El repositorio y las Releases son el canal previsto una vez publicados.

Referencias oficiales:
- https://docs.github.com/en/repositories/releasing-projects-on-github/about-releases
- https://docs.github.com/en/rest/releases/assets
- https://manpages.debian.org/unstable/devscripts/debian-watch.5.en.html
