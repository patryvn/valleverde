# Validación de renderizado en Android

## Estado de la rama

El personaje se construye con `PlayerModelSpec` y `PlayerModelTransform`: piezas 3D procedurales de cabeza, torso, brazos, piernas, ropa, gorra y mochila. La escena usa OpenGL ES 2.0, iluminación direccional por shader y una sombra translúcida bajo el personaje. La versión 0.16.0 añade variación procedural de tejido, algodón, cuero, piel y metal en el shader. No son mapas bitmap ni texturas fotográficas: cada pieza conserva su color base y recibe un patrón sutil calculado en el shader.

Las pruebas unitarias existentes comprueban:
- silueta y piezas principales del personaje;
- dimensiones y materiales dentro de rango;
- simetría de brazos y piernas;
- transformaciones con distintas orientaciones y fases de animación;
- seguimiento de cámara al trasladar y elevar al jugador;
- permanencia de las piezas dentro del volumen visible de la cámara.

Estas pruebas validan matemáticas y datos del modelo; no pueden demostrar por sí solas que un controlador gráfico real dibuje correctamente los píxeles.

## Prueba automatizada en CI

El flujo de GitHub Actions:
1. ejecuta `testDebugUnitTest`;
2. genera APK debug y release de prueba y verifica firma/paquete;
3. instala la APK debug en un emulador Android 35 en orientación horizontal;
4. comprueba que `MainActivity` sigue en primer plano y que el HUD informa de las 60 piezas enviadas sin error OpenGL;
5. guarda capturas antes y después de arrastrar el joystick, además del logcat.

La captura debe inspeccionarse visualmente: el contador confirma llamadas de dibujo sin error GL, pero no prueba que el modelo sea reconocible, esté libre de oclusiones o se vea bien en todos los controladores gráficos.

## Validación física pendiente

No hay un dispositivo Android físico conectado a GitHub Actions. En particular, falta validar en un Poco X6 Pro con Android 16:
- que el personaje completo (cabeza, torso, brazos, piernas, gorra y mochila) sea visible;
- que caminar, reposar, girar y saltar no deformen ni oculten el modelo;
- que la cámara mantenga el encuadre al moverse en varias direcciones;
- que no aparezcan errores GL, parpadeos, caída de FPS o consumo térmico excesivo.

La APK `release-test` se firma con una clave de pruebas del proyecto: sirve para instalación local, pero no es una firma de distribución de Google Play. Si Android detecta una firma distinta de una APK anterior, puede ser necesario desinstalar la anterior; eso puede borrar la partida local.

## Alcance del juego

La existencia de sistemas parciales en el código no equivale a un juego completo. Las funciones de vehículos, cuevas, prevención de incendios, supervivencia y simulación avanzada del pueblo siguen requiriendo integración y pruebas específicas. No se considera terminado hasta que las funciones principales estén conectadas, las pruebas pasen, haya una APK release instalable publicada y se documente la validación en dispositivo físico.

## Incidencias de CI y criterio de aceptación visual

La ejecución 45673544160 consiguió arrancar el emulador con TCG, instalar la APK y abrir la actividad, pero la captura guardada seguía mostrando la pantalla de arranque de Android. El registro contenía `DeadSystemException` de procesos del sistema y el gesto de movimiento terminó fallando porque faltaba el identificador de pantalla en el comando `input swipe`. Esa captura no sirve para validar el personaje.

La ejecución 45679722682 perdió la variable `ANDROID_I_WANT_MY_TCG`; el emulador se lanzó con `-accel off` y no llegó a aparecer como dispositivo ADB. Tampoco es una prueba visual válida.

La ejecución 45701063138 falló antes de compilar: Maven Central devolvió HTTP 429 al descargar JUnit 4.13.2. Se ha añadido un espejo de Maven Central para resolver esa dependencia sin depender exclusivamente del endpoint que limitó las solicitudes.

El flujo actual fuerza TCG mediante `ANDROID_I_WANT_MY_TCG=yes`, con SwiftShader para el renderizado. Espera a que PackageManager y la actividad estén disponibles, despierta la pantalla y especifica el display ID en el gesto. Guarda captura inicial, captura tras movimiento, jerarquía de ventana y logcat completo. Una ejecución verde confirma que la actividad y el HUD responden; aun así, la imagen debe inspeccionarse visualmente antes de declarar resuelto el rectángulo azul. La emulación sigue sin sustituir una prueba física.

