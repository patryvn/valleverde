# Validación y ruta GLES segura del personaje — 0.28.8

Fecha: 27 de noviembre de 2026  
Rama: `game/first-3d-playable`  
Versión: `0.28.8` · versionCode `49`

## Motivo del cambio

La ejecución 297 demostró que el modelo de 60 piezas se ve en el emulador SwiftShader, pero no reproduce el controlador del Poco X6 Pro. El Poco X6 Pro utiliza GPU Mali-G615 MC6; la documentación 0.28.7 que mencionaba Adreno se ha corregido.

La ruta principal utiliza un shader con iluminación, normales y variación procedural de materiales. Un contador de draw calls y `GL_NO_ERROR` no garantiza que todos los controladores muestren el resultado correctamente. Por eso se añade una segunda ruta para el personaje completo.

## Cambio implementado

- Detecta el nombre de `GL_RENDERER` al crear la superficie EGL.
- Si el renderer contiene `Mali-G615`, selecciona automáticamente el shader GLES mínimo.
- La ruta segura sigue dibujando las **60 piezas** del personaje: cabeza, cara, chaqueta, camiseta, mochila, brazos, manos, piernas y botas.
- Conserva los mismos transforms y canales de animación de caminar/reposo.
- Conserva el óvalo de sombra y el depth test.
- El shader mínimo elimina normales, matriz normal y efectos procedurales de material; usa color por pieza para reducir complejidad del controlador.
- El botón 3D/ALT permite cambiar manualmente entre el shader iluminado y el shader mínimo.
- Si el modelo completo con shader mínimo falla, se intenta el humanoide redondeado de 11 piezas y, como último recurso, la silueta cúbica de 11 piezas.
- La elección manual y un fallback por error se conservan al recrearse el contexto EGL.

## Pruebas añadidas o actualizadas

- Pruebas unitarias de selección GPU para Mali-G615, SwiftShader, Adreno, PowerVR y renderer desconocido.
- Prueba de estado HUD para el modo compatible completo de 60 piezas.
- Smoke test del emulador: modelo iluminado 60/0 y modo simple 60/60, ambos en reposo y movimiento.
- Verificación de captura: cabeza, chaqueta, brazos, piernas, botas y terreno.
- Verificación de movimiento: recuento de piezas estable, desplazamiento y error de seguimiento de cámara.

## Qué no se afirma

La selección automática Mali-G615 es una mitigación dirigida al Poco X6 Pro, no una prueba de que el shader complejo sea la causa exacta del rectángulo azul. El emulador SwiftShader no sustituye una prueba con Mali-G615 MC6. Hasta que haya una captura del teléfono físico, no se declara resuelto el defecto en ese dispositivo.

El modelo sigue siendo geometría procedural con materiales por color; el shader seguro no incorpora mapas UV ni texturas fotográficas. La prueba automatizada verifica los grupos de píxeles del emulador y la telemetría, no certifica la calidad de sombras, ciclos de animación, rendimiento térmico ni estabilidad del controlador físico.

## Validación pendiente en Poco X6 Pro

1. Confirmar en logcat `PLAYER_GPU_PROFILE renderer=... policy=mali-g615-simple-shader-60`.
2. Confirmar en HUD `3D COMPATIBLE · 60 piezas · shader GLES plano · GL OK`.
3. Comprobar cabeza, torso, ambos brazos, ambas piernas y botas; no debe aparecer el rectángulo azul.
4. Caminar, detenerse, girar y volver desde segundo plano.
5. Revisar parpadeo, oclusión junto a árboles, sombra, temperatura y fluidez.
6. Si sigue apareciendo el rectángulo, compartir captura completa y el log `ValleCernego`; el nombre del renderer y la ruta elegida son esenciales.

## Distribución

La APK Release de CI continúa siendo una APK de prueba firmada con la clave debug temporal del runner. No es una firma persistente de distribución ni una publicación final de Google Play.
