# Spec Delta

## MODIFIED Requirements

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

## ADDED Requirements

### Requirement: Visual Layout Rule Resolution
The system SHALL support `RuleTrigger.DISPLAY_LAYOUT` rules evaluated against an `EvaluationContext` containing station and environmental display metrics (including `display.headroom`), resolving with first-match semantics where only the highest-priority rule whose conditions are satisfied executes.

#### Scenario: Evaluate highest priority display layout rule
- **WHEN** multiple `DISPLAY_LAYOUT` rules match an evaluated station and satisfy condition expressions
- **THEN** only the single highest-priority matching rule is selected and its actions are applied as the active visual layout override

#### Scenario: No display layout rules match
- **WHEN** no `DISPLAY_LAYOUT` rules satisfy condition expressions for a station
- **THEN** the station retains its base visual configuration without overrides
