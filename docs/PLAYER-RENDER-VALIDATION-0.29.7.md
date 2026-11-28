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

## Firma de preview y actualización

Al inspeccionar los APK de CI anteriores se comprobó que cada runner generaba una clave debug distinta:

- 0.29.5: certificado SHA-256 `36:A8:EE:B5:29:FA:1C:10:87:B0:45:FC:4A:19:C1:A8:A1:F5:B3:A3:F7:A7:80:C6:01:83:EE:A3:09:C9:ED:64`.
- 0.29.6: certificado SHA-256 `01:46:73:68:4E:B8:9E:51:D6:19:10:72:4C:51:E9:4F:7B:85:A1:6D:D2:A6:41:B7:30:E8:FC:88:0C:74:43:BA`.

Por tanto, Android no puede instalar una sobre la otra como actualización. Se corrigió el workflow para restaurar/guardar `~/.android/debug.keystore` con una clave de caché fija (`cernego-preview-signing-v1`) en los workflows automático y manual. La primera ejecución que cree esa caché establece la identidad de firma para las siguientes ejecuciones mientras GitHub conserve la caché.

Para evitar borrar datos o desinstalar la preview antigua, 0.29.7 cambia el identificador a `es.valledecernego.game.preview2`. Se instala en paralelo y deja intacta la app anterior `es.valledecernego.game.preview`, incluidas sus partidas; la nueva instalación empieza con una partida independiente. Los siguientes APK mantendrán `preview2` y la clave de caché, por lo que podrán actualizarse normalmente mientras GitHub conserve la caché.

Para una publicación real se debe configurar una clave privada estable mediante GitHub Secrets/Play App Signing; la clave debug cacheada solo sirve para pruebas.
