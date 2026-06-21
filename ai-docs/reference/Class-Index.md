---
title: Class Index (generated)
aliases: [Class Index, Symbol Index, Code Map]
tags: [reference, generated]
---

# Class Index (generated)

> [!warning] Generated file — do not edit by hand.
> Regenerate with `java rag/CodeMap.java` (or `rag/codemap.bat`). Heuristic parse; the
> source file is always the source of truth. Full machine-readable data (incl. methods)
> is in `rag/code-map.jsonl`.

Total: **840 types** across **68 packages**. See [[Package-Map]] and [[Key-Classes]].

Each row: type, kind, supertypes, line count. File path = `src/<package-as-path>/<Name>.java`.

## `(default)`  (8)

| Type | Kind | Extends / Implements | Lines |
|---|---|---|---|
| `Armpen` | class | → WeaponInfo | 25 |
| `Coolmod` | class | → WeaponInfo | 25 |
| `Damage` | class | → WeaponInfo | 25 |
| `Fadein` | class | → Widget | 94 |
| `Fadeout` | class | → AWidget | 49 |
| `Grievous` | class | → WeaponInfo | 25 |
| `Range` | class | → WeaponInfo | 25 |
| `Shield` | class | : Sprite.Factory | 22 |

## `com.jcraft.jogg`  (5)

| Type | Kind | Extends / Implements | Lines |
|---|---|---|---|
| `Buffer` | class |  | 294 |
| `Packet` | class |  | 47 |
| `Page` | class |  | 135 |
| `StreamState` | class |  | 526 |
| `SyncState` | class |  | 275 |

## `com.jcraft.jorbis`  (31)

| Type | Kind | Extends / Implements | Lines |
|---|---|---|---|
| `Block` | class |  | 128 |
| `ChainingExample` | class |  | 69 |
| `CodeBook` | class |  | 478 |
| `Comment` | class |  | 243 |
| `DecodeExample` | class |  | 324 |
| `Drft` | class |  | 1327 |
| `DspState` | class |  | 374 |
| `Floor0` | class | → FuncFloor | 335 |
| `Floor1` | class | → FuncFloor | 611 |
| `FuncFloor` | class |  | 52 |
| `FuncMapping` | class |  | 45 |
| `FuncResidue` | class |  | 46 |
| `FuncTime` | class |  | 45 |
| `Info` | class |  | 470 |
| `InfoMode` | class |  | 34 |
| `InternSet` | class |  | 65 |
| `JOrbisException` | class | → Exception | 40 |
| `Lookup` | class |  | 152 |
| `Lpc` | class |  | 188 |
| `Lsp` | class |  | 107 |
| `Mapping0` | class | → FuncMapping | 375 |
| `Mdct` | class |  | 250 |
| `PsyInfo` | class |  | 74 |
| `PsyLook` | class |  | 42 |
| `Residue0` | class | → FuncResidue | 330 |
| `Residue1` | class | → Residue0 | 45 |
| `Residue2` | class | → Residue0 | 41 |
| `StaticCodeBook` | class |  | 515 |
| `Time0` | class | → FuncTime | 52 |
| `Util` | class |  | 30 |
| `VorbisFile` | class |  | 1397 |

## `dolda.coe`  (5)

| Type | Kind | Extends / Implements | Lines |
|---|---|---|---|
| `BinaryData` | interface |  | 27 |
| `BinEncoder` | class |  | 211 |
| `ObjectData` | interface |  | 109 |
| `Symbol` | class |  | 97 |
| `Unencodable` | class |  | 11 |

## `dolda.xiphutil`  (6)

| Type | Kind | Extends / Implements | Lines |
|---|---|---|---|
| `FormatException` | class | → java.io.IOException | 19 |
| `OggException` | class | → FormatException | 12 |
| `PacketStream` | class |  | 73 |
| `PageStream` | class |  | 68 |
| `VorbisException` | class | → FormatException | 12 |
| `VorbisStream` | class |  | 266 |

## `haven`  (377)

