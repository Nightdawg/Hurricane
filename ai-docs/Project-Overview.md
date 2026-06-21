---
title: Project Overview
aliases: [Overview, What is Hurricane]
tags: [overview, hurricane]
---

# Project Overview

## What is it?

**Hurricane** is a custom game **client** for [*Haven & Hearth*](https://www.havenandhearth.com/)
(a.k.a. "H&H" / "hafen"), a niche MMO sandbox by Seatribe (Björn "loftar" Johannessen and
Fredrik "dolda2000" Tolf). Hurricane is maintained by **Nightdawg** and is built **on top of the
official open-source "Vanilla" client** (`hafen-client`). It does not depend on any other custom
client.

- Repo: `Nightdawg/Hurricane` (GitHub)
- Current version: **`v1.63b`** (`Config.clientVersion`)
- Config identifier: `Config.confid = "Hurricane"`
- Window title at runtime: `Hurricane (v1.63b)` (see [[Startup-and-Lifecycle]])
- License: LGPL-3 / GPL-3 (see `COPYING`, `doc/LGPL-3`, `doc/GPL-3`)

The client can run **standalone** (via `Play.bat` / `Play_Linux.sh`) or **through Steam** (Steam
Workshop item). It talks **only** to the official Seatribe server unless explicitly configured
otherwise (e.g. an optional private web-map server or cookbook server).

## 30-second mental model

```
Java desktop app (Swing/AWT-free custom window via haven.iosys "Toolkit")
        │
        ▼
haven.Client.main ──► spawns "Haven main thread" ──► main2()
        │                                              │
        │                                              ├─ Config.cmdline(args)
        │                                              ├─ setupres()          (resource URLs/preload)
        │                                              └─ Client.run(Main)    (UI loop)
        ▼
ClientLoop (UILoop)  drives a tree of  Widget  objects each frame
        │
        ▼
Bootstrap ─► LoginScreen ─► (auth) ─► RemoteUI ─► GameUI  (in-game root widget)
        │
        ├─ Networking:  Session  ⇄  Connection   (custom UDP protocol, Message frames)
        ├─ World state: Glob → { OCache (Gobs), MCache (map grids/tiles) }
        ├─ Rendering:   haven.render.*  (GL3/JOGL or LWJGL backend, GLSL shaders)
        └─ Automation:  haven.automated.*  (Hurricane bots, started by keybindings)
```

See [[Architecture-Overview]] for the full breakdown.

## Fork lineage

Hurricane sits at the end of a chain of H&H clients:

```
Seatribe "Vanilla" hafen-client  (loftar/dolda2000, upstream)
        └─► (various community clients, e.g. "Havoc"*)
                └─► Hurricane (Nightdawg)
```

> \* Code comments in this tree occasionally reference **"Havoc"** (e.g. in
> `GameUI.stopActionThread`), reflecting heritage of some automation code. Treat such comments
> as historical breadcrumbs.

The git repository tracks **two remotes**:

| Remote | URL | Role |
|---|---|---|
| `Hurricane` | `https://github.com/Nightdawg/Hurricane` | the fork (origin) |
| `LoftarSeatribe` | `git://sh.seatribe.se/hafen-client` | upstream vanilla |

The maintainer periodically **merges upstream** (`loftar`) changes to stay current and avoid
crashes when the server protocol/resources change. **Implication for contributors/AI:** when
touching core `haven.*` files, prefer minimal, localized changes so upstream merges stay clean.
Hurricane's own features are concentrated in [[Automation-Bots|haven.automated]] and additive
hooks in core classes (new fields/methods/keybindings rather than rewrites).

## What makes Hurricane "custom"?

Compared to vanilla, Hurricane adds (non-exhaustive):
- The entire [[Automation-Bots|`haven.automated`]] package: ~40 bots/scripts + `pathfinder`,
  `mapper`, `cookbook`, and `helpers` subpackages.
- Quality-of-life UI: custom Gob highlights/info overlays (`Gob*Info.java`,
  `Gob*Highlight.java`), inventory search, object/quest helpers, alarms.
- Integrations: optional web-map server upload ([[Mapper-and-MappingClient]]) and
  [[Cookbook-Integration|cookbook]] food stats.
- SQLite-backed data: `static_data.db` and `hitboxes.db` (collision boxes, flower-menu
  auto-choices). See [[Game-State-Model]] and [[Resource-System]].

## Related notes
- [[Architecture-Overview]]
- [[Build-and-Run]]
- [[Glossary]]

#hurricane #overview
