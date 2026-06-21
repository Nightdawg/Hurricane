---
title: Networking and Protocol
aliases: [Networking, Protocol, Session, Connection]
tags: [architecture, networking]
---

# Networking and Protocol

Source: `src/haven/Session.java`, `src/haven/Connection.java`, `src/haven/Transport.java`,
`src/haven/Message.java`, `src/haven/PMessage.java`, `src/haven/RMessage.java`,
`src/haven/MessageBuf.java`, `src/haven/AuthClient.java`.

H&H uses a **custom datagram (UDP-style) protocol** with an application-level reliability layer.
Protocol version: `Session.PVER = 31`.

## Core types

| Type | Role |
|---|---|
| `Session` | A logged-in game session. Owns the [[Game-State-Model\|`Glob`]], resource cache mapping, the `User`, and the link to the UI. `implements Resource.Resolver`. |
| `Transport` | Interface for the underlying transport. `Connection` (real network) and `Transport.Playback` (replay logs) implement it. |
| `Connection` | The real UDP connection + reliability/crypto. `implements Transport`. |
| `Message` | Base read/write buffer for typed primitives (uint8/16/32, strings, coords, etc.). |
| `PMessage` | **Protocol message** = one datagram-level message (has a `type` byte). |
| `RMessage` | **Reliable message** = ordered, ACK'd, may be fragmented (`RMSG_FRAGMENT`). |
| `MessageBuf` | Concrete in-memory `Message` implementation. |

## Datagram message types (`Session.MSG_*`)

| Const | Value | Meaning |
|---|---|---|
| `MSG_SESS` | 0 | Session handshake / open (carries `PVER`, user, cookie; errors via `SESSERR_*`). |
| `MSG_REL` | 1 | Reliable message batch (sequence-numbered `RMessage`s). |
| `MSG_ACK` | 2 | Acknowledge reliable sequence. |
| `MSG_BEAT` | 3 | Keepalive heartbeat. |
| `MSG_MAPREQ` | 4 | Request a map grid. |
| `MSG_MAPDATA` | 5 | Map grid data → [[Game-State-Model\|`MCache`]]. |
| `MSG_OBJDATA` | 6 | Game-object (Gob) updates → [[Game-State-Model\|`OCache`]]. |
| `MSG_OBJACK` | 7 | Acknowledge object updates. |
| `MSG_CLOSE` | 8 | Close session. |
| `MSG_CRYPT` | 9 | Encrypted-channel negotiation/payload (optional, `Connection.encrypt`). |

Session-open error codes: `SESSERR_AUTH=1`, `SESSERR_BUSY=2`, `SESSERR_CONN=3`,
`SESSERR_PVER=4` (protocol mismatch), `SESSERR_EXPR=5`, `SESSERR_MESG=6`.

## Reliability layer

- Reliable messages (`MSG_REL`) carry a 16-bit sequence. The receiver buffers out-of-order
  messages in a `waiting` map and delivers them in order (`gotrel`), ACKing with `MSG_ACK`.
- Large reliable messages are split into `RMessage.RMSG_FRAGMENT` pieces and reassembled
  (`handlerel`).
- `ackthresh = 30` controls ACK batching.
- Delivered reliable UI messages are queued as `PMessage`s in `Session.uimsgs` and consumed by the
  [[UI-and-Widget-System\|widget]] layer (these are the `wdgmsg`/widget-creation messages).

## Encryption

`Connection` supports an optional encrypted channel (`encrypt`/`decrypt` around `MSG_CRYPT`),
gated by `Connection.encrypt` config. The handshake wraps subsequent messages.

## Object & map updates (the important part for game logic)

- `MSG_OBJDATA` → parsed into per-object deltas applied to [[Game-State-Model\|`OCache`]]
  (`Gob` add/move/attribute-change/remove). Acked via `MSG_OBJACK`.
- `MSG_MAPDATA` → map grid tiles/overlays applied to [[Game-State-Model\|`MCache`]].

This is **why game state mutates on a background thread**: the connection receiver thread applies
these updates concurrently with the UI/render loop. See [[Architecture-Overview#Threads]] and
[[Coding-Conventions#Threading]].

## Auth & connect

- `Client.connect(args)` resolves account + cookie (saved token via `AuthClient`, or Steam, or
  explicit creds), resolves the game server address (`Bootstrap.gameserv`/`authserv`/`gameport`),
  then calls `Session.connect(addr, acct, encrypt, cookie, args)`.
- `AuthClient` speaks the **auth-server** protocol (separate from the game server): password and
  token credentials, returns a cookie and candidate game hosts.

## Recording / playback

- `haven.record` (config) can record a session to a log.
- `Transport.Playback` replays such a log through a `Session` without a live server (used by
  `Bootstrap.replay`). See [[Startup-and-Lifecycle]].

## Related
- [[Game-State-Model]] · [[UI-and-Widget-System]] · [[Startup-and-Lifecycle]]

#architecture #networking
