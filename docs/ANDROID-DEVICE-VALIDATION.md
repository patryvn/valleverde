# Validación del personaje y cámara en Android

## Cambios incluidos en 0.10.0

- El personaje principal se compone de botas, pantalones, chaqueta, mangas articuladas, manos, cuello, cabeza ovalada, pelo, gorra y mochila.
- Cabeza y sombra de contacto usan una malla esférica procedural; el resto del cuerpo usa cubos con normales por cara.
- El shader aplica iluminación difusa direccional y luz ambiente a los materiales de color.
- La cámara se sitúa detrás de la dirección de movimiento y calcula su posición a partir de la posición, orientación y altura del jugador.
- La fase de animación continúa avanzando en reposo; brazos y piernas se balancean en sentidos opuestos al caminar.
- La velocidad de desplazamiento se calcula con delta de tiempo para reducir diferencias entre tasas de refresco.

## Comprobaciones automatizadas

GitHub Actions ejecuta `testDebugUnitTest`, `assembleDebug` y `assembleRelease`. Las pruebas unitarias cubren el seguimiento y la órbita de la cámara, la conservación del desplazamiento al saltar y la animación de extremidades. La compilación no demuestra por sí sola que el modelo se vea correctamente en todas las GPU.

## Validación física pendiente

En un móvil Android real, comprobar:

1. Que se vea la silueta completa (cabeza, torso, dos brazos, dos piernas y mochila) al iniciar.
2. Que la cámara permanezca detrás del personaje y lo mantenga en pantalla al caminar en las cuatro direcciones.
3. Que la malla no parpadee, se estire ni desaparezca al girar, saltar o acercarse a objetos.
4. Que la sombra no tape los pies y que los colores sigan siendo legibles con distintas condiciones de luz.
5. Que el movimiento sea fluido y que el juego sobreviva a pausar y reanudar.

## Instalación y firma de prueba

El APK release-test se firma con la clave debug temporal de GitHub Actions para que sea instalable sin configurar una clave de publicación. No es una firma apta para Google Play. Como cada ejecución de CI puede generar una clave debug distinta, Android puede exigir desinstalar la versión anterior antes de instalar una APK nueva; desinstalar borra los datos locales de la partida. Para actualizaciones sin pérdida de progreso hará falta configurar una clave de pruebas persistente como secreto de GitHub.
