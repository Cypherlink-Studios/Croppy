# Spec Delta: crop-commands

## MODIFIED Requirements

### Requirement: Interactive Inspection Tool
The system SHALL support an interactive inspection tool item defined in the item registry and tagged with PersistentDataContainer key `croppy:item_id` set to `croppy:inspection_tool` backed by a neutral base material (`minecraft:stick`), inspecting crops on physical block right-click, virtual interaction entity right-click, or raytraced line-of-sight right-click (including supporting farmland), distributable through the centralized item command without relying on dedicated tool appearance keys in `config.yml`.

#### Scenario: Issue inspection tool
- **WHEN** an administrator runs `/croppy item give tools croppy:inspection_tool`
- **THEN** the system places the configured inspection tool item into the player's inventory backed by `minecraft:stick` with custom item model `croppy:inspection_tool`.

#### Scenario: Right-click crop with inspection tool
- **WHEN** a player holding the inspection tool right-clicks a registered crop block or aims at it within reach
- **THEN** the interaction is cancelled at lowest priority, preventing block placement or third-party wand actions, and the crop's real-time status card is displayed.

#### Scenario: Right-click non-crop block with tool
- **WHEN** a player holding the inspection tool right-clicks an unregistered block or air with no crop in line of sight
- **THEN** the interaction is cancelled and a notification indicates no crop exists at the targeted location.

#### Scenario: Right-click virtual interaction entity with inspection tool
- **WHEN** a player holding the inspection tool right-clicks a crop's virtual interaction entity
- **THEN** the interaction is intercepted, preventing crop harvesting or damage, and the crop's real-time status card is displayed.

#### Scenario: Right-click supporting farmland with inspection tool
- **WHEN** a player holding the inspection tool right-clicks farmland supporting an active crop
- **THEN** the interaction resolves the crop positioned immediately above the soil and displays its real-time status card.
