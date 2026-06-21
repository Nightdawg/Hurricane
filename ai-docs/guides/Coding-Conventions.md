---
title: Coding Conventions
aliases: [Conventions, Style, Patterns, Idioms]
tags: [guide, conventions]
---

# Coding Conventions

Patterns and idioms you **must** respect to fit this codebase (inherited from dolda2000/loftar's
hafen-client). Breaking these causes subtle crashes or ugly upstream-merge conflicts.

## Formatting

- Core `haven.*` files use **tabs** for indentation with a fairly dense style (loftar's style).
  Hurricane's own files (esp. `haven.automated.*`) often use **4-space** indentation.
- **Match the surrounding file.** Do not reformat whole files — it wrecks upstream merges.
- Keep edits **surgical and additive** in core classes (new fields/methods/branches) rather than
  rewrites, so periodic upstream merges from `LoftarSeatribe` stay clean. See
  [[Project-Overview#Fork lineage]].

## The `wdgmsg` / `uimsg` protocol (don't bypass it)

- To make the game *do* something, send `widget.wdgmsg("name", args...)` — never hand-roll network
  packets. The server is authoritative and replies with `uimsg`/object updates.
- See [[UI-and-Widget-System#Two message directions]].

## `Loading` exceptions are control flow, not errors

- Resource access is lazy ([[Resource-System#`Loading` exceptions]]). Touching unloaded data throws
  `haven.Loading` (and subclasses). The render/tick loop **expects** this and retries next frame.
- In bots/background threads, **catch `Loading` and retry** (sleep + continue). Never swallow it as
  a generic error or log-spam it.

## Threading & shared state

- The world (`OCache`, `MCache`, `Glob`) mutates on the **network thread**, is read on the
  **render/UI thread**, and is read+acted-on by **bot threads**.
- **Iterating `OCache` requires `synchronized (oc)`** (`oc = gui.map.glob.oc`). This is the #1 rule
  for automation code.
- Don't block the UI/render thread with sleeps or network waits — do that work on a bot/worker
  thread (`new Thread(runnable, "name").start()` or `GameUI.runActionThread`).
- Threads are created via `HackThread` in core paths (carries the error-handler thread group).

## Laziness & indirection idioms (recognize these)

| Idiom | Meaning |
|---|---|
| `Indir<T>` | A lazy/deferred reference to `T` (call `.get()`); may throw `Loading`. |
| `Defer` / `Defer.Future` | Run work on a background pool; results pulled later. |
| `Loader` | Background resource loader. |
| `Supplier`/`Promise`/`Future`/`Waitable` | Async value plumbing used throughout. |
| `Disposable` / `dispose()` | Manual lifecycle for GPU/native resources — call it; don't rely on GC. |
| `OwnerContext` / `Resolver` | Context for resolving resource-relative references. |

## Coordinates

- Mind the space: `Coord` (int 2D), `Coord2d` (double world), `Coord3f`/`Coordf` (render). Convert
  via `tilesz` / `posres` / `Coord*` helpers. See [[Game-State-Model#Coordinates]].

## Config & prefs

- Persistent prefs go through `Utils.getpref*` / `Utils.setpref*` and `Config` (stored under
  `%APPDATA%/Haven and Hearth` or `~/.haven`). Don't write ad-hoc files.
- Feature toggles generally live in `OptWnd` + `Config`.

## Comments & naming

- Hurricane/`ND` author comments are tagged `// ND:` in the source — a quick way to find
  intentional fork behavior.
- Comments mentioning **"Havoc"** are historical lineage notes (an ancestor client).
- Keep new comments minimal and only where intent isn't obvious (matches repo norm).

## Don'ts

- ❌ Don't add Maven/Gradle or change the build to a different tool — it's **Ant** ([[Build-and-Run]]).
- ❌ Don't reformat or "modernize" core files wholesale.
- ❌ Don't iterate `OCache` without synchronizing.
- ❌ Don't catch-and-ignore `Loading` as if it were a bug.
- ❌ Don't build server packets by hand — use `wdgmsg`.

## Related
- [[Adding-a-New-Bot]] · [[Architecture-Overview]] · [[Resource-System]] · [[Glossary]]

#guide #conventions
