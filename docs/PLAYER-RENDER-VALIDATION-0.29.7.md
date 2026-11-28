# Player visual watchdog 0.29.7

Fecha: 28 de noviembre de 2026  
Rama: `game/first-3d-playable`  
Versión: `0.29.7` · versionCode `58`

## Cambio

La lectura `glReadPixels` es síncrona y puede esperar a que termine la cola de renderizado. En 0.29.6, un desplazamiento de 0,65 m podía disparar otra lectura cada pocas décimas de segundo al caminar deprisa. Además, cada lectura asignaba un nuevo array RGBA en el heap.

0.29.7 corrige ese coste sin retirar la comprobación visual:

- Límite global de una lectura como máximo por segundo, aunque se alcancen antes los umbrales de distancia o giro.
- Se conserva la revalidación a partir de 0,65 m, 0,35 radianes y el intervalo de dos segundos caminando.
- Si el reloj monotónico observado retrocede, no se inicia un bucle de lecturas.
- Se reutiliza el array RGBA de CPU; solo se amplía si cambia la resolución de la región de lectura.
- El buffer directo de GLES también se mantiene reutilizable.

## Pruebas

- Relectura por desplazamiento y giro después del intervalo mínimo.
- Supresión de relecturas antes de un segundo.
- Comportamiento ante marcas de tiempo monotónicas regresivas.
- Se mantienen las pruebas de movimiento continuo, cruce angular de ±π, coordenadas no finitas y readback no disponible.
- CI sigue ejecutando `testDebugUnitTest`, `assembleDebug`, `assembleRelease` y el smoke test Android de reposo, movimiento, cámara y los tres modos gráficos.

## Validación física pendiente

El smoke test usa Android Emulator con Google SwiftShader. No sustituye una prueba en la GPU Mali-G615 MC6 del Poco X6 Pro. En el dispositivo físico aún hay que verificar el modo seleccionado, el estado `VISUAL` al inicio y tras caminar/girar, que no aparezca el rectángulo azul, la estabilidad de cámara y el rendimiento/temperatura.

## Distribución

La APK release-test sigue firmada con la clave debug efímera del runner de CI. No es una clave persistente de distribución. Si la firma cambia entre ejecuciones, Android puede rechazar la actualización sobre una preview anterior; no desinstalar la aplicación original ni borrar partidas sin copia.
