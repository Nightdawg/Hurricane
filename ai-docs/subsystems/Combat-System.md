---
title: Combat System
aliases: [Combat, Fighting, Aggro, FightView]
tags: [subsystem, combat]
---

# Combat System

H&H combat is a "deck of maneuvers" duel system. The client side is split between **vanilla fight
UI** and **Hurricane combat automation**. This note maps both.

## Vanilla fight UI (core `haven.*`)

| Class | Role |
|---|---|
| `Fightview` | The combat HUD: list of relatives/opponents in the fight (`fv.lsrel`), current target (`fv.current`), give/peace controls. Accessed as `gui.fv` on [[UI-and-Widget-System\|`GameUI`]]. |
| `Fightsess` | The active fight session: maneuvers, cooldowns, the move "deck", openings. |
| `FightWnd` | The combat-school / maneuver-configuration window. |
| `Fightview.Relation` | A single combatant relation entry (`fv.current` is one), with `autogive`. |

Useful access points (seen in [[UI-and-Widget-System|`GameUI`]]):
- `gui.fv.current` — current target relation; `gui.fv.current.autogive.remoteTrigger()` re-aggros.
- `gui.fv.targetNearestFoe()` — target nearest enemy.
- `peaceCurrentTarget()` — sends `give`/peace `wdgmsg` to disengage.

## Hurricane combat automation (`haven.automated.*`)

Triggered by [[Automation-Bots#How bots are triggered|keybindings]] in `GameUI`, run via
`runActionThread(...)`:

| Script | Behavior |
|---|---|
| `AggroNearestTarget` | Aggro the nearest valid foe (prioritizes by type; uses `AUtils.potentialAggroTargets`). |
| `AggroNearestPlayer` | Aggro only the nearest **player** (PvP). |
| `AggroEveryoneInRange` | Aggro all non-friendly players in range (group engage). |
| `AggroOrTargetCursorNearest` | Aggro/target whatever is nearest the cursor. |
| `AttackOpponent` | Attack the current opponent. |
| `CombatDistanceTool` | Visualize combat ranges. |
| `CombatDistancerLite` | Lightweight auto-distancing (keep ideal range). |

Targeting relies on [[Automation-Bots|`AUtils`]] helpers: `getAllAttackableMap`,
`getAllAttackablePlayersMap`, `attackGob`, and the `potentialAggroTargets` resource-name set.
Friend/foe checks use party/buddy data (`Party`, `BuddyWnd`) and `Gob.isPlgob(gui)`.

## Aggro targets data

`AUtils.potentialAggroTargets` is a hard-coded `HashSet<String>` of creature **resource names**
(e.g. `gfx/kritter/bear/bear`, `gfx/kritter/troll/troll`). Aurochs/mouflon are handled specially in
code (commented out of the set). Add new creatures here when the game adds them.

## Related
- [[Automation-Bots]] · [[Game-State-Model]] · [[UI-and-Widget-System]]

#subsystem #combat
