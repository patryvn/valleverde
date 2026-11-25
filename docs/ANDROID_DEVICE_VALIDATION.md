# Validación de renderizado en Android

## Estado de la rama

El personaje se construye con `PlayerModelSpec` y `PlayerModelTransform`: piezas 3D procedurales de cabeza, torso, brazos, piernas, ropa, gorra y mochila. La escena usa OpenGL ES 2.0, iluminación direccional por shader y una sombra translúcida bajo el personaje. El modelo no usa todavía texturas fotográficas; sus materiales son colores sólidos.

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

El flujo actualizado vuelve a habilitar TCG, espera a que la actividad de juego esté en primer plano, despierta la pantalla y especifica el display ID en el gesto. Guarda captura inicial, captura tras movimiento, jerarquía de ventana y logcat completo. Una ejecución verde confirma que la actividad y el HUD responden; aun así, la imagen debe revisarse visualmente antes de declarar resuelto el rectángulo azul. La emulación TCG en este runner es muy lenta y no sustituye una prueba física.


### Incidencia más reciente de CI (run 45689670313)

La compilación de la APK y las pruebas unitarias pasaron, pero el emulador Android 35 con TCG terminó el arranque aparente sin que PackageManager estuviera disponible. `adb install` falló con `Can't find service: package` y el servicio de actividad respondió con `Broken pipe`. No se instaló la aplicación y no se generó ninguna captura; por tanto, este run no aporta evidencia sobre el personaje. El log confirma que el runner desactiva la aceleración KVM y arranca con `-accel off`, por lo que el tiempo de arranque y la estabilidad del sistema Android virtual son limitaciones del entorno, no fallos atribuidos al renderizador.

El workflow ahora espera explícitamente a que `cmd package list packages` devuelva paquetes antes de instalar, reintenta la instalación y, si vuelve a fallar, guarda `getprop` y `logcat` completos para distinguir un servicio tardío de un `system_server` caído. Hasta que una ejecución produzca capturas reales de la actividad, no se modifica el modelo por conjetura ni se declara corregido el rectángulo azul.
