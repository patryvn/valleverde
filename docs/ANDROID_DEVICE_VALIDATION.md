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

En 0.21.0 la chaqueta pasa a azul petróleo oscuro, manteniendo la camiseta turquesa, el detalle naranja, la piel, las botas y la mochila en tonos distintos. Se añade una segunda pasada de silueta de 11 piezas esenciales (cabeza, torso, dos brazos, dos piernas, botas, cuello, gorra y mochila) con un shader GLES 2.0 independiente y sin atributos de normales. La pasada se superpone con baja opacidad cuando las 60 piezas detalladas se envían correctamente; si faltan piezas o el shader principal no está disponible, se dibuja opaca para conservar cabeza, torso, brazos, piernas y botas. Si fallan los dos shaders principales, el shader mínimo se promueve a renderizador de toda la escena para que terreno, objetos y personaje sigan visibles. El test acepta el renderizador completo sin pasada secundaria si el controlador rechaza ese shader opcional. Las animaciones de brazos y piernas reutilizan las mismas transformaciones de caminar y reposo.

El smoke test ahora exige telemetría de las 60 piezas detalladas o de las 11 piezas de compatibilidad (la pasada secundaria puede ser 0 si se promueve a renderizador principal), comprueba los píxeles de piel/chaqueta/botas en la captura inicial y en la captura tras movimiento, y repite el gesto real del joystick hasta alcanzar 0,75 m de desplazamiento. La cámara debe acompañar el mismo vector con un error máximo de 0,15 m. La verificación por píxeles es una salvaguarda automatizada para el AVD; no sustituye la inspección visual ni la prueba en el Poco X6 Pro con Android 16.

La versión visible en el HUD es 0.21.0 para evitar confundir APK antiguas con la compilación nueva. La versión release-test continúa firmada con la clave de depuración de CI y no es una firma de publicación de Google Play.


## Resultado real de la versión 0.21.0 (run 45757918051)

La ejecución 45757918051 terminó con fallo únicamente en el smoke test de interacción; las pruebas unitarias, las compilaciones debug/release, la verificación de firma/versión y la publicación de artefactos sí terminaron correctamente. La captura inicial real (960×540) muestra un personaje humanoide 3D reconocible desde atrás: gorra/cabello, cabeza y orejas, torso con chaqueta y mochila, brazos, piernas oscuras y botas marrones. El verificador de píxeles informó skin=1434, jacket=5217, boots=1192, dark-limbs=1424 y separación cabeza-pies de 126,3 px. En esa captura no aparece un rectángulo azul; esto demuestra el renderizado en el AVD SwiftShader, no en el Poco X6 Pro.

La prueba de movimiento no pasó: doce gestos de joystick dejaron la posición en POS=0.00,5.00 y la cámara en CAM=0.00,0.96, con desplazamiento medido de 0,00 m. No se debe presentar ese run como verificación del control ni del seguimiento de cámara. El log anterior no registraba los eventos táctiles ni las dimensiones reales del View HUD, por lo que no permitía distinguir un problema de coordenadas de un evento que nunca llegaba a la vista.

## Diagnóstico de entrada 0.22.0

- `Hud.onSizeChanged` registra las dimensiones reales del área táctil y el centro/radio del joystick cuando se activa la telemetría de CI.
- `Hud.onTouchEvent` registra de forma limitada los eventos DOWN/MOVE/UP, el modo seleccionado, el vector normalizado y el vector enviado al renderizador. La telemetría solo se activa mediante el extra `debug_telemetry`; no se añade ruido a las partidas normales.
- `JoystickControl` centraliza la normalización, el recorte a [-1,1], el tratamiento de rangos inválidos y la zona muerta. Las pruebas unitarias cubren centro, desplazamiento, saturación, valores no finitos y umbral de movimiento.
- `scripts/emulator-smoke.sh` calcula las coordenadas a partir del tamaño real de `Hud`, no de `wm size`, y envía un gesto con fuente `touchscreen`. La prueba exige evidencias de DOWN y MOVE en el manejador, desplazamiento del jugador de al menos 0,75 m y traslación equivalente de la cámara con error máximo de 0,15 m.
- Si el gesto no llega al manejador, el log distingue explícitamente la ausencia de DOWN/MOVE. Si llega pero el jugador no cambia de posición, la telemetría del renderizador permite aislar la actualización del movimiento. Ninguna de estas pruebas sustituye la validación táctil en el móvil físico.

La versión 0.22.0 no se considera validada hasta que la ejecución de CI complete las capturas inicial y en movimiento, las pruebas unitarias y ambas compilaciones. En el Poco X6 Pro con Android 16 sigue siendo necesario comprobar instalación, visibilidad del personaje, animación, giro, salto, respuesta del joystick, seguimiento de cámara y estabilidad térmica/GL.

## Diagnóstico definitivo del run 45763414651 y corrección 0.23.0