| Type | Kind | Extends / Implements | Lines |
|---|---|---|---|
| `AccountList` | class | → Widget | 223 |
| `ACheckBox` | class | → Widget | 79 |
| `ActAudio` | class | → State | 400 |
| `AlarmManager` | class |  | 137 |
| `AlarmWindow` | class | → Window | 460 |
| `AnimGSprite` | class | → GSprite : GSprite.ImageSprite | 77 |
| `AnimSprite` | class | → Sprite | 97 |
| `AnimWidget` | interface |  | 155 |
| `Area` | class | : Iterable<Coord>, java.io.Serializable | 169 |
| `Argon2` | class |  | 340 |
| `ArrayIdentity` | class |  | 102 |
| `Astronomy` | class |  | 92 |
| `AttrBonusesWdg` | class | → Widget : ItemInfo.Owner | 214 |
| `AttributedStringBuffer` | class |  | 101 |
| `Audio` | class |  | 694 |
| `AudioSprite` | class |  | 229 |
| `AuthClient` | class | : Closeable | 543 |
| `AutoDropManagerWindow` | class | → Window | 156 |
| `AutoGiveButton` | class | → Widget | 87 |
| `Avatar` | class | → GAttrib | 78 |
| `Avaview` | class | → PView | 289 |
| `AWidget` | class | → Widget | 42 |
| `AWTCompat` | class |  | 99 |
| `BackCache` | class |  | 88 |
| `BarrelContentsGobInfo` | class | → GobInfo | 113 |
| `BaseFileCache` | class | : ResCache | 231 |
| `BAttrWnd` | class | → Widget | 544 |
| `BinHeap` | class | → AbstractCollection<T> : Queue<T> | 160 |
| `Blake2b` | class | : Digest.Algorithm | 253 |
| `BMap` | interface | → Map<K, V> | 33 |
| `Bootstrap` | class | : UI.Receiver, UI.Runner | 321 |
| `BuddyWnd` | class | → Widget : Iterable<BuddyWnd.Buddy> | 685 |
| `Buff` | class | → Widget : ItemInfo.ResOwner, Bufflist.Managed | 217 |
| `Bufflist` | class | → Widget | 115 |
| `BuildManifest` | class |  | 148 |
| `Button` | class | → SIWidget | 229 |
| `CachedFunction` | class | : Function<P, R> | 72 |
| `CacheMap` | class | → AbstractMap<K, V> | 202 |
| `Cal` | class | → Widget | 100 |
| `Charlist` | class | → Widget | 368 |
| `CharWnd` | class | → Window | 487 |
| `ChatUI` | class | → Widget | 1942 |
| `Chatwindow` | class | → Window : Console.Host | 143 |
| `ChatWnd` | class | → Window | 58 |
| `CheckBox` | class | → ACheckBox | 107 |
| `CheckpointManager` | class | → Window : Runnable | 1194 |
| `CircleFadein` | class | → Widget | 69 |
| `Clickable` | class | → State | 50 |
| `ClickData` | class |  | 58 |
| `ClickLocation` | class | → Texture.Image> extends State | 75 |
| `Client` | class | : Console.Directory | 453 |
| `ClipAmbiance` | class | : RenderTree.Node | 296 |
| `CloudShadow` | class | → State | 107 |
| `CollisionBox` | class | → SlottedNode : Rendered | 291 |
| `ColorMask` | class | → State | 61 |
| `ColorOptionWidget` | class | → Widget | 81 |
| `CompImage` | class |  | 168 |
| `Composite` | class | → Drawable : EquipTarget | 334 |
| `Composited` | class | : RenderTree.Node, EquipTarget | 569 |
| `Config` | class |  | 1259 |
| `Connection` | class | : Transport | 867 |
| `Console` | class |  | 123 |
| `ConsoleHost` | class | → Widget : Console.Host, ReadLine.Owner | 143 |
| `Coord` | class | : Comparable<Coord>, java.io.Serializable | 282 |
| `Coord2d` | class | : Comparable<Coord2d>, java.io.Serializable | 240 |
| `Coord3f` | class |  | 203 |
| `Coordf` | class |  | 22 |
| `CoordNd` | class | : java.io.Serializable | 186 |
| `CountingInputStream` | class | → InputStream | 64 |
| `CPUProfile` | class | → Profile | 146 |
| `CraftWindow` | class | → Window | 107 |
| `DeadlockWatchdog` | class | → HackThread | 103 |
| `Debug` | class |  | 257 |
| `DefaultCollection` | interface | → Collection<E> | 136 |
| `Defer` | class | → ThreadGroup | 324 |
| `Defrag` | class |  | 90 |
| `Digest` | interface |  | 200 |
| `DirLight` | class | → Light | 59 |
| `Disposable` | interface |  | 31 |
| `DRandom` | class |  | 80 |
| `Drawable` | class | → GAttrib : RenderTree.Node | 119 |
| `Drawn` | interface |  | 31 |
| `DrawOffset` | class | → GAttrib | 54 |
| `DropTarget` | interface |  | 121 |
| `DTarget` | interface |  | 84 |
| `DynresWindow` | class | → Window | 881 |
| `EnumInterval` | class | → Enum<E>> extends AbstractList<E> | 67 |
| `Equipory` | class | → Widget : DTarget | 556 |
| `EquipTarget` | interface |  | 65 |
| `EventHandler` | interface |  | 63 |
| `ExtInventory` | class | → Widget | 927 |
| `FastArrayList` | class | → AbstractList<E> | 151 |
| `FastMesh` | class | : Rendered.Instancable, RenderTree.Node, Disposable | 383 |
| `FastText` | class |  | 130 |
| `FColor` | class |  | 150 |
| `Fightsess` | class | → Widget | 1314 |
| `Fightview` | class | → Widget | 550 |
| `FightWnd` | class | → Widget | 1048 |
| `FileCache` | class | : ResCache | 101 |
| `FilteredListBox` | class | → OldListBox<T> : ReadLine.Owner | 100 |
| `Finalizer` | class |  | 456 |
| `FlowerMenu` | class | → Widget | 458 |
| `FlowerMenuAutoSelectManagerWindow` | class | → Window | 187 |
| `Following` | class | → Moving | 117 |
| `Frame` | class | → Widget | 131 |
| `FromResource` | interface |  | 37 |
| `Future` | interface | → Indir<T> | 102 |
| `Fuzzy` | class |  | 83 |
| `GameUI` | class | → ConsoleHost : Console.Directory, UI.Notice.Handler | 3159 |
| `GAttrib` | class |  | 79 |
| `GenFun` | class |  | 101 |
| `GItem` | class | → AWidget : ItemInfo.SpriteOwner, GSprite.Owner, RandomSource | 798 |
| `GitHubVersionFetcher` | class |  | 76 |
| `GiveButton` | class | → Widget | 84 |
| `Glob` | class |  | 525 |
| `Gob` | class | : RenderTree.Node, Sprite.Owner, Skeleton.ModOwner, EquipTarget, RandomSource | 2743 |
| `GobBeeskepHarvestInfo` | class | → GobInfo | 94 |
| `GobCheeseRackInfo` | class | → GobInfo | 66 |
| `GobCombatDataInfo` | class | → GobInfo | 381 |
| `GobCombatHighlight` | class | → GAttrib : Gob.SetupMod | 22 |
| `GobCustomSizeAndRotation` | class | : Gob.SetupMod | 45 |
| `GobDamageInfo` | class | → GobInfo | 109 |
| `GobFoodWaterInfo` | class | → GobInfo | 102 |
| `GobGrowthInfo` | class | → GobInfo | 170 |
| `GobHealth` | class | → GAttrib : Gob.SetupMod | 71 |
| `GobHealthInfo` | class | → GobInfo | 61 |
| `GobIcon` | class | → GAttrib | 1294 |
| `GobIconCategoryList` | class | → OldListBox<GobIconCategoryList.GobCategory> | 336 |
| `GobIconsCustom` | class |  | 98 |
| `GobInfo` | class | → GAttrib : RenderTree.Node, PView.Render2D | 65 |
| `GobPartyHighlight` | class | → GAttrib : Gob.SetupMod | 22 |
| `GobPermanentHighlight` | class | → GAttrib : Gob.SetupMod | 18 |
| `GobPingHighlight` | class | → GAttrib : Gob.SetupMod | 32 |
| `GobQualityInfo` | class | → GobInfo | 83 |
| `GobReadyForHarvestInfo` | class | → GobInfo | 187 |
| `GobSpeedInfo` | class | → GobInfo | 59 |
| `GobStateHighlight` | class | → GAttrib : Gob.SetupMod | 24 |
| `GOut` | class |  | 787 |
| `GPUProfile` | class | → Profile | 134 |
| `GridList` | class | → Widget | 223 |
| `GroundSupportOverlay` | class | : MCache.OverlayInfo | 105 |
| `GSettings` | class | → State : Serializable | 354 |
| `GSprite` | class | : Drawn | 105 |
| `HackSocket` | class | → Socket | 212 |
| `HackThread` | class | → Thread | 75 |
| `HalfFloat` | class | → Number | 111 |
| `Hash` | interface |  | 41 |
| `HashBMap` | class | → AbstractMap<K, V> : BMap<K, V> | 148 |
| `HashDirCache` | class | : ResCache | 419 |
| `HashedMap` | class | → AbstractMap<K, V> | 241 |
| `HashedSet` | class | → AbstractSet<E> | 208 |
| `HashMultiMap` | class | : MultiMap<K, V> | 206 |
| `HeadlessClient` | class | : Console.Directory, Console.Host | 229 |
| `HelpWnd` | class | → Window | 43 |
| `HidingBox` | class | → SlottedNode : Rendered | 184 |
| `HitBoxGobSprite` | class | → RenderTree.Node> extends Sprite | 45 |
| `Homing` | class | → Moving | 92 |
| `HomoCoord4f` | class |  | 128 |
| `HRuler` | class | → Widget | 73 |
| `HSlider` | class | → Widget | 109 |
| `Http` | class |  | 98 |
| `HttpStatus` | class | → HackThread | 152 |
| `I2` | class | : Iterator<T> | 79 |
| `IBox` | interface |  | 109 |
| `IButton` | class | → SIWidget | 161 |
| `ICheckBox` | class | → ACheckBox | 109 |
| `IconSignGobInfo` | class | → GobInfo | 104 |
| `IDRef` | class |  | 79 |
| `IDSet` | class |  | 83 |
| `ILabel` | class | → Widget | 68 |
| `IMeter` | class | → LayerMeter | 204 |
| `Img` | class | → Widget | 109 |
| `Indir` | interface | → java.util.function.Supplier<T> | 30 |
| `IntMap` | class | → AbstractMap<Integer, V> | 173 |
| `Inventory` | class | → Widget : DTarget | 393 |
| `InventoryProxy` | class | → Widget : DTarget | 65 |
| `InventorySearchWindow` | class | → Window | 37 |
| `ISBox` | class | → Widget : DTarget | 262 |
| `ItemDrag` | class | → WItem | 142 |
| `ItemInfo` | class |  | 708 |
| `ItemSpec` | class | : GSprite.Owner, ItemInfo.SpriteOwner, RandomSource | 88 |
| `KeyBinding` | class |  | 122 |
| `KeyMatch` | class |  | 396 |
| `KeywordArgs` | class |  | 171 |
| `Label` | class | → Widget | 108 |
| `LayerMeter` | class | → Widget : ItemInfo.Owner | 128 |
| `Light` | class | : RenderTree.Node | 325 |
| `LimitMessage` | class | → Message | 78 |
| `Line2d` | class |  | 178 |
| `LinMove` | class | → Moving | 115 |
| `Loader` | class |  | 259 |
| `Loading` | class | → RuntimeException : Waitable | 163 |
| `LockDebugList` | class | → AbstractList<E> | 103 |
| `Locked` | class | : AutoCloseable | 47 |
| `LockedFile` | class | : AutoCloseable | 86 |
| `LoginScreen` | class | → Widget | 759 |
| `Lumin` | class | → GAttrib | 49 |
| `Makewindow` | class | → Widget | 527 |
| `MapFile` | class |  | 2041 |
| `MapMesh` | class | : RenderTree.Node, Disposable | 792 |
| `MapPrefs` | class | → AbstractPreferences | 76 |
| `MapSource` | interface |  | 120 |
| `MapView` | class | → PView : DTarget, Console.Directory, PFListener | 3335 |
| `MapWnd` | class | → Window : Console.Directory | 1356 |
| `Material` | class | : Pipe.Op | 331 |
| `Matrix4f` | class |  | 330 |
| `Maybe` | interface | → Supplier<T> | 111 |
| `MCache` | class | : MapSource | 1301 |
| `MenuGrid` | class | → Widget : KeyBinding.Bindable | 1147 |
| `MenuSearch` | class | → Window | 294 |
| `MeshAnim` | class | → State | 480 |
| `MeshBuf` | class |  | 379 |
| `MeshMorph` | class |  | 85 |
| `Message` | class |  | 638 |
| `MessageBuf` | class | → Message : java.io.Serializable | 146 |
| `MessageInputStream` | class | → InputStream | 63 |
| `MiniFloat` | class | → Number | 111 |
| `MiniMap` | class | → Widget | 1733 |
| `MiniStudy` | class | → GameUI.Hidewnd | 96 |
| `Mipmapper` | class |  | 368 |
| `ModSprite` | class | → Sprite : Sprite.CUpd, EquipTarget | 817 |
| `Moving` | class | → GAttrib | 42 |
| `MultiMap` | interface |  | 41 |
| `Music` | class |  | 194 |
| `NamedSocketAddress` | class |  | 105 |
| `NewsFeed` | class | → SListBox<NewsFeed.Entry, Widget> | 223 |
| `NormNumber` | class | → Number | 193 |
| `ObjectSearchWindow` | class | → Window | 46 |
| `OCache` | class | : Iterable<Gob> | 640 |
| `OldDropBox` | class | → OldListWidget<T> | 120 |
| `OldListBox` | class | → OldListWidget<T> | 145 |
| `OldListWidget` | class | → Widget | 76 |
| `OptWnd` | class | → Window | 5636 |
| `Outlines` | class | : RenderTree.Node | 164 |
| `OwnerContext` | interface |  | 126 |
| `PackCont` | class | → Widget | 164 |
| `Pair` | class |  | 56 |
| `Party` | class |  | 206 |
| `Partyview` | class | → Widget | 246 |
| `PeekReader` | class | → Reader | 85 |
| `PMessage` | class | → MessageBuf | 57 |
| `Polity` | class | → Widget | 210 |
| `PoseMorph` | class |  | 353 |
| `PosixArgs` | class |  | 126 |
| `PosLight` | class | → Light | 79 |
| `PrioQueue` | class | → Prioritized> extends LinkedList<E> | 74 |
| `Prioritized` | interface |  | 31 |
| `ProduceSackGobInfo` | class | → GobInfo | 103 |
| `Profdisp` | class | → Widget | 191 |
| `Profile` | class |  | 189 |
| `Profiler` | class |  | 308 |
| `Profwnd` | class | → Window | 67 |
| `Progress` | class | → Widget | 104 |
| `Promise` | class |  | 341 |
| `ProxyFrame` | class | → Widget> extends Frame | 64 |
| `PType` | interface |  | 189 |
| `Pufferfish2` | class |  | 211 |
| `PUtils` | class |  | 694 |
| `PView` | class | → Widget | 438 |
| `QuestObjectivesWindow` | class | → Window | 100 |
| `QuestWnd` | class | → Widget | 737 |
| `QuickSlotsWdg` | class | → Widget : DTarget | 308 |
| `RadioGroup` | class |  | 106 |
| `RandomSource` | interface |  | 33 |
| `Ratio` | class | → Number : Comparable<Ratio> | 227 |
| `ReadLine` | interface |  | 575 |
| `Reflect` | class |  | 125 |
| `RemoteUI` | class | : UI.Receiver, UI.Runner | 155 |
| `RenderContext` | class | → State : OwnerContext | 167 |
| `RenderedNormals` | class | → State | 139 |
| `RenderLink` | interface |  | 265 |
| `RepeatStream` | class | → InputStream | 73 |
| `ResCache` | interface |  | 90 |
| `ResData` | class |  | 72 |
| `ResDrawable` | class | → Drawable : Sprite.Owner, EquipTarget | 136 |
| `ResID` | class | → Number : Indir<Resource> | 88 |
| `Resource` | class | : Serializable | 2412 |
| `RetryingInputStream` | class | → InputStream | 172 |
| `RichText` | class | → Text | 910 |
| `RichTextBox` | class | → Widget | 127 |
| `RMessage` | class | → PMessage | 65 |
| `RootWidget` | class | → ConsoleHost : UI.Notice.Handler, Widget.CursorQuery.Handler, Console.Directory | 198 |
| `RUtils` | class |  | 303 |
| `SAttrWnd` | class | → Widget | 338 |
| `ScaledTex` | class | → Tex> : Tex | 69 |
| `Screenshooter` | class | → Window | 365 |
| `Scrollable` | interface |  | 34 |
| `Scrollbar` | class | → Widget | 159 |
| `Scrollport` | class | → Widget | 114 |
| `SDropBox` | class | → Widget> extends SListWidget<I, W> | 184 |
| `Session` | class | : Resource.Resolver | 346 |
| `SessWidget` | class | → AWidget | 115 |
| `ShadowMap` | class | → State | 342 |
| `SignKey` | interface |  | 671 |
| `SimpleSprite` | class |  | 85 |
| `SimplifiedMapColors` | class |  | 132 |
| `SIterator` | class | : Iterator<T> | 64 |
| `SIWidget` | class | → Widget | 65 |
| `Skeleton` | class |  | 1342 |
| `SkelSprite` | class | → Sprite : Sprite.CUpd, EquipTarget, Sprite.Owner, Skeleton.ModOwner, RandomSource | 331 |
| `SkillWnd` | class | → Widget | 562 |
| `SListBox` | class | → Widget> extends SListWidget<I, W> : Scrollable | 260 |
| `SListMenu` | class | → Widget> extends Widget | 267 |
| `SListWidget` | class | → Widget> extends Widget | 244 |
| `SlottedNode` | class | : RenderTree.Node | 22 |
| `SNoise3` | class |  | 137 |
| `Speaking` | class | → GAttrib : RenderTree.Node, PView.Render2D | 86 |
| `Speedget` | class | → Widget | 151 |
| `SpotLight` | class | → PosLight | 82 |
| `SprDrawable` | class | → Drawable : Sprite.Owner | 79 |
| `Sprite` | class | : RenderTree.Node, PView.Render2D | 231 |
| `SpriteLink` | class | → Resource.Layer | 120 |
| `SSearchBox` | class | → Widget> extends SListBox<I, W> | 160 |
| `SslChannel` | class | : ByteChannel | 289 |
| `SslHelper` | class |  | 177 |
| `StaticGSprite` | class | → GSprite : GSprite.ImageSprite | 59 |
| `StaticSprite` | class | → Sprite | 87 |
| `StatusWdg` | class | → Widget | 113 |
| `Steam` | class |  | 589 |
| `SteamCache` | class | : ResCache | 51 |
| `SteamCreds` | class | → AuthClient.Credentials | 62 |
| `SteamStore` | class |  | 73 |
| `SteamWorkshop` | class |  | 166 |
| `Streamer` | class | : Console.Directory | 222 |
| `StreamMessage` | class | → Message : Closeable, Flushable | 126 |
| `StreamOut` | class |  | 605 |
| `StreamTee` | class | → InputStream | 108 |
| `StudydeskInfo` | class | → Widget | 184 |
| `StudyInventory` | class | → Inventory | 223 |
| `Surface` | class |  | 154 |
| `SystemDrop` | interface |  | 59 |
| `TableBox` | class | → Widget | 231 |
| `TableInfo` | class | → Widget | 19 |
| `Tabs` | class |  | 118 |
| `TabStrip` | class | → Widget | 254 |
| `TestView` | class | → PView | 115 |
| `Tex` | interface | → Disposable | 106 |
| `TexI` | class | : Tex | 190 |
| `TexL` | class | → TexRender | 189 |
| `TexMS` | class | : Tex | 100 |
| `TexR` | class | → Resource.Layer : Resource.IDLayer<Integer> | 207 |
| `TexRaw` | class | : Tex | 76 |
| `TexRender` | class | : Tex, Disposable | 158 |
| `TexSI` | class | : Tex | 52 |
| `Text` | class | : Disposable | 501 |
| `TextEntry` | class | → Widget : ReadLine.Owner | 214 |
| `Textlog` | class | → Widget | 153 |
| `TileHighlight` | class |  | 546 |
| `Tiler` | class |  | 275 |
| `Tileset` | class | → Resource.Layer | 493 |
| `Timeout` | class |  | 198 |
| `Tonemapper` | class | → RenderContext.PostProcessor | 55 |
| `TopoSort` | class |  | 223 |
| `Transport` | interface |  | 220 |
| `UI` | class |  | 1155 |
| `UID` | class | → Number | 60 |
| `UILoop` | class | : Console.Directory | 619 |
| `UnionMap` | class | → AbstractMap<K, V> | 85 |
| `Utils` | class |  | 2916 |
| `VertexBuf` | class |  | 504 |
| `VertexBuilder` | class |  | 217 |
| `VMeter` | class | → LayerMeter | 57 |
| `Volume3f` | class |  | 123 |
| `VRuler` | class | → Widget | 62 |
| `Waitable` | interface |  | 181 |
| `Warning` | class | → Throwable | 108 |
| `WeakHashedSet` | class | → AbstractSet<E> | 264 |
| `WeakList` | class | → AbstractCollection<T> | 134 |
| `WeightList` | class | : java.io.Serializable | 65 |
| `Widget` | class |  | 2021 |
| `Window` | class | → Widget | 989 |
| `WItem` | class | → Widget : DTarget | 513 |
| `WorkshopLauncher` | class |  | 533 |
| `WoundWnd` | class | → Widget | 431 |
| `WrapMode` | enum |  | 37 |
| `XmlPrefs` | class | → AbstractPreferences | 360 |
| `ZMessage` | class | → Message : Closeable, Flushable, Serializable | 130 |

