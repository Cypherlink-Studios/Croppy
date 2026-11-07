# Proposal: Resource Pack Pipeline & Commands

## Why
With the introduction of the modular Addon System (v0.5.7), Croppy enables first-party and third-party content packs to declare custom 3D crop displays, items, stations, textures, and sounds. However, Minecraft clients require a single unified companion resource pack. Currently, server administrators must manually unpack, merge, hash, upload, and distribute resource packs.

Providing an automated build pipeline and lightweight embedded HTTP delivery server directly within Croppy streamlines pack distribution, guarantees hash-verified asset synchronization, and removes external operational friction.

## What Changes
- **Automated Addon Asset Compilation & Merging**: Domain engine compiler in `croppy-core` (`DefaultResourcePackCompiler`) that merges the base companion resource pack with `assets/<namespace>/` subtrees from all discovered and enabled addons, validating Minecraft 1.21.8 path conventions and generating an optimized ZIP archive with atomic SHA-1 checksum calculation.
- **Embedded Lightweight HTTP Server**: Paper platform service in `croppy-paper` (`EmbeddedPackHttpServer`) utilizing Java 21's standard `com.sun.net.httpserver.HttpServer` (zero external dependencies) to serve `croppy-unified-pack.zip` directly to connecting players with proper HTTP headers (`Content-Type: application/zip`, `Content-Length`, `ETag`).
- **Administrative Cloud 2.0 Commands**:
  - `/croppy pack build`: Asynchronously compiles the unified resource pack, updates the active SHA-1 checksum, saves to `exports/croppy-unified-pack.zip`, and reports benchmark metrics (time, byte size, asset counts).
  - `/croppy pack send [player]`: Dispatches the compiled resource pack to the specified player (or all players / command sender) via Paper's `Player.setResourcePack(...)` with configurable MiniMessage prompt.
- **Join Dispatch & Telemetry**: Optional automatic resource pack prompt on player join (`auto-send-on-join`), with event logging via `PlayerResourcePackStatusEvent`.
- **Configuration & Localization**: Configurable port, host, public URL, auto-send preferences, and localized command feedback in English and Spanish.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `companion-resource-pack`: Extends requirements to define the automated asset merge pipeline, SHA-1 checksum generation, embedded HTTP hosting, and player delivery mechanisms.
- `crop-commands`: Extends administrative command dispatch to register `/croppy pack build` and `/croppy pack send [player]` with permission checks and localized feedback.

## Impact
- **Architecture**: Maintains pure Java 21 isolation in `croppy-api` (records & service port) and `croppy-core` (file/zip compilation engine), keeping platform adapters (HttpServer, Paper Player API, Cloud commands) strictly in `croppy-paper`.
- **Dependencies**: No external dependencies added; uses standard `jdk.httpserver` and Paper 1.21.8 APIs.
- **Configuration**: Adds `resource-pack` configuration section to `config.yml`.
- **Localization**: Adds command feedback strings to `en-US.yml` and `es-ES.yml`.
