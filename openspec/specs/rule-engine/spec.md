# rule-engine Specification

## Purpose
Provides a declarative rule system that intercepts simulation events, evaluates AST condition expressions, and applies configurable gameplay consequences and penalties across the plugin.

## Requirements

### Requirement: Declarative Rule Definitions
The system SHALL load immutable `RuleDefinition` models from YAML files in `rules/`, specifying trigger types, execution priorities, optional station scope filters, condition expressions, and target consequences.

#### Scenario: Register rule definition
- **WHEN** a rule definition file defining an overcooking penalty is loaded
- **THEN** it is registered into the rule registry sorted by priority and bound to its target trigger

#### Scenario: Register rule definition with station filter
- **WHEN** a rule definition file defining a station filter with mode `INCLUDE` or `EXCLUDE` and station keys is loaded
- **THEN** it is registered into the rule registry sorted by priority and bound to its target trigger, matching only stations permitted by the filter

#### Scenario: Register rule definition without station filter
- **WHEN** a rule definition file without an explicit `stations` section is loaded
- **THEN** it is registered and treated as applicable to all station types

### Requirement: Condition Expression Interception
The system SHALL evaluate registered rules during station simulation ticks and interactions against an `EvaluationContext` containing station, process, world, and player variables using the Pratt AST condition engine.

#### Scenario: Evaluate overcooking condition
- **WHEN** a station process finishes and remains on high heat past the configured idle tick threshold
- **THEN** the rule condition expression evaluates to true and triggers consequence execution

### Requirement: Rule Consequence Execution
The system SHALL execute configured consequence actions when a rule triggers, supporting output item replacement, quality reduction, player damage, particle emission, and audio feedback.

#### Scenario: Apply burnt food penalty
- **WHEN** an overcooking rule triggers on a finished cooking station
- **THEN** the station output items are replaced with `croppy:burnt_food`, dense smoke particles emit, and a burn sound plays

#### Scenario: Apply thermal burn injury to player
- **WHEN** a player interacts with a scorching station bare-handed and a contact rule triggers
- **THEN** the player receives minor thermal damage, an injury sound plays, and a warning alert is sent

### Requirement: Visual Layout Rule Resolution
The system SHALL support `RuleTrigger.DISPLAY_LAYOUT` rules evaluated against an `EvaluationContext` containing station and environmental display metrics (including `display.headroom`), resolving with first-match semantics where only the highest-priority rule whose conditions are satisfied executes.

#### Scenario: Evaluate highest priority display layout rule
- **WHEN** multiple `DISPLAY_LAYOUT` rules match an evaluated station and satisfy condition expressions
- **THEN** only the single highest-priority matching rule is selected and its actions are applied as the active visual layout override

#### Scenario: No display layout rules match
- **WHEN** no `DISPLAY_LAYOUT` rules satisfy condition expressions for a station
- **THEN** the station retains its base visual configuration without overrides

