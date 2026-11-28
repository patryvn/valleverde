# Validación del protagonista y la identidad de la preview — 0.29.8

## Motivo del cambio

La rama contenía un desajuste verificable: Gradle generaba la versión 0.29.7, pero el nombre del icono de Android seguía mostrando «Prueba visual 3D 0.29.6». Además, la aplicación nueva usa el identificador `es.valledecernego.game.preview2`, por lo que puede convivir con la preview anterior. Si ambas aparecen en el lanzador con nombres parecidos o anticuados, es fácil abrir la instalación equivocada y seguir viendo un renderizador antiguo.

Este desajuste no demuestra por sí solo la causa del rectángulo azul en el Poco X6 Pro. Sí elimina una fuente concreta de confusión durante la validación física.

## Cambios de 0.29.8

- `versionCode=59`, `versionName=0.29.8`.
- Nombre de aplicación: `Valle de Cernego PREVIEW2 v0.29.8`.
- HUD: muestra explícitamente `0.29.8 · PREVIEW2`.
- Prueba JUnit para impedir que el nombre de versión vuelva a quedar desactualizado.
- GitHub Actions comprueba con `aapt dump badging` el paquete de Release, el versionCode y el versionName. La prueba Python de los scripts comprueba que el nombre del icono del manifiesto coincide con esa versión.
- Se conserva el paquete `es.valledecernego.game.preview2`; no se cambia la identidad de instalación de esta línea preview.

## Evidencia disponible antes de 0.29.8

La ejecución [#389](https://github.com/patryvn/valleverde/actions/runs/46097923959), sobre el commit `069ba2b72ff5fab9b8a1cfd7389344cebc111895`, terminó correctamente. Pasaron las pruebas unitarias, las compilaciones Debug y Release, la verificación de firma y el smoke test en Android Emulator.

Las capturas de SwiftShader muestran al protagonista low-poly de 60 piezas en reposo y caminando, con cabeza, torso, brazos, piernas, botas y mochila. La telemetría del emulador registra `VISUAL OK`, 60 piezas y error GLES cero. Esto no equivale a una prueba en el controlador Mali-G615 MC6 del Poco X6 Pro.

## Validación pendiente en dispositivo físico

1. Instalar la APK 0.29.8 como aplicación independiente/actualización de `preview2`; no desinstalar la aplicación anterior.
2. En el lanzador, abrir exactamente **Valle de Cernego PREVIEW2 v0.29.8**.
3. Confirmar en la primera línea del HUD `VALLE DE CERNEGO 0.29.8 · PREVIEW2`.
4. Comprobar que el protagonista tiene cabeza, torso, dos brazos, dos piernas y botas, tanto quieto como caminando y girando.
5. Revisar el estado visual y la GPU que aparecen en el HUD. `VISUAL N/D` significa que la lectura de píxeles no se pudo realizar; no debe interpretarse como éxito.
6. Si persiste el rectángulo azul, capturar la pantalla completa donde se vean versión, GPU, modo de renderizado y estado visual. No desinstalar la versión anterior ni borrar sus datos.

## Alcance y limitaciones

- El protagonista sigue siendo low-poly y procedural; no es todavía una malla artística con rig esquelético y animaciones importadas.
- El smoke test CI usa Android Emulator con SwiftShader. No certifica compatibilidad, temperatura, consumo ni rendimiento en GPU Mali física.
- No se declara resuelto el rectángulo azul hasta obtener evidencia del dispositivo físico.
- No se han cambiado `main`, la visibilidad del repositorio ni los mecanismos de monetización. El juego sigue sin compras obligatorias ni anuncios forzosos.
