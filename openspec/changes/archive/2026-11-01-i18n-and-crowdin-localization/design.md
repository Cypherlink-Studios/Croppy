# Design

## Context

Croppy follows a strict Hexagonal Architecture across three Gradle modules (`croppy-api`, `croppy-core`, `croppy-paper`).
See `proposal.md` for motivation. Currently, user-facing text is fragmented across `config.yml`, event listeners, command handlers, and inspectors, with Spanish string literals embedded in Java code.

The localization system must integrate cleanly into this structure:
- `croppy-api`: Pure Java 21, zero external dependencies.
- `croppy-core`: Domain engine, SnakeYAML 2.2, zero platform/Adventure dependencies.
- `croppy-paper`: Paper 1.21.8 runtime adapter, Kyori Adventure / MiniMessage.

## Goals / Non-Goals

**Goals:**
- Provide a thread-safe, high-performance in-memory message catalog cached at startup and hot-reloadable without server restarts.
- Isolate domain interfaces in `croppy-api` and YAML loading in `croppy-core` from Bukkit and Kyori Adventure.
- Support automatic per-player language resolution via `player.locale()` with a deterministic fallback chain.
- Separate framework messages from gameplay content definitions, allowing optional content name/lore overrides in locale bundles without duplicating content YAML files.
- Protect against MiniMessage tag injection by sanitizing untrusted dynamic arguments.
- Integrate Crowdin configuration (`crowdin.yml`) and GitHub Actions validation tasks without requiring runtime network dependencies.

**Non-Goals:**
- Runtime communication with Crowdin API (Crowdin is strictly a development/CI service).
- Full ICU MessageFormat complex grammatical gender/case engine (MiniMessage + templated parameters are sufficient for Minecraft plugins).
- Translating internal registry keys, PDC tags, permission nodes, or database identifiers.

## Decisions

### 1. Resource Format: Hierarchical YAML Flattened to Dot Keys
- **Decision**: Store translations in hierarchical YAML files (`locales/en-US.yml`, `locales/es-ES.yml`) and flatten keys into dot-separated paths (e.g., `command.item.given`).
- **Rationale**: Server administrators are familiar with YAML. It supports multiline lists (essential for item lore and help menus) and handles `<color>`/`<gradient>` markup cleanly without escaping quotation marks.
- **Alternatives Considered**:
  - *Java ResourceBundle (`.properties`)*: Poor multiline support for lore, unsuited for nested groupings.
  - *JSON*: Lacks comments, hostile for manual editing by server administrators.

### 2. Module Boundary & Port-Adapter Architecture
- **Decision**:
  - `croppy-api` declares `MessageKey` (record), `LocaleRef` (record), and `TranslationProvider` (port interface).
  - `croppy-core` implements `YamlLocaleLoader` (using SnakeYAML with `SafeConstructor`), `DefaultMessageCatalog`, and `LocaleValidator`.
  - `croppy-paper` implements `PaperMessageService` adapting Adventure's `Audience` and `MiniMessage`, resolving player locales, and executing command/listener notifications.
- **Rationale**: Keeps domain logic completely independent of Paper or Adventure APIs while enabling unit testing of loading, fallback, and validation in standard JVM tests.

### 3. Locale Resolution & Fallback Precedence
- **Decision**: Implement a 5-tier deterministic fallback order:
  1. Requested player locale (e.g. `es-AR` or `es_AR`).
  2. Base language fallback (e.g. `es`).
  3. Configured server default locale (`config.yml` `messages.default-locale`, default `en-US`).
  4. Bundled source locale (`en-US`).
  5. Missing key placeholder: `<red><missing:key></red>` + logged diagnostic warning.
- **Rationale**: Prevents broken UI or raw nulls when players use regional dialects not explicitly bundled.

### 4. Separation of Framework Messages and Content Localization
- **Decision**:
  - Framework messages (commands, prompts, errors, inspect cards) use dedicated keys (`command.give.success`, `inspector.label.hydration`).
  - Content definitions (`ItemDefinition`, `Species`, `StationDefinition`) maintain their default `displayName` and `lore` in their existing YAML files.
  - `PaperMessageService` checks for optional translation keys matching `content.<category>.<id>.name` or `content.<category>.<id>.lore`. If missing, it uses the definition's native display name.
- **Rationale**: Avoids duplicating dozens of item YAMLs for each language while enabling full item localization for translators via Crowdin.

### 5. Argument Sanitization & MiniMessage Injection Protection
- **Decision**: External parameters (such as player names, unformatted input, raw numbers) are injected using Adventure's `Placeholder.unparsed(key, value)` or converted to `Component.text(value)`.
- **Rationale**: Prevents malicious players from executing exploit tags (e.g. `<click:run_command:/op>`) by embedding markup into chat prompts or item names.

### 6. Atomic Catalog Swapping during Reload
- **Decision**: The active message catalog is encapsulated inside an `AtomicReference<MessageCatalog>`. Hot reloading parses all files into a temporary staging catalog, validates it, and replaces the reference atomically.
- **Rationale**: Eliminates race conditions or partially loaded catalogs during `/croppy reload` on live servers.

## Risks / Trade-offs

- **[Risk] Missing translation log flooding**: A missing key in a high-frequency ticker could flood server logs.
  - *Mitigation*: Cache reported missing keys in a concurrent set; log each missing key at most once per server run.
- **[Risk] Regional locale variants (`es_ES` vs `es-ES`)**: Minecraft clients and Java use varying locale string formats.
  - *Mitigation*: Normalize all locale identifiers using `LocaleRef.from(String)` which standardizes on lower-case language and upper-case country tags (e.g. `en-US`).
- **[Risk] Crowdin PRs breaking build with invalid YAML**:
  - *Mitigation*: Add an automated GitHub Actions step running `LocaleValidatorTest` to fail PRs with malformed YAML before merging.

## Migration Plan

1. Create pure API contracts in `croppy-api`.
2. Implement YAML loading, fallback chain, catalog, and validation in `croppy-core`.
3. Implement `PaperMessageService` and Adventure rendering in `croppy-paper`.
4. Create default `locales/en-US.yml` (source) and `locales/es-ES.yml` (bundled secondary).
5. Externalize all hardcoded strings in commands, listeners, `CropInspectorService`, `ChatPromptService`, and station interact handlers.
6. Add `crowdin.yml` and `.github/workflows/` CI scripts.
7. Verify all unit tests and locale validation gates pass.
