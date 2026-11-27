# Validación visual y área de lectura — Valle de Cernego 0.29.4

Fecha: 28 de noviembre de 2026  
Rama: `game/first-3d-playable`  
Versión: `0.29.4` · versionCode `55`

## Ajustes del watchdog

La versión 0.29.3 añadió una lectura de píxeles para detectar el caso en que GLES devuelve `GL_NO_ERROR` pero la imagen del protagonista se reduce a un rectángulo. La prueba 0.29.3 pasó en Google SwiftShader y mostró el modelo de 60 piezas en los tres modos.

En una pantalla física de alta resolución, una ventana fija de 480×320 píxeles podía recortar la cabeza o las botas porque el tamaño del personaje crece con la altura del viewport. La 0.29.4 corrige esto:

- El ancho de lectura escala hasta dos tercios del viewport y se limita a 720 píxeles.
- La altura escala hasta cuatro quintos del viewport y se limita a 640 píxeles.
- La región sigue centrada en el personaje y evita incluir innecesariamente los extremos de la escena.
- Hay pruebas unitarias para 720×405 (emulador), 2712×1220 (pantalla de alta resolución) y 360×203 (pantalla pequeña).

También se corrige el HUD: si el watchdog activa el respaldo plano de 11 piezas, el botón y la línea GPU muestran `HUM11`, no `BOX60`. Así el diagnóstico no confunde el modelo principal de 60 piezas con el respaldo de emergencia.

## Evidencia previa 0.29.3

La ejecución 46083841177 terminó correctamente:
- 54 pruebas Python;
- `testDebugUnitTest`;
- `assembleDebug` y `assembleRelease`;
- verificación de paquete, versionCode 54 y firma;
- smoke test de la APK Release Preview en Android Emulator con Google SwiftShader.

El watchdog encontró grupos de piel, chaqueta, botas y píxeles oscuros en el modelo detallado, el BOX60 manual y el BOX60 forzado. Las tres rutas conservaron 60 piezas y `GL_NO_ERROR`.

Movimiento y cámara medidos:
- modelo detallado: 7,36 m; error de cámara 0,01 m;
- BOX60 manual: 7,69 m; error de cámara 0,01 m;
- BOX60 forzado: 7,64 m; error de cámara 0,01 m.

## Validación pendiente

La ejecución anterior usa Google SwiftShader; no es una prueba del controlador Mali-G615 MC6. La versión 0.29.4 debe volver a pasar CI. Después todavía hace falta comprobar en el Poco X6 Pro que el HUD muestre `VISUAL OK` o `VISUAL FALLBACK OK`, que el personaje conserve cabeza, torso, brazos, piernas y botas, y que movimiento, giro, salto, cámara, temperatura y estabilidad sean correctos.

El watchdog solo verifica grupos de píxeles y activa un respaldo; no demuestra por sí mismo calidad artística ni sustituye la revisión de la captura física.

## Distribución

La APK mantiene el paquete `es.valledecernego.game.preview`. La clave de firma de CI es temporal y no es una firma de distribución persistente. Si Android rechaza una actualización por conflicto de firma, no desinstalar la aplicación original: desinstalar la preview puede borrar sus datos locales.
