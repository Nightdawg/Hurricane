---
title: Package Map
aliases: [Packages, Package Map, Source Layout]
tags: [reference, packages]
---

# Package Map

Where things live. ~840 `.java` files under `src/`. Root namespaces: `haven`, `dolda`, `com`, `org`.

## `haven` — the client (≈377 files at top level + subpackages)

The main namespace. Everything game-related is here unless noted.

| Area | Representative classes |
|---|---|
| **Bootstrap/lifecycle** | `Client`, `Bootstrap`, `LoginScreen`, `Charlist`, `AccountList`, `RemoteUI`, `HeadlessClient` → [[Startup-and-Lifecycle]] |
| **Networking** | `Session`, `Connection`, `Transport`, `Message`, `PMessage`, `RMessage`, `MessageBuf`, `AuthClient` → [[Networking-and-Protocol]] |
| **Game state** | `Glob`, `OCache`, `Gob`, `GAttrib`, `MCache`, `MapFile`, `Party`, `Polity`, `Astronomy` → [[Game-State-Model]] |
| **UI / widgets** | `UI`, `Widget`, `RootWidget`, `GameUI`, `Window`, `MenuGrid`, `FlowerMenu`, `Inventory`, `Equipory`, `CharWnd`, `MapWnd`, `ChatUI`, `OptWnd` → [[UI-and-Widget-System]] |
| **Rendering (2D HUD)** | `GOut`, `Text`, `RichText`, `Tex*`, `Img` |
| **Rendering (3D)** | `MapView`, `PView`, `Sprite`, `Composite`, `Material`, `Skeleton`, `Light` → [[Rendering-Pipeline]] |
| **Resources** | `Resource`, `ResCache`, `FileCache`, `HashDirCache`, `FromResource`, `Drawable`, `GSprite` → [[Resource-System]] |
| **Combat** | `Fightview`, `Fightsess`, `FightWnd` → [[Combat-System]] |
| **Hurricane QoL** | `Gob*Info`, `Gob*Highlight`, `AlarmManager`/`AlarmWindow`, `InventorySearchWindow`, `ObjectSearchWindow`, `QuestWnd`, `GobIconsCustom`, `HitBoxGobSprite` |
| **Utilities** | `Utils`, `Config`, `Coord`/`Coord2d`/`Coord3f`, `Defer`, `Loader`, `Indir`, `Hash`/`Digest`, collections (`IntMap`, `HashedSet`, …) |

### Notable `haven` subpackages

| Package | # files | Purpose |
|---|---|---|
| `haven.automated` | 40 (+subpkgs) | **Hurricane bots/scripts.** → [[Automation-Bots]] |
| `haven.automated.pathfinder` | 9 | A* navigation → [[Pathfinding]] |
| `haven.automated.mapper` | 3 | Web-map upload → [[Mapper-and-MappingClient]] |
| `haven.automated.cookbook` | 1 | Food stats → [[Cookbook-Integration]] |
| `haven.automated.helpers` | 3 | `HitBoxes`, `FishingAtlas`, `AreaSelectCallback` |
| `haven.render` | 55 | Backend-agnostic render API → [[Rendering-Pipeline]] |
| `haven.render.sl` | 65 | GLSL shader-language AST/codegen |
| `haven.render.gl` | 34 | Core OpenGL backend |
| `haven.render.jogl` | 6 | JOGL backend binding |
| `haven.render.lwjgl` | 4 | LWJGL backend binding |
| `haven.sprites` | 20 | Sprite/visual effect code |
| `haven.resutil` | 18 | Resource utilities (caves, ridges, etc.) |
| `haven.res.**` | ~100+ | **Fetched resource code** (`@FromResource`) → [[Resource-System]] |
| `haven.iosys`, `haven.iosys.tk`, `haven.iosys.audio` | ~21 | OS window/input/audio toolkit (`Toolkit`, `Windeye`) |
| `haven.error` | 7 | Crash/error reporting (`ErrorHandler`, `ErrorGui`) |
| `haven.test`, `haven.rs`, `haven.widgets` | few | Tests / misc |

## Third-party / vendored

| Package | Purpose |
|---|---|
| `dolda.*` (`dolda.coe`, `dolda.xiphutil`) | Fredrik Tolf's utility libs (Vorbis/Xiph, encoding). |
| `com.jcraft.jogg`, `com.jcraft.jorbis` | JOrbis OGG Vorbis audio decoding. |
| `org.json` | JSON parser/writer (used by manifests, mapper, cookbook). |

Plus binary deps in `lib/`: `jglob.jar`, `rxjava-1.1.5.jar`, `sqlite-jdbc-3.42.0.0.jar`, and
Ant-fetched native libs in `lib/ext/` (JOGL, LWJGL, steamworks4j). See [[Build-and-Run]].

## Non-source dirs (repo root)

| Dir/file | What |
|---|---|
| `res/` | Client-side resources, incl. `res/customclient/*` (Hurricane custom icons/menus/sfx/buffs). |
| `etc/` | Certs (`ressrv.crt`, `authsrv.crt`), preload lists, helper scripts. |
| `doc/` | `GPL-3`, `LGPL-3`, `resource-code`, `gpu-profiling`. |
| `opt/panama` | Optional Java 22+ Panama FFI module (skipped on 17–21). |
| `AlarmSounds/`, `midiFiles/`, `MapIconsPresets/` | Runtime assets copied into `bin/`. |
| `static_data.db`, `hitboxes.db` | SQLite data (flower-menu auto-choose, collision boxes). |
| `Play*.bat`, `Play_Linux.sh` | Launchers. `build.xml` | Ant build. `nd-notes.txt` | maintainer CLI notes. |

## Related
- [[Architecture-Overview]] · [[Key-Classes]] · [[Home]]

#reference #packages