## `haven.automated`  (40)

| Type | Kind | Extends / Implements | Lines |
|---|---|---|---|
| `AddBranchesToFurnace` | class | : Runnable | 92 |
| `AddCoalToSmelter` | class | : Runnable | 92 |
| `AddWoodBlocksToSmokeShed` | class | : Runnable | 92 |
| `AggroEveryoneInRange` | class | : Runnable | 76 |
| `AggroNearestPlayer` | class | : Runnable | 91 |
| `AggroNearestTarget` | class | : Runnable | 158 |
| `AggroOrTargetCursorNearest` | class | : Runnable | 114 |
| `AttackOpponent` | class | : Runnable | 23 |
| `AUtils` | class |  | 448 |
| `AutoRepeatFlowerMenuScript` | class | : Runnable | 105 |
| `CellarDiggingBot` | class | → Window : Runnable | 234 |
| `CleanupBot` | class | → Window : Runnable | 265 |
| `CloverScript` | class | : Runnable | 142 |
| `CombatDistancerLite` | class | : Runnable | 87 |
| `CombatDistanceTool` | class | → Window : Runnable | 235 |
| `CoracleScript` | class | : Runnable | 244 |
| `DestroyNearestTrellisPlantScript` | class | : Runnable | 53 |
| `EnterNearestVehicle` | class | : Runnable | 118 |
| `EquipFromBelt` | class | : Runnable | 375 |
| `FillCheeseTray` | class | : Runnable | 56 |
| `FishingBot` | class | → Window : Runnable | 573 |
| `GrubGrubBot` | class | → Window : Runnable | 106 |
| `HarvestNearestDreamcatcher` | class | : Runnable | 58 |
| `InteractWithCursorNearest` | class | : Runnable | 186 |
| `InteractWithNearestObject` | class | : Runnable | 237 |
| `InventorySorter` | class | : Defer.Callable<Void> | 251 |
| `LootNearestKnockedPlayer` | class | : Runnable | 47 |
| `MiningSafetyAssistant` | class | → Window : Runnable | 375 |
| `OceanScoutBot` | class | → Window : Runnable | 274 |
| `OreAndStoneCounter` | class | → Window : Runnable | 187 |
| `PointerTriangulation` | class | → Window | 153 |
| `PushPlayer` | class | : Runnable | 46 |
| `QuestHelper` | class | → Window | 341 |
| `RefillWaterContainers` | class | : Runnable | 263 |
| `RoastingSpitBot` | class | → Window : Runnable | 267 |
| `SkisScript` | class | : Runnable | 151 |
| `StackAllItems` | class | : Runnable | 93 |
| `TarKilnCleanerBot` | class | → Window : Runnable | 130 |
| `UnstackAllItems` | class | : Runnable | 39 |
| `WagonNearestLiftable` | class | : Runnable | 258 |

