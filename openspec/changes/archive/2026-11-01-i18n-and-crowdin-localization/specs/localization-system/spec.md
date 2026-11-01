# Spec Delta

## Purpose

Provides a centralized, extensible internationalization and localization system that resolves localized MiniMessage text templates per player locale with deterministic fallbacks, automated resource validation, and Crowdin synchronization.

## ADDED Requirements

### Requirement: Message Catalog and YAML Resource Loading
The system SHALL parse hierarchical YAML locale resource files from disk or bundled JAR resources into flattened dot-delimited translation keys stored within an in-memory, thread-safe message catalog.

#### Scenario: Successful locale loading
- **WHEN** the plugin initializes or reloads localization resources
- **THEN** YAML files in the `locales/` directory are parsed, flattened into key-value pairs, and registered in the active message catalog.

#### Scenario: Malformed YAML handling
- **WHEN** a locale YAML file contains syntax errors or invalid mapping structures
- **THEN** the system logs a diagnostic error identifying the offending file and line, skips the corrupted file, and preserves previously loaded catalogs.

### Requirement: Deterministic Locale Fallback Resolution
The system SHALL resolve requested translation keys using a deterministic fallback order: requested client locale, base language, configured server default locale, bundled source locale (`en-US`), and final safe missing-key token.

#### Scenario: Direct locale match
- **WHEN** a player with client locale `es-ES` triggers a message and `es-ES.yml` defines the corresponding key
- **THEN** the message is resolved using the `es-ES` translation template.

#### Scenario: Regional dialect fallback to base language
- **WHEN** a player with client locale `es-AR` triggers a message not defined in `es-AR` but present in `es`
- **THEN** the system falls back to and resolves the translation from `es`.

#### Scenario: Fallback to source locale
- **WHEN** a translation key is missing from the player's language and server default locale
- **THEN** the system resolves the translation from the bundled `en-US` source catalog.

#### Scenario: Completely missing translation key
- **WHEN** a queried translation key does not exist in any locale or fallback catalog
- **THEN** the system returns a safe formatted token `<missing:key>` and logs a single diagnostic warning per session.

### Requirement: Dynamic Arguments and MiniMessage Rendering
The system SHALL render localized templates into Adventure Components using MiniMessage, inserting dynamic arguments while sanitizing untrusted inputs to prevent formatting tag injection.

#### Scenario: Format message with dynamic placeholders
- **WHEN** a message template containing `<player>` and `<amount>` is rendered with arguments
- **THEN** the resulting Component displays the substituted values with declared styling intact.

#### Scenario: Untrusted argument sanitization
- **WHEN** a dynamic argument contains MiniMessage formatting tags such as `<red>` or `<click:run_command:/op>`
- **THEN** the argument is escaped or inserted as plain text, preventing unauthorized tag parsing or formatting injection.

### Requirement: Content Definition Localization Overrides
The system SHALL support localized display names and lore lines for custom items, crop species, and stations by checking for content translation keys before falling back to native definition fields.

#### Scenario: Localized item name resolution
- **WHEN** generating an item stack or GUI element for a player and a translation key `content.item.<namespace>.<value>.name` exists for their locale
- **THEN** the item displays the localized name instead of the default display name from its item definition YAML.

#### Scenario: Fallback to definition default name
- **WHEN** no content localization override exists for the target locale
- **THEN** the item retains its default display name as declared in the content YAML file.

### Requirement: Automated Locale Validation and Diagnostics
The system SHALL validate localization resource bundles during build execution and startup, comparing secondary locales against the source locale for missing keys, empty values, and structural syntax errors.

#### Scenario: Structural validation in test suite
- **WHEN** automated tests execute against the locale resource bundles
- **THEN** syntax errors, duplicate keys, or missing source keys immediately cause test failure.

#### Scenario: Incomplete secondary locale warning
- **WHEN** a secondary locale file lacks keys present in the source `en-US` locale
- **THEN** the validation system reports missing keys as diagnostic warnings without aborting startup.

### Requirement: Crowdin Synchronization Configuration
The system SHALL maintain a repository-level `crowdin.yml` configuration mapping source files to target translations for automated synchronization, operating without network connections at runtime.

#### Scenario: Crowdin configuration file validation
- **WHEN** Crowdin CLI or GitHub Actions inspects repository configuration
- **THEN** `crowdin.yml` specifies `en-US.yml` as source and maps target translations to `locales/%locale%.yml`.

#### Scenario: Runtime independence from external services
- **WHEN** the plugin starts or operates in a disconnected or air-gapped server environment
- **THEN** the plugin loads local locale files without attempting any network requests to Crowdin.
