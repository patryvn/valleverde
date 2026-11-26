# Validación del personaje y cámara en Android

## Cambios incluidos en 0.27.2

- El personaje se construye en el renderizador OpenGL ES 2.0 con piezas 3D separadas y reutiliza la misma especificación en las pruebas.
- La silueta incluye cabeza, cuello, torso, chaqueta abierta, brazos, manos, pantalón, piernas, botas con suela y cordones, gorro tejido, pelo, ojos verdes, pupilas, boca y bandolera.
- El diseño usa materiales de color con iluminación por normales; no incorpora todavía texturas fotográficas ni mapas UV. La franja naranja de la camiseta y la correa de la bandolera tienen rotación 3D explícita.
- Brazos y piernas mantienen canales de animación opuestos; el torso acompaña el paso con un balanceo leve y conserva una animación de reposo.
- La cámara de tercera persona sigue la posición y altura del jugador y conserva su órbita; se ha acercado de 4,8 m a 4,2 m para mejorar el encuadre móvil. El HUD informa del número de piezas enviadas y de los errores GL detectados.
- La mochila, correas, solapa y bolsillo se sitúan en la cara posterior del torso, visible desde la cámara de tercera persona. Tras enviar las 60 piezas, el renderizador consulta `glGetError()`: `GL OK` significa que no se notificó un error GL en ese fotograma, pero no demuestra por sí solo que los píxeles sean visibles. La matriz normal aplica las mismas rotaciones y la escala inversa para que la iluminación no se deforme en las piezas elípticas.

## Comprobaciones automatizadas

GitHub Actions ejecuta `:app:testDebugUnitTest`, `:app:assembleDebug` y `:app:assembleRelease`. Las pruebas comprueban presencia de piezas, dimensiones positivas, materiales válidos, colocación frontal de los ojos, rotación de la franja, pivotes de extremidades, transformación al desplazarse y seguimiento de cámara.

Las pruebas unitarias validan geometría declarativa, materiales, accesorios traseros, transformaciones y seguimiento de cámara; no pueden certificar que una GPU concreta dibuje correctamente el personaje.

## Verificación automatizada de la versión 0.27.2

El workflow de esta rama ejecuta las pruebas unitarias, compila APK debug y release-test, verifica ambas firmas y ejecuta una prueba de escena en un emulador Android API 35. La captura se analiza por grupos de color de piel, chaqueta, botas y extremidades, además de comprobar la separación vertical entre cabeza y pies. El emulador usa Google SwiftShader; este resultado sirve para detectar regresiones reproducibles, pero no representa la GPU física del Poco X6 Pro.

El personaje principal tiene 60 piezas procedurales y una base humana opaca de 11 piezas para compatibilidad. Las piezas usan materiales de color e iluminación por normales; todavía no hay mapas UV ni texturas fotográficas. La animación de caminar y el balanceo de reposo se calculan por separado de la cámara, que sigue la posición del jugador.

## Validación física pendiente

En un móvil Android real, comprobar:

1. Instalar la APK 0.27.2 y comprobar que el HUD indique `3D OK · 60 piezas · GL OK` después del primer fotograma. Este estado confirma el envío de las piezas sin error GL notificado, no su visibilidad real.
2. Que el personaje se vea completo, no como un rectángulo o una pantalla azul.
3. Que se distingan cabeza, torso, brazos, piernas, gorro, botas, mochila y bandolera desde el inicio.
4. Que la cámara permanezca detrás del personaje y lo mantenga en pantalla al caminar en las cuatro direcciones.
5. Que la malla no parpadee, se estire ni desaparezca al girar, saltar o acercarse a objetos.
6. Que caminar y reposar tengan movimiento fluido, sin saltos de fase.
7. Que la sombra no tape los pies y que el sombreado se vea bien en distintas GPU y resoluciones.
8. Que pausar y reanudar no deje la escena en negro ni pierda el estado guardado.

Si aparece otra vez el rectángulo azul, anotar el estado completo del HUD: `3D NO DISPONIBLE` apunta a inicialización/shader; `error GL 0x…` apunta a un error OpenGL; `60 piezas · GL OK` indica que las llamadas se enviaron sin error notificado y deja como sospechosos el encuadre, la oclusión, el APK instalado o la salida visual de la GPU. Este último caso requiere captura del móvil.

## Instalación y firma de prueba

El APK release de CI se firma con la clave debug temporal de GitHub Actions para permitir la instalación directa. No es una firma apta para Google Play. Como cada ejecución puede generar una clave debug distinta, Android puede exigir desinstalar la versión anterior antes de instalar una APK nueva; desinstalar borra los datos locales de la partida. Para actualizaciones sin pérdida de progreso hará falta configurar una clave de pruebas persistente como secreto de GitHub.
