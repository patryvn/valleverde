# BOX60 con materiales y luz direccional — Valle de Cernego 0.29.2

Fecha: 27 de noviembre de 2026  
Rama: `game/first-3d-playable`  
Versión preview: `0.29.2` · versionCode `53`

## Motivo

El modo BOX60 de 0.29.1 conservaba 60 piezas y movimiento, pero el shader de compatibilidad dibujaba cada cubo con un color plano. En pantallas pequeñas eso hacía que la figura pareciera una silueta rectangular aunque la geometría estuviera separada.

## Cambios

- Se añade un atlas procedural GLES2 de seis teselas: sólido, tejido, algodón, cuero, piel y metal.
- El modelo BOX60 conserva los 60 componentes y sus transformaciones articuladas; cada componente usa la tesela de su material.
- El shader de compatibilidad usa las normales de las caras para iluminación direccional sencilla, con luz ambiente para evitar caras negras.
- Se mantiene el shader plano anterior como fallback si falla la compilación, la carga de textura o el dibujo del atlas.
- El HUD añade `TEX` únicamente cuando el último fotograma BOX60 se ha dibujado con el atlas.
- El smoke test exige que tanto la selección manual como la ruta forzada BOX60 registren `PLAYER_BOX_MATERIALS frame=textured parts=60`, además de comprobar movimiento, cámara y GL_NO_ERROR.

## Límites

El atlas es procedural y de baja resolución, no una textura fotográfica ni un rig esquelético externo. BOX60 sigue siendo low-poly cúbico; la ruta LIT60 conserva las mallas redondeadas. El shader usa GLES 2.0 y una sola textura pequeña para reducir dependencias.

La ejecución de CI con Google SwiftShader valida compilación, carga del atlas, píxeles visibles, movimiento y seguimiento de cámara en emulador. No reproduce el controlador Mali-G615 MC6 del Poco X6 Pro. La selección de GPU, el filtrado de textura, la orientación UV, el rendimiento térmico y la visibilidad en el dispositivo físico siguen pendientes de comprobación en ese teléfono.

## Distribución

La APK Release de CI usa el paquete preview `es.valledecernego.game.preview` y la clave debug efímera del runner. Es instalable para pruebas, pero no es una release pública con firma persistente.
