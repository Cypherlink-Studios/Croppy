# Design: Addon Configuration System

## Context

Croppy's modular architecture separates pure Java 21 ports (`croppy-api`), pure domain logic (`croppy-core`), and Paper platform adapters (`croppy-paper`). The addon system discovers both compiled JARs and unpacked directories under `plugins/Croppy/addons/`. Currently, `AddonContext.getDataFolder()` resolves paths, but addons lack a standardized API and engine to load, query, reload, and persist `config.yml` files, as well as protection against directory collisions.

See `proposal.md` for background and `specs/addon-system/spec.md` for requirement specifications.

## Goals / Non-Goals

**Goals:**
- Provide a pure Java 21 `AddonConfig` interface in `croppy-api` with zero external dependencies.
- Implement a pure Java 21 `YamlAddonConfig` in `croppy-core` using SnakeYAML 2.2, enabling 100% headless unit testing.
- Implement `PaperAddonConfig` in `croppy-paper` wrapping Bukkit's `YamlConfiguration` to preserve comments and formatting during runtime.
- Provide Bukkit-like ergonomic methods on `AddonContext`: `saveDefaultConfig()`, `getConfig()`, `reloadConfig()`, and `saveConfig()`.
- Unify directory vs JAR data folder resolution: directory addons use their root folder as `dataFolder`, while JAR addons use `plugins/Croppy/addons/<addon-id>/`.
- Guard against filesystem collisions with a **Path Ownership Gate** enforcing fail-fast errors on duplicate IDs or cross-occupancy folder collisions.

**Non-Goals:**
- Implementing automatic multi-version schema migrations or remote cloud configuration synchronization.
- Modifying Bukkit's global server configuration format.

## Decisions

### Decision 1: Hybrid Core/Paper Architecture
- **Rationale**: Strict adherence to Hexagonal Architecture (**AGENTS.md**). `croppy-api` cannot import Bukkit or SnakeYAML. `croppy-core` implements `YamlAddonConfig` with SnakeYAML 2.2 for decoupled, fast unit tests without Paper server mocks. In production, `croppy-paper` utilizes `PaperAddonConfig` wrapping Bukkit's `YamlConfiguration` to preserve inline comments and headers.
- **Alternatives Considered**:
  - *Bukkit-only*: Would violate Hexagonal boundary rules and make `croppy-core` impossible to unit-test without heavy Bukkit mocks.
  - *SnakeYAML-only*: Standard SnakeYAML loses user comments upon calling `saveConfig()`.

### Decision 2: Classical Bukkit-Like Ergonomics on `AddonContext`
- **Rationale**: Minecraft plugin and addon developers expect familiar patterns: `context.saveDefaultConfig()`, `AddonConfig cfg = context.getConfig()`, `context.reloadConfig()`, and `context.saveConfig()`.
- **Alternatives Considered**:
  - *Injected Configuration Record*: Less flexible for dynamic key lookups across deeply nested custom addon sections.

### Decision 3: Directory Addon Root as Data Folder
- **Rationale**: For declarative content packs installed as directories (e.g. `addons/coffee/`), creating a nested subfolder `addons/coffee/coffee/` would be unintuitive and redundant. The directory itself serves as both the asset bundle and runtime data folder.
- **Alternatives Considered**:
  - *Always create subfolder*: Clutters workspace and confuses users editing unpacked content packs.

### Decision 4: Path Ownership Gate & Fail-Fast Collision Prevention
- **Rationale**: If a compiled JAR (`frost.jar`, id `frost`) generates its data folder at `addons/frost/`, but `addons/frost/` is already the root of an unpacked directory addon (`winter-pack`), severe data corruption would occur. The manager must track both `addonRoots` and `dataFolders` during discovery and fail-fast with clear diagnostic logs before initializing colliding addons.
- **Alternatives Considered**:
  - *Automatic path renaming (e.g. `addons/frost-data`)*: Unpredictable for server admins managing file paths. Fail-fast makes misconfigurations immediately actionable.

## Architecture & Data Flow

```
+-----------------------------------------------------------------------------------------+
|                                    croppy-api                                           |
|                                                                                         |
|   +-----------------------+              +------------------------------------------+   |
|   |     AddonContext      | -----------> |               AddonConfig                |   |
|   |-----------------------|              |------------------------------------------|   |
|   | + getConfig()         |              | + getString(path, def) : String          |   |
|   | + reloadConfig()      |              | + getInt(path, def) : int                |   |
|   | + saveConfig()        |              | + getBoolean(path, def) : boolean        |   |
|   | + saveDefaultConfig() |              | + getDouble(path, def) : double          |   |
|   +-----------------------+              | + getStringList(path) : List<String>     |   |
|                                          | + getSection(path) : Optional<AddonConfig|   |
|                                          | + set(path, Object)                      |   |
|                                          | + reload() / save()                      |   |
|                                          +------------------------------------------+   |
+-----------------------------------------------------------------------------------------+
                                           ^
                                           |
                  +------------------------+------------------------+
                  |                                                 |
+-----------------v------------------+            +-----------------v------------------+
|            croppy-core             |            |            croppy-paper            |
|  +------------------------------+  |            |  +------------------------------+  |
|  |       YamlAddonConfig        |  |            |  |       PaperAddonConfig       |  |
|  |  (SnakeYAML 2.2 / Pure Java) |  |            |  |  (Wraps YamlConfiguration)   |  |
|  +------------------------------+  |            |  +------------------------------+  |
|                                    |            |                                    |
|  +------------------------------+  |            |  +------------------------------+  |
|  |     DefaultAddonManager      |  |            |  |      PaperAddonManager       |  |
|  |   (Path Ownership Gate)      |  |            |  |  (Extracts bundled config)   |  |
|  +------------------------------+  |            |  +------------------------------+  |
+------------------------------------+            +------------------------------------+
```

## Risks / Trade-offs

- **[Risk] Missing bundled `config.yml` when addon calls `saveDefaultConfig()`** → Mitigation: Check `getResourceAsStream("config.yml")`. If absent, gracefully log a debug message without throwing an exception or creating an invalid empty file.
- **[Risk] Directory name differs from manifest ID (e.g., folder `frost/` has `id: winter`)** → Mitigation: `DefaultAddonManager` logs a warning recommending that directory names match their manifest `id`, while the Path Ownership Gate ensures no other addon can claim `frost/` as a data folder.
- **[Risk] Concurrent read/write on configuration** → Mitigation: `YamlAddonConfig` uses synchronized maps and defensive accessors.
