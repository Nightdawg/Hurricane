---
title: Cookbook Integration
aliases: [Cookbook, FoodService, Food Stats]
tags: [subsystem, integration, cookbook]
---

# Cookbook Integration — `haven.automated.cookbook`

Optional integration that submits **food/recipe stats** to a "cookbook" service (e.g. Cediner's
`hnh-food-book`, or a public cookbook). **Disabled by default.** Source:
`src/haven/automated/cookbook/FoodService.java`.

## What it does

- When you hover/inspect food, `FoodService.checkFood(List<ItemInfo> ii, Resource res, String genus)`
  parses the food's ingredients/FEPs into a `ParsedFoodInfo` and de-duplicates via a content `hash`.
- New entries are batched and sent by `sendItems()` on a `ScheduledExecutorService` (2-thread pool)
  to the configured cookbook endpoint.
- Auth: a **Bearer token** read from `OptWnd.cookBookTokenTextEntry` is attached as the
  `Authorization` header. `isValidEndpoint()` gates whether anything is sent.
- `cookbookDebug` (compile-time `false`) toggles verbose logging.

> [!info] Opt-in & privacy
> Like the [[Mapper-and-MappingClient|mapper]], this only contacts a server **you configure**. With
> no token/endpoint set, nothing is sent. You can use a public cookbook token or self-host
> `hnh-food-book` (see root `README.md`).

## Data model (in `FoodService`)

- `FoodIngredient { name; … }`, `ParsedFoodInfo`, `HashedFoodInfo { hash; foodInfo }`.
- Parses standard H&H food tooltip info (`ItemInfo`) — see [[Resource-System]] for where item
  tooltip code comes from (`haven.res.ui.tt.*`).

## Related
- [[Mapper-and-MappingClient]] · [[Resource-System]] · [[Automation-Bots]]

#subsystem #integration
