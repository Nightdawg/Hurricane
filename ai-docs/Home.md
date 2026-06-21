---
title: Hurricane Client — AI Knowledge Base
aliases: [Home, MOC, Index, Start Here]
tags: [moc, hurricane, haven-and-hearth]
---

# 🌀 Hurricane Client — AI Knowledge Base

> [!info] What is this?
> This is an **AI-readable knowledge base** (an Obsidian-style vault) for the **Hurricane** client —
> a custom Java client for the game *Haven & Hearth*, forked from Loftar's "Vanilla" `hafen-client`.
> It is written **for AI coding assistants** (and humans) to understand the codebase quickly.
> Notes are interlinked with `[[wikilinks]]`. Start here and follow the links.

## 🚀 Start Here

- [[Project-Overview]] — what Hurricane is, lineage, and the 30-second mental model
- [[Architecture-Overview]] — how the pieces fit together (the big picture)
- [[Build-and-Run]] — how to compile and launch the client
- [[Common-Tasks-and-FAQ]] — quick recipes & "how do I…" answers
- [[Glossary]] — domain & codebase vocabulary (read this if terms confuse you)
- 🗺️ **[[Architecture-Map.canvas|Visual map (Obsidian Canvas)]]** — open in Obsidian for a clickable board

## 🧭 Map of Content

### Architecture
- [[Architecture-Overview]]
- [[Startup-and-Lifecycle]] — `main()` → threads → login → in-game
- [[Networking-and-Protocol]] — Session / Connection / Message wire format
- [[Game-State-Model]] — `Glob`, `OCache`, `Gob`, `MCache`
- [[UI-and-Widget-System]] — `Widget` tree, `wdgmsg` protocol, `GameUI`
- [[Rendering-Pipeline]] — `haven.render`, GL/JOGL/LWJGL backends, shaders
- [[Resource-System]] — `.res` files, `haven.res.*`, `FromResource`

### Subsystems (Hurricane's custom additions)
- [[Automation-Bots]] ⭐ — the `haven.automated` package (bots/scripts)
- [[Bot-Index]] — verified trigger map (keybinding / menu / context) for every bot
- [[Pathfinding]] — A* navigation engine
- [[Mapper-and-MappingClient]] — web-map grid uploads
- [[Cookbook-Integration]] — food-stats service
- [[Combat-System]] — fight UI, aggro, targeting

### Guides
- [[Build-and-Run]]
- [[AI-Agent-Playbook]] — step-by-step working method for an AI agent
- [[Adding-a-New-Bot]] — step-by-step recipe
- [[Coding-Conventions]] — patterns you MUST follow in this codebase
- [[Common-Tasks-and-FAQ]] — quick recipes & "how do I…" answers

### Reference
- [[Package-Map]] — every package and what lives in it
- [[Key-Classes]] — the ~40 classes you'll touch most
- [[Class-Index]] — generated index of all ~840 types (kind, supertypes, line counts)
- [[Code-Metrics]] — generated hotspots: load-bearing classes, largest files, `// ND:` density
- [[Resource-Naming]] — decode `gob.getres().name` (creatures, items, objects, actions)
- [[Glossary]]

## 🤖 For AI agents — read this first

> [!important] Operating rules
> 1. This is a **fork** with two upstreams. Hurricane-specific code is mostly in
>    `haven.automated.*` and scattered additions to core `haven.*` classes. Do **not**
>    assume a change belongs to vanilla — see [[Project-Overview#Fork lineage]].
> 2. Build with **Apache Ant**, not Maven/Gradle. See [[Build-and-Run]].
> 3. Follow the established [[Coding-Conventions]] (8-space-ish tab indent in core files,
>    the `wdgmsg` message protocol, `Loading` exceptions, `Indir`/`Defer` laziness).
> 4. Much game logic lives in **resources**, not this source tree. See [[Resource-System]].
> 5. There is a companion **RAG retrieval tool** in `rag/` you can query for relevant
>    snippets — see `rag/README.md`.
>
> **New here? Follow the [[AI-Agent-Playbook]] for a step-by-step working method.**

#hurricane #moc
