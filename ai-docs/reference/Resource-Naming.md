---
title: Resource Naming Conventions
aliases: [Resource Names, Res Names, gob.getres, Naming]
tags: [reference, resources, automation]
---

# Resource Naming Conventions

Almost everything in [[Automation-Bots|automation]] is identified by a **resource name** — the
path-like string from `gob.getres().name` (a `Gob`) or `item.getres().name` (an item). Knowing the
prefixes lets you recognize creatures, items, buildings, actions, and effects without guessing.

> [!note] Source & scope
> Prefixes below are confirmed from this repo's `res/gfx/*` and `res/paginae/*` trees and from the
> creature list in `AUtils.potentialAggroTargets` (`src/haven/automated/AUtils.java`). Many full
> resources live server-side (in `hafen-res.jar`), so this is a **curated map of conventions**, not
> an exhaustive listing. Always confirm a specific name against live data (log `gob.getres().name`).

## Top-level prefixes

| Prefix | Meaning | Examples |
|---|---|---|
| `gfx/borka/` | **The player character body** (and player rendering) | `gfx/borka/body` |
| `gfx/kritter/` | **Creatures / animals** (the main aggro targets) | `gfx/kritter/bear/bear`, `gfx/kritter/horse/horse` |
| `gfx/terobjs/` | **Terrain objects** — buildings, furniture, crafting stations, plants, vehicles, placed items | `gfx/terobjs/...` |
| `gfx/invobjs/` | **Inventory item icons/objects** | `gfx/invobjs/...` |
| `gfx/hud/` | HUD / UI graphics | `gfx/hud/...` |
| `gfx/tiles/` | Ground tile graphics (terrain types) | `gfx/tiles/...` |
| `gfx/fx/` | Visual effects / overlays | `gfx/fx/...` (see `haven.res.gfx.fx.*`) |
| `paginae/` | **Actions** ("paginae") shown in the [[UI-and-Widget-System\|`MenuGrid`]] | `paginae/atk/...`, `paginae/act/...` |
| `paginae/atk/` | **Combat maneuvers / attacks** (the fight "deck") | `paginae/atk/...` (see [[Combat-System]]) |
| `ui/tt/` | Item tooltip info (quality, wear, slots, content) | `ui/tt/name`, `ui/tt/wear` (`haven.res.ui.tt.*`) |
| `ui/` | Widget resources | `ui/...` |
| `sfx/` | Sound effects | `sfx/...` |

## Creatures (`gfx/kritter/<species>/<name>`)

The structure is `gfx/kritter/<species>/<variant>`. Verified examples from
`AUtils.potentialAggroTargets`:

`adder`, `ants/ants`, `badger`, `bat/bat`, `bear/bear`, `bear/polarbear`, `beaver`, `boar`,
`boreworm`, `caveangler`, `cavelouse`, `chasmconch`, `eagleowl`, `fox`, `goat/wildgoat`,
`goldeneagle`, `greyseal`, `horse/horse`, `lynx`, `mammoth/mammoth`, `moose`, `nidbane`,
`ooze/greenooze`, `orca`, `otter`, `pelican`, `rat/caverat`, `reddeer`, `reindeer`, `roedeer`,
`spermwhale`, `stoat`, `swan`, `troll`, `walrus`, `wolf`, `wolverine`, `woodgrouse/woodgrouse-m`,
`garefowl`, `goshawk`.

> [!tip] Special cases
> Aurochs (`gfx/kritter/cattle/cattle`) and mouflon (`gfx/kritter/sheep/sheep`) are **commented out**
> of `potentialAggroTargets` and handled separately in the targeting code — they're domesticated
> look-alikes. When adding new creatures, append their `gfx/kritter/...` name to that set
> (see [[Combat-System#Aggro targets data]]).

## How bots use these names

```java
// Identify and act on a specific object type
synchronized (gui.map.glob.oc) {
    for (Gob g : gui.map.glob.oc) {
        Resource res = g.getres();                 // may be null while Loading — skip
        if (res == null) continue;
        if (res.name.equals("gfx/terobjs/dframe")) { /* a drying frame */ }
        if (res.name.startsWith("gfx/kritter/")) { /* a creature */ }
    }
}
```

- Exact match for one object: `res.name.equals("gfx/terobjs/...")`.
- Category match: `res.name.startsWith("gfx/kritter/")`.
- Membership in a known set: `AUtils.potentialAggroTargets.contains(res.name)`.
- Overlays (progress, state) are matched separately via `AUtils.gobHasOverlay(gob, overlayResName)`.

## Discovering a name you don't know

1. Log it in-game: temporarily print `gob.getres().name` (or `item.getres().name`) for the object.
2. Or fetch the resource's code to inspect it: `java -cp bin/hafen.jar haven.Resource get-code <path>`
   (see [[Resource-System]]).
3. Search existing references: `grep -rn "gfx/terobjs/" src/haven/automated`.

## Related
- [[Automation-Bots]] · [[Combat-System]] · [[Game-State-Model#`Gob` — a game object]] · [[Resource-System]]

#reference #resources #automation
