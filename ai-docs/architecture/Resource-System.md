---
title: Resource System
aliases: [Resources, Resource, haven.res, FromResource]
tags: [architecture, resources]
---

# Resource System

Source: `src/haven/Resource.java` (~2200 lines), `src/haven/ResCache.java`,
`src/haven/FileCache.java`, `src/haven/HashDirCache.java`, `src/haven/FromResource.java`,
`src/haven/res/**` (fetched resource code), and `doc/resource-code` (read this!).

## What a "resource" is

H&H content is delivered as **resources**: versioned bundles (`.res`) identified by a path-like
name, e.g. `gfx/kritter/bear/bear`, `ui/tt/name`, `paginae/act/fish`. A resource is a set of
**layers**, each decoded by a `Resource.LayerFactory`:
- image layers, tile/tileset layers, sprite/animation layers, audio layers,
- tooltips, action ("paginae") definitions,
- and **code layers** — actual compiled Java loaded at runtime (see below).

Resources are fetched from the server (or a configured `Resource.resurl`), cached on disk
(`ResCache`/`FileCache`/`HashDirCache`), and resolved lazily. The client ships two resource jars,
fetched by Ant: `builtin-res.jar` and `hafen-res.jar` (see [[Build-and-Run]]).

## Why so much logic isn't in this source tree

Per `doc/resource-code`: a large amount of game-specific code (special widgets, sprites with custom
behavior, item tooltips, etc.) lives **inside resources**, loaded via Java's dynamic class loading.
Benefits: the server can ship new client code in updates without every player updating their client.

**Consequence for AI/devs:** if you can't find a behavior in `src/`, it may live in a resource.
You can fetch the corresponding source into the tree (see below) to read/modify it.

## `haven.res.*` — fetched resource code

The `src/haven/res/**` packages contain Java source **extracted from resources** so it can be read
and modified locally. Each top-level class is annotated:

```java
@FromResource(name = "ui/tt/name", version = 7)
public class Name extends ItemInfo.Tip { … }
```

### Fetching / updating (CLI via the built jar)

`haven.Resource` has a `main()` with sub-commands (see `nd-notes.txt` and `doc/resource-code`):

```bash
# Fetch (or overwrite) the Java code from a resource into src/haven/res/...
java -cp bin/hafen.jar haven.Resource get-code res/path/here
java -cp bin/hafen.jar haven.Resource get-code ui/tt/name ui/tt/wear   # multiple
java -cp bin/hafen.jar haven.Resource get-code -o staging ui/tt/name   # custom out dir

# List resources whose server version is newer than the fetched code (does NOT modify files)
java -cp bin/hafen.jar haven.Resource find-updates
```

> [!warning] `get-code` overwrites files without asking.
> Commit/stash first. Tip from the docs: keep an `upstream-resources` branch to manage merges.

### Version override (graceful degradation)

The resource classloader normally lets **local** (fetched) code override the resource's own code
**only if** the `@FromResource` name **and version** match. If the server bumps a resource version,
the client falls back to the server's newer code instead of crashing on stale local code. Overrides:
- `@FromResource(override = true)` forces local code to win for that class.
- `Resource.OVERRIDE_ALL` forces all local code to win (not recommended).

## Caches & local data

- `ResCache.global`, `FileCache`, `HashDirCache`, `BaseFileCache` — on-disk resource caches.
- Local config/data dir: `%APPDATA%/Haven and Hearth` (Windows) or `~/.haven` (fallback)
  — see `Config.localdir()`.
- SQLite data files in the repo root: `static_data.db` (flower-menu auto-choose etc.) and
  `hitboxes.db` (collision boxes) — populated/read at startup (see [[Startup-and-Lifecycle]]).

## Drawables from resources

`ResDrawable`, `Sprite`, `GSprite`, `Composite`, `Tileset`/`Tiler` all resolve their visuals from
resources. A `Gob`'s appearance = its `Drawable` `GAttrib` pointing at a resource. See
[[Rendering-Pipeline]] and [[Game-State-Model]].

## `Loading` exceptions (don't fight them)

Resource access is lazy. Touching a not-yet-loaded resource throws a `Loading` exception (a control
flow signal, not an error). The render/tick loop catches it and retries next frame. **Bots and any
state-reading code must tolerate `Loading`** — wrap in try/catch and retry. See
[[Coding-Conventions#Loading]].

## Related
- [[Rendering-Pipeline]] · [[Game-State-Model]] · [[Build-and-Run]] · [[Coding-Conventions]]

#architecture #resources
