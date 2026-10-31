# Spec Delta

## Purpose

Provides a configurable, extensible, and secure YAML-driven inventory menu engine supporting matrix layouts, dynamic conditional templates, and anti-duplication transaction rules.

## ADDED Requirements

### Requirement: Menu Definition and Pattern Parsing
The system SHALL parse and validate inventory GUI menu configurations from YAML files, mapping 9-column ASCII pattern matrices across 1 to 6 rows into discrete slot elements and character bindings.

#### Scenario: Parse valid 4-row matrix layout
- **WHEN** a YAML menu definition with 4 rows and 9-character pattern rows is loaded
- **THEN** a `MenuDefinition` is constructed with 36 validated slot positions and character element mappings

#### Scenario: Reject mismatched pattern dimensions
- **WHEN** a YAML menu defines 3 rows but provides 4 pattern lines or a line with fewer than 9 characters
- **THEN** a configuration validation error is thrown and loading of that menu fails fast

### Requirement: Functional Slot Roles and Storage Binding
The system SHALL support specialized slot types including `INPUT`, `OUTPUT`, `STATUS`, `ACTION`, and `DECORATION`, sequentially binding `INPUT` and `OUTPUT` slots to the backing storage component slots in order of pattern appearance.

#### Scenario: Sequential multi-slot storage binding
- **WHEN** a menu defines 6 input characters ('I') and 2 output characters ('O') in its pattern
- **THEN** the input slots map to storage inputs indices 0 through 5 and output slots map to storage output indices 0 and 1

#### Scenario: Decorative and action slot click cancellation
- **WHEN** a player clicks on a slot designated as `DECORATION`, `STATUS`, or `ACTION`
- **THEN** the click event is cancelled and no items are transferred or modified

### Requirement: Dynamic Conditional State Evaluation
The system SHALL dynamically evaluate expressions against the runtime evaluation context to select between prioritized conditional item templates for status and indicator slots.

#### Scenario: Running process status rendering
- **WHEN** an open station menu has an active process in `RUNNING` state matching a conditional template
- **THEN** the status slot displays the running template with interpolated recipe name, progress percent, and temperature lore

#### Scenario: Default template fallback
- **WHEN** none of the conditional template expressions evaluate to true
- **THEN** the slot renders the base default item template

### Requirement: Anti-Duplication and Inventory Security
The system SHALL enforce strict inventory interaction rules, preventing item duplication exploits across all player click types, drags, hotbar swaps, and shift-clicks.

#### Scenario: Output slot pickup only
- **WHEN** a player attempts to place an item into or drag an item over an `OUTPUT` slot
- **THEN** the interaction is cancelled and only item extraction is permitted

#### Scenario: Shift-click routing to input slots
- **WHEN** a player shift-clicks a valid ingredient from their player inventory
- **THEN** the item is routed strictly to the first available `INPUT` slot without overflowing into decorative or output slots

#### Scenario: Shift-click cancelled when input slots full
- **WHEN** a player shift-clicks an item from their player inventory while all `INPUT` slots are occupied
- **THEN** the shift-click event is cancelled and no items move

#### Scenario: Block hotbar number key swap on non-input slots
- **WHEN** a player presses a hotbar number key (1-9) or offhand swap key while hovering over a non-input slot
- **THEN** the swap action is cancelled immediately

### Requirement: Action Execution and Command Dispatch
The system SHALL execute configured actions upon player click on `ACTION` slots, including station process management, console or player commands, audio feedback, and menu navigation.

#### Scenario: Trigger station process start action
- **WHEN** a player clicks an action button configured with `STATION_ACTION` targeting `START_PROCESS`
- **THEN** the station process engine attempts to start a matching recipe for the current inputs and feedback is emitted

#### Scenario: Condition-gated action execution
- **WHEN** an action specifies an execution condition that evaluates to false
- **THEN** the action does not execute and the player is notified or sound is suppressed

### Requirement: Lightweight Menu Ticking and Dynamic Refresh
The system SHALL periodically refresh status indicators and placeholder lore for open menu viewers during active processes without reconstructing the underlying inventory container.

#### Scenario: Periodic status slot update
- **WHEN** an inventory viewer remains inside a station menu while a cooking recipe progresses
- **THEN** a background menu ticker updates the status item stack metadata and progress lore without clearing or resetting player cursor items
