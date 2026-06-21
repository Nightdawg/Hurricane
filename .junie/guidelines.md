# Junie Guidelines — Hurricane Client

These are project guidelines for JetBrains **Junie** (and other IDE assistants). The canonical,
complete instructions live in [`AGENTS.md`](../AGENTS.md); the full architecture knowledge base is
the Obsidian vault in [`ai-docs/`](../ai-docs/Home.md). **Read those for depth.**

## Project
Hurricane is a custom **Java** desktop client for the game *Haven & Hearth*, forked from Loftar's
open-source `hafen-client` (version `v1.63b`). Entry point: `haven.Client` (`src/haven/Client.java`).
~840 Java files. Java 17–21 (language level 15).

## Build & validate (IMPORTANT — it's Ant, not Gradle/Maven)
- `ant jar` — compile + package; use this to verify a change compiles (main validation gate).
- `ant run` — build and launch the client. `ant` — full build into `bin/`. `ant clean` — reset.
- There is **no unit-test suite**.

## Rules (do not violate)
1. Keep core `haven.*` edits **surgical and additive** — this fork periodically merges the
   `LoftarSeatribe` upstream. Match each file's existing indentation; never reformat whole files.
2. Interact with the server only through the widget-message protocol: `wdgmsg(...)` (client→server)
   and `uimsg(...)` (server→client). Never hand-build network packets.
3. `haven.Loading` is normal control flow (lazy resource loading) — catch and retry; it is not a bug.
4. World state (`OCache`/`MCache`/`Glob`) mutates on the network thread. Always
   `synchronized (gui.map.glob.oc)` when iterating Gobs; never block the UI/render thread.
5. Hurricane's automation lives in `haven.automated.*`; bots are `Window implements Runnable`
   launched from `GameUI` (keybinding) or `MenuGrid` (windowed bot). See
   [`ai-docs/guides/Adding-a-New-Bot.md`](../ai-docs/guides/Adding-a-New-Bot.md).
6. Privacy: only the official Seatribe server is contacted by default — do not add telemetry or new
   outbound calls. Don't change the build system.

## Where to look
- In-game hub & bot keybindings: `src/haven/GameUI.java`; menu launchers: `src/haven/MenuGrid.java`
- Settings: `src/haven/OptWnd.java`, `src/haven/Config.java`
- World model: `Glob`, `OCache`, `Gob`, `MCache`; Networking: `Session`, `Connection`, `Message`
- Symbol lookup: [`ai-docs/reference/Class-Index.md`](../ai-docs/reference/Class-Index.md)
- Working method: [`ai-docs/guides/AI-Agent-Playbook.md`](../ai-docs/guides/AI-Agent-Playbook.md)

If these guidelines conflict with the code, prefer the code, then update the docs.
