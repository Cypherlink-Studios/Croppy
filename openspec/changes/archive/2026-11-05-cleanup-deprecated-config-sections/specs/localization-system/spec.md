# Spec Delta

## MODIFIED Requirements

### Requirement: Deterministic Locale Fallback Resolution
The system SHALL resolve requested translation keys using a deterministic fallback order: requested client locale, base language, configured server default locale (resolved from `locale.default` in `config.yml` with fallback to `messages.default-locale`), bundled source locale (`en-US`), and final safe missing-key token.

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

#### Scenario: Configured default locale resolution
- **WHEN** the plugin initializes or reloads localization settings
- **THEN** the server default locale is resolved from `locale.default` in `config.yml`, falling back to `messages.default-locale` if `locale.default` is not defined.