### Incidencia de CI (run 45689670313)

La compilación de la APK y las pruebas unitarias pasaron, pero el emulador Android 35 con TCG terminó el arranque aparente sin que PackageManager estuviera disponible. `adb install` falló con `Can't find service: package` y el servicio de actividad respondió con `Broken pipe`. No se instaló la aplicación y no se generó ninguna captura; por tanto, este run no aporta evidencia sobre el personaje. El log confirma que el runner desactiva la aceleración KVM y arranca con `-accel off`, por lo que el tiempo de arranque y la estabilidad del sistema Android virtual son limitaciones del entorno, no fallos atribuidos al renderizador.

El workflow espera explícitamente a que `cmd package list packages` devuelva paquetes antes de instalar, reintenta la instalación y, si vuelve a fallar, guarda `getprop` y `logcat` completos para distinguir un servicio tardío de un `system_server` caído. Hasta que una ejecución produzca capturas reales de la actividad, no se modifica el modelo por conjetura ni se declara corregido el rectángulo azul.

## Cambios del modelo 0.16.0

- Se asignan estilos de material distintos a chaqueta/pantalón, camiseta, cuero, piel y piezas metálicas.
- El shader aplica variaciones de baja amplitud basadas en coordenadas locales; si el shader iluminado no compila, el shader plano de compatibilidad sigue disponible, pero sin estos patrones.
- `GLSurfaceView` solicita RGBA8888 y profundidad de 16 bits; al inicio de cada fotograma se restablecen la máscara de profundidad, la prueba de profundidad y la mezcla para evitar heredar estados GL.
- Las pruebas unitarias verifican la asignación de estilos. No validan el resultado de píxeles: eso requiere una captura real del emulador o, preferiblemente, del Poco X6 Pro.

## Ajuste de contacto con el suelo (0.17.0)

El terreno de juego se dibuja ahora con su cara superior en `PlayerModelSpec.GROUND_LEVEL` (0,05 unidades), coincidente con la parte inferior de las suelas. Antes, el terreno terminaba en Y=-0,10 y los pies comenzaban en Y=0,055, dejando un hueco visible de 0,155 unidades. La sombra aplanada se ha recolocado sobre ese plano y se ha ensanchado ligeramente en vertical para que el test de profundidad no la deje suspendida sobre el suelo. Una prueba unitaria comprueba el contacto de ambas botas con el plano. Esto corrige una desalineación geométrica real, pero no demuestra que el rectángulo azul comunicado por la usuaria haya desaparecido; la inspección de píxeles sigue pendiente.

La ejecución 45711019642 mostró que el emulador se cerraba con `Software CPU emulation (-accel off) is not supported` cuando no se definía `ANDROID_I_WANT_MY_TCG`. El flujo fija ahora esa variable, amplía el arranque a 15 minutos y reduce el AVD a 2 núcleos y 2 GB.

## Corrección del ejecutor de scripts (run 45721050836)

El emulador sí terminó de arrancar y PackageManager respondió, pero el paso de prueba falló antes de instalar la APK. Los registros muestran que el ejecutor del action lanzó cada línea del bloque `script` como un comando separado; la cabecera multilínea `for attempt ...; do` llegó sola a `sh -c` y produjo `Syntax error: end of file unexpected (expecting "done")`. No hubo captura de la aplicación ni evidencia visual del personaje.

Para evitar que el intérprete fragmente estructuras de control, la lógica de espera, instalación, apertura, captura y movimiento se ha trasladado a `scripts/emulator-smoke.sh`. El action ejecuta ahora una única orden: `bash scripts/emulator-smoke.sh`. El script guarda siempre `getprop`, `logcat`, actividades, ventanas y `gfxinfo`; falla si no puede instalar/abrir la actividad, si el HUD no informa de las 60 piezas o si la captura no cambia tras mover el joystick. La comparación de capturas demuestra que hubo un cambio visual, no que la silueta sea correcta: ambas imágenes aún deben inspeccionarse.

La siguiente ejecución debe volver a generar las APK, ejecutar `testDebugUnitTest`, `assembleDebug` y `assembleRelease`, y producir las dos capturas del emulador. La validación física en el Poco X6 Pro con Android 16 sigue pendiente.


## Captura real de la versión 0.17.0 (run 45724145643)

