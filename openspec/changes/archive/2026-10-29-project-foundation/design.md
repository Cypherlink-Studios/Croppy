# Design: Project Foundation & Core Architecture

## Context

See `proposal.md` for background and motivation. Croppy is starting fresh without pre-existing source code. To support long-term extensibility (intermediate simulation depth, extensible species, custom recipes, and future integrations), we establish a multi-module architecture on Java 21 and Paper 1.21.8.

## Goals / Non-Goals

**Goals:**
- Provide a reproducible, compile-time enforced multi-module build via Gradle Kotlin DSL.
- Guarantee that `croppy-api` and `croppy-core` remain 100% free of Bukkit / Paper classes.
- Deliver an immutable `Key` identifier record and a robust in-memory `Registry<T>` interface and implementation.
- Wire core lifecycle states (`initialize`, `shutdown`) and connect them to Paper's `JavaPlugin` lifecycle.
- Provide fast JUnit 5 unit tests validating registry and key behavior without running a Minecraft server.

**Non-Goals:**
- No YAML configuration parsing for crops or species (deferred to Change 2: `species-registry-and-definitions`).
- No Minecraft block listeners, tick loops, or crop simulation logic in this change.
- No database or persistent storage engines (PDC/SQLite) in this change.

## Decisions

### 1. Multi-Module Layout over Monolithic Package Structure
- **Decision:** Split into three Gradle subprojects:
  - `croppy-api`: Public interfaces and contracts (pure Java).
  - `croppy-core`: Internal domain logic, registries, services (pure Java, depends on `croppy-api`).
  - `croppy-paper`: Bukkit/Paper runtime adapter, `paper-plugin.yml`, fatjar distribution packaging (depends on `croppy-core` and Paper API 1.21.8).
- **Rationale:** Prevents domain classes from accidentally importing `org.bukkit.*` classes. The Java compiler acts as a strict boundary.
- **Alternatives Considered:** Single module with package-by-feature conventions. Rejected because architectural discipline without compiler boundaries degrades easily over time.

### 2. Standalone Domain Identifier (`Key`)
- **Decision:** Implement `dev.darkblade.croppy.api.identifier.Key` as an immutable record `(String namespace, String value)` with validation rules (`^[a-z0-9_.-]+$`).
- **Rationale:** Eliminates dependency on `org.bukkit.NamespacedKey`, making domain models portable and testable in headless unit tests.
- **Alternatives Considered:** Using Bukkit's `NamespacedKey` everywhere. Rejected because it directly couples core domain logic to the server platform.

### 3. Service Container & In-Memory Registry
- **Decision:** `SimpleRegistry<T extends Keyed>` using concurrent map storage, rejecting duplicate key registrations, and exposing unmodifiable views.
- **Decision:** `CroppyCore` orchestrates domain registry initialization and service lifecycle. `CroppyPlugin` holds a reference to `CroppyCore` and delegates lifecycle callbacks.
- **Rationale:** Keeps service wiring simple, transparent, and debuggable without the reflection overhead or complexity of external dependency injection frameworks (e.g. Guice/Dagger).

### 4. Packaging via Shadow / Repackaging
- **Decision:** `croppy-paper` packages `croppy-api` and `croppy-core` into the final server plugin jar.
- **Rationale:** Paper server expects a single self-contained plugin jar in the `plugins/` directory.

## Risks / Trade-offs

- **[Multi-module build overhead]** → Centralize common settings (Java 21 toolchain, encoding, compiler flags) in root `build.gradle.kts`.
- **[Descriptor format drift]** → Use `src/main/resources/paper-plugin.yml` conforming to Paper 1.21+ specifications rather than legacy `plugin.yml`.
- **[ClassLoader conflicts]** → Minimize external libraries in `croppy-core` and keep core lightweight pure Java standard library.
