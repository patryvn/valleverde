# Valle de Cernego — Diseño del juego 3D y hoja de ruta

## 1. Visión del juego
Valle de Cernego será un simulador rural de exploración, supervivencia y gestión en mundo abierto 3D para Android. El jugador controla a un personaje en tercera persona y recorre libremente un valle ficticio de inspiración gallega. El mundo debe sentirse vivo y coherente: el jugador puede caminar por él, trabajar la tierra, criar animales, construir, comerciar y transformar el paisaje.

El objetivo es empezar con pocos recursos y una explotación pequeña, aprender a sobrevivir y convertirla progresivamente en una granja y una comunidad rural prósperas.

## 2. Decisiones confirmadas
- Nombre del juego: Valle de Cernego.
- Perspectiva: tercera persona, personaje visible y control directo.
- Mapa: valle ficticio, no una reproducción exacta de Cernego.
- Estética: rural gallega, con montañas, laderas, bosques de castaños y robles, prados, viñedos, arroyos, caminos de tierra y aldeas de piedra.
- Plataforma objetivo: Android.
- Enfoque: exploración libre, agricultura avanzada, ganadería, aldeanos, construcción, supervivencia, vehículos, comercio y cuidado del monte.
- Economía amable: monedas y premios que se consiguen jugando, sin compras obligatorias ni esperas artificiales.
- No incluir multijugador en el plan inicial.

## 3. Pilares de diseño
1. Libertad de movimiento: recorrer el mundo a pie y, más adelante, en vehículos.
2. Progreso visible: las parcelas, edificios y zonas del valle cambian por las acciones del jugador.
3. Sistemas conectados: la agricultura alimenta al ganado; el ganado produce recursos; los recursos permiten fabricar y construir; el comercio financia la expansión.
4. Comunidad: los aldeanos tienen trabajos, habilidades, niveles y necesidades.
5. Mundo vivo: estaciones, clima, crecimiento de cultivos y rutinas sencillas de animales y habitantes.
6. Responsabilidad ambiental: cuidar bosques, agua y caminos y prevenir incendios.

## 4. Bucle principal de juego
1. Explorar una zona y descubrir recursos, parcelas, cuevas o encargos.
2. Recolectar madera, piedra, fibras, minerales, agua y alimentos.
3. Fabricar herramientas y preparar terrenos.
4. Sembrar, cuidar y cosechar cultivos según temporada y clima.
5. Alimentar, cuidar y reproducir ganado; atender nacimientos y crecimiento de terneros.
6. Asignar trabajos a aldeanos y mejorar sus habilidades.
7. Construir y ampliar viviendas, establos, talleres, almacenes y caminos.
8. Transportar productos y comerciar con pueblos vecinos.
9. Completar encargos, retos y logros para obtener monedas y premios.
10. Reinvertir ganancias y recursos para desbloquear nuevas áreas y mejoras.
10. Mantener el monte y las infraestructuras para reducir riesgos ambientales.

## 5. Mapa y regiones
El mapa será ficticio, compacto al principio y ampliable mediante actualizaciones:
- Aldea de Valdemonte: plaza, casas, mercado, taller y caminos.
- Prado del Río: pastos, arroyo, puente, huertas y campos.
- Laderas del Castañar: bosque de castaños y robles, senderos y recursos forestales.
- Alto de las Viñas: viñedos en bancales y caminos en pendiente.
- Brañas del Norte: praderas de pastoreo y zonas de expansión ganadera.
- Cantera Vieja: piedra y minerales para construcción y fabricación.
- Sierra Hueca: montañas, cuevas y rutas de exploración, inicialmente bloqueadas hasta completar el sistema de cuevas.
- Monte do Vento: zona forestal donde practicar limpieza, gestión de combustible vegetal y prevención de incendios.

Las zonas se conectarán con caminos reconocibles y puntos de referencia. No construir un mapa enorme vacío: cada zona debe ofrecer recursos, actividades o motivos para visitarla.

## 6. Sistemas de juego

### 6.1 Movimiento y exploración
- Caminar, correr, saltar en lugares adecuados e interactuar con objetos.
- Cámara de seguimiento en tercera persona, control táctil y colisiones.
- Explorar bosques, montañas, riberas, aldeas y cuevas.
- Descubrir recursos, puntos de interés y rutas alternativas.