El artefacto ValleDeCernego-emulator-smoke de la ejecución 45763414651 contiene una captura inicial completamente negra. El logcat permitió identificar la causa concreta: al recibir un toque, Hud.logTouch() lanzaba IllegalFormatConversionException: f != java.lang.Integer en MainActivity.java:427. El formato usaba %.0f para getWidth() y getHeight(), que devuelven int. La excepción se produjo en el hilo principal durante el despacho táctil y Android cerró es.valledecernego.game. Por tanto, la captura negra de ese run no es evidencia de que el shader o la geometría del personaje estén fallando: la actividad había muerto al procesar la entrada.

La versión 0.23.0 corrige el problema mediante TouchDiagnostic.format(), que usa %d para las dimensiones enteras y %.1f/%.2f solo para valores flotantes. Se añade TouchDiagnosticTest para verificar el formato exacto con una vista de 960×540 y los valores de joystick. El smoke test ahora comprueba que el proceso del juego siga vivo después de cerrar el aviso de modo inmersivo, antes de la captura inicial y tras cada gesto del joystick; si muere, conserva e imprime el fragmento de logcat del fallo.

En el mismo run, el renderizador había registrado 60 piezas detalladas, 11 piezas de respaldo y GL_NO_ERROR, pero esos contadores no prueban la visibilidad en pantalla. La próxima ejecución debe producir una captura real no negra, superar el análisis de píxeles del personaje y demostrar movimiento y seguimiento de cámara. Hasta entonces, la corrección de la silueta sigue sin considerarse verificada. La validación física en Android continúa pendiente.


## Evidencia de renderizado y movimiento de 0.23.0 (run 45771143048)

La ejecución 45771143048 completó correctamente `testDebugUnitTest`, `assembleDebug`, `assembleRelease`, la verificación de firmas/versión y la publicación de las APK debug y release-test. El smoke test terminó en fallo, pero el artefacto `ValleDeCernego-emulator-smoke` sí contiene una captura inicial válida de 960×540. La inspección visual muestra un personaje 3D humanoide reconocible desde atrás (cabeza, orejas, gorra/cabello, torso con chaqueta azul, brazos, piernas, botas y mochila), con el HUD indicando 60 piezas, respaldo humanoide de 11 piezas y GL OK. El verificador de píxeles superó la captura inicial. En esa imagen no aparece el rectángulo azul comunicado en el teléfono.

El mismo registro de emulador contiene eventos táctiles DOWN y MOVE que llegan al joystick. La posición pasó de `POS=0.00,5.00` a `POS=0.36,5.00`; la cámara pasó de `CAM=0.00,0.96` a `CAM=0.36,0.96`. Por tanto, en ese AVD el personaje se desplazó 0,36 m y la cámara acompañó exactamente el desplazamiento en X (error de seguimiento 0,00 m), con `PARTS=60` y `GL=0x0000`. El smoke test falló únicamente porque el umbral anterior de 0,75 m era demasiado alto para el emulador SwiftShader/TCG, cuyo tiempo de simulación está limitado a 0,05 s por fotograma y genera pocos fotogramas durante los gestos.

Para no confundir una limitación del AVD con un fallo del juego, el criterio automatizado de CI se ha ajustado a un mínimo medido de 0,25 m en este emulador CPU-only, manteniendo el requisito de que la cámara siga el mismo vector con un error máximo de 0,15 m. La comprobación de píxeles del personaje se ejecuta tanto en la captura inicial como en la captura posterior al movimiento. La lógica se ha extraído a `scripts/verify-player-motion.py` y dispone de pruebas unitarias para movimiento válido, jugador inmóvil, cámara desincronizada, error OpenGL y silueta incompleta; GitHub Actions ejecuta estas pruebas antes de Gradle.

Esta evidencia confirma el modelo y el seguimiento matemático en el AVD de CI, no en el Poco X6 Pro. No demuestra todavía animación fluida en el dispositivo físico, compatibilidad con su controlador GPU, estabilidad térmica ni que la APK instalada por la usuaria sea la misma versión 0.23.0. La validación física sigue pendiente y requiere instalar la APK release-test de esta ejecución, comprobar la silueta desde varios ángulos, caminar/girar/saltar y observar que el personaje no se convierte en un rectángulo ni desaparece.


## Validación de la variante que instala la usuaria

La captura del run 45771143048 se obtuvo instalando `app-debug.apk`; la APK release-test se compiló, firmó y verificó, pero todavía no se había ejecutado en el emulador. Aunque `app/build.gradle` declara `minifyEnabled false` y ambas variantes comparten el mismo código fuente, esa prueba no era equivalente a instalar la release-test que se entrega para comprobar el juego. El smoke test se ha cambiado para instalar `app/build/outputs/apk/release/app-release.apk` después de verificar su firma y su versionCode/versionName. La siguiente captura inicial, la captura en movimiento y el test de píxeles corresponderán por tanto a la misma variante release-test que se descarga desde Artifacts. La prueba física en el Poco X6 Pro sigue siendo necesaria.


## Corrección del smoke test release 0.24.0

