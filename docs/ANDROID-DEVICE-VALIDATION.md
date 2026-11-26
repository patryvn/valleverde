# Validación del personaje y cámara en Android

## Cambios incluidos en 0.27.4

- El personaje se construye en el renderizador OpenGL ES 2.0 con piezas 3D separadas y reutiliza la misma especificación en las pruebas.
- La silueta incluye cabeza, cuello, torso, chaqueta abierta, brazos, manos, pantalón, piernas, botas con suela y cordones, gorro tejido, pelo, ojos verdes, pupilas, boca y bandolera.
- El diseño usa materiales de color con iluminación por normales; no incorpora todavía texturas fotográficas ni mapas UV. La franja naranja de la camiseta y la correa de la bandolera tienen rotación 3D explícita.
- Brazos y piernas mantienen canales de animación opuestos; el torso acompaña el paso con un balanceo leve y conserva una animación de reposo.
- La cámara de tercera persona sigue la posición y altura del jugador y conserva su órbita; se ha acercado de 4,8 m a 4,2 m para mejorar el encuadre móvil. El HUD informa del número de piezas enviadas y de los errores GL detectados.
- La mochila, correas, solapa y bolsillo se sitúan en la cara posterior del torso, visible desde la cámara de tercera persona. Tras enviar las 60 piezas, el renderizador consulta `glGetError()`: `GL OK` significa que no se notificó un error GL en ese fotograma, pero no demuestra por sí solo que los píxeles sean visibles. La matriz normal aplica las mismas rotaciones y la escala inversa para que la iluminación no se deforme en las piezas elípticas.

## Visibilidad de árboles y objetos cercanos en 0.27.3

El filtro de visibilidad conserva ahora un margen radial alrededor del jugador: los árboles y otros objetos cercanos no se descartan solo porque queden detrás o al lado de la orientación del personaje mientras la cámara de tercera persona gira. El límite de distancia máxima sigue aplicándose. Las pruebas unitarias cubren árboles cercanos detrás del jugador, a un lado y el respeto del límite lejano. Es una corrección de la regla de culling; su resultado visual final todavía debe comprobarse en el móvil.

## Variedad de árboles en 0.27.4

La zona inicial mezcla frondosas atlánticas estilizadas y coníferas perennes. La especie, escala y variación de follaje se calculan de forma determinista a partir de las coordenadas del árbol, por lo que no cambian entre fotogramas ni alteran la partida guardada. Las coníferas usan cuatro niveles de copa progresivamente más estrechos y conservan tonos verdes en invierno; las frondosas mantienen los cambios cromáticos estacionales. A más de 20 m se usa una silueta simplificada para limitar el coste de dibujo. Son modelos low-poly procedurales, no escaneos ni texturas fotográficas.

Las pruebas `TreeVisualSpecTest` comprueban que el bosque inicial contiene ambas variantes, que el aspecto es estable y que la copa de las coníferas se estrecha y asciende correctamente. La prueba de escena de Android sigue comprobando que el renderizador no genera errores y que el personaje y la cámara continúan funcionando; la apariencia de los árboles debe validarse también en el móvil físico.

## Comprobaciones automatizadas

GitHub Actions ejecuta `:app:testDebugUnitTest`, `:app:assembleDebug` y `:app:assembleRelease`. Las pruebas comprueban presencia de piezas, dimensiones positivas, materiales válidos, colocación frontal de los ojos, rotación de la franja, pivotes de extremidades, transformación al desplazarse y seguimiento de cámara.

Las pruebas unitarias validan geometría declarativa, materiales, accesorios traseros, transformaciones y seguimiento de cámara; no pueden certificar que una GPU concreta dibuje correctamente el personaje.

## Verificación automatizada de la versión 0.27.4

El workflow de esta rama ejecuta las pruebas unitarias, compila APK debug y release-test, verifica ambas firmas y ejecuta una prueba de escena en un emulador Android API 35. La captura inicial se analiza por grupos de color de piel, chaqueta, botas y extremidades, además de comprobar la separación vertical entre cabeza y pies. Tras el movimiento, el test exige telemetría de desplazamiento, 60 piezas, GL_NO_ERROR y error de seguimiento de cámara dentro de tolerancia. Intenta además una segunda captura de píxeles; solo la omite si detecta una ventana visible de ANR de SystemUI (no un ANR del juego ni otra superposición). El emulador usa Google SwiftShader y puede sufrir ese ANR externo bajo carga; el resultado no representa la GPU física del Poco X6 Pro.

El personaje principal tiene 60 piezas procedurales y una base humana opaca de 11 piezas para compatibilidad. Las piezas usan materiales de color e iluminación por normales; todavía no hay mapas UV ni texturas fotográficas. La animación de caminar y el balanceo de reposo se calculan por separado de la cámara, que sigue la posición del jugador.

## Validación física pendiente

En un móvil Android real, comprobar:

1. Que los árboles no desaparezcan al acercarse desde distintos ángulos ni al girar la cámara.

2. Instalar la APK 0.27.4 y comprobar que el HUD indique `3D OK · 60 piezas · GL OK` después del primer fotograma. Este estado confirma el envío de las piezas sin error GL notificado, no su visibilidad real.
3. Que el personaje se vea completo, no como un rectángulo o una pantalla azul.
4. Que se distingan cabeza, torso, brazos, piernas, gorro, botas, mochila y bandolera desde el inicio.
5. Que la cámara permanezca detrás del personaje y lo mantenga en pantalla al caminar en las cuatro direcciones.
6. Que la malla no parpadee, se estire ni desaparezca al girar, saltar o acercarse a objetos.
7. Que caminar y reposar tengan movimiento fluido, sin saltos de fase.
8. Que la sombra no tape los pies y que el sombreado se vea bien en distintas GPU y resoluciones.
9. Que pausar y reanudar no deje la escena en negro ni pierda el estado guardado.

Si aparece otra vez el rectángulo azul, anotar el estado completo del HUD: `3D NO DISPONIBLE` apunta a inicialización/shader; `error GL 0x…` apunta a un error OpenGL; `60 piezas · GL OK` indica que las llamadas se enviaron sin error notificado y deja como sospechosos el encuadre, la oclusión, el APK instalado o la salida visual de la GPU. Este último caso requiere captura del móvil.

## Instalación y firma de prueba

El APK release de CI se firma con la clave debug temporal de GitHub Actions para permitir la instalación directa. No es una firma apta para Google Play. Como cada ejecución puede generar una clave debug distinta, Android puede exigir desinstalar la versión anterior antes de instalar una APK nueva; desinstalar borra los datos locales de la partida. Para actualizaciones sin pérdida de progreso hará falta configurar una clave de pruebas persistente como secreto de GitHub.