## `haven.automated.cookbook`  (1)

| Type | Kind | Extends / Implements | Lines |
|---|---|---|---|
| `FoodService` | class |  | 293 |

## `haven.automated.helpers`  (3)

| Type | Kind | Extends / Implements | Lines |
|---|---|---|---|
| `AreaSelectCallback` | interface |  | 7 |
| `FishingAtlas` | class |  | 42 |
| `HitBoxes` | class |  | 325 |

## `haven.automated.mapper`  (3)

| Type | Kind | Extends / Implements | Lines |
|---|---|---|---|
| `MappingClient` | class |  | 517 |
| `MinimapImageGenerator` | class |  | 80 |
| `MultipartUtility` | class |  | 136 |

## `haven.automated.pathfinder`  (9)

| Type | Kind | Extends / Implements | Lines |
|---|---|---|---|
| `AStar` | class |  | 124 |
| `Dbg` | class |  | 88 |
| `Edge` | class |  | 13 |
| `Map` | class |  | 602 |
| `Pathfinder` | class | : Runnable | 308 |
| `PFListener` | interface |  | 5 |
| `TraversableObstacle` | class |  | 24 |
| `Utils` | class |  | 350 |
| `Vertex` | class |  | 15 |

## `haven.error`  (7)

| Type | Kind | Extends / Implements | Lines |
|---|---|---|---|
| `ErrorGui` | class | → JDialog : ErrorStatus | 239 |
| `ErrorHandler` | class | → ThreadGroup | 184 |
| `ErrorStatus` | interface |  | 62 |
| `HtmlReporter` | class |  | 374 |
| `Report` | class | : java.io.Serializable | 55 |
| `ReportException` | class | → IOException | 35 |
| `SimpleHandler` | class | → ThreadGroup | 42 |

