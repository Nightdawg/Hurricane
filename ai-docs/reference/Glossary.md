---
title: Glossary
aliases: [Glossary, Terms, Vocabulary]
tags: [reference, glossary]
---

# Glossary

Domain (Haven & Hearth) and codebase vocabulary. If a term in another note confuses you, it's here.

## Project / domain

- **Haven & Hearth (H&H, "hafen")** — the MMO sandbox game this is a client for, by Seatribe.
- **Hurricane** — this client (Nightdawg's fork). → [[Project-Overview]]
- **Vanilla / hafen-client** — the official open-source client this forks from (loftar/dolda2000).
- **Loftar** — Björn Johannessen, game/client author (upstream). **dolda2000** — Fredrik Tolf,
  co-author of much core/utility code.
- **Havoc** — an ancestor community client referenced in some code comments; lineage breadcrumb.
- **ND** — the maintainer's initials; `// ND:` comments mark intentional Hurricane behavior.
- **Seatribe** — the studio / official server operator.

## Game objects & world

- **Gob** — *Game OBject*. Anything in the world (player, animal, tree, item, building…).
  `haven.Gob`. → [[Game-State-Model]]
- **GAttrib** — a typed attribute attached to a `Gob` (drawable, movement, health…). ECS-like.
- **Overlay** — a transient layer on a `Gob` (effect, progress, highlight).
- **OCache** — *Object Cache*, the registry of all known `Gob`s. Mutates on the network thread.
- **MCache** — *Map Cache*, the tiled terrain (grids/tiles/heights).
- **Grid** — a fixed block of map tiles (`MCache.cmaps` tiles per side).
- **Tile** — one map cell (`MCache.tilesz` world-units per side).
- **Glob** — global per-session world state (owns `oc`, `map`, astronomy, party…).
- **Paginae** — the server-defined **actions** shown in the `MenuGrid` (verb/skill menu entries).
- **Flower menu** — the radial right-click context menu (`FlowerMenu`); Hurricane can auto-select
  its petals.
- **FEP / food info** — food event points; parsed by [[Cookbook-Integration|FoodService]].

## Networking

- **Session** — a logged-in connection (`haven.Session`); owns the `Glob`.
- **Connection / Transport** — the UDP transport + reliability/crypto layer.
- **PMessage** — a *protocol* (datagram-level) message with a `type` byte.
- **RMessage** — a *reliable*, ordered, ACK'd (and possibly fragmented) message.
- **PVER** — protocol version (currently `31`); mismatch → `SESSERR_PVER`.
- → [[Networking-and-Protocol]]

## UI

- **Widget** — base UI element; the screen is a tree of these. → [[UI-and-Widget-System]]
- **wdgmsg** — *widget message*: **client → server** ("I clicked/selected …"). Bubbles up to `UI`.
- **uimsg** — **server → client** ("update yourself"). The server drives UI state.
- **GameUI** — the in-game root widget; aggregates map, inventory, chat, meters, combat, etc.
- **GOut** — the 2D drawing context passed to `Widget.draw`.
- **KeyBinding / KeyMatch** — the hotkey system (`kb_*` fields) used for all keyboard actions,
  including launching [[Automation-Bots|bots]].

## Rendering

- **PView** — a widget hosting a 3D scene; `MapView` extends it. → [[Rendering-Pipeline]]
- **Pipe** — composable, mostly-immutable render **state** pipeline.
- **Environment** — the render backend abstraction; `GLEnvironment` for OpenGL.
- **render.sl** — the GLSL **shader-language** AST/codegen ("ShaderLang").
- **JOGL / LWJGL** — the two OpenGL binding backends (`haven.renderer` property selects one).
- **Drawable / Composite / ResDrawable / Sprite** — how a `Gob` is visually rendered.

## Resources

- **Resource** — a versioned, named content bundle of **layers** (image/tile/sprite/code/…).
  → [[Resource-System]]
- **Resource code / `haven.res.*`** — Java code shipped *inside* resources, fetched into `src/` for
  modding; annotated `@FromResource(name, version)`.
- **`get-code` / `find-updates`** — `haven.Resource` CLI sub-commands to fetch/check resource code.
- **OVERRIDE_ALL / `override=true`** — make local fetched code win over the server's resource code.

## Codebase idioms

- **Loading** — a `Throwable` thrown when accessing not-yet-loaded data; **control flow**, caught &
  retried, not an error. → [[Coding-Conventions#`Loading` exceptions are control flow]]
- **Indir<T>** — a lazy/deferred reference (`.get()` may throw `Loading`).
- **Defer / Loader** — background work/loading executors.
- **Disposable / dispose()** — explicit cleanup of native/GPU resources.
- **HackThread** — `Thread` subclass that carries the error-handler `ThreadGroup`.
- **Coord / Coord2d / Coord3f** — int 2D / double world / float-3D coordinate types.
- **WItem / GItem** — a UI inventory item widget / the underlying game item.

## Build

- **Ant** — the build tool (`build.xml`). No Maven/Gradle. → [[Build-and-Run]]
- **`bin/`** — the assembled runnable client (jar + libs + res + launchers).
- **builtin-res.jar / hafen-res.jar** — bundled resource packs fetched at build time.
- **Panama / opt** — optional Java 22+ FFI module; skipped on Java 17–21.

## Related
- [[Home]] · [[Architecture-Overview]] · [[Key-Classes]]

#reference #glossary
