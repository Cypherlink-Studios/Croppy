# Spec Delta: companion-resource-pack

## MODIFIED Requirements

### Requirement: Automated Addon Asset Merging
The companion resource pack compiler SHALL discover `assets/` subdirectories inside enabled addons and merge their models, textures, and sounds into the compiled companion resource pack output file at `plugins/Croppy/exports/croppy-unified-pack.zip`, computing its SHA-1 checksum upon completion.

#### Scenario: Merging addon models and textures
- **WHEN** an addon containing `assets/custom/models/item/wand.json` is enabled
- **THEN** the compiled resource pack output includes `assets/custom/models/item/wand.json` alongside base Croppy assets

#### Scenario: Addon asset cleanup on disable
- **WHEN** an addon is disabled or unloaded
- **THEN** its asset tree is excluded from subsequent resource pack builds without corrupting base Croppy assets

#### Scenario: Computing SHA-1 checksum
- **WHEN** the resource pack compilation finishes successfully
- **THEN** the system generates an exact 40-character hexadecimal SHA-1 digest matching the ZIP archive contents

## ADDED Requirements

### Requirement: Embedded Resource Pack HTTP Server
The system SHALL provide an embedded HTTP server using standard Java 21 runtime facilities (`jdk.httpserver`) that binds to a configurable host and port, serving the active unified resource pack archive over HTTP with proper `Content-Type: application/zip`, `Content-Length`, and `ETag` headers.

#### Scenario: Serving the compiled pack over HTTP
- **WHEN** a client performs an HTTP `GET` request to the configured pack endpoint
- **THEN** the server streams the resource pack ZIP archive with status 200, matching content length, and an ETag header corresponding to the SHA-1 checksum

#### Scenario: Graceful shutdown without thread leaks
- **WHEN** the plugin is disabled or reloaded
- **THEN** the embedded HTTP server terminates immediately, closing listening sockets and releasing executor threads

### Requirement: In-Game Resource Pack Dispatch and Delivery
The system SHALL support dispatching the compiled companion resource pack to connected players using Paper's resource pack delivery API, prompting players with a configurable MiniMessage prompt.

#### Scenario: Auto-sending pack on player join
- **WHEN** a player joins the server and `resource-pack.delivery.auto-send-on-join` is enabled
- **THEN** the system automatically prompts the player to download the active resource pack URL with its SHA-1 hash

#### Scenario: Logging resource pack status telemetry
- **WHEN** a player accepts, successfully downloads, or declines the resource pack prompt
- **THEN** the system captures the event telemetry and logs diagnostic status without interrupting player gameplay
