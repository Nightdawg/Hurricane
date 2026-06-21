---
title: Adding a New Bot
aliases: [New Bot, Add Bot, Bot Recipe]
tags: [guide, automation, recipe]
---

# Adding a New Bot

A step-by-step recipe for adding a Hurricane [[Automation-Bots|automation]] script. Follow the
existing pattern; do not invent a new framework.

## 1. Create the class in `src/haven/automated/`

For a **windowed, toggleable** bot, mirror `CellarDiggingBot`:

```java
package haven.automated;

import haven.*;
import haven.Button;
import haven.Window;
import static java.lang.Thread.sleep;

public class MyBot extends Window implements Runnable {
    private final GameUI gui;
    private boolean stop = false;
    private boolean active = false;
    private final Button activeButton;

    public MyBot(GameUI gui) {
        super(UI.scale(150, 70), "My Bot");
        this.gui = gui;
        activeButton = new Button(UI.scale(150), "Start") {
            public void click() {
                active = !active;
                change(active ? "Stop" : "Start");
            }
        };
        add(activeButton, UI.scale(0, 10));
        pack();
    }

    @Override public void run() {
        try {
            while (!stop) {
                if (active) {
                    // 1 unit of work: read state via gui.*, act via AUtils.* / wdgmsg
                }
                sleep(150);                  // ALWAYS sleep — polling loop, be a good net citizen
            }
        } catch (InterruptedException e) {
            // stopped via thread interrupt
        }
    }

    @Override public void destroy() {        // stop the loop when the window closes
        stop = true;
        super.destroy();
    }
}
```

For a **one-shot** action (no window), just `implements Runnable` and return from `run()` when done
(see the `Aggro*` classes / `StackAllItems`).

## 2. Use `AUtils`, never raw loops, for common tasks

- Items: `AUtils.findItemByPrefixInAllInventories(gui, "gfx/invobjs/…")`.
- Gobs: `synchronized (gui.map.glob.oc) { for (Gob g : gui.map.glob.oc) { … } }` or
  `AUtils.getGobs(name, gui)`.
- Waiting: `AUtils.waitProgBar(gui)`, `AUtils.waitForEmptyHand(gui, …)`, `AUtils.waitPf(gui)`.
- See the full toolkit in [[Automation-Bots#`AUtils` — the shared toolkit]].

## 3. Tolerate `Loading` and concurrency

- Wrap state reads that may touch unloaded [[Resource-System|resources]] in `try { … } catch
  (Loading l) { continue; }`.
- **Always** `synchronized(gui.map.glob.oc)` when iterating Gobs. See
  [[Coding-Conventions#Threading]].

## 4. Wire up a trigger in `GameUI`

Add a keybinding and launch it. In `src/haven/GameUI.java`:

```java
// declare a KeyBinding field near the other kb_* fields:
public static final KeyBinding kb_myBot = KeyBinding.get("mybot", KeyMatch.nil);

// in keydown(KeyDownEvent ev), add a branch:
} else if (kb_myBot.key().match(ev)) {
    MyBot bot = new MyBot(this);
    add(bot, /* placement */ );             // show its window (windowed bots)
    runActionThread(new Thread(bot, "MyBot"));
    return true;
}
```

- `runActionThread` ensures only one keybound action runs at a time (interrupts the previous).
- For a bot you want to run **alongside** others, keep a dedicated thread field instead (pattern:
  `lootNearestKnockedPlayerThread`).
- Expose the keybinding in `OptWnd` so users can rebind it (follow existing `kb_*` registration).

> [!tip] Alternative: expose it from the action menu
> Windowed bots (a `Window` with a Start/Stop button) are usually toggled from the custom
> `MenuGrid` menu (category `Bots`) rather than a keybinding — see how `FishingBot` etc. are added
> in `src/haven/MenuGrid.java`, and the full pattern in [[Bot-Index]]. Pick keybinding for quick
> actions, MenuGrid for persistent windowed bots.

## 5. (Optional) Register creature/object names

If your bot targets creatures, add their **resource names** to `AUtils.potentialAggroTargets` (see
[[Combat-System#Aggro targets data]]) or query `gob.getres().name` directly.

## 6. Build & smoke-test

```bash
ant jar           # fast compile check
ant run           # launch and exercise the keybinding in-game
```

## Checklist

- [ ] Class in `haven.automated`, follows Window+Runnable (or plain Runnable) pattern.
- [ ] `sleep(...)` in every loop iteration.
- [ ] `synchronized(oc)` around all Gob iteration; `Loading` tolerated.
- [ ] Actions go through `AUtils` / `wdgmsg` (no direct server packet building).
- [ ] Keybinding added in `GameUI` + exposed in `OptWnd`.
- [ ] Compiles with `ant jar`; behaves in `ant run`.

## Related
- [[Automation-Bots]] · [[Coding-Conventions]] · [[UI-and-Widget-System]] · [[Build-and-Run]]

#guide #automation
