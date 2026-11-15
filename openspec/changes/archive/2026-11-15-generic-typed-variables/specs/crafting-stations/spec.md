# Spec Delta

## MODIFIED Requirements

### Requirement: Declarative Station States and Reactive Evaluation
The system SHALL support configuring prioritized declarative station states in workstation definitions, resolving the active state reactively from physical station components (thermal, storage, heat source, and process state) and typed variable stores (`var.*`) using AST condition expressions without requiring persistent state records.

#### Scenario: Resolve station state by expression priority
- **WHEN** a workstation instance ticks with components matching multiple state conditions
- **THEN** the state definition with the highest priority whose condition expression evaluates to true is resolved as the active state

#### Scenario: Fallback to default state when conditions are unmet
- **WHEN** a workstation instance ticks and none of the declared conditional expressions evaluate to true
- **THEN** the workstation resolves to the fallback default state inheriting base visual and hitbox configurations

#### Scenario: Recalculate station state on chunk restore
- **WHEN** a chunk containing workstations is loaded into memory from persistent storage
- **THEN** the active state for each restored workstation instance is evaluated dynamically from its restored physical components without reading a dedicated persisted state field

#### Scenario: Resolve state using namespaced component variables
- **WHEN** a workstation state condition specifies expressions using component namespaces such as `thermal.temp >= 100.0 && process.state == 'RUNNING'` or `heat.fuel_ticks > 0`
- **THEN** the AST expression evaluates against the modular component variable providers successfully

#### Scenario: Resolve state using arbitrary typed variables
- **WHEN** a workstation state condition specifies expressions referencing custom variables such as `var.is_covered == true && var.cleanliness > 50`
- **THEN** the AST expression resolves the values from the station's typed variable store and evaluates the condition

#### Scenario: Backward-compatible variable alias resolution
- **WHEN** an existing workstation configuration evaluates legacy un-namespaced identifiers like `temperature` or `fuel_ticks`
- **THEN** the evaluation context resolves them to their canonical component counterparts preserving backwards compatibility
