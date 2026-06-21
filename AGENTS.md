# AGENTS.md — Hurricane Client

> Canonical instructions for AI coding agents (and humans) working in this repo.
> Conventions follow the emerging `AGENTS.md` standard. Thin pointers for specific tools redirect
> here: `CLAUDE.md`, `GEMINI.md`, `.github/copilot-instructions.md`, `.junie/guidelines.md`
> (JetBrains), `.cursor/rules/hurricane.mdc`, `.windsurfrules`, `.clinerules`, and `llms.txt`.
> **For deep architecture docs, read the Obsidian knowledge vault in
> [`ai-docs/`](ai-docs/Home.md).** A queryable RAG retrieval tool lives in [`rag/`](rag/README.md).

## What this project is

**Hurricane** is a custom **Java desktop game client** for *Haven & Hearth* ("hafen"), forked from
Loftar/Seatribe's open-source "Vanilla" `hafen-client` and maintained by **Nightdawg**.

- Version: `v1.63b` (`haven.Config.clientVersion`), `confid = "Hurricane"`.
- Entry point / Main-Class: **`haven.Client`** (`src/haven/Client.java`).
- ~840 `.java` files. Java **17–21** (21 recommended), source/target level **15**.
- Build tool: **Apache Ant** (`build.xml`). There is **no** Maven or Gradle.
- Two git remotes: `Hurricane` (fork/origin) and `LoftarSeatribe` (upstream vanilla,
  `git://sh.seatribe.se/hafen-client`). Upstream is merged periodically.

## Build / run / test commands

```bash
ant            # full build -> bin/ (default target "deftgt")
ant jar        # fast: compile + package build/hafen.jar (use this to check compilation)
ant bin        # assemble runnable bin/ folder
ant run        # build + launch the client
ant clean      # remove build/, bin/, downloaded lib/ext/
```

- There is **no unit-test suite** to run; validate changes with `ant jar` (compiles) and, when
  feasible, `ant run` (launches). `haven.test.*` contains a few ad-hoc harnesses, not a CI suite.
- First build needs internet (Ant downloads JOGL/LWJGL/steamworks + `builtin-res.jar`/`hafen-res.jar`).

## Architecture in one breath

`Client.main` → spawns the *Haven main thread* → `main2()` (`Config.cmdline`, `setupres`,
`Client.run`) → a `ClientLoop` (`UILoop`) ticks a tree of `Widget`s. `Bootstrap`→`LoginScreen`→
`RemoteUI`→`GameUI` (in-game root). Networking is `Session`/`Connection` (custom UDP + reliability).
World state is `Glob`→`OCache`(Gobs)+`MCache`(map). Rendering is the `haven.render` pipeline
(GL via JOGL/LWJGL). Game logic also lives in **resources** (`haven.res.*`, `@FromResource`).
Hurricane's custom features are concentrated in **`haven.automated.*`**.

Full details: [`ai-docs/Home.md`](ai-docs/Home.md) (Map of Content). Highlights:
- Architecture: [`Architecture-Overview`](ai-docs/architecture/Architecture-Overview.md),
  [`Startup-and-Lifecycle`](ai-docs/architecture/Startup-and-Lifecycle.md),
  [`Networking-and-Protocol`](ai-docs/architecture/Networking-and-Protocol.md),
  [`Game-State-Model`](ai-docs/architecture/Game-State-Model.md),
  [`UI-and-Widget-System`](ai-docs/architecture/UI-and-Widget-System.md),
  [`Rendering-Pipeline`](ai-docs/architecture/Rendering-Pipeline.md),
  [`Resource-System`](ai-docs/architecture/Resource-System.md).
- Hurricane subsystems: [`Automation-Bots`](ai-docs/subsystems/Automation-Bots.md) ⭐,
  [`Pathfinding`](ai-docs/subsystems/Pathfinding.md),
  [`Mapper-and-MappingClient`](ai-docs/subsystems/Mapper-and-MappingClient.md),
  [`Cookbook-Integration`](ai-docs/subsystems/Cookbook-Integration.md),
  [`Combat-System`](ai-docs/subsystems/Combat-System.md).
- Reference: [`Package-Map`](ai-docs/reference/Package-Map.md),
  [`Key-Classes`](ai-docs/reference/Key-Classes.md),
  [`Glossary`](ai-docs/reference/Glossary.md).

## Rules / conventions you MUST follow

1. **Keep core `haven.*` edits surgical and additive.** This is a fork that merges upstream; large
   rewrites/reformatting cause merge hell. Match each file's existing style (core files use tabs;
   `haven.automated.*` often uses 4 spaces). Don't reformat whole files.
