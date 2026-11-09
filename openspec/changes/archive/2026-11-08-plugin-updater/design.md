# Design

## Context

See `proposal.md` for background and motivation. Croppy currently builds and releases its runtime plugin (`Croppy-<version>.jar`), companion API, resource pack, and SHA-256 manifests under GitHub repository `Cypherlink-Studios/Croppy`. This design establishes a secure, decoupled update checking and staging subsystem adhering strictly to Hexagonal Architecture, Java 21 standards, and Paper runtime safety.

## Goals / Non-Goals

**Goals:**
- Provide pure Java 21 API ports for version resolution and update checking.
- Enable asynchronous, non-blocking querying of GitHub Releases API via Java 21 native `HttpClient`.
- Support release channels (`STABLE` vs `BETA`) to allow operators to track either official releases or pre-release builds.
- Verify downloaded binary integrity using SHA-256 checksums from `checksums-sha256.txt`.
- Safely stage validated binaries in `plugins/update/<currentJarName>` for seamless automatic swap on next server reboot.
- Provide interactive Cloud 2.0 commands (`/croppy update check` and `/croppy update apply`) with localized MiniMessage feedback.
- Deliver non-intrusive startup checks and operator join notifications.

**Non-Goals:**
- Hot-swapping classloaders or reloading the plugin JAR in runtime memory without a server restart (violates Paper thread safety and causes memory leaks).
- Updating companion resource packs or third-party addons (scoped solely to the core plugin JAR in this phase).
- Automatic self-restarting of the server instance (server restart orchestration belongs to host process managers such as systemd or Pterodactyl).

## Decisions

### 1. Decoupled Hexagonal Architecture (`croppy-api` vs `croppy-paper`)
- **Choice**: Place all contracts and immutable domain models in `croppy-api` (`dev.darkblade.croppy.api.update`):
  - `UpdateService`: Asynchronous interface returning `CompletableFuture`.
  - `SemanticVersion`: Immutable record parsing `v<major>.<minor>.<patch>[-<prerelease>]` and implementing `Comparable<SemanticVersion>`.
  - `UpdateChannel`: Enum (`STABLE`, `BETA`).
  - `UpdateCheckResult`: Record holding check status, version records, release notes URL, and publication timestamp.
  - `UpdateApplyResult`: Record holding operation outcome, target staged path, and diagnostic message.
  - Implement the engine exclusively in `croppy-paper` (`PaperUpdateService`).
- **Rationale**: Strict adherence to AGENTS.md rule prohibiting platform or external library leakage into API and domain models.
- **Alternatives Considered**: Keeping all update logic embedded directly within Paper commands without API interfaces; rejected as it prevents programmatic querying by addons and violates modular boundaries.

### 2. Native Java 21 `HttpClient` for Remote API & Downloads
- **Choice**: Use standard `java.net.http.HttpClient` with explicit connect (10s) and read (15s) timeouts.
- **Rationale**: Included in Java 21 toolchain with zero third-party dependencies. Supports asynchronous non-blocking futures, TLS/SSL, and streaming downloads.
- **Alternatives Considered**: OkHttp or Apache HttpClient; rejected to avoid unnecessary shaded dependencies or Libby runtime downloads.

### 3. Paper Staging Mechanism via `plugins/update/`
- **Choice**: Write downloaded updates to `plugins/update/<currentJarName>`.
- **Rationale**: On Windows and Linux, running JAR files are locked by the host JVM. Attempting to overwrite `plugins/<currentJarName>` while running throws `FileSystemException` / `AccessDeniedException`. Paper's native plugin loader automatically inspects `plugins/update/` upon startup and replaces corresponding files before initializing the classloader.
- **Alternatives Considered**: In-place overwrite (fails on Windows); custom external bash/powershell script (requires external host permissions and complicates multi-platform deployment).

### 4. Cryptographic SHA-256 Integrity Validation
- **Choice**: Download `checksums-sha256.txt` from the release assets and verify the computed SHA-256 hash of the downloaded JAR before renaming the temporary staging file.
- **Rationale**: Protects against corrupted downloads, incomplete streams, or potential distribution tampering.
- **Alternatives Considered**: Checking file size only (unreliable); omitting checksum validation (risks corrupted JARs crashing the server upon restart).

### 5. In-Memory Result Caching (15-Minute TTL)
- **Choice**: Store the latest `UpdateCheckResult` in an in-memory cache with a 15-minute expiration timestamp.
- **Rationale**: GitHub REST API enforces a 60 request/hour rate limit for unauthenticated IP addresses. Repeatedly running `/croppy update check` or having players rejoin would quickly exhaust this quota.
- **Alternatives Considered**: File-based caching (unnecessary I/O overhead); no caching (breaks under rapid command usage).

### 6. Cloud 2.0 Command Routing & Permissions
- **Choice**: Bind commands under `/croppy update check` and `/croppy update apply` using Cloud 2.0 annotations in `CroppyCommands.java`, requiring permission `croppy.admin.update`.
- **Rationale**: Seamlessly integrates into existing command tree with uniform autocomplete and localized feedback.

## Risks / Trade-offs

- **[Risk] GitHub API Rate Limiting (HTTP 403)**
  - *Mitigation*: 15-minute in-memory caching. If a 403 response is received, parse `x-ratelimit-reset` header and deliver a friendly MiniMessage notice showing when checks can resume.
- **[Risk] Interrupted Download Leading to Corrupted Binary**
  - *Mitigation*: Download into a `.tmp` file first (`plugins/update/.Croppy-<version>.jar.tmp`). Only atomically move to the target file after SHA-256 validation succeeds. Delete `.tmp` on any failure.
- **[Risk] Network Downtime or Connection Timeouts**
  - *Mitigation*: Isolate all I/O inside asynchronous tasks. Catch network exceptions gracefully and notify the user via localized catalog keys without dumping Java stack traces to the console.
- **[Risk] Running JAR Filename Resolution**
  - *Mitigation*: Resolve the target file name via `JavaPlugin#getFile().getName()`. If running as a symlink or during development unit tests, fallback safely to `Croppy.jar`.

## Migration Plan

1. Configuration settings (`updates.enabled`, `updates.check-on-startup`, `updates.channel`) auto-populate with safe defaults in `config.yml`.
2. Existing servers remain fully backwards-compatible. Disabling `updates.enabled` completely halts background polling and disables update commands.
