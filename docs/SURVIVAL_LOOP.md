# Supervivencia básica — Valle de Cernego 0.25.0

## Controles
- **COMER**: consume una unidad de cosecha para recuperar hambre.
- **HACER** junto al manantial de piedra del regato: beber y recuperar sed.
- **Joystick**: caminar; reduce energía poco a poco.
- **SALTAR**: consume energía. Si no queda suficiente, el salto no se inicia.

## Ritmo de juego
La energía se recupera al descansar. El hambre baja una unidad cada dos minutos de juego y la sed una unidad cada minuto y medio, con modificadores estacionales y meteorológicos. La lluvia reduce el consumo de agua y el invierno aumenta el consumo de alimento. Si hambre o sed llegan a cero, la salud empieza a disminuir; con necesidades cubiertas, la salud se recupera lentamente al descansar.

El reloj solo avanza mientras el renderizador está activo. Los valores y los acumuladores parciales se guardan en preferencias locales; los datos fuera de rango se descartan sin aplicar una carga parcial.

## Validación pendiente en Android físico
1. Instalar la APK 0.25.0 y confirmar la versión en el HUD.
2. Recoger una cosecha, pulsar COMER y comprobar que el inventario baja en una unidad y el hambre aumenta.
3. Caminar hasta el manantial al oeste del punto inicial; pulsar HACER y comprobar que sube la sed.
4. Caminar y descansar para comprobar consumo/recuperación de energía; probar SALTAR con energía suficiente y agotada.
5. Cerrar y reabrir la aplicación para verificar que necesidades e inventario se conservan.
6. Repetir en Wi-Fi y datos desactivados: el juego debe seguir siendo completamente offline.

No hay anuncios, compras obligatorias ni bloqueos de pago.
