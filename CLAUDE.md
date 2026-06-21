# CLAUDE.md

This file orients Claude / Claude Code in the **Hurricane** repository.

👉 **Read [`AGENTS.md`](AGENTS.md) first — it is the canonical, single source of truth.**
For deep architecture, browse the linked Obsidian vault at [`ai-docs/Home.md`](ai-docs/Home.md).
A queryable retrieval tool (RAG) is in [`rag/`](rag/README.md).

## TL;DR

- **What:** a custom Java client for *Haven & Hearth*, fork of Loftar's `hafen-client` (`v1.63b`).
- **Entry point:** `haven.Client` (`src/haven/Client.java`).
- **Build:** Apache **Ant** — `ant jar` to check compilation, `ant run` to launch, `ant` for a full
  build. **No Maven/Gradle. No unit-test suite** (validate via `ant jar`).
- **Java:** 17–21 (21 recommended), language level 15.

## Non-negotiable rules (see AGENTS.md for the full list)

1. Keep core `haven.*` changes **surgical & additive** — this fork merges upstream
   (`LoftarSeatribe`). Don't reformat whole files; match existing style.
2. Interact with the server only via the **`wdgmsg`** (client→server) / **`uimsg`** (server→client)
   widget-message protocol. Never build packets by hand.
3. `haven.Loading` is **control flow, not an error** — catch and retry.
4. World state (`OCache`/`MCache`/`Glob`) mutates on the network thread — **`synchronized
   (gui.map.glob.oc)`** to iterate Gobs; never block the UI thread.
5. Hurricane's custom features live in **`haven.automated.*`**; bots are `Window implements
   Runnable` launched from `GameUI` keybindings via `runActionThread(...)`.
6. Don't add telemetry/new outbound connections; don't change the build system.

## Map

- Custom automation ⭐ → [`ai-docs/subsystems/Automation-Bots.md`](ai-docs/subsystems/Automation-Bots.md)
- How to add a bot → [`ai-docs/guides/Adding-a-New-Bot.md`](ai-docs/guides/Adding-a-New-Bot.md)
- Conventions → [`ai-docs/guides/Coding-Conventions.md`](ai-docs/guides/Coding-Conventions.md)
- Class index → [`ai-docs/reference/Key-Classes.md`](ai-docs/reference/Key-Classes.md)
- Vocabulary → [`ai-docs/reference/Glossary.md`](ai-docs/reference/Glossary.md)

If these docs ever disagree with the code, **the code wins** — then fix the docs.