### 6.2 Agricultura avanzada y maquinaria
- Preparar la tierra, arar, sembrar, regar, abonar, controlar malas hierbas y cosechar.
- Cultivos iniciales: patatas, maíz, trigo, verduras y viña.
- Calendario de siembra y cosecha ligado a las estaciones.
- Calidad y rendimiento afectados por suelo, agua, clima y cuidados.
- Herramientas manuales primero; después tractor, arado, remolque y maquinaria de cosecha.
- Almacenamiento de cosechas y conservación de semillas y forraje.

### 6.3 Ganadería y reproducción
- Primera especie: vacas y terneros; ampliación posterior a ovejas, cabras y gallinas.
- Necesidades: alimento, agua, refugio, espacio y cuidados.
- Reproducción con gestación, nacimiento, crecimiento y madurez de los animales.
- Diferenciar edades y necesidades de terneros y adultos.
- Producción y venta de productos ganaderos.
- Cercados, pastos rotativos y establos mejorables.
- Evitar reproducción instantánea: debe tener tiempos de juego comprensibles y controlables.

### 6.4 Aldeanos, trabajos y habilidades
- Reclutar habitantes mediante encargos y progreso del pueblo.
- Asignar tareas: agricultura, ganadería, construcción, transporte, recolección y comercio.
- Habilidades y niveles que mejoran con la práctica.
- Necesidades sencillas: descanso, alimento y vivienda.
- El jugador puede trabajar personalmente o delegar tareas.
- La automatización debe ser gradual: un aldeano no produce recursos sin límites ni materiales.

### 6.5 Construcción del pueblo
- Colocar y mejorar casa, viviendas, establos, graneros, almacenes, talleres, huertos, cercas, caminos y puentes.
- Requerir parcelas adecuadas, materiales y mano de obra.
- Mostrar coste y materiales antes de construir.
- El pueblo evoluciona visualmente con nuevas viviendas, servicios y habitantes.
- Permitir reorganizar instalaciones sin perder arbitrariamente recursos.

### 6.6 Supervivencia, recursos y fabricación
- Recolectar madera, piedra, fibras, minerales, agua y alimentos.
- Fabricar herramientas, cercas, piezas, muebles y materiales de construcción.
- Durabilidad y reparación de herramientas de forma sencilla.
- Gestionar reservas de comida, semillas, forraje y materiales.
- La supervivencia debe crear decisiones interesantes, no castigos constantes.

### 6.7 Estaciones y clima
- Primavera, verano, otoño e invierno.
- Lluvia, niebla, viento, calor, heladas y nieve según zona y estación.
- El clima afecta al crecimiento de cultivos, el estado de caminos y las necesidades de los animales.
- Ciclo día/noche.
- Empezar con efectos funcionales sencillos; aumentar variedad visual después.

### 6.8 Vehículos conducibles
- Introducir primero un tractor y un remolque.
- Controles táctiles de conducir, frenar y marcha atrás.
- Vehículos con combustible o mantenimiento solo si aporta decisiones útiles.
- Después añadir todoterreno y camión.
- Los caminos y pendientes deben ser transitables para cada vehículo; no todos los senderos admitirán maquinaria.

### 6.9 Comercio y mercado entre pueblos
- Mercado local para comprar semillas, herramientas y vender excedentes.
- Comerciantes y pueblos vecinos con productos y demandas diferentes.
- Encargos de entrega y contratos sencillos.
- Precios variables de forma moderada, evitando una economía confusa.
- Transporte de mercancías con remolques y vehículos.

### 6.10 Cuevas, bosques y montañas
- Senderos, miradores, arroyos, zonas de recolección y cuevas.
- Recursos especiales y pequeños descubrimientos.
- Riesgos ambientales moderados y rutas que requieran herramientas o preparación.
- Las cuevas se incorporarán después de que movimiento, iluminación y guardado sean estables.

### 6.11 Prevención de incendios y cuidado del monte
- Limpiar vegetación acumulada en zonas señaladas y mantener franjas de protección.
- Mantener caminos forestales y puntos de agua.
- Misiones de prevención, vigilancia y respuesta a pequeños conatos simulados.
- El fuego, si se incorpora, debe ser un sistema controlado y optimizado, no una destrucción aleatoria frustrante.
- Recompensar la prevención y el cuidado del entorno.