La ejecución terminó correctamente: pasaron `testDebugUnitTest`, `assembleDebug`, `assembleRelease`, la verificación de firmas/versión y el arranque de la aplicación en Android 35. El artefacto de capturas contiene dos PNG reales. En ellos se aprecia un personaje humanoide 3D estilizado desde atrás, con cabeza, gorra, torso, brazos, piernas, botas y mochila; la segunda captura muestra `3D OK · 60 piezas enviadas · GL OK`. No aparece el rectángulo azul en esa ejecución del emulador.

La comparación anterior de PNG completos no probaba por sí sola el movimiento: el HUD cambia al terminar de inicializar el renderizador y la animación también altera píxeles. Además, el escenario de inicio carecía de referencias visibles delante del personaje, por lo que no permitía juzgar el paralaje de la cámara. Esta limitación se corrige en 0.18.0 con objetos rurales situados delante del punto de aparición, un camino, una cerca, lomas de fondo y telemetría de prueba del jugador/cámara. El script espera a que el HUD confirme 60 piezas y GL OK antes de capturar; después comprueba que el joystick desplaza al jugador y que la cámara se traslada exactamente con él. La telemetría solo se activa mediante un extra de Intent usado por CI y no se muestra en partidas normales.

## Escenario y prueba de seguimiento (0.18.0)

- El punto de inicio ahora mira hacia un pequeño conjunto de robles, rocas, matorrales, ganado, tablón, mercado, cuadra y casa; el plano de terreno cubre esa zona.
- Se añade un camino de tierra, una cerca de madera y lomas suaves para ofrecer referencias visuales de profundidad.
- `WorldLayoutTest` comprueba que los elementos de la zona rural están delante del jugador, dentro de los límites del escenario y que el catálogo no se comparte entre partidas.
- El smoke test de Android espera al estado `60 piezas enviadas · GL OK`, captura el estado inicial, realiza un gesto en el centro real del joystick y verifica por telemetría que el jugador se desplaza al menos 0,75 m y que la cámara acompaña el mismo desplazamiento. Esto valida el seguimiento matemático y el movimiento en el emulador; la inspección visual y el dispositivo físico siguen siendo necesarios.


## Diagnóstico y rendimiento del emulador (0.18.0 → 0.19.0)

La ejecución 45731395340 pasó pruebas unitarias, compilaciones debug/release y verificación de firma/paquete, pero falló en el smoke test. El emulador instaló la APK y creó la actividad, aunque `am start -W` agotó su espera. El artefacto de diagnóstico contiene una jerarquía de ventana cuyo diálogo es «Pixel Launcher isn't responding»; el logcat registra ANR de varios procesos del sistema y miles de fotogramas omitidos. No hay una excepción fatal atribuida al proceso del juego ni capturas PNG de la escena. Esta ejecución no permite concluir si el personaje se veía bien en 0.18.0.

El script anterior ejecutaba `uiautomator dump` repetidamente durante hasta 60 intentos iniciales y otros 20 tras el movimiento. En un AVD x86_64 con SwiftShader/TCG, esas invocaciones frecuentes y la carga gráfica saturaban el emulador y hacían que el lanzador dejara de responder. Se ha sustituido la consulta repetida de jerarquía por telemetría de una línea por segundo en logcat, activada únicamente con el extra `debug_telemetry`. El script toma dos capturas con `screencap`, una antes y otra después del joystick, y verifica que las 60 piezas se envíen sin error, que el jugador se desplace y que la cámara acompañe el mismo vector. Reduce además la superficie del AVD a 960×540 y usa la imagen Android `default` sin Google APIs, que no son necesarias para el juego.

En el renderizador 0.19.0, el chequeo `glGetError` deja de ejecutarse 60 veces por fotograma (una llamada por pieza) y pasa a comprobar el lote completo del personaje. Se añade descarte de objetos y aldeanos fuera del campo de visión y nivel de detalle para árboles lejanos. Estas optimizaciones reducen sincronizaciones con el driver y geometría que no aporta píxeles visibles; no sustituyen la inspección de las capturas ni la prueba en el Poco X6 Pro con Android 16.


## Corrección del diálogo inmersivo y prueba de joystick (0.20.0)

