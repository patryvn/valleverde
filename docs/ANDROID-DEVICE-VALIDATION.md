# Validación del personaje y cámara en Android

## Cambios incluidos en 0.13.0

- El personaje se construye en el renderizador OpenGL ES 2.0 con piezas 3D separadas y reutiliza la misma especificación en las pruebas.
- La silueta incluye cabeza, cuello, torso, chaqueta abierta, brazos, manos, pantalón, piernas, botas con suela y cordones, gorro tejido, pelo, ojos verdes, pupilas, boca y bandolera.
- El diseño usa materiales de color con iluminación por normales; no incorpora todavía texturas fotográficas ni mapas UV. La franja naranja de la camiseta y la correa de la bandolera tienen rotación 3D explícita.
- Brazos y piernas mantienen canales de animación opuestos; el torso acompaña el paso con un balanceo leve y conserva una animación de reposo.
- La cámara de tercera persona sigue la posición y altura del jugador y conserva su órbita. El HUD muestra el estado del renderizador (3D OK y número de piezas) para distinguir un fallo de shader de un problema de encuadre.
- La matriz normal aplica las mismas rotaciones y la escala inversa para que la iluminación no se deforme en las piezas elípticas.

## Comprobaciones automatizadas

GitHub Actions ejecuta `:app:testDebugUnitTest`, `:app:assembleDebug` y `:app:assembleRelease`. Las pruebas comprueban presencia de piezas, dimensiones positivas, materiales válidos, colocación frontal de los ojos, rotación de la franja, pivotes de extremidades, transformación al desplazarse y seguimiento de cámara.

Las pruebas unitarias validan geometría declarativa y matemáticas; no pueden certificar que una GPU concreta dibuje correctamente el personaje.

## Validación física pendiente

En un móvil Android real, comprobar:

1. Que el HUD indique `3D OK` y el personaje se vea completo, no como un rectángulo o una pantalla azul.
2. Que se distingan cabeza, torso, brazos, piernas, gorro, botas y bandolera desde el inicio.
3. Que la cámara permanezca detrás del personaje y lo mantenga en pantalla al caminar en las cuatro direcciones.
4. Que la malla no parpadee, se estire ni desaparezca al girar, saltar o acercarse a objetos.
5. Que caminar y reposar tengan movimiento fluido, sin saltos de fase.
6. Que la sombra no tape los pies y que el sombreado se vea bien en distintas GPU y resoluciones.
7. Que pausar y reanudar no deje la escena en negro ni pierda el estado guardado.

Si aparece otra vez el rectángulo azul, anotar si el HUD muestra `3D OK` o `3D NO DISPONIBLE`: esa diferencia acota el problema entre la geometría/cámara y la inicialización de OpenGL.

## Instalación y firma de prueba

El APK release de CI se firma con la clave debug temporal de GitHub Actions para permitir la instalación directa. No es una firma apta para Google Play. Como cada ejecución puede generar una clave debug distinta, Android puede exigir desinstalar la versión anterior antes de instalar una APK nueva; desinstalar borra los datos locales de la partida. Para actualizaciones sin pérdida de progreso hará falta configurar una clave de pruebas persistente como secreto de GitHub.
