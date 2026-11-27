# Prevención de incendios — Valle de Cernego 0.27.7

## Alcance implementado

- Tablón interactivo de prevención en la zona inicial.
- Cuatro tramos construibles de cortafuegos.
- Coste por tramo: 2 unidades de madera y 1 de piedra.
- Franjas de suelo expuesto y postes visibles para cada tramo construido.
- Índice de riesgo de juego que cambia con la estación y el tiempo meteorológico.
- Cada tramo completado reduce el índice en 14 puntos.
- Progreso persistente mediante el guardado local de la partida.
- Pruebas unitarias para costes, límite de construcción, riesgo, persistencia y presencia del tablón.

## Límites importantes

El índice es una simplificación de juego. No es una previsión real de incendios, no usa datos de combustible vegetal, pendiente, viento ni humedad del suelo, y todavía no simula ignición, propagación, brigadas ni extinción. Las franjas representadas son elementos visuales de la zona inicial; no modifican la vegetación del resto del mapa. No debe utilizarse como herramienta de prevención real.

## Validación manual en Android

- Acercarse al tablón del este y pulsar HACER.
- Construir un tramo y confirmar el descuento exacto de recursos.
- Comprobar que aparece la franja y que el contador cambia.
- Construir los cuatro tramos y confirmar que el sistema impide un quinto.
- Reiniciar la aplicación y comprobar la persistencia.
- Observar el índice al cambiar estación y meteorología.
- Revisar que el tablón, las franjas y el personaje permanecen visibles al mover la cámara.

GitHub Actions ejecuta las pruebas unitarias, compila Debug y Release y realiza una prueba de escena con emulador. La apariencia, el rendimiento y la persistencia en una GPU física Android requieren validación adicional en el móvil.