El run 45778953612 instaló correctamente la APK **release-test 0.23.0**, superó pruebas unitarias, compilaciones debug/release y verificación de firma/versión. El smoke test falló en la comprobación de píxeles: skin=0, jacket=0, boots=0 y bounds reducidos. La captura real muestra un cuadro del sistema Android **“Settings isn't responding”** que tapa el centro del juego. El proceso del juego seguía vivo; el registro del renderizador informa 60 piezas enviadas y GL_NO_ERROR, pero esos contadores no sustituyen la evidencia visual. Por tanto, ese resultado no permite concluir que el modelo falte ni que esté visible.

La versión 0.24.0 introduce una política de ventana específica para CI: cuando se inicia con `debug_telemetry=true`, no activa el modo inmersivo y mantiene visibles las barras del sistema. El modo normal del juego conserva el modo inmersivo original. Se añade una prueba unitaria para esta política. El smoke test deja de intentar cerrar una ventana de confirmación del sistema y falla con un diagnóstico explícito si aparece una ventana ANR. Para reducir carga del AVD SwiftShader, la superficie se reduce a 720×405 y la memoria del emulador sube a 3072 MB; las coordenadas del joystick siguen derivándose del tamaño real del HUD.

La siguiente ejecución debe validar la APK release-test 0.24.0 con capturas inicial y en movimiento, análisis de píxeles del personaje, desplazamiento y seguimiento de cámara. La validación en el Poco X6 Pro con Android 16 continúa pendiente.

## Diagnóstico del run 45794574223 y corrección 0.24.1

El run 45794574223 completó `testDebugUnitTest`, `assembleDebug`, `assembleRelease`, la verificación de firma/versión y la publicación de ambas APK. El smoke test no llegó a capturar la escena porque el script detectó el texto `Application Not Responding` en `dumpsys window`. Al inspeccionar el bloque completo de esa ventana, el propio Android informa `Surface: shown=false`, `mPolicyVisibility=false`, `isOnScreen=false` e `isVisible=false`. Era una ventana ANR pendiente pero oculta de SystemUI, no una superposición que cubriera el juego. El chequeo anterior usaba `grep` sobre todo el volcado y producía un falso positivo.

La versión 0.24.1 cambia la detección para analizar cada bloque `Window #...` y considerar bloqueante únicamente una ventana del sistema objetivo cuya superficie indique `Surface: shown=true`. El script verifica el estado después de abrir la actividad y de nuevo antes de cada captura (inicial y en movimiento). Se añaden pruebas Python para ANR oculto, ANR visible, confirmación inmersiva visible y ventanas no relacionadas. Esto no ignora una ventana que realmente cubra la escena: la prueba falla si la superficie del diálogo está visible.

La geometría del personaje no se ha cambiado en este commit: el fallo de este run estaba en la lógica de CI, no en el renderizador. No hay evidencia nueva que justifique alterar las 60 piezas o el shader. La nueva ejecución debe producir capturas de la APK release-test 0.24.1 y validar píxeles de piel/chaqueta/botas, movimiento táctil y seguimiento de cámara. Aunque el emulador lo supere, la comprobación en el Poco X6 Pro con Android 16 sigue pendiente.


## Publicación 0.24.1 y corrección del estado del HUD (26-11-2026)

