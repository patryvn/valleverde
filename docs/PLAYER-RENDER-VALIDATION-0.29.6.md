# Player visual watchdog 0.29.6

Fecha: 28 de noviembre de 2026  
Rama: `game/first-3d-playable`  
Versión: `0.29.6` · versionCode `57`

## Cambio

La versión 0.29.5 ejecutaba el análisis de píxeles una vez al iniciar el renderizador (y al cambiar manualmente el modo). Una captura inicial correcta no demostraba que el personaje siguiera siendo visible después de caminar o girar.

0.29.6 añade revalidación limitada por movimiento:

- Nuevo `PlayerVisualProbePolicy`: solicita otra lectura cuando el personaje se desplaza al menos 0,65 m, gira al menos 0,35 radianes, o permanece caminando y han transcurrido 2 segundos desde la última lectura.
- El delta angular se normaliza alrededor de ±π para evitar relecturas falsas al cruzar el límite.
- La lectura sigue usando la región central y el análisis espacial de cabeza, torso, ambos brazos, ambas piernas y botas.
- Se conserva el resultado `VISUAL OK`, `VISUAL FALLBACK OK` o `VISUAL FAILED`; la relectura no cambia por sí sola el modelo si no se detecta una regresión.
- El coste se limita a una lectura ocasional de `glReadPixels`, no a cada fotograma.

## Pruebas añadidas

- No repetir la lectura cuando el jugador está quieto.
- Repetir después de traslación y giro.
- Tratar correctamente el salto angular de +π a -π.
- Cadencia de dos segundos mientras camina.
- No repetir lecturas cuando GLES no admite readback.
- Rechazar coordenadas no finitas.
- El smoke test de Android exige un resultado nuevo del watchdog después del gesto de movimiento, además de comprobar desplazamiento, cámara, 60 piezas y `GL_NO_ERROR`.

## Evidencia y límites

La ejecución 0.29.5 anterior usó Android Emulator con Google SwiftShader, no la GPU Mali física del Poco X6 Pro. Sus capturas muestran un avatar low-poly reconocible y sus logs registran 60 piezas, movimiento y 0,01 m de error de seguimiento de cámara. Esa evidencia no permite asegurar que el controlador Mali real dibuje igual.

La validación física sigue pendiente: comprobar en el Poco X6 Pro el modo GPU seleccionado, el estado del watchdog al inicio y después de caminar/girar, la ausencia del rectángulo azul, estabilidad de cámara, temperatura, memoria y fluidez.

## Distribución

La APK sigue siendo una preview firmada con la clave debug de CI y el paquete `es.valledecernego.game.preview`. No es una release de distribución con firma persistente. Si Android rechaza la actualización por conflicto de firma, no desinstalar la aplicación original ni borrar datos sin copia.
