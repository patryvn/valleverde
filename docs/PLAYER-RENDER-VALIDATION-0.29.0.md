# Validación del personaje 3D — Valle de Cernego 0.29.0

Fecha: 27 de noviembre de 2026  
Rama: `game/first-3d-playable`  
Versión preview: `0.29.0` · versionCode `51`

## Motivo

La prueba CI de 0.28.9 (run 46055335665, commit `cdced03`) completó las pruebas unitarias, las compilaciones Debug/Release, la verificación de firma y el smoke test Android 35/SwiftShader. Los registros de esa ejecución muestran 60 piezas, GL_NO_ERROR, 7,73 m de desplazamiento y 0,01 m de error de seguimiento de cámara en el modo detallado; el modo de shader sencillo también movió 60 piezas con GL_NO_ERROR. Eso valida SwiftShader, no el controlador Mali del Poco X6 Pro.

La usuaria ha informado que en el teléfono el protagonista sigue apareciendo como un rectángulo azul. No se dispone de una captura del HUD 0.28.9 tomada en el Poco, por lo que no se puede confirmar qué paquete/versión ni qué renderer GLES se estaban ejecutando.

## Cambio de 0.29.0

- Se añade un tercer camino de renderizado: **BOX60**, que convierte las 60 piezas del protagonista a la malla de cubo ya utilizada por el motor.
- En Mali-G615 MC6 se selecciona BOX60 automáticamente al iniciar en modo compatible. Así se evita depender de la rasterización de esferas y cápsulas en ese controlador, manteniendo cabeza, cuello, torso, brazos, manos, piernas, botas, ropa, gorra y mochila como piezas separadas.
- Las transformaciones de brazos y piernas reutilizan los mismos canales de animación y el mismo seguimiento de cámara.
- Se añade una malla ovalada de sombra independiente de las esferas.
- El HUD diferencia `BOX60`, `SIMPLE60` y `LIT60`.
- El modo de prueba `force_box_compatibility` solo se acepta cuando `smoke_test_mode=true`; no cambia el comportamiento de una instalación normal.
- El smoke test añade captura inicial y en movimiento del camino BOX60, además de los caminos detallado y SIMPLE60.

## Criterios CI

La ejecución 0.29.0 debe demostrar para cada modo:
- captura sin ventana superpuesta y con píxeles de cabeza, torso, ambos brazos, ambas piernas y botas;
- 60 piezas enviadas y GL_NO_ERROR;
- desplazamiento mínimo de 0,25 m tras gesto táctil real;
- error de seguimiento de cámara no superior a 0,15 m;
- estabilidad del recuento de piezas durante el movimiento.

## Validación física pendiente

SwiftShader no reproduce el controlador GLES/EGL Mali-G615 MC6. En el Poco X6 Pro hay que instalar la aplicación cuyo nombre incluye `Prueba 3D 0.29.0`, comprobar que el HUD indica `GPU Mali-G615 MC6 · BOX60`, y verificar que se distinguen cabeza, torso, dos brazos, dos piernas, botas y mochila en reposo y al caminar. También hay que girar la cámara, comprobar la sombra, volver de segundo plano y observar rendimiento/temperatura.

Si el HUD no indica BOX60, la instalación no está ejecutando la ruta esperada. Si indica BOX60 y sigue apareciendo un rectángulo azul, hace falta una captura completa del teléfono: el problema ya no se puede atribuir sin más a las mallas de esfera/cápsula.

## Distribución

La APK Release CI es una preview firmada con la clave debug temporal del runner y con package id `es.valledecernego.game.preview`. No es una release de distribución con una clave persistente. No se afirma que el problema físico esté resuelto hasta probarlo en el Poco X6 Pro.