## `haven.iosys`  (1)

| Type | Kind | Extends / Implements | Lines |
|---|---|---|---|
| `Unavailable` | class | → RuntimeException | 34 |

## `haven.iosys.audio`  (3)

| Type | Kind | Extends / Implements | Lines |
|---|---|---|---|
| `AudioSystem` | interface |  | 185 |
| `DummyAudio` | class | : AudioSystem | 109 |
| `JavaSound` | class | : haven.iosys.audio.AudioSystem | 259 |

## `haven.iosys.tk`  (17)

| Type | Kind | Extends / Implements | Lines |
|---|---|---|---|
| `Acephal` | interface |  | 150 |
| `AWTToolkit` | class | : Toolkit | 1122 |
| `Clipboard` | interface |  | 139 |
| `Cursor` | interface | → Disposable | 51 |
| `DropHandler` | interface |  | 52 |
| `DummyToolkit` | class | : Toolkit | 78 |
| `FilePicker` | interface |  | 54 |
| `JOGLOffscreen` | class | : Acephal | 155 |
| `JOGLToolkit` | class | → AWTToolkit | 269 |
| `Key` | interface |  | 247 |
| `LWJGLToolkit` | class | → AWTToolkit | 211 |
| `Monitor` | interface |  | 35 |
| `MouseBtn` | interface |  | 50 |
| `NEWTContext` | class | : Toolkit.Factory | 687 |
| `Test` | class |  | 47 |
| `Toolkit` | interface |  | 203 |
| `Windeye` | interface |  | 84 |

## `haven.render`  (55)

| Type | Kind | Extends / Implements | Lines |
|---|---|---|---|
| `Abortable` | interface |  | 34 |
| `BaseColor` | class | → State | 69 |
| `BlendMode` | class |  | 83 |
| `BufPipe` | class | : Pipe | 99 |
| `Camera` | class | → Transform | 73 |
| `ColorTex` | class | → State | 49 |
| `ColorVary` | class | → State | 52 |
| `DataBuffer` | interface |  | 113 |
| `DefPipe` | class | → BufPipe | 73 |
| `DepthBuffer` | class | → State | 58 |
| `DrawList` | interface | → RenderList<Rendered>, Disposable | 36 |
| `Environment` | interface | → haven.Disposable | 72 |
| `FillBuffer` | interface | → haven.Disposable | 37 |
| `FragColor` | class | → State | 118 |
| `FragID` | class | → Texture.Image> extends State | 116 |
| `FragTarget` | class |  | 74 |
| `FrameConfig` | class | → State | 56 |
| `FrameInfo` | class | → State | 56 |
| `GroupPipe` | interface | → Pipe | 56 |
| `Homo3D` | class |  | 213 |
| `InstanceBatch` | interface |  | 152 |
| `InstanceList` | class | : RenderList<Rendered>, RenderList.Adapter, Disposable | 884 |
| `Lighting` | interface |  | 593 |
| `Location` | class | → Transform | 184 |
| `MixColor` | class | → State : InstanceBatch.AttribState | 90 |
| `Model` | class | : Rendered, RenderTree.Node, Disposable | 153 |
| `NodeWrap` | interface |  | 91 |
| `NumberFormat` | enum |  | 41 |
| `Ortho2D` | class | → State | 61 |
| `Phong` | class | → ValBlock.Group : Lighting | 228 |
| `Pipe` | interface |  | 189 |
| `PointSize` | class | → State | 47 |
| `Projection` | class | → Transform | 93 |
| `ProxyPipe` | class | : Pipe | 71 |
| `Render` | interface | → Disposable | 70 |
| `Rendered` | interface |  | 125 |
| `RenderList` | interface |  | 113 |
| `RenderTree` | class | : RenderList.Adapter, Disposable | 926 |
| `SinglePipe` | class | → State> : Pipe | 56 |
| `State` | class | : Pipe.Op | 123 |
| `States` | class |  | 209 |
| `Swizzle` | class | : java.io.Serializable | 86 |
| `Tex2D` | class |  | 102 |
| `Texture` | class | : Disposable | 206 |
| `Texture2D` | class | → Texture | 106 |
| `Texture2DArray` | class | → TextureArray | 111 |
| `Texture2DMS` | class | → Texture | 88 |
| `Texture3D` | class | → Texture | 98 |
| `TextureArray` | class | → Texture | 66 |
| `TextureCube` | class | → Texture | 142 |
| `TickList` | class | : RenderList<TickList.TickNode> | 200 |
| `Transform` | class | → State | 156 |
| `VectorFormat` | class | : java.io.Serializable | 60 |
| `VertexArray` | class | : Disposable | 231 |
| `VertexColor` | class | → State | 49 |

## `haven.render.gl`  (34)

