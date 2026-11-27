# Validación visual del personaje 3D — 0.28.7

Fecha: 27 de noviembre de 2026  
Rama: `game/first-3d-playable`  
Commit probado: `bfbb8b8e2410d5f8f67e0bc24c9c6bc52eb3da78`  
GitHub Actions: [ejecución 297](https://github.com/patryvn/valleverde/actions/runs/46024261193)  
Job Android: `140591467052`

## Resultado

La ejecución 297 terminó correctamente. En el emulador Android API 35 x86_64 con SwiftShader, a 720 × 405, se instalaron y ejecutaron los dos modos del personaje. El análisis de imagen encontró píxeles independientes de piel, chaqueta, brazos, piernas y botas en las cuatro capturas: reposo y movimiento del modelo detallado, y reposo y movimiento del modo ALT.

| Modo | Captura | Piel | Chaqueta | Brazos izq./der. | Piernas izq./der. | Botas |
|---|---|---:|---:|---:|---:|---:|
| Detallado, 60 piezas | Reposo | 1258 | 4021 | 1401 / 1601 | 539 / 648 | 536 |
| Detallado, 60 piezas | Movimiento | 699 | 2763 | 694 / 1074 | 476 / 453 | 592 |
| ALT, 11 piezas | Reposo | 1170 | 1612 | 129 / 783 | 258 / 275 | 660 |
| ALT, 11 piezas | Movimiento | 1150 | 1615 | 130 / 783 | 250 / 272 | 660 |

Las cuatro capturas se generaron y superaron el validador de grupos de píxeles y terreno. Los registros del renderizador informaron `GL_NO_ERROR`.

## Movimiento y cámara

- Modelo detallado: desplazamiento medido de **7,81 m**; error de seguimiento de cámara **0,01 m**.
- Modelo ALT: desplazamiento medido de **7,74 m**; error de seguimiento de cámara **0,01 m**.
- La cantidad de piezas permaneció estable durante cada desplazamiento: 60 en modo detallado y 11 en modo ALT.
- El modo ALT utiliza una silueta simplificada; no conserva los detalles de cara, ropa y accesorios del modelo de 60 piezas.

## Compilación y pruebas

En la misma ejecución terminaron correctamente:
- `gradle :app:testDebugUnitTest --no-daemon`
- `gradle :app:assembleDebug --no-daemon`
- `gradle :app:assembleRelease --no-daemon`
- Verificación de firmas APK y metadatos: paquete `es.valledecernego.game.preview`, versionCode `48`, versionName `0.28.7`.
- Smoke test de escena Android y carga de las cuatro capturas.

Artefactos de la ejecución: [Debug](https://github.com/patryvn/valleverde/actions/runs/46024261193/artifacts/15212096131), [Release de prueba](https://github.com/patryvn/valleverde/actions/runs/46024261193/artifacts/15211156360), [capturas y diagnósticos](https://github.com/patryvn/valleverde/actions/runs/46024261193/artifacts/15211581722).

## Qué demuestra y qué no demuestra

Esta ejecución demuestra que el código de 0.28.7 dibuja una figura humanoide en **ese emulador** y que el personaje y la cámara se desplazan de forma coherente. No demuestra que la GPU Mali-G615 MC6 del Poco X6 Pro se comporte igual, ni que el rectángulo azul comunicado por la usuaria esté resuelto en el teléfono físico.

El personaje utiliza geometría procedural (esferas, cápsulas y cajas), materiales por color y variación procedural de iluminación. Todavía no usa mapas UV ni texturas fotográficas. El análisis de capturas verifica grupos de color y extensión de la figura, pero no certifica por sí solo la calidad de cada ciclo de animación ni las sombras en una GPU física.

## Validación física pendiente

En el Poco X6 Pro todavía hay que comprobar:
1. Que se vean cabeza, torso, ambos brazos, ambas piernas y botas, sin el rectángulo azul.
2. Que caminar, detenerse y girar mantengan las proporciones y la animación.
3. Que la cámara siga al personaje en las cuatro direcciones.
4. Que el modelo no parpadee ni desaparezca al acercarse a árboles, saltar o volver desde segundo plano.
5. Que las sombras y el rendimiento térmico sean aceptables.

Si el teléfono continúa mostrando un rectángulo azul mientras el HUD indica `3D OK · 60 piezas · GL OK`, se necesita una captura completa del HUD y, si es posible, el registro `ValleCernego` del dispositivo. Ese estado significa que se enviaron las llamadas de dibujo sin error GL notificado; no prueba que la GPU haya mostrado los píxeles correctamente.

## Distribución

La APK de CI es una APK de prueba firmada con la clave debug temporal del runner. No es una release de distribución ni una firma persistente apta para Google Play. Una actualización firmada con otra clave puede requerir desinstalar la aplicación de prueba anterior, lo que elimina sus datos locales.
