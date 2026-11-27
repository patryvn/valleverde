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

## Suelo atlántico con variación multiescala en 0.27.6

La captura visual de 0.27.5 detectó solo cinco tonos verdes cuantizados en la zona inferior del terreno, por debajo del mínimo de diez exigido por el smoke test. Las pruebas unitarias y las compilaciones Debug/Release sí terminaron correctamente, pero la verificación de escena falló y no debe considerarse una aprobación visual.

En 0.27.6 el shader GLES mezcla tres escalas de ruido (parches amplios, variación media y detalle fino), una modulación de hierba y una pequeña variación de suelo seco. La paleta usa tonos de sombra, hierba media y zonas iluminadas para evitar una superficie verde plana. La fórmula CPU de referencia está cubierta por pruebas de rango, determinismo y variación espacial.

Sigue siendo un material procedural por color: no incorpora todavía mapas UV, texturas fotográficas, geometría individual de briznas ni sombras proyectadas reales. La iluminación por normales existente continúa aplicándose sobre el terreno. El aspecto final debe comprobarse en el emulador y en una GPU física Android.

## Comprobaciones automatizadas

GitHub Actions ejecuta `:app:testDebugUnitTest`, `:app:assembleDebug` y `:app:assembleRelease`. Las pruebas comprueban presencia de piezas, dimensiones positivas, materiales válidos, colocación frontal de los ojos, rotación de la franja, pivotes de extremidades, transformación al desplazarse y seguimiento de cámara.

Las pruebas unitarias validan geometría declarativa, materiales, accesorios traseros, transformaciones y seguimiento de cámara; no pueden certificar que una GPU concreta dibuje correctamente el personaje.

## Verificación automatizada de la versión 0.27.4

El workflow de esta rama ejecuta las pruebas unitarias, compila APK debug y release-test, verifica ambas firmas y ejecuta una prueba de escena en un emulador Android API 35. La captura inicial se analiza por grupos de color de piel, chaqueta, botas y extremidades, además de comprobar la separación vertical entre cabeza y pies. Tras el movimiento, el test exige telemetría de desplazamiento, 60 piezas, GL_NO_ERROR y error de seguimiento de cámara dentro de tolerancia. Intenta además una segunda captura de píxeles; solo la omite si detecta una ventana visible de ANR de SystemUI (no un ANR del juego ni otra superposición). El emulador usa Google SwiftShader y puede sufrir ese ANR externo bajo carga; el resultado no representa la GPU física del Poco X6 Pro.

El personaje principal tiene 60 piezas procedurales. La silueta de 11 piezas se conserva como ruta de emergencia cuando el shader principal no inicializa o una tanda detallada queda incompleta; no se dibuja debajo del modelo completo. Las piezas usan materiales de color e iluminación por normales; todavía no hay mapas UV ni texturas fotográficas. La animación de caminar y el balanceo de reposo se calculan por separado de la cámara, que sigue la posición del jugador.

## Validación física pendiente

En un móvil Android real, comprobar:

1. Que los árboles no desaparezcan al acercarse desde distintos ángulos ni al girar la cámara.

2. Instalar la APK 0.27.6 y comprobar que el HUD indique `3D OK · 60 piezas · GL OK` después del primer fotograma. Este estado confirma el envío de las piezas sin error GL notificado, no su visibilidad real.
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


## Prevención de incendios en 0.27.7

La primera versión del sistema incorpora un tablón de prevención en la zona inicial y cuatro tramos de cortafuegos construibles. Cada tramo cuesta 2 unidades de madera y 1 de piedra, se guarda en la partida y se representa con una franja de suelo expuesto y postes. El índice del juego varía según estación y meteorología y disminuye al completar tramos.

El índice es una abstracción de jugabilidad: no es una predicción meteorológica, no modela combustibles reales y todavía no simula ignición, propagación ni extinción de incendios. No debe utilizarse para tomar decisiones de prevención reales.

Validación física pendiente en Android:
1. Localizar el tablón de prevención al este de la zona inicial y abrirlo con HACER.
2. Comprobar que el coste se descuenta solo cuando se construye un tramo.
3. Confirmar que cada tramo aparece como una franja de tierra y que el contador del HUD cambia.
4. Cerrar y volver a abrir la aplicación para comprobar que los tramos construidos persisten.
5. Probar el índice en distintas estaciones y condiciones meteorológicas y comprobar que lluvia reduce el valor frente a verano despejado.
6. Confirmar que la nueva geometría no produce parpadeo, errores GL ni pérdida de rendimiento en el dispositivo.

La prueba de emulador valida compilación, lógica unitaria, escena y movimiento; no sustituye la comprobación de visibilidad, rendimiento y guardado en el Poco X6 Pro.


## Corrección del aspecto del personaje en 0.27.8

La inspección de la captura de CI de 0.27.7 mostró que el personaje sí se dibujaba como humanoide: la captura inicial tenía grupos de píxeles de piel, chaqueta, botas y extremidades; la prueba de movimiento confirmó 60 piezas, 0,28 m de desplazamiento y 0,00 m de error de seguimiento de cámara. El problema visual restante era la política de respaldo: el renderizador dibujaba siempre una silueta opaca de 11 cajas antes del modelo completo. Esa capa quedaba debajo de la cabeza y del torso y hacía que la figura pareciese un bloque azul, aunque las 60 piezas detalladas se enviaran correctamente.

En 0.27.8:

- El modelo detallado de 60 piezas se dibuja sin la silueta de cajas cuando el shader principal está disponible.
- La silueta de compatibilidad solo se usa si el renderizador principal no puede inicializarse; si una tanda detallada queda incompleta, el respaldo se dibuja después, no debajo.
- El torso de la chaqueta pasa de caja a elipsoide 3D con las mismas dimensiones exteriores (0,52 × 0,64 × 0,32 m).
- Se conserva el sombreado por normales, el material de tela, la sombra elíptica, los canales de animación de brazos/piernas y el seguimiento de cámara.
- El HUD diferencia el camino normal (modelo detallado) del respaldo humanoide.

Las pruebas unitarias cubren la política de respaldo, la forma y dimensiones del torso, las dimensiones del torso en el fallback y los estados del HUD. GitHub Actions debe volver a comprobar la captura inicial y durante el movimiento; el criterio visual sigue siendo más importante que el número de llamadas GL.

### Validación física pendiente en Android

En el Poco X6 Pro o en otro Android físico, comprobar con la APK 0.27.8:

1. Que la cabeza y el torso se vean redondeados y no como un rectángulo azul.
2. Que se distingan cuello, brazos, manos, dos piernas y botas desde atrás y al girar la cámara.
3. Que caminar, detenerse y girar mantengan las extremidades articuladas sin deformación ni parpadeo.
4. Que la cámara siga al personaje en las cuatro direcciones y después de saltar.
5. Que la sombra permanezca debajo de los pies sin oscurecerlos.
6. Que pausar/reanudar y volver a abrir el juego no pierda la escena ni la partida.

El test de CI usa un emulador API 35 con Google SwiftShader a 720 × 405; demuestra el comportamiento de esa configuración, no el de la GPU Adreno del teléfono. No hay todavía validación física ni firma persistente de distribución. La APK release-test continúa firmada con la clave debug temporal de GitHub Actions y no es una release apta para Google Play.