| Type | Kind | Extends / Implements | Lines |
|---|---|---|---|
| `Applier` | class |  | 343 |
| `BGL` | class |  | 1120 |
| `BufferBGL` | class | → BGL | 153 |
| `FboState` | class | → GLState | 309 |
| `Fence` | class | : BGL.Request | 62 |
| `FillBuffers` | class |  | 82 |
| `GL` | interface |  | 351 |
| `GLBuffer` | class | → GLObject : BGL.ID | 60 |
| `GLDoubleBuffer` | class |  | 107 |
| `GLDrawList` | class | : DrawList | 1092 |
| `GLEnvironment` | class | : Environment | 1050 |
| `GLException` | class | → RuntimeException | 115 |
| `GLFence` | class | → GLQuery | 63 |
| `GLFrameBuffer` | class | → GLObject : BGL.ID | 185 |
| `GLObject` | class | : Disposable | 125 |
| `GLPipeState` | class | → State> | 160 |
| `GLProgram` | class | : Disposable | 512 |
| `GLQuery` | class | → GLObject | 40 |
| `GLReference` | class | → Disposable> extends Finalizer.Reference<T> | 44 |
| `GLRender` | class | : Render, Disposable | 586 |
| `GLState` | class |  | 56 |
| `GLTexture` | class | → GLObject : BGL.ID | 717 |
| `GLTimestamp` | class | → GLQuery | 68 |
| `GLVertexArray` | class | → GLObject : BGL.ID | 251 |
| `HeapBuffer` | class | : Disposable | 46 |
| `NotImplemented` | class | → RuntimeException | 41 |
| `StreamBuffer` | class | : haven.Disposable | 153 |
| `SysBuffer` | interface | → Disposable, AutoCloseable | 35 |
| `TexState` | class | → GLState | 128 |
| `UniformApplier` | interface |  | 244 |
| `Vao0State` | class | → VaoState | 134 |
| `VaoBindState` | class | → VaoState | 71 |
| `VaoState` | class | → GLState | 32 |
| `VboState` | class | → GLState | 66 |

## `haven.render.jogl`  (6)

| Type | Kind | Extends / Implements | Lines |
|---|---|---|---|
| `JOGLBuffer` | class | : SysBuffer | 45 |
| `JOGLEnvironment` | class | → GLEnvironment | 89 |
| `JOGLWrap` | class | : GL, WrappedJOGL | 201 |
| `JOGLWrapBackup` | class | : GL, WrappedJOGL | 169 |
| `Test` | class | : GLEventListener, KeyListener | 240 |
| `WrappedJOGL` | interface |  | 33 |

## `haven.render.lwjgl`  (4)

| Type | Kind | Extends / Implements | Lines |
|---|---|---|---|
| `LWJGLBuffer` | class | → GLObject : SysBuffer | 84 |
| `LWJGLEnvironment` | class | → GLEnvironment | 73 |
| `LWJGLWrap` | class | : GL | 190 |
| `Test` | class |  | 124 |

## `haven.render.sl`  (65)

| Type | Kind | Extends / Implements | Lines |
|---|---|---|---|
| `Add` | class | → Expression | 54 |
| `Array` | class | → Type | 63 |
| `Attribute` | class | → Variable.Global | 81 |
| `AutoVarying` | class | → Varying | 76 |
| `BinOp` | class | → Expression | 68 |
| `Block` | class | → Statement | 129 |
| `Cons` | class |  | 155 |
| `Context` | class |  | 62 |
| `Discard` | class | → Statement | 38 |
| `Element` | class |  | 35 |
| `Expression` | class | → Element | 30 |
| `FieldRef` | class | → Expression | 51 |
| `FloatCons` | class | → Expression | 45 |
| `FloatLiteral` | class | → Expression | 44 |
| `For` | class | → Statement | 67 |
| `FragData` | class | → Variable.Global | 113 |
| `FragmentContext` | class | → ShaderContext | 63 |
| `Function` | class |  | 278 |
| `If` | class | → Statement | 81 |
| `Index` | class | → LValue | 52 |
| `InstancedAttribute` | class | → Attribute | 40 |
| `InstancedUniform` | class |  | 166 |
| `IntCons` | class | → Expression | 45 |
| `IntLiteral` | class | → Expression | 44 |
| `IVec2Cons` | class | → Expression | 54 |
| `IVec3Cons` | class | → Expression | 54 |
| `IVec4Cons` | class | → Expression | 54 |
| `LBinOp` | class | → Expression | 60 |
| `LFieldRef` | class | → LValue | 51 |
| `LPick` | class | → LValue | 61 |
| `LPostOp` | class | → Expression | 53 |
| `LPreOp` | class | → Expression | 53 |
| `LValue` | class | → Expression | 30 |
| `Mat3Cons` | class | → Expression | 52 |
| `MiscLib` | class |  | 119 |
| `Mul` | class | → Expression | 54 |
| `OrderList` | class | → AbstractCollection<E> | 76 |
| `Output` | class |  | 65 |
| `Pick` | class | → Expression | 61 |
| `Placeholder` | class | → Statement | 47 |
| `PostProc` | class | : Walker | 141 |
| `PreOp` | class | → Expression | 52 |
| `ProgramContext` | class |  | 65 |
| `Return` | class | → Statement | 45 |
| `ShaderContext` | class | → Context | 35 |
| `ShaderMacro` | interface |  | 83 |
| `Statement` | class | → Element | 42 |
| `Struct` | class | → Type | 143 |
| `Symbol` | class |  | 119 |
| `Toplevel` | class | → Element | 31 |
| `Type` | class |  | 101 |
| `UIntCons` | class | → Expression | 45 |
| `UIntLiteral` | class | → Expression | 43 |
| `Uniform` | class | → Variable.Global | 87 |
| `UVec2Cons` | class | → Expression | 54 |
| `UVec3Cons` | class | → Expression | 54 |
| `UVec4Cons` | class | → Expression | 54 |
| `ValBlock` | class |  | 252 |
| `Variable` | class |  | 115 |
| `Varying` | class | → Variable.Global | 62 |
| `Vec2Cons` | class | → Expression | 54 |
| `Vec3Cons` | class | → Expression | 54 |
| `Vec4Cons` | class | → Expression | 54 |
| `VertexContext` | class | → ShaderContext | 86 |
| `Walker` | interface |  | 31 |

## `haven.res.gfx.fx.bprad`  (1)

| Type | Kind | Extends / Implements | Lines |
|---|---|---|---|
| `BPRad` | class | → Sprite | 100 |

## `haven.res.gfx.fx.cavewarn`  (1)

| Type | Kind | Extends / Implements | Lines |
|---|---|---|---|
| `Cavein` | class | → Sprite : Sprite.CDel, PView.Render2D | 160 |

## `haven.res.gfx.fx.dowse`  (1)

| Type | Kind | Extends / Implements | Lines |
|---|---|---|---|
| `DowseFx` | class | → Sprite | 138 |

## `haven.res.gfx.fx.floatimg`  (3)

| Type | Kind | Extends / Implements | Lines |
|---|---|---|---|
| `FloatSprite` | class | → Sprite : PView.Render2D | 87 |
| `FloatText` | class | → FloatSprite | 29 |
| `Score` | class | : Sprite.Factory | 65 |

## `haven.res.gfx.fx.mscover`  (5)

| Type | Kind | Extends / Implements | Lines |
|---|---|---|---|
| `Coverage` | class | → GAttrib | 46 |
| `Data` | class |  | 37 |
| `Global` | class | : LocalOverlay | 192 |
| `Info` | class | : OverlayInfo | 25 |
| `ShowCover` | class | → MenuGrid.PagButton | 57 |

## `haven.res.gfx.fx.msrad`  (2)

| Type | Kind | Extends / Implements | Lines |
|---|---|---|---|
| `BuildOl` | class | → Sprite | 23 |
| `Radius` | class | → Coverage | 50 |

