# Validación visual del protagonista — Valle de Cernego 0.29.3

Fecha: 28 de noviembre de 2026  
Rama: `game/first-3d-playable`  
Versión: `0.29.3` · versionCode `54`

## Cambio principal: watchdog de píxeles en tiempo de ejecución

Hasta 0.29.2, el renderizador consideraba correcto un personaje si enviaba las 60 piezas y OpenGL devolvía `GL_NO_ERROR`. Eso no demuestra que el resultado de píxeles sea un humanoide: un controlador puede aceptar las llamadas y mostrar una región plana o un rectángulo.

La versión 0.29.3 añade `PlayerPixelWatchdog`:

- Lee una única región central de la superficie GLES2 al iniciar la escena y tras cambiar de modo o recrear el contexto.
- Busca cuatro grupos de color independientes: piel/cabeza, chaqueta/torso, botas y píxeles oscuros de pelo/piernas.
- Repite hasta tres lecturas si la primera imagen todavía no está estable.
- Si no encuentra esos grupos, cambia automáticamente a un humanoide de emergencia de 11 piezas, dibujado con el shader GLES plano independiente, conservando articulaciones, cámara y sombra.
- Verifica de nuevo el fallback y publica `VISUAL OK`, `VISUAL FALLBACK OK`, `VISUAL N/D` o `VISUAL FAILED` en el HUD.
- Si `glReadPixels` no está disponible, no fuerza un fallback por una lectura inválida: informa `VISUAL N/D`.

El watchdog no sustituye una revisión visual humana ni pretende identificar modelos con precisión artística. Su función es detectar el caso concreto de una figura que solo produce un bloque de color, incluso cuando el contador de draw calls y el estado GL parecen correctos.

## Pruebas

Se añaden pruebas unitarias para:
- aceptar una muestra sintética con grupos separados de piel, chaqueta, piernas y botas;
- rechazar un rectángulo azul uniforme;
- manejar buffers nulos, vacíos o de dimensiones incorrectas sin excepción.

El smoke test instala la APK Release Preview (no la Debug), y ahora exige que el watchdog confirme píxeles humanoides en:
- el modo detallado inicial;
- el modo BOX60 seleccionado desde el HUD;
- el modo BOX60 forzado en el segundo arranque.

Además mantiene las verificaciones anteriores de 60 piezas, movimiento, seguimiento de cámara y `GL_NO_ERROR`.

## Evidencia y límites

La ejecución anterior 46075639466 (versión 0.29.2) terminó correctamente en Android Emulator con Google SwiftShader. El artefacto de capturas contiene personaje visible en reposo y movimiento para el modelo detallado y BOX60. El registro del emulador identifica `Google SwiftShader`, no Mali.

La nueva lectura de píxeles debe volver a pasar en CI antes de considerar 0.29.3 validada. SwiftShader no reproduce el controlador ARM Mali-G615 MC6 del Poco X6 Pro.

## Validación física pendiente

En el Poco X6 Pro hay que confirmar:
- que el HUD identifica la GPU y el modo realmente seleccionado;
- que el watchdog muestra `VISUAL OK` o `VISUAL FALLBACK OK`;
- que la cabeza, torso, ambos brazos, ambas piernas y botas son distinguibles;
- que caminar, girar y saltar no deforman ni hacen desaparecer el modelo;
- que la cámara mantiene el encuadre y que no hay parpadeos, errores GLES o calentamiento anómalo.

Si aparece `VISUAL FAILED`, la captura completa del HUD y del personaje será necesaria para determinar si también falla el fallback plano. Sin un dispositivo físico conectado no se puede afirmar que el problema del Poco esté resuelto.

## Distribución

La APK Release Preview mantiene el paquete `es.valledecernego.game.preview` y se firma con la clave de depuración del runner de CI. Es una compilación instalable para pruebas, no una release pública con firma persistente. Un conflicto de firma puede impedir la actualización de una preview anterior y desinstalarla puede borrar sus datos locales.
