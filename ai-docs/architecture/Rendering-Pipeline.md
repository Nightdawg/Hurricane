---
title: Rendering Pipeline
aliases: [Rendering, Render, Graphics, haven.render]
tags: [architecture, rendering]
---

# Rendering Pipeline

Source: `src/haven/render/**` (~150+ files across `render`, `render.sl`, `render.gl`,
`render.jogl`, `render.lwjgl`), `src/haven/MapView.java`, `src/haven/PView.java`,
`src/haven/GOut.java`, `src/haven/Sprite.java`, `src/haven/Material.java`.

> [!note] This is mostly **upstream (loftar) code**.
> The `haven.render` package is loftar's modern abstract render layer. Hurricane rarely modifies
> it; treat it as a stable dependency. Understand it enough to *use* it, not rewrite it.

## Layered design

```
Widgets / MapView  ──build──►  render graph (state + draw ops)
        │
        ▼
haven.render  (backend-agnostic API)
  ├─ Render        (command interface; extends Disposable)         render/Render.java
  ├─ Pipe          (immutable render-state pipeline)               render/Pipe.java
  ├─ Environment   (backend abstraction)                           render/Environment.java
  ├─ Model/VertexArray/Texture/FrameBuffer/State/...               render/*
  └─ sl/           GLSL shader AST + code generation ("ShaderLang") render/sl/*
        │
        ▼  (one backend is chosen at runtime)
  render.gl    core OpenGL implementation (GLEnvironment, GLRender, …)
  render.jogl  JOGL bindings backend
  render.lwjgl LWJGL bindings backend
```

- **`Pipe`** is a key idea: render **state** (camera, lighting, materials, blending, etc.) is a
  composable, mostly-immutable pipeline rather than mutable global GL state.
- **`render.sl`** is a small DSL/AST for GLSL: shaders are built programmatically and compiled to
  GLSL for the active backend. Files like `render/sl/*.java` are the shader-language nodes.
- The backend is selected via the `haven.renderer` system property (see commented
  `haven.renderer=lwjgl` in `build.xml`'s `run` target). JOGL and LWJGL jars are auto-fetched by
  Ant (`extlib/jogl`, `extlib/lwjgl-gl`).

## 3D world: `PView` → `MapView`

- `PView` is a widget that hosts a 3D scene (sets up camera/projection, a render tree).
- `MapView` (`src/haven/MapView.java`, ~3000 lines) is the big one: it renders the game world from
  [[Game-State-Model|`Glob`/`MCache`/`OCache`]] — terrain (`MapMesh`, `Tiler`/`Tileset`), all the
  `Gob`s (via their `Drawable`/`Composite`), overlays, weather/lighting, the grid, placement
  previews, click-mapping, the camera, and the **pathfinder thread** (`gui.map.pfthread`, see
  [[Pathfinding]]).
- Clicking the world sends `gui.map.wdgmsg("click", …)` / object clicks to the server — the main
  way movement & interaction happen. Bots reuse these.

## 2D HUD: `GOut`

`GOut` is the 2D drawing context handed to `Widget.draw(GOut g)`. It provides text, images, rects,
lines, clipping, and color/alpha — implemented on top of the same render backend. See
[[UI-and-Widget-System]].

## Drawables & sprites

- A `Gob` is drawn via a `Drawable` `GAttrib` — usually `ResDrawable` (a static resource image),
  `Composite` (a skeletal/equippable composite, e.g. player bodies), or `StaticSprite`.
- `Sprite` / `GSprite` + `haven.sprites.*` provide animated/custom-coded visuals (many loaded from
  [[Resource-System|resources]]).
- `Material`, `Light`, `DirLight`, `ShadowMap`, `Tonemapper`, etc. configure shading.

## GPU profiling

`GPUProfile` / `doc/gpu-profiling` document the in-client GPU profiler.

## Related
- [[Game-State-Model]] · [[Resource-System]] · [[UI-and-Widget-System]] · [[Pathfinding]]

#architecture #rendering