## `haven.res.gfx.fx.shroomflash`  (1)

| Type | Kind | Extends / Implements | Lines |
|---|---|---|---|
| `Flash` | class | → Sprite : PView.Render2D | 75 |

## `haven.res.gfx.hud.mmap.plo`  (3)

| Type | Kind | Extends / Implements | Lines |
|---|---|---|---|
| `DeadPlayer` | class | → GobIcon.ImageIcon | 34 |
| `Factory` | class | : GobIcon.Icon.Factory | 27 |
| `Player` | class | → GobIcon.Icon | 90 |

## `haven.res.gfx.tiles.flavor.ridge_edge`  (1)

| Type | Kind | Extends / Implements | Lines |
|---|---|---|---|
| `RidgeEdge` | class | : Tileset.Flavor | 176 |

## `haven.res.lib.bollar`  (2)

| Type | Kind | Extends / Implements | Lines |
|---|---|---|---|
| `BollData` | class | : Rendered, RenderTree.Node, Disposable | 71 |
| `ScreenPointSize` | class | → State | 47 |

## `haven.res.lib.env`  (1)

| Type | Kind | Extends / Implements | Lines |
|---|---|---|---|
| `Environ` | class | → GlobData | 53 |

## `haven.res.lib.globfx`  (5)

| Type | Kind | Extends / Implements | Lines |
|---|---|---|---|
| `Datum` | interface |  | 14 |
| `Effect` | interface | → RenderTree.Node | 15 |
| `GlobData` | class | : Datum | 20 |
| `GlobEffect` | class | : Effect | 20 |
| `GlobEffector` | class | → Drawable | 170 |

## `haven.res.lib.gplant`  (1)

| Type | Kind | Extends / Implements | Lines |
|---|---|---|---|
| `GaussianPlant` | class | : Sprite.Factory | 52 |

## `haven.res.lib.leaves`  (1)

| Type | Kind | Extends / Implements | Lines |
|---|---|---|---|
| `FallingLeaves` | class | → GlobEffect | 320 |

## `haven.res.lib.plants`  (2)

| Type | Kind | Extends / Implements | Lines |
|---|---|---|---|
| `GrowingPlant` | class | : Sprite.Factory | 53 |
| `TrellisPlant` | class | : Sprite.Factory | 59 |

## `haven.res.lib.svaj`  (3)

| Type | Kind | Extends / Implements | Lines |
|---|---|---|---|
| `GobSvaj` | class | → GAttrib : Gob.SetupMod | 61 |
| `Svaj` | class | → State : InstanceBatch.AttribState | 84 |
| `SvajOl` | class | → Sprite : Gob.SetupMod | 58 |

## `haven.res.lib.tree`  (6)

| Type | Kind | Extends / Implements | Lines |
|---|---|---|---|
| `Factory` | class | : Sprite.Factory | 68 |
| `LeafSpec` | class |  | 20 |
| `StdLeaf` | class | → FallingLeaves.Leaf | 24 |
| `Tree` | class | → Sprite | 163 |
| `TreeRotation` | class | → GAttrib : Gob.SetupMod | 26 |
| `TreeScale` | class | → GAttrib : Gob.SetupMod | 50 |

## `haven.res.lib.vmat`  (4)

| Type | Kind | Extends / Implements | Lines |
|---|---|---|---|
| `AttrMats` | class | → VarMats | 57 |
| `VarMats` | class | → GAttrib : Mod | 36 |
| `VarSprite` | class | → ModSprite | 15 |
| `VarWrap` | class | → Pipe.Op.Wrapping | 36 |

## `haven.res.sfx.ambient.weather.wsound`  (1)

| Type | Kind | Extends / Implements | Lines |
|---|---|---|---|
| `WeatherSound` | class | : Glob.Weather, RenderTree.Node | 60 |

## `haven.res.ui.barterbox`  (1)

| Type | Kind | Extends / Implements | Lines |
|---|---|---|---|
| `Shopbox` | class | → Widget : ItemInfo.SpriteOwner, GSprite.Owner | 345 |

## `haven.res.ui.croster`  (7)

| Type | Kind | Extends / Implements | Lines |
|---|---|---|---|
| `CattleId` | class | → GAttrib : RenderTree.Node, PView.Render2D | 81 |
| `CattleRoster` | class | → Entry> extends Widget | 222 |
| `Column` | class | → Entry> | 52 |
| `Entry` | class | → Widget | 97 |
| `RosterButton` | class | → MenuGrid.PagButton | 55 |
| `RosterWindow` | class | → Window | 55 |
| `TypeButton` | class | → IButton | 28 |

## `haven.res.ui.expwnd`  (1)

| Type | Kind | Extends / Implements | Lines |
|---|---|---|---|
| `ExpWnd` | class | → Window | 67 |

## `haven.res.ui.hondead`  (1)

| Type | Kind | Extends / Implements | Lines |
|---|---|---|---|
| `DeadWnd` | class | → Window | 86 |

## `haven.res.ui.locptr`  (1)

| Type | Kind | Extends / Implements | Lines |
|---|---|---|---|
| `Pointer` | class | → Widget | 190 |

## `haven.res.ui.music`  (4)

| Type | Kind | Extends / Implements | Lines |
|---|---|---|---|
| `Decoder` | class | : Sprite.Factory | 31 |
| `MusicOverlay` | class | → Sprite : CS | 95 |
| `MusicWnd` | class | → Window | 565 |
| `NoteOverlay` | class | → Sprite : Sprite.CUpd | 102 |

## `haven.res.ui.obj.buddy`  (3)

| Type | Kind | Extends / Implements | Lines |
|---|---|---|---|
| `Buddy` | class | → GAttrib : InfoPart | 94 |
| `Info` | class | → GAttrib : RenderTree.Node, PView.Render2D | 102 |
| `InfoPart` | interface |  | 25 |

## `haven.res.ui.obj.buddy_n`  (1)

| Type | Kind | Extends / Implements | Lines |
|---|---|---|---|
| `Named` | class | → GAttrib : InfoPart | 48 |

## `haven.res.ui.obj.buddy_v`  (1)

| Type | Kind | Extends / Implements | Lines |
|---|---|---|---|
| `Vilmate` | class | → GAttrib : InfoPart | 40 |

## `haven.res.ui.pag.toggle`  (2)

| Type | Kind | Extends / Implements | Lines |
|---|---|---|---|
| `Fac` | class | : PagButton.Factory | 21 |
| `Toggle` | class | → PagButton | 26 |

## `haven.res.ui.r_enact`  (3)

| Type | Kind | Extends / Implements | Lines |
|---|---|---|---|
| `Cost` | class |  | 20 |
| `Enactment` | class |  | 32 |
| `Enactments` | class | → Widget | 264 |

## `haven.res.ui.rchan`  (1)

| Type | Kind | Extends / Implements | Lines |
|---|---|---|---|
| `RealmChannel` | class | → ChatUI.MultiChat | 263 |

## `haven.res.ui.stackinv`  (1)

| Type | Kind | Extends / Implements | Lines |
|---|---|---|---|
| `ItemStack` | class | → Widget : DTarget | 126 |

## `haven.res.ui.tt.armor`  (1)

| Type | Kind | Extends / Implements | Lines |
|---|---|---|---|
| `Armor` | class | → ItemInfo.Tip | 30 |

