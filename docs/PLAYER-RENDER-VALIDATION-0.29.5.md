# Player visual watchdog 0.29.5

Fecha: 28 de noviembre de 2026  
Rama: `game/first-3d-playable`  
Versión: `0.29.5` · versionCode `56`

## Hallazgo de revisión

La versión 0.29.4 leía una ventana de hasta 720×640 píxeles y sumaba colores de piel, chaqueta, botas y zonas oscuras sin comprobar dónde aparecían. En el mundo completo, esos colores también pueden proceder de animales, aldeanos, recursos o terreno. Por tanto, un conjunto de colores de la escena podía satisfacer el watchdog aunque el protagonista no tuviera una silueta humana completa.

Esto es una debilidad real de la prueba, no una demostración de que sea la causa única del rectángulo azul en el Poco X6 Pro.

## Corrección 0.29.5

- La región de lectura se centra en la proyección del jugador y se estrecha al 30 % del ancho y 60 % de la altura del viewport (con límites para móviles pequeños y pantallas de alta resolución).
- El analizador respeta el orden bottom-up que devuelve `glReadPixels`.
- Ya no basta con acumular colores en cualquier parte: exige piel en la banda superior, chaqueta en el torso, botas en la banda inferior, y píxeles diferenciados en ambos brazos y ambas piernas.
- Los umbrales escalan con el tamaño del recorte.
- El log de `PLAYER_VISUAL_WATCHDOG` incluye recuentos independientes de brazos y piernas.
- Se amplía el reconocimiento de GPU Mali-G615 para aceptar variantes de nombre con espacios, guiones o guiones bajos.

## Pruebas de regresión

Pruebas unitarias:
- humanoide sintético con cabeza, torso, dos brazos, dos piernas y botas;
- rechazo del rectángulo azul;
- rechazo de colores de fondo/NPC fuera de la proyección central;
- dimensiones del recorte para 720×405, 2712×1220 y 360×203;
- rechazo de lecturas nulas o truncadas;
- identificación Mali-G615 con distintas cadenas del controlador.

Las capturas de 0.29.4 disponibles se analizaron offline con las nuevas bandas espaciales. El modelo detallado produjo 1.020 píxeles de piel, 4.471 de chaqueta, 563 de botas, 1.476 oscuros, 857/1.070 de brazos y 512/598 de piernas. BOX60 produjo 1.166 de piel, 5.193 de chaqueta, 446 de botas, 1.865 oscuros, 1.127/1.292 de brazos y 729/666 de piernas. Son recuentos de capturas SwiftShader anteriores, no resultados de un Poco físico ni sustituyen la nueva ejecución CI.

## Criterio de aceptación

La nueva CI debe ejecutar `testDebugUnitTest`, `assembleDebug`, `assembleRelease` y el smoke test Android con capturas iniciales y en movimiento para el modelo detallado y BOX60. La prueba debe pasar el watchdog espacial y conservar las 60 piezas, `GL_NO_ERROR`, desplazamiento del jugador y seguimiento de cámara.

La validación física sigue pendiente: instalar la APK 0.29.5 en el Poco X6 Pro, comprobar el modo GPU seleccionado, observar `VISUAL OK` o `VISUAL FALLBACK OK`, y verificar que el protagonista no se reduce al rectángulo azul al caminar, girar y saltar. SwiftShader no reproduce el controlador Mali-G615 MC6.

## Distribución

La APK Release Preview conserva el paquete `es.valledecernego.game.preview` y se firma con la clave debug temporal de GitHub Actions. No es una firma persistente de distribución. No desinstalar la aplicación original si Android rechaza la actualización por conflicto de firma.
