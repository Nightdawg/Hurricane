---
title: Pathfinding
aliases: [Pathfinding, Pathfinder, A-star, AStar]
tags: [subsystem, automation, navigation]
---

# Pathfinding — `haven.automated.pathfinder`

A from-scratch **A\*** navigation engine used by [[Automation-Bots|bots]] (and movement helpers) to
walk the character around obstacles. Source: `src/haven/automated/pathfinder/`.

## Files

| File | Role |
|---|---|
| `Pathfinder.java` | Orchestrator. `implements Runnable` — runs on its own thread, computes a path and walks it. |
| `AStar.java` | The A* search algorithm over the obstacle graph. |
| `Map.java` | Builds the navigation graph for a given `src → dest` from the [[Game-State-Model\|`MCache`]] + obstacles. |
| `Vertex.java`, `Edge.java` | Graph primitives. |
| `TraversableObstacle.java` | Obstacle model (footprint of a Gob you must route around). |
| `PFListener.java` | Callback interface: `pfDone(Pathfinder)` when a path completes. |
| `Utils.java`, `Dbg.java` | Helpers / debug drawing. |

## How it works (from `Pathfinder.run()` / `pathfind()`)

1. Constructed with a `MapView`, a destination `Coord`, and an `action` (the click action to send
   on arrival, e.g. interact). A second constructor also takes a target `Gob`, `meshid`, `clickb`
   (mouse button), and `modflags` (key modifiers) for "go to and click this object."
2. Caches `oc = mv.glob.oc` and `map = mv.glob.map`.
3. `run()` loops: `pathfind(player.rc.floor())`, repeating while `moveinterupted && !terminate`
   (re-plans if movement got interrupted), then `notifyListeners()`.
4. `pathfind(src)` builds a `Map(src, dest, map)`, then **`synchronized(oc)`** iterates every `Gob`,
   skipping the player and the target, and adds each as an obstacle using **collision boxes** from
   `HitBoxes.collisionBoxMap` (keyed by resource name; see [[Automation-Bots#helpers]]).
5. A* finds a route; the path is sent as a series of map clicks (`mv.wdgmsg("click", …)`); waypoints
   are exposed via `pathWaypoints` for debug rendering.

## Integration points

- Owned/driven via the `MapView` (`gui.map.pfthread` in [[Automation-Bots|AUtils]] /
  [[UI-and-Widget-System|GameUI]] context). Bots call `AUtils.waitPf(gui)` to block until a path
  finishes.
- Depends on `HitBoxes` (SQLite `hitboxes.db`) for accurate obstacle footprints —
  see [[Resource-System]] and `Client` static init in [[Startup-and-Lifecycle]].
- `RESPONSE_TIMEOUT = 800` ms governs how long it waits for movement responses; it retries
  interrupted moves up to `interruptedRetries` times.

## Related
- [[Automation-Bots]] · [[Game-State-Model]] · [[Rendering-Pipeline]]

#subsystem #navigation
