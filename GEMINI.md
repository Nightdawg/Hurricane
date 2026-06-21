# GEMINI.md

Instructions for Google **Gemini** (Gemini CLI / Code Assist) in the **Hurricane** repository.

👉 The canonical guide is [`AGENTS.md`](AGENTS.md). The full architecture knowledge base is the
Obsidian vault at [`ai-docs/Home.md`](ai-docs/Home.md). A queryable RAG tool is in
[`rag/`](rag/README.md).

## TL;DR
- **What:** a custom Java client for *Haven & Hearth*, fork of Loftar's `hafen-client` (`v1.63b`).
- **Entry point:** `haven.Client` (`src/haven/Client.java`).
- **Build:** Apache **Ant** — `ant jar` (compile-check), `ant run` (launch), `ant` (full). No
  Maven/Gradle. **No unit-test suite.** Java 17–21 (level 15).

## Rules (do not violate)
1. Keep core `haven.*` edits **surgical & additive** — the fork merges the `LoftarSeatribe` upstream.
   Match existing file style; don't reformat.
2. Talk to the server only via `wdgmsg(...)` (client→server) / `uimsg(...)` (server→client). Never
   hand-build packets.
3. `haven.Loading` is control flow (lazy resource loading), not an error — catch and retry.
4. World state (`OCache`/`MCache`/`Glob`) mutates on the network thread — `synchronized
   (gui.map.glob.oc)` to iterate Gobs; never block the UI thread.
5. Automation lives in `haven.automated.*` (Window+Runnable), launched from `GameUI` keybindings or
   the `MenuGrid` menu.
6. No telemetry / new outbound calls; don't change the build system.

## Navigation
Bots → [`ai-docs/reference/Bot-Index.md`](ai-docs/reference/Bot-Index.md) ·
Symbols → [`ai-docs/reference/Class-Index.md`](ai-docs/reference/Class-Index.md) ·
Hotspots → [`ai-docs/reference/Code-Metrics.md`](ai-docs/reference/Code-Metrics.md) ·
Resource names → [`ai-docs/reference/Resource-Naming.md`](ai-docs/reference/Resource-Naming.md) ·
Method → [`ai-docs/guides/AI-Agent-Playbook.md`](ai-docs/guides/AI-Agent-Playbook.md).

If these instructions conflict with the code, prefer the code, then update the docs.
