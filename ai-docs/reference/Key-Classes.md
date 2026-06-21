---
title: Key Classes
aliases: [Key Classes, Important Classes, Cheat Sheet]
tags: [reference, classes]
---

# Key Classes

The classes you will touch or read most, with size and one-liner. Line counts are approximate.

## Entry & lifecycle
| Class | ~Lines | Role |
|---|---|---|
| `haven.Client` | 425 | `main()`, window, UI loop driver, connect(). → [[Startup-and-Lifecycle]] |
| `haven.Bootstrap` | 301 | Login/auth flow, server props, replay/servargs entry. |
| `haven.Config` | 1193 | Config + CLI parsing; `clientVersion="v1.63b"`, `confid="Hurricane"`. |
| `haven.RemoteUI` | — | Binds a `Session` to a `UI` (the in-game runner). |

## Networking
| Class | ~Lines | Role |
|---|---|---|
| `haven.Session` | 302 | One game session; owns `Glob`; `MSG_*`/`SESSERR_*` constants; `PVER=31`. |
| `haven.Connection` | 809 | UDP transport + reliability + optional crypto. |
| `haven.Message`/`PMessage`/`RMessage` | — | Wire message buffers (datagram / reliable). |
| `haven.AuthClient` | — | Auth-server protocol (password/token creds). |

## Game state
| Class | ~Lines | Role |
|---|---|---|
| `haven.Glob` | 483 | Root world state: `oc`, `map`, astronomy, party, lighting. |
| `haven.OCache` | 581 | Object cache (all `Gob`s); **synchronize to iterate**. |
| `haven.Gob` | 2536 | A world object: position, `GAttrib`s, overlays, resource. |
| `haven.MCache` | 1182 | Tiled map cache (grids/tiles/height); `cmaps`, `tilesz`. |
| `haven.MapFile` | — | Persisted explored map. |

## UI
| Class | ~Lines | Role |
|---|---|---|
| `haven.Widget` | 1803 | Base of the UI tree; `wdgmsg`/`uimsg`/`tick`/`draw`/input. |
| `haven.UI` | 1002 | Dispatch root; holds session, console, keybindings. |
| `haven.GameUI` | 2939 | In-game hub; **most-edited core class**; bot keybindings live here. |
| `haven.MapView` | 3064 | 3D world widget; rendering + click-mapping + pathfinder thread. |
| `haven.OptWnd` | — | Settings window; most Hurricane toggles + keybindings. |
| `haven.MenuGrid` / `haven.FlowerMenu` | — | Action grid / radial menu (+ auto-select). |

## Rendering & resources
| Class | ~Lines | Role |
|---|---|---|
| `haven.Resource` | 2187 | Resource loading/caching + `get-code`/`find-updates` CLI. |
| `haven.render.Render` / `Pipe` / `Environment` | — | Backend-agnostic render API. → [[Rendering-Pipeline]] |
| `haven.GOut` | — | 2D HUD drawing context. |
| `haven.Sprite` / `Composite` / `ResDrawable` | — | Drawables resolved from resources. |

## Hurricane automation (the custom layer)
| Class | ~Lines | Role |
|---|---|---|
| `haven.automated.AUtils` | ~450 | Shared bot toolkit (targeting, items, waits, gobs). → [[Automation-Bots]] |
| `haven.automated.FishingBot` | ~570 | Representative full Window+Runnable bot. |
| `haven.automated.CellarDiggingBot` | ~234 | Canonical bot template (see [[Adding-a-New-Bot]]). |
| `haven.automated.pathfinder.Pathfinder` | — | A* navigation runner. → [[Pathfinding]] |
| `haven.automated.helpers.HitBoxes` | — | Collision boxes (SQLite `hitboxes.db`). |
| `haven.automated.mapper.MappingClient` | — | Web-map grid upload. → [[Mapper-and-MappingClient]] |
| `haven.automated.cookbook.FoodService` | — | Food-stats upload. → [[Cookbook-Integration]] |

## Related
- [[Package-Map]] · [[Architecture-Overview]] · [[Home]]

#reference #classes
