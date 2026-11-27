# Diagnóstico visible de GPU — Valle de Cernego 0.28.9

Fecha: 27 de noviembre de 2026  
Rama: `game/first-3d-playable`  
Versión: `0.28.9` · versionCode `50`

## Motivo

La validación 0.28.8 en CI confirmó que SwiftShader dibuja el modelo procedural de 60 piezas y que la cámara lo sigue. Eso no reproduce la GPU Mali-G615 MC6 del Poco X6 Pro. Sin conocer el valor real de `GL_RENDERER` del teléfono, no es correcto atribuir el rectángulo azul a una causa concreta.

## Cambio

- El estado persistente del HUD muestra el renderer GLES y la ruta seleccionada; por ejemplo: `GPU Mali-G615 MC6 · SIMPLE60`.
- El aviso al pulsar 3D/ALT repite el diagnóstico.
- El nombre del renderer se limita a 22 caracteres.
- Se añaden pruebas unitarias y de scripts para Mali-G615, SwiftShader, renderer desconocido y nombres largos.
- El log `PLAYER_GPU_PROFILE` se conserva como diagnóstico completo.

## Qué demuestra y qué no

Este cambio mejora el diagnóstico; no modifica geometría, shaders ni transformaciones del personaje. No se afirma que el rectángulo azul del Poco esté corregido. Las capturas de 0.28.8 son del emulador SwiftShader y muestran un humanoide procedural de 60 piezas, no una prueba del dispositivo físico.

## Prueba en el Poco X6 Pro

1. Instalar la preview 0.28.9; la etiqueta de aplicación debe mostrar esa versión.
2. Esperar a que el mundo termine de cargar y leer la línea del HUD que empieza por `GPU`.
3. Si aparece `GPU Mali-G615 MC6 · SIMPLE60`, la selección de la ruta conservadora se ha realizado.
4. Si aparece `GPU ... · LIT60`, guardar una captura completa para comprobar qué renderer informa el dispositivo.
5. Comprobar cabeza, torso, ambos brazos y ambas piernas al caminar y al detenerse.
6. Si persiste el rectángulo azul, enviar una captura donde se vea la línea GPU y el HUD completo. No hace falta activar opciones de desarrollador ni extraer logcat.

La prueba física sigue siendo necesaria para validar el controlador Mali, la estabilidad al volver de segundo plano, las sombras, la animación y el rendimiento térmico.

## Distribución

La variante Release de CI es una preview firmada con la clave debug efímera del runner. No es una release de distribución con clave persistente ni una publicación final.