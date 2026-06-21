# GitHub Copilot — Repository Instructions (Hurricane Client)

These instructions are auto-loaded by GitHub Copilot. The **canonical** guide is
[`AGENTS.md`](../AGENTS.md); the full architecture vault is in
[`ai-docs/Home.md`](../ai-docs/Home.md); a RAG retrieval tool is in [`rag/`](../rag/README.md).

## Project

**Hurricane** is a custom **Java** desktop client for the game *Haven & Hearth*, forked from
Loftar/Seatribe's open-source `hafen-client` (maintainer: Nightdawg). Version `v1.63b`. Entry point
is `haven.Client` (`src/haven/Client.java`). ~840 Java files. Java 17–21 (level 15).

## Build & validate

- Build system is **Apache Ant** (`build.xml`) — **not** Maven/Gradle/npm.
- `ant jar` → compile + package (use to verify a change compiles).
- `ant run` → build and launch. `ant` → full build into `bin/`. `ant clean` → reset.
- There is **no unit-test suite**; validate by compiling with `ant jar` (and launching if feasible).

## When generating or editing code, follow these rules

1. **Surgical, additive edits to core `haven.*` files.** This is a fork that periodically merges the
   `LoftarSeatribe` upstream; avoid wholesale reformatting/rewrites. Match the file's existing
   indentation (core files use tabs; `haven.automated.*` often uses 4 spaces).
2. **Use the widget message protocol** for anything that affects the game: `wdgmsg(...)` is
   client→server, `uimsg(...)` is server→client. Don't hand-build network packets.
3. **`haven.Loading` is normal control flow** (lazy resource loading). Catch and retry; never treat
   it as an error or spam logs.
4. **Concurrency:** world state (`OCache`, `MCache`, `Glob`) is mutated by the network thread.
   Always wrap Gob iteration in `synchronized (gui.map.glob.oc) { ... }`. Do slow work on a worker
   thread, never on the UI/render thread.
5. **Automation features** belong in `haven.automated.*`. Bots are typically
   `Window implements Runnable`, poll with `Thread.sleep`, use `AUtils` helpers, and are launched
   from `GameUI` keybindings via `runActionThread(...)`. See
   [`Adding-a-New-Bot`](../ai-docs/guides/Adding-a-New-Bot.md).
6. **Privacy:** only the official Seatribe server is contacted by default. Don't add telemetry or new
   outbound network calls; the mapper/cookbook integrations are opt-in and user-configured.
7. **Don't change the build system** and don't add new heavyweight dependencies.

## Key locations

- In-game hub & bot keybindings: `src/haven/GameUI.java`
- Settings/toggles: `src/haven/OptWnd.java`, `src/haven/Config.java`
- Automation: `src/haven/automated/` (bots), `AUtils.java` (shared helpers)
- World model: `Glob`, `OCache`, `Gob`, `MCache`
- Networking: `Session`, `Connection`, `Message`/`PMessage`/`RMessage`
- Resource-bundled code: `src/haven/res/**` (`@FromResource`)

If these instructions conflict with the actual code, prefer the code, then update the docs.