2. **Use the `wdgmsg`/`uimsg` protocol** to interact with the server — never hand-build packets.
   `wdgmsg` = client→server; `uimsg` = server→client. The server is authoritative.
3. **`Loading` is control flow, not an error.** Accessing unloaded resources throws `haven.Loading`;
   catch & retry (the render loop does this every frame). Never log-spam or treat it as a bug.
4. **Thread safety:** world state (`OCache`/`MCache`/`Glob`) mutates on the network thread. Always
   `synchronized (gui.map.glob.oc)` when iterating Gobs. Don't block the UI thread; do slow work on
   a worker/bot thread.
5. **Bots** (`haven.automated.*`) are usually `Window implements Runnable`, run on their own thread,
   poll with `Thread.sleep`, and use `AUtils` helpers. Launch them from `GameUI` keybindings via
   `runActionThread(...)`. Recipe: [`Adding-a-New-Bot`](ai-docs/guides/Adding-a-New-Bot.md).
6. **Don't change the build system.** It's Ant. Don't add Maven/Gradle/npm.
7. **Privacy:** the client must only contact the official Seatribe server unless the user explicitly
   configures an opt-in integration ([mapper](ai-docs/subsystems/Mapper-and-MappingClient.md),
   [cookbook](ai-docs/subsystems/Cookbook-Integration.md)). Don't add new outbound network calls.
8. Persist settings via `Utils.getpref*/setpref*` + `Config`/`OptWnd`, not ad-hoc files.

Full conventions: [`Coding-Conventions`](ai-docs/guides/Coding-Conventions.md).

## Where to make common changes

| Task | Start here |
|---|---|
| Add an automation bot | `src/haven/automated/` + a keybinding in `src/haven/GameUI.java`; see [Adding-a-New-Bot](ai-docs/guides/Adding-a-New-Bot.md) |
| Add a setting/toggle | `src/haven/OptWnd.java` + `src/haven/Config.java` |
| Tweak in-game HUD | `src/haven/GameUI.java` and the relevant `*Wnd`/`*Widget` class |
| Combat behavior | `Fightview`/`Fightsess` + `haven.automated` `Aggro*` ([Combat-System](ai-docs/subsystems/Combat-System.md)) |
| Change creature targeting | `AUtils.potentialAggroTargets` in `src/haven/automated/AUtils.java` |
| Modify resource-bundled logic | fetch with `haven.Resource get-code <path>` ([Resource-System](ai-docs/architecture/Resource-System.md)) |

## Don'ts

- ❌ Don't iterate `OCache` without `synchronized`. ❌ Don't swallow `Loading` as an error.
- ❌ Don't reformat/modernize core files wholesale. ❌ Don't switch build tools.
- ❌ Don't add telemetry or new outbound connections. ❌ Don't hand-roll server packets.

## Provenance

This file and `ai-docs/` were generated by reading the codebase. Class/line references were accurate
as of `Config.clientVersion = v1.63b`. If something here conflicts with the code, **the code wins** —
update these docs.

## AI artifact map (everything generated for assistants)

| Artifact | What | Regenerate |
|---|---|---|
| [`llms.txt`](llms.txt) | Curated AI index (llmstxt.org standard). | hand-maintained |
| [`ai-docs/`](ai-docs/Home.md) | Obsidian vault: 23 linked notes + canvas + mermaid diagrams. | hand-maintained |
| [`ai-docs/reference/Bot-Index.md`](ai-docs/reference/Bot-Index.md) | Verified bot→trigger map (key / menu / context). | hand-maintained |
| [`ai-docs/reference/Class-Index.md`](ai-docs/reference/Class-Index.md) | All ~840 types → kind, supertypes, lines. | `java rag/CodeMap.java` |
| `rag/code-map.jsonl` | Full per-type API (methods/fields), machine-readable. | `java rag/CodeMap.java` |
| [`ai-docs/reference/Code-Metrics.md`](ai-docs/reference/Code-Metrics.md) | Hotspots: most-referenced (load-bearing) classes, largest files, `// ND:` density, TODO counts. | `java rag/DepGraph.java` |
| `rag/import-graph.jsonl` | Per-type project dependencies (fan-out), machine-readable. | `java rag/DepGraph.java` |
| `rag/rag-index.txt` | TF-IDF retrieval index. | `java rag/HurricaneRAG.java index` |
| `llms-full.txt` | Whole knowledge base in one file. | `java rag/HurricaneRAG.java bundle` |
| [`.github/workflows/ai-docs.yml`](.github/workflows/ai-docs.yml) | CI: validates links + rebuilds artifacts. | auto on push |

Query the docs anytime: `java rag/HurricaneRAG.java query "..."` (or `rag/ask.bat "..."`).
