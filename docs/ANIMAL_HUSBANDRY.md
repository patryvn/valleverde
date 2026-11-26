# Ganadería de Cernego

La primera parcela de ganado tiene dos adultos independientes: una vaca y un toro. Sus coordenadas se definen una sola vez en `WorldLayout` y las reutilizan el renderizador y `AnimalSystem`; así, el punto de interacción no puede quedar separado del animal que se ve en pantalla.

## Cuidado y reproducción

- Recoge forraje de los matorrales y acércate a la vaca o al toro para alimentarlos.
- La cuadra puede almacenar heno y alimentar automáticamente al adulto más hambriento.
- La reproducción requiere salud suficiente y hambre controlada en ambos adultos.
- La gestación y el crecimiento avanzan con el tiempo de juego activo.
- El ternero no aparece en el mundo antes de nacer. Tras el nacimiento se dibuja junto a la vaca y aumenta de tamaño gradualmente hasta alcanzar la escala adulta.
- El cambio de estación y la lluvia/frío modifican la demanda de alimento.

## Validación automatizada

`AnimalSystemTest` comprueba que las posiciones reales de la vaca y el toro admiten alimentación, que la reproducción produce un ternero y que su progreso visual pasa de 0 a 1. `WorldLayoutTest` verifica que el ternero no se coloca como recurso adulto al iniciar una partida.

La prueba de emulador sigue siendo necesaria para inspeccionar el modelo de los animales y su posición. La simulación no se considera validada en todos los controladores gráficos Android hasta probarla también en un teléfono físico.
