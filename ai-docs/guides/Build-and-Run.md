---
title: Build and Run
aliases: [Build, Run, Compile, How to build]
tags: [guide, build]
---

# Build and Run

The build system is **Apache Ant** (`build.xml`). There is **no** Maven/Gradle. Output is
`bin/hafen.jar` plus a launchable `bin/` folder.

## Requirements

- **Java 17–21** (21 strongly recommended; GraalVM 21 works well). Source/target level is **15**
  (`build.xml` `javac source="15" target="15"`), so the code itself avoids newer language features.
- **Apache Ant**.
- Internet on first build — Ant auto-downloads native libs and resource jars from
  `http://www.havenandhearth.com/java` (JOGL, LWJGL, steamworks4j) and `builtin-res.jar` /
  `hafen-res.jar`.

## Common commands

```bash
ant                 # default target "deftgt": jars + opt/panama + bin  → full client in bin/
ant jar             # compile + package build/hafen.jar only
ant bin             # assemble the runnable bin/ folder (jars, res, launchers, manifest.json)
ant run             # build bin/ and launch the client (haven.Client) with proper JVM flags
ant clean           # delete build/, bin/, and downloaded lib/ext/
```

There are also IDE-friendly Ant targets: `"run > Play (Bat File)"`,
`"Util: Find fetched resource updates"`, `"Util: Upload to steam workshop"`.

## What the build does (high level)

- `extlib/*` — download JOGL, LWJGL (base/gl), steamworks libs into `lib/ext/`.
- `hafen-client` — `javac` all of `src/` into `build/classes` (classpath includes the ext jars +
  `lib/jglob.jar`, `lib/rxjava-1.1.5.jar`, `lib/sqlite-jdbc-3.42.0.0.jar`). Copies non-`.java`
  resources, certs (`ressrv.crt`, `authsrv.crt`), preload lists, icon.
- `opt/panama` — *(only on Java ≥ 22)* compiles the optional Panama FFI module
  (`build/hafen-panama.jar`). Skipped on Java 17–21.
- `jar` — package `build/hafen.jar` with `Main-Class: haven.Client` and a `Class-Path` listing the
  side jars; also bakes `buildinfo` (git rev).
- `bin` — copy jar + libs + `res/`, `AlarmSounds/`, `midiFiles/`, `MapIconsPresets/`, launchers
  (`Play.bat`, `Play_Linux.sh`, `Play_WithSteam.bat`), Steam files, `hafen.hl`/`launcher.hl`, then
  runs `haven.BuildManifest` to generate `bin/manifest.json`.

## Running

- Dev: `ant run`.
- End-user: run `bin/Play.bat` (Windows), `bin/Play_Linux.sh` (Linux/macOS), or
  `bin/Play_WithSteam.bat`. These set JVM flags (`--add-exports …`, native access) and launch
  `bin/hafen.jar`.
- The window title becomes `Hurricane (v1.63b)` — see [[Startup-and-Lifecycle]].

## Useful runtime knobs (system properties / [[Glossary|Config]] vars)

- `haven.fullscreen=true` — start fullscreen (`Client.initfullscreen`).
- `haven.renderer=lwjgl` — force the LWJGL [[Rendering-Pipeline|render backend]] (else JOGL).
- `haven.nopreload=true` — skip resource preload.
- `haven.errorurl=stderr` — print errors to stderr instead of the GUI error reporter.
- `haven.record=<path>` / `Bootstrap.replay` — record / replay a session (see
  [[Networking-and-Protocol#Recording / playback]]).

## Resource-code CLI (not a build step, but uses the jar)

```bash
java -cp bin/hafen.jar haven.Resource get-code <res/path>   # fetch resource Java source
java -cp bin/hafen.jar haven.Resource find-updates          # check for updated resources
java -cp bin/hafen.jar haven.SteamWorkshop upload bin       # publish to Steam Workshop
```

See [[Resource-System]] and `nd-notes.txt`.

## Updating from upstream

The repo has a `LoftarSeatribe` remote (`git://sh.seatribe.se/hafen-client`). The maintainer merges
upstream into `master`. Keep core edits minimal to ease merges (see [[Project-Overview#Fork lineage]]).

## Related
- [[Resource-System]] · [[Startup-and-Lifecycle]] · [[Coding-Conventions]]

#guide #build
