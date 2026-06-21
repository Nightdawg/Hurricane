---
title: Common Tasks and FAQ
aliases: [FAQ, Common Tasks, Recipes, How do I]
tags: [guide, faq, recipes]
---

# Common Tasks & FAQ

Fast answers + copy-pasteable recipes. Grounded in the actual codebase; verify the exact API in
source before relying on it. See also [[Coding-Conventions]] and [[Key-Classes]].

## "How do I…"

### …find where a feature/bot is wired up?
Bots are launched from **four** places: `GameUI` (keybindings), `MenuGrid` (the custom action
menu — primary for windowed bots), `Window.java` (inventory context menus), and `WItem.java` (item
interactions). Search for the keybinding or class name:
```bash
grep -rn "kb_" src/haven/GameUI.java            # keybinding branches
grep -rn "new FishingBot" src/haven             # who constructs a given bot
grep -rn "haven.automated" src/haven/*.java     # all references to automation
```
Then read the matching branch in `GameUI.keydown(...)` or `MenuGrid`. The full verified trigger map
is in [[Bot-Index]]; see also [[Automation-Bots#How bots are triggered]].

### …make the character do something (move, click, select)?
Go through the widget message protocol — never build packets. Examples:
```java
gui.map.wdgmsg("click", pixelCoord, worldCoord.floor(), button, modflags);   // click the world
gui.map.wdgmsg("click", ...);                                                // move / interact
someInventory.wdgmsg("drop", slotCoord);                                     // inventory op
flowerMenuPetal.wdgmsg("cl", index);                                         // pick a menu option
```
See [[UI-and-Widget-System#Two message directions]].

### …identify what a Gob is (tree? bear? player?)
```java
synchronized (gui.map.glob.oc) {
    for (Gob g : gui.map.glob.oc) {
        if (g.getres() == null) continue;          // may be Loading; skip
        String name = g.getres().name;             // e.g. "gfx/kritter/bear/bear"
        // match against AUtils.potentialAggroTargets, etc.
    }
}
```
See [[Game-State-Model#`Gob` — a game object]] and [[Combat-System#Aggro targets data]].

### …read player stats (stamina / health / energy)?
```java
double stam = gui.getmeter("stam", 0).a;            // 0..1
double hp   = gui.getmeters("hp").get(1).a;         // 0..1 (soft hp)
double nrj  = gui.getmeter("nrj", 0).a;             // 0..1 (energy)
boolean busy = gui.prog != null;                    // a progress bar is active
```

### …add a new setting/toggle?
1. Add a field + read/write in `src/haven/Config.java` (use `Utils.getprefb/setprefb`, or a
   `Config.Variable`).
2. Add a checkbox/control in `src/haven/OptWnd.java` next to the existing ones.
3. Read it where needed. Persisted under `%APPDATA%/Haven and Hearth` (or `~/.haven`).

### …add a new bot?
Follow [[Adding-a-New-Bot]] — `Window implements Runnable` in `haven.automated`, a keybinding in
`GameUI`, and use [[Automation-Bots#`AUtils` — the shared toolkit|AUtils]] helpers.

### …fetch/modify game logic that isn't in `src/`?
It lives in a resource. Fetch its Java source:
```bash
java -cp bin/hafen.jar haven.Resource get-code ui/tt/name   # extracts to src/haven/res/...
java -cp bin/hafen.jar haven.Resource find-updates          # list outdated fetched code
```
See [[Resource-System]] (and **commit/stash first** — `get-code` overwrites).

### …build / run / check that my change compiles?
```bash
ant jar     # fast compile + package (use as the "does it compile?" check)
ant run     # build bin/ and launch the client
ant         # full build into bin/
```
See [[Build-and-Run]].

### …query the docs with the RAG tool?
```bash
java rag/HurricaneRAG.java index
java rag/HurricaneRAG.java query "how does pathfinding avoid obstacles?"
```
See `rag/README.md`.

## "Why is…"

### …my code throwing `haven.Loading`?
That's **normal**. Resource data is loaded lazily; touching it before it's ready throws `Loading`.
It's control flow, not a bug — catch it and retry next tick/loop. See
[[Coding-Conventions#`Loading` exceptions are control flow]].

### …iterating the world list crashing with `ConcurrentModificationException`?
You iterated `OCache` without locking. The network thread mutates it. Wrap iteration in
`synchronized (gui.map.glob.oc) { ... }`. See [[Coding-Conventions#Threading]].

### …the first `ant` build failing on downloads?
The build fetches JOGL/LWJGL/steamworks + resource jars from `havenandhearth.com/java` on first run.
You need internet for the first build; later builds are offline. See [[Build-and-Run]].

### …the window titled "Hurricane (v1.63b)"?
Set in `Client.Main` from `Config.clientVersion`. See [[Startup-and-Lifecycle]].

## "Where is…"

| Thing | Location |
|---|---|
| Entry point / `main()` | `src/haven/Client.java` |
| In-game hub + bot keybindings | `src/haven/GameUI.java` |
| Settings UI / toggles | `src/haven/OptWnd.java`, `src/haven/Config.java` |
| Bot scripts | `src/haven/automated/` |
| Shared bot helpers | `src/haven/automated/AUtils.java` |
| World objects / map | `Gob`, `OCache`, `MCache`, `Glob` |
| Networking | `Session`, `Connection`, `Message`/`PMessage`/`RMessage` |
| Creature target list | `AUtils.potentialAggroTargets` |
| Resource-bundled code | `src/haven/res/**` (`@FromResource`) |
| User config on disk | `%APPDATA%/Haven and Hearth` or `~/.haven` |

## Related
- [[Coding-Conventions]] · [[Adding-a-New-Bot]] · [[Key-Classes]] · [[Glossary]] · [[Home]]

#guide #faq
