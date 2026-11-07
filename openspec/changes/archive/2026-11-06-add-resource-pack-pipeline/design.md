# Design: Resource Pack Pipeline & Commands

## Context
Croppy uses a strict Hexagonal Architecture across three modules: `croppy-api`, `croppy-core`, and `croppy-paper`. Addon content packs declare custom 3D displays and items under `assets/<namespace>/`. To deliver custom assets to Minecraft 1.21.8 clients without requiring third-party plugins or manual archive manipulation, Croppy requires an automated compilation pipeline and lightweight hosting mechanism.

## Goals / Non-Goals

**Goals:**
- Provide a pure Java 21 `ResourcePackCompiler` in `croppy-core` with full JUnit 5 test coverage.
- Deliver an embedded HTTP server using standard `jdk.httpserver` in `croppy-paper` with zero external dependencies.
- Implement atomic ZIP generation and SHA-1 checksum calculation for client cache integrity.
- Integrate Cloud Paper 2.0 commands `/croppy pack build` and `/croppy pack send [player]` with permission checks and rich MiniMessage feedback.
- Support optional auto-dispatch on player join with event status telemetry.

**Non-Goals:**
- Heavy client-side texture baking or procedural model generation.
- Bundling external web server libraries (Netty, Jetty, or Ktor).
- Direct integration with third-party cloud CDNs (users wanting external CDNs can point `public-url` to their CDN).

## Decisions

### 1. Pure Domain Compiler in `croppy-core`
- **Choice**: Implement `DefaultResourcePackCompiler` inside `croppy-core` implementing an interface in `croppy-api`.
- **Rationale**: Merging files, reading ZIP entries, checking path regexes (`^[a-z0-9_.-]+$`), and computing SHA-1 hashes require only standard Java 21 I/O. Keeping this logic in `croppy-core` preserves hexagonal isolation and enables fast, comprehensive JUnit 5 unit tests without mocking Bukkit.
- **Alternatives Considered**: Putting the merger in `croppy-paper` (rejected: tightly couples file manipulation to Bukkit and hampers testability).

### 2. Embedded `com.sun.net.httpserver.HttpServer`
- **Choice**: Use the standard JDK HTTP server provided by Java 21 (`jdk.httpserver`).
- **Rationale**: It has zero external dependencies, adds no JAR weight, uses minimal memory, and natively supports static file streaming with `Content-Type: application/zip`, `Content-Length`, and `ETag`.
- **Alternatives Considered**: Netty / Jetty / Ktor (rejected: adds megabytes of dependencies and complex classloading), External web server only (rejected: poor out-of-the-box user experience).

### 3. Atomic Streaming Compilation & SHA-1 Digest
- **Choice**: Build the ZIP archive to a temporary file using `DigestOutputStream` wrapping `ZipOutputStream` with `MessageDigest.getInstance("SHA-1")`, then atomically move it to `plugins/Croppy/exports/croppy-unified-pack.zip`.
- **Rationale**: Avoids reading the archive twice from disk to compute the hash, and prevents partial/corrupted ZIPs from being served while a build is in progress.

### 4. Asynchronous Task Execution
- **Choice**: Resource pack compilation and disk operations run asynchronously on Paper's async scheduler.
- **Rationale**: Paper's server main thread must never perform blocking disk I/O or archive compression.

## Risks / Trade-offs

- **[Port Conflict on Startup]** → Catch `BindException` gracefully during `onEnable()`, log a clear warning suggesting another port in `config.yml`, and disable the embedded server without failing plugin startup.
- **[Addon Asset Overwrites]** → Enforce namespaced paths (`assets/<namespace>/...`). Addons cannot overwrite base Croppy textures unless explicitly designed as overrides, and conflicting addon namespaces trigger a warning.
- **[Client Rejection of Resource Pack]** → Listen to `PlayerResourcePackStatusEvent` to capture `DECLINED` or `FAILED_DOWNLOAD` status and provide non-intrusive feedback without kicking players.
