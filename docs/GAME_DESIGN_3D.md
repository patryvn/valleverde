# Valle Verde — Diseño del juego 3D

## Visión
Valle Verde será un simulador rural de exploración y gestión en mundo abierto 3D. El jugador controla a un personaje en tercera persona y recorre libremente un valle ficticio de inspiración gallega. La prioridad es que el mundo se sienta como un lugar habitable y coherente, no como una pantalla de gestión con edificios colocados.

## Decisiones confirmadas
- Cámara en tercera persona, con personaje visible y control directo.
- Mapa ficticio, sin copiar la geografía exacta de Cernego.
- Identidad visual rural gallega: montañas verdes, laderas, bosques de castaños y robles, prados, viñedos, arroyos, caminos de tierra y pequeñas aldeas de piedra.
- Progresión basada en explorar, recolectar, cultivar, criar ganado, construir y mejorar la explotación.

## Fantasía principal
Empezar con una pequeña casa y una parcela humilde. Recorrer el valle a pie, conocer sus zonas, recuperar terrenos abandonados y convertir una explotación pequeña en una granja próspera. Las decisiones del jugador cambian el paisaje: aparecen cultivos, cercas, establos, caminos mejorados y nuevas instalaciones.

## Bucle principal
1. Explorar el valle y localizar recursos, parcelas y lugares de interés.
2. Recoger madera, piedra, fibras, agua y alimentos.
3. Preparar terrenos, sembrar, regar, cuidar y cosechar.
4. Alimentar y atender animales; criar terneros y ampliar el rebaño.
5. Construir o mejorar casa, cobertizos, establo, almacén, cercas y huerto.
6. Vender o intercambiar productos para financiar mejoras.
7. Desbloquear nuevas zonas, herramientas, cultivos y edificios.

## Regiones del mapa inicial
- Aldea de Valdemonte: núcleo con casas, plaza, pequeño comercio y caminos.
- Prado del Río: pastos húmedos, arroyo, puente y zona de cultivo.
- Laderas del Castañar: bosque recolectable con senderos y madera.
- Alto de las Viñas: bancales y viñedos en pendiente.
- Brañas del Norte: praderas abiertas para pastoreo y futuras ampliaciones.
- Cantera Vieja: piedra y materiales de construcción.

El primer mapa debe ser compacto y explorable, con límites naturales y caminos que conecten las zonas. No intentar crear un mundo enorme en la primera versión.

## Controles y cámara
- Joystick virtual izquierdo para caminar y correr.
- Arrastre en el lado derecho para orientar la cámara.
- Botón contextual para interactuar, recoger, abrir puertas o usar herramientas.
- Botones de acción para saltar y usar herramienta cuando corresponda.
- Cámara tercera persona con seguimiento suave, distancia ajustable y colisiones para que no atraviese paredes o terreno.
- Controles táctiles grandes y cómodos en móvil.

## Sistemas de juego
### Movimiento e interacción
Caminar, correr, girar, entrar en edificios seleccionados, abrir puertas, recoger objetos y activar estaciones de trabajo. Las interacciones deben tener animaciones y respuesta visual.

### Agricultura
Preparar suelo, sembrar, regar, esperar el crecimiento por etapas y cosechar. Empezar con patatas, maíz, trigo, verduras y viña. Los cultivos necesitan temporadas y agua; evitar sistemas complejos en la primera demo.

### Ganadería
Comenzar con vacas y terneros. Necesitan alimento, agua, refugio y cuidados. El jugador puede conducir el ganado entre cercados y ampliar el establo. Añadir gallinas en una actualización posterior.

### Construcción
Construcción modular y situada en parcelas válidas: casa, almacén, cobertizo, establo, cercas, huerto y depósito de agua. Mostrar coste y materiales antes de confirmar.

### Economía y progresión
Vender productos, comprar semillas y herramientas, mejorar instalaciones y desbloquear nuevas parcelas. Sin pagos obligatorios ni ventajas de pago en la primera versión.

### Mundo vivo
Ciclo día/noche sencillo, clima básico (sol y lluvia), cambios estacionales graduales, sonidos ambientales, animales con rutinas simples y aldeanos con horarios básicos.

## Dirección artística
- 3D estilizado-realista: materiales naturales, proporciones creíbles y buena iluminación, priorizando rendimiento en Android.
- Terreno con pendientes, caminos irregulares, vegetación variada y agua visible.
- Arquitectura rural de piedra, madera y tejados tradicionales.
- Animales con modelos y animaciones reconocibles.
- Evitar aspecto de bloques, iconos planos como mundo principal o edificios flotantes.

## Primera versión jugable (vertical slice)
La primera entrega no debe prometer todas las funciones. Debe demostrar que el concepto funciona:
1. Un personaje controlable en tercera persona.
2. Un pequeño valle 3D con una casa, camino, prado, árboles, arroyo y colinas.
3. Cámara táctil funcional y colisiones.
4. Una parcela que se pueda preparar, sembrar y cosechar.
5. Un cobertizo o establo sencillo y una vaca interactiva.
6. Recolección de madera y piedra.
7. Guardado local básico de progreso.

## Orden recomendado de desarrollo
1. Verificar motor y estructura técnica existentes antes de reemplazar código.
2. Prototipo de movimiento, cámara y colisiones en un terreno pequeño.
3. Construcción del escenario y navegación entre sus zonas.
4. Interacciones y recolección.
5. Agricultura con un cultivo completo.
6. Primera vaca y necesidades básicas.
7. Construcción y economía.
8. Optimización Android, guardado y pruebas en dispositivo.

## Criterios de aceptación de la demo
- El jugador puede caminar y correr por el escenario sin quedarse atascado.
- La cámara se controla con el dedo y no atraviesa el terreno ni edificios.
- El escenario tiene relieve y caminos transitables.
- Se puede completar el ciclo sembrar → crecer → cosechar.
- La vaca responde a una interacción y permanece dentro de su zona.
- La demo mantiene un rendimiento estable en un Android de gama media.
- La APK se instala y abre sin errores.

## Fuera del alcance inicial
Multijugador, mundo gigantesco, tráfico complejo, cientos de aldeanos, simulación económica avanzada y realismo gráfico extremo. Se posponen para evitar una demo pesada e inacabada.

## Principio de diseño
Primero hacer que moverse por el valle sea divertido y convincente. Después añadir sistemas de granja. Un mundo pequeño, bonito y funcional es mejor que un mapa enorme vacío.
