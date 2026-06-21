---
title: Code Metrics (generated)
aliases: [Code Metrics, Hotspots, Complexity]
tags: [reference, generated]
---

# Code Metrics (generated)

> [!warning] Generated file — do not edit by hand.
> Regenerate with `java rag/DepGraph.java`. Reference detection is heuristic (whole-word
> simple-name matching); same-name types across packages are conflated. Full per-type fan-out is in
> `rag/import-graph.jsonl`.

**Totals:** 840 types · 182,091 source lines · 341 `// ND:` Hurricane markers · 144 TODO/FIXME/XXX/HACK.

See [[Package-Map]], [[Key-Classes]], [[Class-Index]].

## Most-referenced types (fan-in) — the load-bearing classes

How many files reference each type. High fan-in = change with extra care.

| Type | Referenced by (files) |
|---|---|
| `Coord` | 293 |
| `Utils` | 271 |
| `Resource` | 232 |
| `UI` | 162 |
| `GOut` | 158 |
| `Widget` | 154 |
| `Pipe` | 150 |
| `Gob` | 136 |
| `Tex` | 122 |
| `Loading` | 122 |
| `FromResource` | 100 |
| `Message` | 99 |
| `Text` | 98 |
| `GameUI` | 93 |
| `Indir` | 87 |
| `OptWnd` | 86 |
| `Coord3f` | 83 |
| `TexI` | 79 |
| `Coord2d` | 78 |
| `State` | 74 |
| `RenderTree` | 72 |
| `Area` | 70 |
| `Expression` | 70 |
| `Render` | 65 |
| `PUtils` | 65 |
| `Window` | 62 |
| `Type` | 57 |
| `ItemInfo` | 55 |
| `DataBuffer` | 55 |
| `ShaderMacro` | 53 |
| `VectorFormat` | 52 |
| `Config` | 50 |
| `Button` | 49 |
| `Sprite` | 49 |
| `NumberFormat` | 49 |

## Largest types by line count

| Type | Package | Lines |
|---|---|---|
| `OptWnd` | `haven` | 5636 |
| `MapView` | `haven` | 3335 |
| `GameUI` | `haven` | 3159 |
| `Utils` | `haven` | 2916 |
| `Gob` | `haven` | 2743 |
| `Resource` | `haven` | 2412 |
| `MapFile` | `haven` | 2041 |
| `Widget` | `haven` | 2021 |
| `ChatUI` | `haven` | 1942 |
| `JSONObject` | `org.json` | 1842 |
| `MiniMap` | `haven` | 1733 |
| `VorbisFile` | `com.jcraft.jorbis` | 1397 |
| `MapWnd` | `haven` | 1356 |
| `Skeleton` | `haven` | 1342 |
| `Drft` | `com.jcraft.jorbis` | 1327 |
| `Fightsess` | `haven` | 1314 |
| `MCache` | `haven` | 1301 |
| `GobIcon` | `haven` | 1294 |
| `Config` | `haven` | 1259 |
| `CheckpointManager` | `haven` | 1194 |
| `UI` | `haven` | 1155 |
| `MenuGrid` | `haven` | 1147 |
| `JSONArray` | `org.json` | 1130 |
| `AWTToolkit` | `haven.iosys.tk` | 1122 |
| `BGL` | `haven.render.gl` | 1120 |
| `GLDrawList` | `haven.render.gl` | 1092 |
| `GLEnvironment` | `haven.render.gl` | 1050 |
| `FightWnd` | `haven` | 1048 |
| `Window` | `haven` | 989 |
| `ExtInventory` | `haven` | 927 |
| `RenderTree` | `haven.render` | 926 |
| `RichText` | `haven` | 910 |
| `InstanceList` | `haven.render` | 884 |
| `DynresWindow` | `haven` | 881 |
| `Connection` | `haven` | 867 |

## Hurricane change density — files with the most `// ND:` markers

`// ND:` comments mark intentional Hurricane (Nightdawg) behavior. High counts = heavily customized.

| File | `// ND:` markers | Lines |
|---|---|---|
| `src/haven/OptWnd.java` | 28 | 5636 |
| `src/haven/Gob.java` | 26 | 2743 |
| `src/haven/Config.java` | 19 | 1259 |
| `src/haven/MapView.java` | 17 | 3335 |
| `src/haven/GameUI.java` | 16 | 3159 |
| `src/haven/LoginScreen.java` | 14 | 759 |
| `src/haven/automated/CoracleScript.java` | 13 | 244 |
| `src/haven/Window.java` | 13 | 989 |
| `src/haven/MapWnd.java` | 9 | 1356 |
| `src/haven/automated/AggroNearestTarget.java` | 6 | 158 |
| `src/haven/automated/AUtils.java` | 6 | 448 |
| `src/haven/automated/SkisScript.java` | 6 | 151 |
| `src/haven/Equipory.java` | 6 | 556 |
| `src/haven/GItem.java` | 6 | 798 |
| `src/haven/GobReadyForHarvestInfo.java` | 6 | 187 |
| `src/haven/automated/EnterNearestVehicle.java` | 5 | 118 |
| `src/haven/GobIcon.java` | 5 | 1294 |
| `src/haven/IMeter.java` | 5 | 204 |
| `src/haven/MiniMap.java` | 5 | 1733 |
| `src/haven/automated/AggroOrTargetCursorNearest.java` | 4 | 114 |
| `src/haven/automated/CloverScript.java` | 4 | 142 |
| `src/haven/automated/StackAllItems.java` | 4 | 93 |
| `src/haven/CheckpointManager.java` | 4 | 1194 |
| `src/haven/ExtInventory.java` | 4 | 927 |
| `src/haven/Fightsess.java` | 4 | 1314 |

## Packages by total source lines

| Package | Types | Lines |
|---|---|---|
| `haven` | 377 | 113,547 |
| `haven.render.gl` | 34 | 8,601 |
| `com.jcraft.jorbis` | 31 | 8,284 |
| `haven.render` | 55 | 7,737 |
| `haven.automated` | 40 | 7,135 |
| `org.json` | 16 | 6,060 |
| `haven.render.sl` | 65 | 4,726 |
| `haven.resutil` | 18 | 3,771 |
| `haven.iosys.tk` | 17 | 3,634 |
| `haven.automated.pathfinder` | 9 | 1,529 |
| `haven.sprites` | 20 | 1,431 |
| `com.jcraft.jogg` | 5 | 1,277 |
| `haven.error` | 7 | 991 |
| `haven.res.ui.music` | 4 | 793 |
| `haven.render.jogl` | 6 | 777 |
| `haven.automated.mapper` | 3 | 733 |
| `haven.res.ui.croster` | 7 | 590 |
| `haven.test` | 7 | 560 |
| `haven.iosys.audio` | 3 | 553 |
| `haven.rs` | 4 | 485 |
| `haven.render.lwjgl` | 4 | 471 |
| `dolda.coe` | 5 | 455 |
| `dolda.xiphutil` | 6 | 450 |
| `haven.widgets` | 3 | 443 |
| `haven.automated.helpers` | 3 | 374 |
| `haven.res.gfx.fx.mscover` | 5 | 357 |
| `haven.res.lib.tree` | 6 | 351 |
| `haven.res.ui.barterbox` | 1 | 345 |
| `haven.res.ui.tt.attrmod` | 10 | 332 |
| `haven.res.lib.leaves` | 1 | 320 |

#reference #generated