La ejecución 45738355635 (versión 0.19.0) pasó `testDebugUnitTest`, `assembleDebug`, `assembleRelease` y la verificación de firma/paquete. El AVD arrancó, instaló la APK y abrió `MainActivity`. La captura inicial muestra el personaje procedural humanoide (cabeza/gorra parcialmente tapadas por un aviso del sistema, torso, brazos, piernas, botas y mochila) y el log informa `PARTS=60 GL=0x0000`. Sin embargo, el cuadro «Viewing full screen / Got it» de Android permaneció encima de la aplicación y capturó los toques. Por eso la telemetría se mantuvo en `POS=0.00,5.00` y el test falló con `Joystick moved the player only 0.00 m`. No era un error OpenGL ni una regresión del modelo: el diálogo del sistema bloqueaba la entrada táctil. La cámara no llegó a probarse en movimiento.

La versión 0.20.0 actualiza `scripts/emulator-smoke.sh` para detectar `ImmersiveModeConfirmation`, pulsar «Got it» según el tamaño real del AVD y comprobar que la ventana haya desaparecido antes de capturar o usar el joystick. El gesto se mantiene en el centro del joystick calculado a partir de los mismos factores que usa `Hud.onDraw()`. Se aumenta su duración a 1,2 s y se deja tiempo para que la telemetría recoja el desplazamiento. La prueba sigue exigiendo al menos 0,75 m de movimiento y que la cámara acompañe el vector con un error máximo de 0,15 m.

Esta corrección valida el flujo de prueba, no altera la geometría del personaje ni sustituye la inspección visual. La captura sin el diálogo deberá mostrar el modelo completo y se inspeccionarán tanto la imagen inicial como la imagen en movimiento. La validación física en el Poco X6 Pro con Android 16 sigue pendiente.


## Corrección de espera de superficie del diálogo (0.20.1)

La ejecución 45745324047 volvió a pasar las pruebas unitarias, ambas compilaciones y la verificación de APK. El smoke test detectó que el diálogo inmersivo seguía visible. El registro final muestra que el primer intento se envió antes de que Android hubiera creado la superficie del diálogo; al finalizar el script, la ventana todavía tenía `Surface: shown=true` y seguía siendo `mCurrentFocus`. Por tanto, el tap no había llegado al botón «Got it». El archivo de diagnóstico de este run no contiene captura de la escena ni telemetría del personaje, así que no se puede juzgar el movimiento en 0.20.0.

En 0.20.1 el script no confunde la existencia de la ventana con su visibilidad. Espera a que el bloque `ImmersiveModeConfirmation` indique `Surface: shown=true`, entonces pulsa el botón y vuelve a consultar el estado de la superficie y el foco de entrada. Solo continúa con la captura y el joystick cuando el aviso ya no es visible ni conserva el foco. Así se evita enviar los toques antes de que el diálogo esté listo.


## Contraste y silueta de compatibilidad (0.21.0)

La captura real del run 45746378469 muestra que el modelo humanoide sí se dibuja en el AVD: cabeza, torso, brazos, piernas y botas aparecen centrados y sin error OpenGL. Sin embargo, el movimiento acumulado fue solo 0,11 m y el test falló; por ello esa ejecución no demuestra todavía el seguimiento de cámara en movimiento. Además, la chaqueta verde anterior tenía luminancia casi idéntica al terreno verde y perdía separación visual a escala de teléfono.

En 0.21.0 la chaqueta pasa a azul petróleo oscuro, manteniendo la camiseta turquesa, el detalle naranja, la piel, las botas y la mochila en tonos distintos. Se añade una segunda pasada de silueta de 11 piezas esenciales (cabeza, torso, dos brazos, dos piernas, botas, cuello, gorra y mochila) con un shader GLES 2.0 independiente y sin atributos de normales. La pasada se superpone con baja opacidad cuando el modelo detallado funciona y queda opaca como respaldo si el shader principal no está disponible; el test acepta también el renderizador completo sin pasada secundaria si el controlador rechaza ese shader opcional. Las animaciones de brazos y piernas reutilizan las mismas transformaciones de caminar y reposo.

El smoke test ahora exige telemetría de las 60 piezas detalladas o de las 11 piezas de compatibilidad, comprueba los píxeles de piel/chaqueta/botas en la captura inicial y en la captura tras movimiento, y repite el gesto real del joystick hasta alcanzar 0,75 m de desplazamiento. La cámara debe acompañar el mismo vector con un error máximo de 0,15 m. La verificación por píxeles es una salvaguarda automatizada para el AVD; no sustituye la inspección visual ni la prueba en el Poco X6 Pro con Android 16.

La versión visible en el HUD es 0.21.0 para evitar confundir APK antiguas con la compilación nueva. La versión release-test continúa firmada con la clave de depuración de CI y no es una firma de publicación de Google Play.