## 6.12 Monedas, premios y recompensas gratuitas
El juego debe ser generoso y permitir avanzar jugando. No habrá compras obligatorias, energía que bloquee la partida ni esperas artificiales para cobrar recompensas.

**Moneda principal: monedas de oro**
- Se ganan vendiendo cosechas, productos ganaderos, materiales y objetos fabricados.
- Se obtienen por encargos del mercado, entregas y trabajos para aldeanos.
- Sirven para comprar semillas, animales, herramientas, mejoras, vehículos y edificios.
- Los precios y recompensas deben estar equilibrados para que las tareas normales siempre aporten progreso.

**Premios y recompensas adicionales**
- Regalo diario gratuito con monedas, semillas, alimento o materiales; no se pierde el progreso por saltarse un día.
- Tareas diarias cortas y variadas, por ejemplo cosechar, alimentar animales o recoger madera.
- Retos semanales con premios mayores y objetivos que se puedan completar jugando con calma.
- Logros permanentes por hitos: primera cosecha, primer ternero, primera casa construida, primera cueva explorada o monte limpiado.
- Cofres de exploración escondidos en bosques, cuevas y miradores, con recompensas definidas y sin apuestas.
- Ferias y festivales de temporada con concursos agrícolas, ganaderos y de construcción.
- Recompensas por completar encargos, ayudar a vecinos y mantener caminos, fuentes y cortafuegos.
- Bonificaciones por rachas moderadas, sin castigar ni reiniciar premios importantes si el jugador descansa.

**Reglas de equilibrio y transparencia**
- Mostrar siempre qué tarea da qué recompensa antes de aceptarla.
- Evitar anuncios obligatorios, cajas de botín aleatorias de pago y monedas premium necesarias para progresar.
- No poner temporizadores que obliguen a pagar para acelerar edificios, cultivos o animales.
- Mantener una economía que premie jugar de distintas maneras, no repetir una única tarea durante horas.
- Los premios especiales deben poder conseguirse mediante eventos o logros dentro del juego.

## 7. Dirección artística y sonido
- 3D estilizado-realista: materiales naturales, proporciones creíbles, iluminación cuidada y rendimiento adecuado para Android.
- Terreno con pendientes, vegetación variada, caminos irregulares y agua.
- Arquitectura de piedra y madera, tejados rurales y construcciones funcionales.
- Animaciones claras para personaje, herramientas, maquinaria y animales.
- Sonido ambiental por bioma y estación: viento, agua, pájaros, ganado, maquinaria y actividad del pueblo.
- Priorizar lectura visual, buena iluminación y fluidez sobre realismo gráfico extremo.

## 8. Hoja de ruta por fases

### Fase 0 — Auditoría técnica y elección del motor
- Inspeccionar el ZIP y los archivos reales del proyecto actual.
- Confirmar motor, estructura, licencias de recursos y posibilidad real de exportar a Android.
- Decidir si se conserva la base o se crea un proyecto 3D limpio.
- Definir presupuesto de rendimiento para móviles.
**Salida:** proyecto reproducible y decisión técnica documentada. No empezar a añadir sistemas antes de conocer la base.

### Fase 1 — Prototipo de movimiento y valle
- Crear un terreno 3D pequeño con colinas, camino, prado, árboles, casa y arroyo.
- Añadir personaje en tercera persona, joystick táctil y control de cámara.
- Implementar colisiones, pendientes y límites del mapa.
- Probar instalación y rendimiento en Android.
**Salida:** APK donde el jugador puede recorrer el valle con controles cómodos.

### Fase 2 — Interacción y recolección
- Añadir objetos recogibles: madera, piedra, fibras y agua.
- Inventario, herramientas básicas y estaciones de trabajo.
- Interacciones contextuales y guardado local.
**Salida:** explorar, recoger, almacenar y fabricar objetos básicos.

### Fase 3 — Agricultura jugable
- Preparar una parcela, arar, sembrar, regar y cosechar un cultivo completo.
- Añadir inventario de semillas, crecimiento por etapas y almacén.
- Incorporar varios cultivos después de validar el primero.
**Salida:** ciclo agrícola completo y guardado correcto.