## `haven.res.ui.tt.attrmod`  (10)

| Type | Kind | Extends / Implements | Lines |
|---|---|---|---|
| `Attribute` | interface |  | 26 |
| `AttrMod` | class | → ItemInfo.Tip | 107 |
| `Entry` | class |  | 20 |
| `inormattr` | class | → resattr | 29 |
| `intattr` | class | → resattr | 22 |
| `Mod` | class | → Entry | 25 |
| `normattr` | class | → resattr | 28 |
| `pmattr` | class | → resattr | 28 |
| `resattr` | class | : Attribute | 27 |
| `StringEntry` | class | → Entry | 20 |

## `haven.res.ui.tt.level`  (1)

| Type | Kind | Extends / Implements | Lines |
|---|---|---|---|
| `Level` | class | → ItemInfo : GItem.OverlayInfo<Double> | 57 |

## `haven.res.ui.tt.q.qbuff`  (1)

| Type | Kind | Extends / Implements | Lines |
|---|---|---|---|
| `QBuff` | class | → ItemInfo.Tip | 107 |

## `haven.res.ui.tt.q.qtoggle`  (1)

| Type | Kind | Extends / Implements | Lines |
|---|---|---|---|
| `ShowQuality` | class | → MenuGrid.PagButton | 39 |

## `haven.res.ui.tt.q.quality`  (1)

| Type | Kind | Extends / Implements | Lines |
|---|---|---|---|
| `Quality` | class | → QBuff : GItem.OverlayInfo<Tex> | 108 |

## `haven.res.ui.tt.slots`  (2)

| Type | Kind | Extends / Implements | Lines |
|---|---|---|---|
| `Fac` | class | : ItemInfo.InfoFactory | 39 |
| `ISlots` | class | → ItemInfo.Tip : GItem.NumberInfo | 209 |

## `haven.res.ui.tt.slots_alt`  (2)

| Type | Kind | Extends / Implements | Lines |
|---|---|---|---|
| `Fac` | class | : ItemInfo.InfoFactory | 38 |
| `ISlots` | class | → ItemInfo.Tip : GItem.NumberInfo | 216 |

## `haven.res.ui.tt.wear`  (1)

| Type | Kind | Extends / Implements | Lines |
|---|---|---|---|
| `Wear` | class | → ItemInfo.Tip | 35 |

## `haven.res.ui.tt.wpn.info`  (1)

| Type | Kind | Extends / Implements | Lines |
|---|---|---|---|
| `WeaponInfo` | class | → ItemInfo.Tip | 45 |

## `haven.resutil`  (18)

| Type | Kind | Extends / Implements | Lines |
|---|---|---|---|
| `AlphaTex` | class | → State | 91 |
| `BumpMap` | class | → State | 143 |
| `CaveTile` | class | → Tiler | 173 |
| `CompilerClassLoader` | class | → ClassLoader | 26 |
| `CrackTex` | class | → State : InstanceBatch.AttribState | 200 |
| `CSprite` | class | → Sprite | 70 |
| `Curiosity` | class | → ItemInfo.Tip : GItem.ColorInfo | 174 |
| `EnvMap` | class | → State | 76 |
| `FoodInfo` | class | → ItemInfo.Tip | 234 |
| `GroundTile` | class | → Tiler : Tiler.MCons, Tiler.CTrans | 159 |
| `HueMod` | class | → State | 66 |
| `LatentMat` | class | → State | 68 |
| `OverTex` | class | → State | 138 |
| `Ridges` | class | : MapMesh.ConsHooks | 795 |
| `TerrainTile` | class | → Tiler : Tiler.MCons, Tiler.CTrans | 466 |
| `TexAnim` | class | → State | 62 |
| `TexPal` | class | → State | 67 |
| `WaterTile` | class | → Tiler | 763 |

## `haven.rs`  (4)

| Type | Kind | Extends / Implements | Lines |
|---|---|---|---|
| `AvaRender` | class |  | 142 |
| `DrawBuffer` | class | : Disposable | 115 |
| `JythonRNode` | class | : RenderTree.Node | 41 |
| `Server` | class | → Thread | 187 |

## `haven.sprites`  (20)

| Type | Kind | Extends / Implements | Lines |
|---|---|---|---|
| `AggroCircleSprite` | class | → ColoredCircleSprite | 31 |
| `ArcheryRadiusSprite` | class | → ColoredCircleSprite | 15 |
| `ArcheryVectorSprite` | class | → Sprite | 119 |
| `AuraCircleSprite` | class | → ColoredCircleSprite | 16 |
| `ChaseVectorSprite` | class | → Sprite : PView.Render2D | 114 |
| `ClueSprite` | class | → MapSprite | 41 |
| `ColoredCircleSprite` | class | → Sprite | 139 |
| `ColoredSupportMesh` | class | → FastMesh | 112 |
| `CombatRangeSprite` | class | → ColoredCircleSprite | 13 |
| `CurrentAggroSprite` | class | → Sprite | 62 |
| `GobSearchHighlight` | class | → Sprite | 133 |
| `LeaderPingArrowSprite` | class | → Sprite : PView.Render2D | 82 |
| `MapSprite` | class |  | 13 |
| `Obst` | class |  | 165 |
| `ObstMesh` | class | → FastMesh | 23 |
| `PartyCircleSprite` | class | → ColoredCircleSprite | 19 |
| `PartyMarkSprite` | class | → Sprite : PView.Render2D | 29 |
| `PingSprite` | class | → MapSprite | 42 |
| `RangeRadiusSprite` | class | → Sprite | 109 |
| `SkyBoxSprite` | class | → Sprite | 154 |

## `haven.test`  (7)

| Type | Kind | Extends / Implements | Lines |
|---|---|---|---|
| `BaseTest` | class | : Runnable | 65 |
| `CharSelector` | class | → Robot | 83 |
| `DumpBot` | class | → Robot | 47 |
| `MultiClient` | class | → BaseTest | 114 |
| `Robot` | class |  | 51 |
| `RobotException` | class | → RuntimeException | 40 |
| `TestClient` | class | : Runnable | 160 |

## `haven.widgets`  (3)

| Type | Kind | Extends / Implements | Lines |
|---|---|---|---|
| `MultiSelectList` | class | → Widget | 168 |
| `SingleSelectList` | class | → Widget | 157 |
| `TwoOptionSwitch` | class | → Widget | 118 |

## `org.json`  (16)

| Type | Kind | Extends / Implements | Lines |
|---|---|---|---|
| `CDL` | class |  | 279 |
| `Cookie` | class |  | 169 |
| `CookieList` | class |  | 89 |
| `HTTP` | class |  | 163 |
| `HTTPTokener` | class | → JSONTokener | 77 |
| `JSONArray` | class | : Iterable<Object> | 1130 |
| `JSONException` | class | → RuntimeException | 43 |
| `JSONML` | class |  | 469 |
| `JSONObject` | class |  | 1842 |
| `JSONString` | interface |  | 18 |
| `JSONStringer` | class | → JSONWriter | 78 |
| `JSONTokener` | class |  | 446 |
| `JSONWriter` | class |  | 327 |
| `Property` | class |  | 72 |
| `XML` | class |  | 493 |
| `XMLTokener` | class | → JSONTokener | 365 |

#reference #generated
