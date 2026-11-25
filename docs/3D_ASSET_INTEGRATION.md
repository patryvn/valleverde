# Valle de Cernego — 3D asset integration plan

## Selected free assets (reviewed 25 Nov 2026)

The current Android renderer is a custom OpenGL ES 2.0 renderer. It draws its own vertex buffers and does not currently contain a glTF/GLB importer, skeletal animation system, or PBR material pipeline. Therefore, downloading GLB files alone would NOT make them appear in the game. Do not claim these assets are integrated until the runtime loader and in-game rendering are implemented and tested.

### First environment pack: Cozy Farm and Homestead
- Source: https://3dassets.dev/packs/cozy-farm-and-homestead
- License: CC0 1.0 Universal (as stated by publisher).
- 66 GLB models, 1.3 MB total, 25,472 triangles.
- Suitable for first pass: farmhouse/barn, fences, garden beds, crops, hand tools and farm props.
- Publisher notes: metre units, +Y up; GLB loading examples use three.js GLTFLoader. This project must adapt coordinate conventions to its OpenGL renderer.

### Alternative matching farm pack: Shuriken Studio Farm
- Source: https://shuriken.studio/kits/farm
- License: publisher's Free Model License; page says free use in any project, as-is, no credit.
- 30 GLB models, 2.8 MB.
- Includes barn, tractor/trailer, coop, greenhouse, cow, sheep, pig, fruit trees, crops, garden plots, fences and tools.
- Models are low-poly and visually coherent. Check individual source licence before redistributing files.

### Animated animals and characters: Shared World Props
- Source: https://bitwisegaming.itch.io/shared-world-props
- License: publisher permits unlimited commercial projects with no royalty; attribution not required; no resale/redistribution as standalone model files. Preserve the supplied LICENSE.txt with any acquired files.
- 51 models, GLB + FBX; 14 rigged/animated, 130 clips; 37 static props with LODs.
- Includes farmer/villager, horse, cow, sheep, pig, goat, rooster, buildings, trees and rocks.
- Known limitations reported by publisher: some extreme poses stretch; animal feet can sink slightly; rigged models have no LOD.

### Alternative animated farm characters: Cozy Farm (BlockImagine)
- Source: https://1camou.itch.io/cozy-farm-blockimagine
- License: CC0, according to publisher.
- 30 models; 25 rigged/animated; includes farmer characters, cow, pig, hen, sheep, horse, duck, dog, farmhouse, barn, windmill, tractor, apple tree and props.
- Visual style is intentionally blocky/pixel-textured, not realistic.

## Integration rules

1. Keep the current game playable while replacing one asset category at a time.
2. Add a glTF 2.0 runtime importer compatible with Android and OpenGL ES 2.0, or migrate rendering in a separate tested step. Do not mix an untested renderer migration with gameplay changes.
3. Support embedded/external textures, node transforms, indexed meshes, normals, and material base-colour first. Skeletal animation and animation clips are a separate milestone.
4. Convert source units/axes consistently; validate model bounds and ground contact.
5. Add low-poly/LOD variants and texture-size limits for mobile memory/performance.
6. Store each asset's original author, source URL, exact licence and any attribution text beside the asset. Do not commit paid, restricted, or unlicensed game-ripped content.
7. Add automated tests for asset manifest parsing, model bounds, vertex/index counts and coordinate conversion. Device tests must check GPU rendering, frame rate, memory, touch controls and animation.

## Current status

- Asset candidates and licence claims documented.
- No third-party GLB model is yet in the Android app; the custom OpenGL ES 2.0 renderer cannot consume GLB directly.
- Next implementation milestone: add a small, maintained glTF importer/decoder and render one static CC0 farmhouse/prop model in a dedicated test scene before migrating the player, villagers or animals.
- Physical Android validation remains required for GPU compatibility and performance.
