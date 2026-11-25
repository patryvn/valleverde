# Validación del personaje y cámara en Android

## Cambios incluidos en 0.11.0

- El personaje se renderiza desde una especificación de 28 piezas compartida entre el renderizador y las pruebas unitarias: botas, piernas, chaqueta, brazos, manos, cuello, cabeza, orejas, rasgos faciales, pelo, gorra y mochila con correas.
- Las piezas usan primitivas 3D (cubos y esferas) y materiales de color coherentes. No se han añadido mapas de textura fotográfica; el sombreado se calcula en el shader.
- El renderizador calcula la matriz normal mediante la inversa transpuesta del modelo-vista, incluida la escala no uniforme de cabeza, mochila y sombra. Esto mejora la iluminación de las superficies curvas y aplanadas.
- Brazos y piernas mantienen canales de animación opuestos al caminar; el torso y la cabeza tienen un balanceo pequeño en movimiento y reposo.
- La cámara de tercera persona sigue la posición y altura del jugador y orbita con la orientación de mirada.

## Comprobaciones automatizadas

GitHub Actions ejecuta :app:testDebugUnitTest, :app:assembleDebug y :app:assembleRelease. Las pruebas comprueban la presencia de las piezas humanas, dimensiones positivas, materiales dentro del rango válido, simetría de extremidades, canales de animación y seguimiento de cámara.

Estas pruebas validan los datos del modelo y la lógica de cámara/animación, pero no sustituyen una comprobación visual de OpenGL ES en el teléfono.

## Validación física pendiente

En un móvil Android real, comprobar:

1. Que se vea la silueta completa (cabeza, torso, dos brazos, dos piernas, gorra y mochila) al iniciar.
2. Que la cámara permanezca detrás del personaje y lo mantenga en pantalla al caminar en las cuatro direcciones.
3. Que la malla no parpadee, se estire ni desaparezca al girar, saltar o acercarse a objetos.
4. Que la sombra no tape los pies y que los colores sigan siendo legibles con distintas condiciones de luz.
5. Que el movimiento sea fluido y que el juego sobreviva a pausar y reanudar.
6. Que la GPU del dispositivo compile el shader y renderice correctamente las normales y transparencias.

## Instalación y firma de prueba

El APK release-test se firma con la clave debug temporal de GitHub Actions para que sea instalable sin configurar una clave de publicación. No es una firma apta para Google Play. Como cada ejecución de CI puede generar una clave debug distinta, Android puede exigir desinstalar la versión anterior antes de instalar una APK nueva; desinstalar borra los datos locales de la partida. Para actualizaciones sin pérdida de progreso hará falta configurar una clave de pruebas persistente como secreto de GitHub.
