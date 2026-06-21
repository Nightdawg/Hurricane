---
title: Mapper and MappingClient
aliases: [Mapper, MappingClient, Web Map]
tags: [subsystem, integration, mapping]
---

# Mapper — `haven.automated.mapper`

Optional integration that uploads explored map **grids** to a **private web-map server** (e.g.
Cediner's `hnh-map-vuetify`, or dafels' mapping service). **Disabled unless configured.** Source:
`src/haven/automated/mapper/`.

> [!info] Privacy
> Per the README, the client sends data **only** to the official Seatribe server *unless you set it
> to do so*. The mapper is exactly such an opt-in: it talks to a map-server **endpoint you provide**.

## Files

| File | Role |
|---|---|
| `MappingClient.java` | Singleton client. Lifecycle: `init(Glob)`, `getInstance()`, `initialized()`, `destroy()`. Tracks the player, detects new/updated map grids, and POSTs them to the configured endpoint. |
| `MinimapImageGenerator.java` | Renders map grids to minimap PNG tiles for upload. |
| `MultipartUtility.java` | Helper for `multipart/form-data` HTTP uploads. |

## How it hooks in

- `MappingClient.init(glob)` is called once a [[Game-State-Model|`Glob`]] exists (referenced from
  `Config` / [[UI-and-Widget-System|`GameUI`]]); `destroy()` tears it down on logout.
- It converts world coordinates to grid coordinates (`toGridCoordinate`, `gridOffset2`) and uploads
  grid images + position info so the web map can stitch the world together.
- Endpoint URL / auth are configured through the options UI (`OptWnd`) and `Config`.

## Related
- [[Game-State-Model]] · [[Cookbook-Integration]] · [[Automation-Bots]]
- External servers: `hnh-map-vuetify` (Cediner), dafels' mapping service (see root `README.md`).

#subsystem #integration
