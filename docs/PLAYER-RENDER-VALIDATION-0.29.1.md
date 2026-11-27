# Validación del selector manual BOX60 — Valle de Cernego 0.29.1

Fecha: 27 de noviembre de 2026  
Rama: `game/first-3d-playable`  
Versión preview: `0.29.1` · versionCode `52`

## Problema que aborda

La usuaria ha informado de que puede desplazarse en el Poco X6 Pro, pero el protagonista aparece como un rectángulo azul. El CI de 0.29.0 mostró que el modelo BOX60 completo se dibuja correctamente en Google SwiftShader, con 60 piezas y sin errores GLES. Esa evidencia no demuestra el comportamiento del controlador físico Mali.

Una causa aún no descartada es que algunos controladores Android publiquen un nombre de GPU genérico o distinto del patrón `Mali-G615`. En ese caso, la política automática podría no elegir BOX60 aunque la ruta BOX60 sea la alternativa más compatible.

## Cambio de 0.29.1

- El botón de modo de renderizado alterna entre el renderizador detallado y BOX60 de forma explícita.
- La selección manual BOX60 ya no depende del texto devuelto por `GL_RENDERER`.
- El estado manual se conserva al recrear el contexto EGL durante la sesión.
- El botón muestra `BOX` cuando BOX60 está activo; el HUD superior sigue mostrando el nombre de GPU y el modo efectivo.
- La selección automática Mali se mantiene para nuevas sesiones; la selección manual prevalece después de que la persona toque el botón.
- Se añade una prueba unitaria para renderer genérico/desconocido y el diagnóstico `BOX60`.

## Prueba física necesaria

En el Poco X6 Pro, instalar la APK 0.29.1. Si el HUD no muestra `BOX60`, tocar el botón de modo de renderizado hasta que el botón muestre `BOX` y la línea GPU indique `BOX60`. Comprobar en reposo y caminando que se distinguen cabeza, torso, dos brazos, dos piernas, botas y mochila; girar la cámara y volver de segundo plano.

Si el modo `BOX60` aparece en el HUD y la figura sigue siendo un rectángulo azul, el problema no es la selección de geometría. Se necesita una captura completa de la pantalla y, si es posible, el texto exacto de GPU/mode mostrado en el HUD. CI SwiftShader no sustituye esta comprobación física.

## Distribución

La APK Release de CI continúa siendo una preview firmada con la clave debug temporal del runner y package id `es.valledecernego.game.preview`. No es una release de distribución con una clave persistente.
