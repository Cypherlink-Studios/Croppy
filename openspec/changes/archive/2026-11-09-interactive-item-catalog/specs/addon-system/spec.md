# Spec Delta: addon-system

## MODIFIED Requirements

### Requirement: Scoped and Tracked Execution Context
The system SHALL provide each enabled addon with an isolated `AddonContext` tracking all domain registry keys (including species, items, item categories, recipes, recipe categories, processes, stations, menus, and interactions), Bukkit event listeners, scheduled tasks, and growth modifiers registered during its lifecycle.

#### Scenario: Scoped registration tracking
- **WHEN** an enabled addon registers custom items, item categories, species definitions, menus, processes, stations, interactions, recipe categories, or Bukkit event listeners via its `AddonContext`
- **THEN** the system records ownership of those resources under the addon's unique identifier

#### Scenario: Managed async task execution
- **WHEN** an addon schedules asynchronous work through its `AddonContext`
- **THEN** the tasks execute within a managed thread executor monitored for orderly shutdown