### Fase 4 — Primera ganadería
- Añadir una vaca y un ternero con modelos y animaciones.
- Implementar alimentación, agua, refugio y cercados.
- Probar crecimiento y reproducción con tiempos de juego ajustables.
**Salida:** sistema ganadero funcional antes de ampliar especies.

### Fase 5 — Construcción y pueblo
- Construir casa, almacén, establo, cercas y taller.
- Gestionar materiales, parcelas y costes.
- Añadir primeras viviendas y evolución visual del pueblo.
**Salida:** el jugador puede transformar su explotación y ampliar el asentamiento.

### Fase 6 — Aldeanos y especialización
- Introducir habitantes con trabajos, habilidades y niveles.
- Asignar tareas y comprobar producción y consumo.
- Añadir necesidades básicas y progresión.
**Salida:** delegación de tareas útil y equilibrada.

### Fase 7 — Estaciones y supervivencia
- Incorporar calendario, día/noche, lluvia, niebla, heladas y nieve.
- Relacionar clima con cultivos, caminos y necesidades ganaderas.
- Añadir reservas y fabricación ampliada.
**Salida:** mundo con ciclos y decisiones estacionales.

### Fase 8 — Vehículos y transporte
- Implementar tractor, remolque y transporte de cargas.
- Ajustar física, controles y caminos.
- Añadir maquinaria agrícola gradualmente.
**Salida:** desplazamiento y trabajo mecanizado fiables.

### Fase 9 — Comercio y expansión del mapa
- Mercado local, encargos, precios y rutas entre pueblos.
- Abrir nuevas regiones con recursos y actividades diferenciadas.
- Añadir cuevas y exploración avanzada.
**Salida:** economía conectada con exploración y producción.

### Fase 10 — Gestión forestal, prevención y pulido
- Misiones de limpieza y mantenimiento forestal, cortafuegos y puntos de agua.
- Optimización de gráficos, memoria, batería y controles.
- Pruebas de guardado, errores, equilibrio y compatibilidad Android.
**Salida:** versión amplia y estable, lista para una beta de pruebas.

### Integración de recompensas
- Añadir la primera versión de monedas al comercio y los encargos cuando exista inventario y economía básica.
- Incorporar logros y premios por hitos al completar los primeros sistemas.
- Añadir regalo diario y retos cuando el guardado local sea fiable.
- Incorporar cofres de exploración, ferias y premios de temporada al ampliar el mapa.
- Probar el equilibrio de monedas y precios antes de publicar cada beta.

## 9. Primera demo: alcance obligatorio
La primera demo no intentará incluir todas las fases. Debe ofrecer:
1. Personaje controlable en tercera persona.
2. Valle pequeño con relieve, camino, prado, casa, árboles y arroyo.
3. Cámara táctil, colisiones y controles cómodos.
4. Recolección de madera y piedra.
5. Una parcela con ciclo completo de sembrar y cosechar.
6. Una vaca interactiva y un establo sencillo.
7. Guardado local.
8. Rendimiento estable en un Android de gama media.

## 10. Criterios de calidad
- Movimiento y cámara fiables, sin atravesar terreno ni edificios.
- Interfaz legible en pantalla móvil.
- Sistemas conectados y comprensibles, sin tareas repetitivas innecesarias.
- Guardado que conserve inventario, edificios, cultivos y animales.
- Rendimiento estable y tiempos de carga razonables.
- Cada fase debe probarse en un dispositivo Android antes de darla por terminada.
- No anunciar una función como terminada hasta que esté implementada y probada.

## 11. Fuera del alcance inicial
- Multijugador y visitas online.
- Mundo gigantesco desde el primer lanzamiento.
- Simulación económica compleja.
- Cientos de aldeanos con inteligencia avanzada.
- Destrucción forestal por fuego a gran escala.
Estas funciones podrán estudiarse cuando la base del juego sea estable y exista capacidad técnica suficiente.

## 12. Principio de desarrollo
Construir por capas. Primero un valle pequeño que se pueda recorrer y que funcione bien; después agricultura y ganadería; luego pueblo, aldeanos, estaciones, vehículos, comercio y gestión forestal. Una experiencia pequeña, coherente y pulida es mejor que prometer un mundo enorme que no funcione.
