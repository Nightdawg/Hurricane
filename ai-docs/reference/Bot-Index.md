---
title: Bot Index (verified triggers)
aliases: [Bot Index, Bot Triggers, Keybindings, Bot Launchers]
tags: [reference, automation, hurricane]
---

# Bot Index — verified triggers

How every Hurricane automation script is actually launched, extracted from `src/haven/GameUI.java`,
`src/haven/MenuGrid.java`, `src/haven/Window.java`, and `src/haven/WItem.java`. See
[[Automation-Bots]] for the pattern and [[Adding-a-New-Bot]] to add one.

> Modifier shorthand (hafen `KeyMatch`): **C** = Ctrl, **M** = Alt/Meta, **S** = Shift.
> `KeyMatch.nil` = unbound by default (user must bind it in `OptWnd`). Keys are defaults and are
> rebindable. Always confirm in source — the kb id (the `KeyBinding.get("…")` string) is the prefs key.

## Launched by keybinding (`GameUI.keydown`)

| Action / bot | Default key | Pref id (`KeyBinding.get`) | Launch | Class |
|---|---|---|---|---|
| Aggro nearest target | **Shift+Space** | `AggroNearestTargetButtonKB` | `runActionThread` | `AggroNearestTarget` |
| Aggro nearest player | unbound | `AggroNearestPlayerButtonKB` | `runActionThread` | `AggroNearestPlayer` |
| Aggro/target nearest cursor | unbound | `AggroOrTargetNearestCursorButtonKB` | `runActionThread` | `AggroOrTargetCursorNearest` |
| Aggro all non-friendly players | unbound | `AggroAllNonFriendlyPlayers` | `runActionThread` | `AggroEveryoneInRange` |
| Push player | unbound | `PushPlayerButtonKB` | `runActionThread` | `PushPlayer` |
| Combat auto-distance (lite) | **K** | `AutoCombatDistanceKB` | `runActionThread` | `CombatDistancerLite` |
| Loot nearest knocked player | **Shift+D** | `lootNearestKnockedPlayerKB` | dedicated thread | `LootNearestKnockedPlayer` |
| Interact nearest object | **Q** | `clickNearestObjectKB` | dedicated thread (`interactWithNearestObjectThread`) | `InteractWithNearestObject` |
| Interact nearest cursor object | (bindable) | `clickNearestCursorObjectKB` | dedicated thread | `InteractWithCursorNearest` |
| Enter nearest vehicle | **Ctrl+Q** | `enderNearestVehicle` *(sic)* | dedicated thread | `EnterNearestVehicle` |
| Lift nearest wagon-liftable | (bindable) | `wagonNearestLiftable…` | dedicated thread | `WagonNearestLiftable` |
| Auto re-aggro current target | **P** | `autoReaggroTarget` | `fv.current.autogive.remoteTrigger()` | (uses `Fightview`) |

Related non-bot quick actions also keybound in `GameUI`: drink (`` ` ``, `DrinkButtonKB`), search
inventories / objects, night vision, and many display toggles (hiding boxes, collision boxes,
growth/harvest/speed info, etc.). See `OptWnd.java` for the full list of `addbtnImproved(...)` entries.

## Launched from the custom action menu (`MenuGrid`, category `Bots`)

These are **windowed** bots/utilities. The menu entry **toggles** them: create + `gui.add(...)` +
start thread, or `stop()` + `reqdestroy()`. Instance + thread are stored on `GameUI`
(`gui.<field>` / `gui.<field>Thread`); window pos saved as `wndc-<bot>Window`.

| Menu action (`ad[2]`) | Class | GameUI field |
|---|---|---|
| `OceanScoutBot` | `OceanScoutBot` | `gui.OceanScoutBot` |
| `TarKilnEmptierBot` | `TarKilnCleanerBot` | `gui.tarKilnCleanerBot` |
| `FishingBot` | `FishingBot` | `gui.fishingBot` |
| `CleanupBot` | `CleanupBot` | `gui.cleanupBot` |
| `GrubGrubBot` | `GrubGrubBot` | `gui.grubGrubBot` |
| (cellar) | `CellarDiggingBot` | `gui.cellarDiggingBot` |
| (roasting) | `RoastingSpitBot` | `gui.roastingSpitBot` |
| (mining safety) | `MiningSafetyAssistant` | `gui.miningSafetyAssistantWindow` |
| (pointer triangulation) | `PointerTriangulation` | `gui.pointerTriangulation` |
| (ore/stone counter) | `OreAndStoneCounter` | `gui.oreAndStoneCounter` |
| (combat distance tool) | `CombatDistanceTool` | `gui.combatDistanceTool` |

Also launched from `MenuGrid` as **fire-and-forget threads** (via `runActionThread` or a dedicated
thread field): `AddCoalToSmelter` (9 / 12), `AddBranchesToFurnace` (4), `AddWoodBlocksToSmokeShed`
(5), `FillCheeseTray`, `CloverScript`, `CoracleScript`, `SkisScript`, `RefillWaterContainers`,
`HarvestNearestDreamcatcher`, `DestroyNearestTrellisPlantScript`, `EquipFromBelt`.

## Launched from inventory / item context

| Trigger | Class | Where |
|---|---|---|
| Inventory window right-click → stack | `StackAllItems` | `Window.java` |
| Inventory window right-click → unstack | `UnstackAllItems` | `Window.java` |
| Inventory sort | `InventorySorter` (`InventorySorter.start(...)`) | inventory context |
| Item interaction → auto-repeat flower menu | `AutoRepeatFlowerMenuScript` | `WItem.java` |

## Created directly / persistent

| Class | Where |
|---|---|
| `QuestHelper` | `GameUI` (`questhelper = new QuestHelper()`) |

## Related
- [[Automation-Bots]] · [[Adding-a-New-Bot]] · [[Combat-System]] · [[UI-and-Widget-System]]

#reference #automation #hurricane