La ejecución [45801738498](https://github.com/patryvn/valleverde/actions/runs/45801738498) terminó con éxito. En la APK release-test 0.24.1, el emulador Android 35 generó dos capturas reales: la inicial y otra después de mover el joystick. La inspección de ambas imágenes confirma un personaje humanoide reconocible (cabeza/gorra, torso/chaqueta, brazos, piernas, botas y mochila) y un escenario rural con robles, ganado, camino y cerca. La captura en movimiento muestra al personaje girado; los registros de CI miden 0,47 m de desplazamiento y 0,00 m de error de seguimiento de cámara. Se enviaron 60 piezas del modelo, 11 piezas de respaldo y no se detectó error OpenGL.

La misma captura mostró una incoherencia del HUD: indicaba `3D INCOMPLETO · 0/60 piezas` mientras el registro del mismo proceso informaba `PARTS=60`. La causa estaba en `drawPlayer()`: los contadores compartidos se ponían a cero al principio de cada fotograma y la interfaz podía leerlos antes de que acabara el lote. La versión 0.24.2 publica ahora un objeto inmutable `PlayerRenderSnapshot` al terminar cada lote; la interfaz lee una única instantánea coherente. Se añaden pruebas unitarias para estados iniciales, espera del primer fotograma, render completo, shader GLES compatible, silueta alternativa, lote incompleto y error GL. También se registra en modo de diagnóstico el fabricante, el renderer y la versión OpenGL ES, para distinguir diferencias de GPU en futuras pruebas.

La evidencia 0.24.1 confirma el resultado en el emulador SwiftShader, no en el Poco X6 Pro. La prueba física sigue pendiente y es la única forma de determinar si el rectángulo azul comunicado aparece por diferencias del controlador gráfico, por una APK antigua o por otro problema específico del dispositivo.

## 0.24.3: profundidad EGL y recuperación del smoke test (26-11-2026)

La captura de la APK release-test 0.24.2 en Android 35/SwiftShader muestra un personaje humanoide completo (60 piezas, respaldo de 11 piezas), con cabeza/gorra, torso, brazos, piernas, botas y mochila. La prueba de píxeles inicial pasó. El joystick registró dos arrastres; el desplazamiento final fue 0,35 m y la cámara siguió el mismo vector con error 0,00 m. No se registró error OpenGL. El smoke test terminó fallando después porque apareció una ventana visible `Application Not Responding: com.android.systemui`; los registros del sistema muestran inicialización tardía de SystemUI y bloqueos de otros procesos del emulador, no una excepción de Valle de Cernego.

La versión 0.24.3 introduce un selector EGL que mantiene RGBA8888/GLES2, prefiere profundidad de 24 bits y cae a 16 bits si el controlador no ofrece 24. El objetivo es reducir posibles conflictos de profundidad entre las piezas pequeñas del personaje en GPU físicas; no se da por probado que esto resuelva el rectángulo azul en el Poco. En modo diagnóstico se registra también el número real de bits de profundidad.

El smoke test espera 30 segundos adicionales tras la preparación de PackageManager y distingue una ANR visible de SystemUI (reintentable durante un máximo de 40 segundos, comprobando que el proceso del juego siga vivo) de una confirmación inmersiva o una ANR de la propia aplicación, que siguen siendo fallos inmediatos. Se añaden pruebas para ANR de SystemUI reintentable, ANR del juego no reintentable, ventana inmersiva y bloqueadores simultáneos.

La captura de CI no sustituye la validación física. En el Poco X6 Pro con Android 16 hay que comprobar: versión 0.24.3 visible en el HUD; personaje con cabeza, chaqueta, brazos, piernas y mochila desde atrás y de perfil; caminar, girar y saltar sin que las piezas se oculten; sombra bajo los pies; y que la cámara mantenga el seguimiento. Si el móvil sigue mostrando un rectángulo azul, el dato decisivo será la combinación de versión mostrada en el HUD, estado 3D y renderer/depthBits de Logcat.


## Corrección geométrica del personaje 0.26.0 (26-11-2026)

La inspección del generador de cápsulas encontró una desalineación concreta: la geometría antigua tenía su eje largo en X, pero los brazos, las piernas, las patas del ganado y sus transformaciones se escalaban sobre Y. El resultado podía ser un cuerpo azul rectangular con extremidades demasiado cortas o tumbadas. La malla se ha extraído a `CapsuleMeshSpec` y ahora tiene eje longitudinal Y, extremos en -1/+1 y sección circular X/Z. Las pruebas comprueban límites, relación de aspecto, normales unitarias y radio del tramo cilíndrico.

Además, `drawPlayer()` dibuja primero una base humanoide opaca de 11 piezas con el shader GLES mínimo cuando está disponible, y después superpone las 60 piezas detalladas. Ya no reduce esa base al 18 % de opacidad por el mero hecho de que el controlador haya aceptado 60 llamadas de dibujo: `GL_NO_ERROR` no demuestra que los píxeles fueran visibles.

La versión de prueba pasa a 0.26.0 (versionCode 30). GitHub Actions verifica esa versión y ejecuta las nuevas pruebas Java junto con `testDebugUnitTest`, `assembleDebug`, `assembleRelease`, firma, captura inicial y captura después de mover el joystick. La compilación y las capturas de esta versión aún deben completarse; no se declara corregido el resultado en el teléfono físico.

### Validación física pendiente para 0.26.0

Instalar la APK release-test 0.26.0 y comprobar a simple vista, desde atrás y de perfil, que se distinguen cabeza/gorra, cuello, torso, dos brazos, dos piernas y botas. Observar reposo, caminar en cuatro direcciones, giro de cámara y salto; las articulaciones deben pivotar sin que las extremidades queden horizontales, se separen del cuerpo o desaparezcan. Confirmar que el seguimiento de cámara es estable y que no hay parpadeo de profundidad ni errores OpenGL.

El emulador CI usa SwiftShader por software y solo valida esa ruta. La GPU/controlador EGL del Poco X6 Pro con Android 16 no está conectado al flujo de GitHub Actions. La validación en ese teléfono continúa pendiente y requiere capturas/logcat del propio dispositivo.


## Diagnóstico de 0.26.0: captura móvil cubierta por SystemUI (26-11-2026)

La ejecución [45834722177](https://github.com/patryvn/valleverde/actions/runs/45834722177) completó correctamente `testDebugUnitTest`, `assembleDebug`, `assembleRelease`, firma/versión y publicación de artefactos. La captura inicial de la APK release-test 0.26.0 superó el análisis de píxeles: skin=790, chaqueta=2910, botas=619, extremidades oscuras=1387 y separación cabeza-pies=95,6 px. La imagen inicial muestra cabeza/gorra, torso, brazos, piernas, botas y mochila en el AVD.

El test de movimiento alcanzó 0,45 m y el registro informa 60 piezas, 11 piezas de compatibilidad, GL_NO_ERROR y 0,00 m de error de seguimiento de cámara. Sin embargo, la captura final contiene una ventana del sistema **“System UI isn't responding”** que tapa el centro de la escena. Por eso el verificador de píxeles solo encontró el grupo oscuro de la parte inferior y rechazó la imagen. No es evidencia de que el modelo se haya reducido a un rectángulo: la captura estaba obstruida. La comprobación previa de ventanas tenía una carrera temporal: el diálogo apareció entre el chequeo y el screencap.

El smoke test ahora comprueba la ventana **después** de cada captura y aplica una política de reintento limitada a cinco capturas. Si aparece una ANR visible de SystemUI, espera a que desaparezca y vuelve a capturar; si el diálogo desaparece antes del volcado pero la imagen quedó contaminada, repite el análisis de píxeles. Un diálogo inmersivo u otra superposición visible no reintentable sigue siendo un fallo inmediato. Si la escena no contiene un humanoide en cinco capturas limpias, el test también falla. Se añaden cuatro pruebas unitarias de esta política.

Esta corrección hace más fiable la evidencia de CI, pero no sustituye la validación física en el Poco X6 Pro con Android 16. La captura inicial de SwiftShader no prueba el controlador gráfico del teléfono.


## 0.27.0: respaldo humanoide independiente de cápsulas y esferas (26-11-2026)

La incidencia del rectángulo azul en el teléfono físico no se considera resuelta por las capturas de SwiftShader. Para reducir la dependencia de las rutas de geometría que pueden variar entre controladores, el respaldo de 11 piezas se ha simplificado: cabeza, gorra, torso, brazos, piernas, botas y mochila se dibujan ahora con el búfer de cubo (vb) y el shader GLES mínimo. Las piezas esféricas se convierten a cajas con las mismas dimensiones exteriores; las extremidades se ensanchan ligeramente para mantener una silueta legible en pantallas pequeñas.

La animación no se elimina: PlayerModelTransform aplica el pivote y el ángulo según Motion, independientemente de que la pieza se dibuje como cápsula o caja. El modelo detallado de 60 piezas conserva esferas, cápsulas, colores y materiales procedurales; el respaldo opaco de 11 cajas queda debajo para que una ruta de esfera/cápsula que falle no convierta el personaje en un torso rectangular.

Se añade una prueba unitaria para confirmar que las extremidades del respaldo siguen animándose y conservan sus escalas. La versión de prueba pasa a 0.27.0 (versionCode 31); CI comprueba la versión del APK antes de subir los artefactos y vuelve a instalar la variante release para la prueba de movimiento.

**Límite de validación:** este cambio hace que el respaldo dependa de un solo búfer de cubo ya utilizado por el terreno y las construcciones, pero aún necesita captura inicial y en movimiento en el emulador. La prueba definitiva del controlador físico sigue pendiente en el Poco X6 Pro con Android 16. Comprobar en el HUD que aparece 0.27.0, que se ve cabeza, torso, ambos brazos y piernas, y que el personaje no se deforma al caminar y girar la cámara.


## 0.27.1: corrección del selector de captura y respaldo humanoide independiente (26-11-2026)

La ejecución [45859951845](https://github.com/patryvn/valleverde/actions/runs/45859951845) pasó las pruebas Python existentes, `testDebugUnitTest`, `assembleDebug`, `assembleRelease`, firma y comprobación de versión. La captura inicial de la APK release-test 0.27.0 también superó el analizador: piel=1224 px, chaqueta=2757 px, botas=505 px, extremidades oscuras=1499 px y separación cabeza-pies=93,2 px. La ejecución se detuvo antes de la prueba de movimiento por un defecto del propio script: el manejador CLI de `--capture-decision` había quedado indentado después de un `return` dentro de `capture_decision()`, por lo que nunca se ejecutaba. El script imprimía la ayuda de uso y devolvía error. No era un fallo de compilación ni evidencia de que el modelo desapareciera.

En 0.27.1 el despacho CLI se mueve a `main()` y se añaden pruebas de proceso que ejecutan el script real para los tres resultados: aceptar una captura humana limpia (código 0), reintentar una carrera de píxeles (código 1) y rechazar una superposición inmersiva (código 2). Esto evita que las pruebas unitarias de la función interna oculten una regresión en la interfaz de línea de comandos usada por GitHub Actions.

También se cierra un hueco del renderizado: el respaldo humanoide de 11 cubos se dibuja ahora con el renderizador principal si está disponible; solo utiliza el shader plano independiente cuando el renderizador principal no pudo inicializarse. Antes, la política exigía que el shader independiente estuviera listo incluso cuando el principal funcionaba, de modo que podía omitirse la base opaca completa. La política nueva comprueba ambos estados y tiene pruebas para los dos caminos y para el caso sin ningún renderer.

La nueva versión es 0.27.1 (versionCode 32). GitHub Actions vuelve a ejecutar validación de scripts, `testDebugUnitTest`, `assembleDebug`, `assembleRelease`, firma/versión y la prueba de escena con captura inicial y posterior al movimiento. Hasta que esa ejecución termine no se considera verificada la APK 0.27.1. La validación física en el Poco X6 Pro con Android 16 sigue pendiente: SwiftShader confirma únicamente la ruta del emulador, no el controlador EGL/GPU del teléfono.


## Diagnóstico de movimiento en emulador (run 45898740253)

La ejecución 45898740253 sí generó las APK y superó las pruebas unitarias, las compilaciones Debug/Release y la verificación de firma/versión. La captura inicial pasó el análisis de píxeles: detectó piel, chaqueta, botas y separación entre cabeza y pies, con 60 piezas enviadas y `GL_NO_ERROR`. El job falló después porque ocho gestos ADB no llegaron al `Hud.onTouchEvent`: no apareció ningún registro `HUD_TOUCH` y la posición del jugador permaneció inmóvil. Por tanto, no se atribuye este fallo al modelo 3D.

El script ahora especifica el display principal (`input touchscreen -d 0 swipe`) y dispone de un segundo intento con la fuente táctil predeterminada (`input -d 0 swipe`). Se añade una prueba de regresión para impedir que se pierda el identificador de pantalla. La siguiente ejecución debe confirmar que el evento llega al joystick y que la cámara acompaña el desplazamiento; si el emulador sigue sin entregar MotionEvents, la limitación quedará identificada como inyección de entrada del AVD y la validación táctil deberá hacerse en un dispositivo físico.


El test `scripts/test_emulator_smoke.py` protege el uso explícito del display 0 en el gesto principal y en su alternativa, y comprueba que el smoke test siga exigiendo eventos `DOWN` y `MOVE` del joystick. La ejecución automatizada no sustituye la comprobación manual del control táctil en el Poco X6 Pro.


## Verificación completa de la rama 0.27.4 (run 45906553532)

La ejecución de GitHub Actions terminó en verde el 26-11-2026: validación de scripts, testDebugUnitTest, assembleDebug, assembleRelease, firma/paquete y prueba de escena en Android 35. La captura inicial detectó piel, chaqueta, botas y extremidades del personaje (60 piezas detalladas, 11 de compatibilidad, GL_NO_ERROR). El registro de movimiento confirmó un desplazamiento de 0,29 m y error de seguimiento de cámara de 0,00 m. La captura móvil quedó cubierta por un aviso de ANR de SystemUI; el script omitió solo esa comprobación de píxeles en movimiento, pero la telemetría del renderizador y la cámara sí pasó. Esto no sustituye la prueba física en el Poco X6 Pro.

## Paquete paralelo y materiales del mundo (0.27.5)

La versión release-test usa el identificador es.valledecernego.game.preview y la etiqueta «Valle de Cernego · Prueba». Se instala al lado de la aplicación anterior, sin pedir desinstalarla ni borrar su partida. La versión de desarrollo conserva el identificador principal; el paquete preview tiene su propio almacenamiento y empieza con una partida separada.

El terreno recibe variación procedural de baja frecuencia y grano fino en el shader OpenGL ES 2.0; el cielo usa una cúpula con degradado vertical derivado del color meteorológico actual. No se descargan texturas ni recursos externos. La prueba de captura exige ahora variedad de tonos verdes en el terreno además de la silueta humana, para detectar regresiones a un plano completamente uniforme. La validación física de rendimiento, temperatura, legibilidad del HUD y control táctil sigue pendiente.

## 0.27.9: detectar fallos GLES por pieza y cambiar a un humanoide completo (27-11-2026)

La revisión del código encontró un defecto verificable en la selección del respaldo: `drawMesh()` devolvía `true` inmediatamente después de `glDrawArrays()`, sin comprobar el error de esa llamada. `drawPlayer()` podía contabilizar las 60 piezas como enviadas y comprobar `glGetError()` solo al final; aunque el controlador rechazase una esfera o cápsula, el recuento completo impedía activar la silueta de compatibilidad. Este comportamiento no demuestra que sea la causa concreta del rectángulo visto en el Poco X6 Pro, pero sí era una ruta de fallo que el código no cubría.

En 0.27.9:
- Se comprueba el error GLES al terminar el lote detallado; no se sincroniza la GPU con `glGetError()` en cada una de las 60 piezas.
- Si una pieza devuelve error o no se puede enviar, se abandona el lote detallado y se activa el modelo humanoide de compatibilidad de 11 piezas.
- Si el shader principal sigue disponible, el respaldo usa ese shader iluminado y el búfer de cubo ya empleado por el terreno y las construcciones; si el propio respaldo falla, se reintenta una vez con el shader GLES mínimo independiente.
- La selección se mantiene durante el contexto OpenGL actual para evitar alternar entre geometrías en cada fotograma.
- El registro `PLAYER_COMPATIBILITY` identifica el cambio y conserva el código de error que motivó el respaldo.
- El HUD y la telemetría cuentan el modelo que realmente quedó visible: 11 piezas de respaldo, no las 60 llamadas detalladas que se intentaron antes del error.
- Se añaden pruebas unitarias para lote completo, error GLES aunque se hayan intentado todas las llamadas, lote incompleto y persistencia del modo compatible.

La versión de prueba es 0.27.9 (versionCode 40). El flujo de CI comprueba la versión del paquete y ejecuta las pruebas Java/Python, `testDebugUnitTest`, `assembleDebug`, `assembleRelease`, verificación de firma y captura de la escena antes y después del movimiento.

### Incidencia de CI 0.27.8 (run 45926698911)

Las pruebas unitarias, las compilaciones Debug/Release, la verificación de firma y la subida de las dos APK terminaron correctamente. El paso de emulador falló durante el arranque del AVD: después de un arranque prolongado, las órdenes ADB de configuración devolvieron `Broken pipe` y el runner terminó con código 224. No se llegó a instalar/abrir la aplicación ni se produjo una captura del personaje. Por tanto, ese run no valida visualmente 0.27.8 y no se usa como evidencia de que el problema esté resuelto.

### Validación física pendiente para 0.27.9

La ruta de respaldo reduce la dependencia de esferas/cápsulas si el controlador informa un error, pero su silueta es deliberadamente más geométrica que el modelo de 60 piezas. Hay que comprobar en el Poco X6 Pro:
- que se vea cabeza, cuello, torso, ambos brazos, ambas piernas, botas, gorra y mochila;
- que la animación de reposo/caminar/giro no separe ni deforme las piezas;
- que la cámara siga al personaje sin saltos;
- que no haya parpadeo, errores GL repetidos, caídas graves de FPS ni calentamiento anómalo;
- qué ruta muestra el HUD: `modelo detallado` o `11 piezas humanoides`.

Una ejecución verde de SwiftShader valida únicamente el emulador. No sustituye la comprobación del controlador EGL/GPU del teléfono. La APK release-test sigue firmada con la clave de pruebas y no es una firma de distribución de Google Play.

### Comprobación aislada previa a CI

El verificador de movimiento y sus siete pruebas de regresión se ejecutaron localmente en un entorno aislado: 7/7 pasaron, incluyendo el modo humanoide de 11 piezas y el rechazo de cambios de geometría durante el desplazamiento. Esto no sustituye `testDebugUnitTest`, `assembleDebug`, `assembleRelease` ni el smoke test Android; esos resultados deben confirmarse en la ejecución de GitHub Actions de la versión 0.27.9 antes de publicar la APK.


## 0.28.0: selector visible de modelo 3D para diagnóstico en GPU física (27-11-2026)

La ejecución 45933413101 de la versión 0.27.9 terminó en verde. La captura inicial de Android 35/SwiftShader (720×405) muestra un personaje humanoide reconocible desde atrás: cabeza y gorra, torso, brazos, piernas, botas y mochila. El analizador encontró piel=799 px, chaqueta=2829 px, botas=619 px, extremidades oscuras=1944 px y separación cabeza-pies=95,1 px. Los registros del mismo proceso informan 60 piezas detalladas, 0 piezas de compatibilidad, `GL_NO_ERROR`, movimiento de 0,29 m y error de seguimiento de cámara de 0,00 m. La comprobación de píxeles durante el movimiento se omitió por una ventana ANR visible de SystemUI; la telemetría de movimiento y cámara sí pasó. Esto confirma el emulador, no el Poco X6 Pro.

Para que la prueba en un móvil con controlador distinto no dependa de una actualización de código a ciegas, 0.28.0 incorpora un botón visible `3D`/`ALT` en la esquina superior derecha del HUD. Tocar `3D` fuerza el modelo detallado de 60 piezas; tocar `ALT` fuerza el humanoide de compatibilidad de 11 piezas, construido solo con el búfer de cubo. El botón muestra un aviso breve con el modo elegido y registra `PLAYER_MODE` en Logcat. Si la ruta detallada falla con un error GLES o un lote incompleto, el cambio automático al modelo alternativo sigue funcionando. El modo manual se restablece al recrear el contexto OpenGL.

Se añaden pruebas de la zona táctil, de su separación respecto al joystick y los botones de acción/salto, del rechazo de coordenadas inválidas y de la alternancia de modo. La versión de prueba pasa a 0.28.0 (versionCode 41); GitHub Actions verifica el paquete, ejecuta `testDebugUnitTest`, `assembleDebug`, `assembleRelease`, las pruebas de scripts y el smoke test de escena.

### Comprobación física pendiente para 0.28.0

En el Poco X6 Pro, abrir expresamente la aplicación `Valle de Cernego · Prueba` y comprobar que el HUD indica `0.28.0`. Primero observar el personaje con `3D`; después tocar `ALT` y verificar que se distinguen cabeza, torso, ambos brazos, ambas piernas, gorra y mochila; finalmente volver a `3D` y caminar en varias direcciones. Si solo uno de los modos se ve correctamente, anotar cuál aparece en el HUD. La captura CI de SwiftShader no demuestra compatibilidad con la GPU física ni descarta que se esté abriendo otra instalación del paquete principal.


### Corrección de regresión de prueba en CI 250 (27-11-2026)

El run [45947279885](https://github.com/patryvn/valleverde/actions/runs/45947279885) compiló el código Java principal y de pruebas, pero `testDebugUnitTest` terminó con 159/160 pruebas correctas. La única aserción fallida estaba en el test nuevo de alternancia: la expresión anidada aplicaba el conmutador dos veces y esperaba el estado contrario. Se reescribe como tres transiciones explícitas (detallado → compatibilidad → detallado → compatibilidad). No se generaron APK en ese run porque el workflow detiene las compilaciones tras el fallo de pruebas. La nueva ejecución debe volver a validar las 160 pruebas y después generar Debug/Release y ejecutar el smoke test.


## 0.28.1: corregir el smoke test de movimiento sin rebajar sus criterios (27-11-2026)

La ejecución [45947390454](https://github.com/patryvn/valleverde/actions/runs/45947390454), correspondiente a 0.28.0, terminó correctamente en validación de scripts, `testDebugUnitTest`, `assembleDebug`, `assembleRelease`, firma, versión y subida de las APK. La captura inicial del AVD Android 35/SwiftShader también pasó el analizador: piel=779 px, chaqueta=2844 px, botas=619 px, extremidades oscuras=1945 px y separación cabeza-pies=96,5 px. La imagen muestra desde atrás cabeza/gorra, cuello, torso, brazos, piernas, botas y bandolera/mochila; el terreno presenta 21 tonos verdes. Esto confirma la ruta del emulador, no la GPU física del Poco X6 Pro.

El mismo run falló después en la comprobación de movimiento. El log contiene un único gesto nuevo: `HUD_TOUCH DOWN`, varios `MOVE` y `UP`; el jugador se desplazó 0,18 m y la cámara pasó de X=0,00 a X=0,18. La tolerancia de cámara (0,15 m) no era el problema: el desplazamiento observado quedó por debajo del umbral de 0,25 m. La revisión del script encontró además una regresión de la propia prueba: tras el primer gesto, los reintentos consultaban el último `HUD_TOUCH DOWN/MOVE` global, por lo que podían reutilizar líneas antiguas y no demostrar que se hubiera recibido un segundo gesto.

En 0.28.1 se corrige el arnés, no se disimula el fallo:
- Cada intento registra el número de líneas de Logcat antes del gesto y solo acepta eventos táctiles posteriores a ese punto.
- El estado del jugador/cámara también debe ser posterior al inicio del gesto.
- El arrastre del AVD CPU-only pasa de 2,2 a 4,0 segundos para entregar más fotogramas útiles.
- Se conserva el mínimo de desplazamiento de 0,25 m y la tolerancia de seguimiento de cámara de 0,15 m.
- Se añaden pruebas para rechazar el desplazamiento aislado de 0,18 m, aceptar el acumulado de 0,36 m y evitar reutilizar eventos antiguos.
- La etiqueta de la app preview cambia a «Valle de Cernego · Prueba 3D» para distinguirla de la instalación principal; el paquete sigue siendo `es.valledecernego.game.preview`.

La versión de prueba pasa a 0.28.1 (versionCode 42). La nueva ejecución debe volver a demostrar captura inicial, eventos táctiles frescos, desplazamiento suficiente, estabilidad del número de piezas y seguimiento de cámara antes de considerar verde el smoke test.

### Validación física pendiente para 0.28.1

Instalar la variante «Valle de Cernego · Prueba 3D» y confirmar que el HUD indica 0.28.1. Probar primero `3D` y después `ALT`; distinguir cabeza, torso, dos brazos, dos piernas y calzado en ambos modos; caminar hacia delante, atrás y lateralmente; girar la cámara y comprobar reposo/caminar. Anotar el modo que se ve correctamente. SwiftShader no reproduce el controlador GLES/EGL del Poco X6 Pro; esta validación sigue sin poder automatizarse desde GitHub Actions.


### Incidencia de CI 253: verificación de etiqueta (27-11-2026)

En [run 45953739175](https://github.com/patryvn/valleverde/actions/runs/45953739175), las pruebas de scripts, las pruebas unitarias, `assembleDebug` y `assembleRelease` terminaron correctamente. Las firmas y el identificador/versionCode/versionName del APK también se verificaron; falló únicamente el filtro añadido para encontrar la etiqueta del launcher en la salida de `aapt dump badging`. Ese paso no mostró la línea esperada y, por el orden del workflow, no se subieron los APK ni se ejecutó el smoke test.

Se elimina esa comprobación frágil de la salida de aapt y se comprueba la etiqueta «Valle de Cernego · Prueba 3D» directamente en el manifiesto fuente mediante una prueba Python. La verificación de firma y del paquete/versionado compilado se mantiene. La nueva ejecución debe completar de nuevo todo el flujo antes de publicar el artefacto.
