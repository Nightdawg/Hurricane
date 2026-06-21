---
title: AI Agent Playbook
aliases: [Playbook, Agent Workflow, How to work here]
tags: [guide, agent, workflow]
---

# AI Agent Playbook

A concrete workflow for an AI assistant given a task in this repo. Optimized for correctness and for
keeping the [[Project-Overview#Fork lineage|upstream-mergeable]] fork clean.

## 0. Orient (once per task)

1. Read [[Home]] (this vault's map) and the relevant subsystem note.
2. For anything you're unsure about, **query the RAG tool** instead of grepping blindly:
   ```bash
   java rag/HurricaneRAG.java query "your question"      # or: rag/ask.bat "..."
   ```
3. For symbol/structure lookups, consult the generated [[Class-Index]] (`rag/code-map.jsonl` for full
   API) and [[Code-Metrics]] (to see which classes are load-bearing / heavily customized).

## 1. Locate the code

- Feature/bot wiring → [[Bot-Index]] (verified triggers) + `grep -rn "new <Bot>" src`.
- A class → [[Class-Index]] or `glob **/<Name>.java`.
- "Where does X live?" → [[Package-Map]] and [[Common-Tasks-and-FAQ]].
- Object/resource identity → [[Resource-Naming]] (`gob.getres().name`).

## 2. Understand before editing

- Check [[Code-Metrics]] fan-in: if you're touching a high-fan-in class (`Coord`, `Utils`,
  `Resource`, `UI`, `Widget`, `Gob`, `GameUI`…), changes ripple widely — be conservative.
- Note whether the file is heavily marked `// ND:` (Hurricane-custom) vs vanilla. Search `// ND:` to
  see intentional fork behavior. Vanilla-looking core files should change as little as possible.

## 3. Make the change (respect the rules)

Follow [[Coding-Conventions]]. The five that bite hardest:

1. **Surgical & additive** in core `haven.*` (this fork merges upstream). Match the file's existing
   indentation; don't reformat.
2. Interact with the server only via **`wdgmsg`** (client→server) / **`uimsg`** (server→client).
   Never hand-build packets.
3. **`Loading` is control flow** — catch & retry, don't treat as an error.
4. **`synchronized (gui.map.glob.oc)`** around all Gob iteration; do slow work off the UI thread.
5. New automation goes in **`haven.automated.*`** following the Window+Runnable pattern
   ([[Adding-a-New-Bot]]); wire a trigger in `GameUI` (keybinding) or `MenuGrid` (windowed bot).

## 4. Validate

```bash
ant jar     # does it compile? (there is NO unit-test suite — this is your main gate)
ant run     # launch and exercise the change in-game if feasible
```

If you changed the docs, the [docs CI](../../.github/workflows/ai-docs.yml) will re-validate wikilinks
and rebuild the AI artifacts; you can do it locally too:
```bash
java rag/HurricaneRAG.java index ; java rag/CodeMap.java ; java rag/DepGraph.java
```

## 5. Don'ts (fast reference)

❌ Don't switch build tools (it's **Ant**). ❌ Don't add telemetry / new outbound calls.
❌ Don't iterate `OCache` unsynchronized. ❌ Don't swallow `Loading`. ❌ Don't rewrite/reformat core
files wholesale. ❌ Don't hand-roll server packets.

## When stuck

- Re-query the RAG tool with different keywords (it's lexical — try synonyms).
- Read the actual source the docs cite; **the code is the source of truth** (docs note this
  everywhere). If docs are wrong, fix the code first, then the docs.
- The behavior may live in a **resource**, not `src/` — see [[Resource-System]].

## Related
- [[Home]] · [[Coding-Conventions]] · [[Common-Tasks-and-FAQ]] · [[Bot-Index]] · [[Code-Metrics]]

#guide #agent
