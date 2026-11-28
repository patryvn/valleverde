# Validación del protagonista y paquete de prueba independiente — 0.29.9

## Hallazgo de instalación

Se compararon los certificados de las APK Release-test reales:
- 0.29.7, paquete `es.valledecernego.game.preview2`: SHA-256 del certificado `A9:3A:B5:0B:42:73:72:2D:15:47:39:AE:1F:AF:E7:48:EE:63:82:8A:AE:FE:E1:01:6F:63:6C:67:E7:6D:91:F2`.
- 0.29.8, paquete `es.valledecernego.game.preview2`: SHA-256 del certificado `0A:0B:5B:1F:79:EE:1B:8B:09:DE:B0:A1:A2:CE:4A:86:E4:19:13:85:21:95:D2:55:85:7C:B6:C3:DD:D8:2A:C5`.

Los certificados no coinciden. Android no permitirá instalar 0.29.8 encima de 0.29.7 con el mismo identificador, aunque el código y versionCode sean posteriores. Por eso no se debe entregar 0.29.8 como actualización directa de preview2 ni pedir que se desinstale la app anterior, porque podría perder sus datos.

## Solución 0.29.9

- Nuevo paquete: `es.valledecernego.game.preview3`.
- `versionCode=60`, `versionName=0.29.9`.
- Nombre visible: **Valle de Cernego PREVIEW3 v0.29.9**.
- El HUD identifica `0.29.9 · PREVIEW3`.
- El smoke test instala y lanza explícitamente `preview3`, no una instalación antigua.
- Se mantiene el mismo renderizador 3D, el modelo low-poly de 60 piezas, la cámara y las comprobaciones de movimiento/visibilidad.
- El flujo de CI verifica package, versionCode y versionName; las pruebas Python comprueban que manifiesto, Gradle y paquete de smoke test coinciden.

## Instalación sin borrar versiones anteriores

Instalar PREVIEW3 como aplicación nueva. No desinstalar PREVIEW ni PREVIEW2: sus datos permanecen en esas instalaciones. PREVIEW3 empieza con una partida nueva; no se implementa migración automática de partidas entre paquetes.

## Evidencia del renderizador

La ejecución #389 mostró en SwiftShader el personaje de 60 piezas en reposo y caminando, con `VISUAL OK`, seguimiento de cámara y error GLES cero. Es una prueba del emulador, no de la GPU Mali-G615 MC6 física.

La ejecución #393 (0.29.8) debe completar su smoke test antes de considerar verificado ese mismo renderizador en el baseline 0.29.8. La 0.29.9 volverá a ejecutar el smoke test con el nuevo package id.

## Validación pendiente en el Poco X6 Pro

1. Instalar la APK 0.29.9 y abrir **Valle de Cernego PREVIEW3 v0.29.9**.
2. Confirmar en el HUD `VALLE DE CERNEGO 0.29.9 · PREVIEW3`.
3. Confirmar que la GPU y el modo aparecen en el HUD.
4. Observar cabeza, torso, ambos brazos, ambas piernas y botas en reposo, caminando y girando.
5. Si sigue apareciendo el rectángulo azul, enviar una captura completa con el HUD visible. No desinstalar las versiones anteriores.

## Limitaciones

- El personaje sigue siendo low-poly y procedural; no tiene todavía una malla artística con rig esquelético ni animaciones importadas.
- SwiftShader no valida el controlador Mali, rendimiento, temperatura ni consumo del teléfono.
- No se declara corregido el problema físico hasta validar en el dispositivo.
- No se modifican `main` ni la visibilidad pública del repositorio.
- No se añaden compras obligatorias, anuncios forzosos ni bloqueos de pago.
