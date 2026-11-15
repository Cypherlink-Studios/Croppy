# Spec Delta

## Purpose

Provides an extensible, type-safe variable management engine allowing domain entities and stations to declare, mutate, evaluate, and persist arbitrary simple and compound typed variables.

## ADDED Requirements

### Requirement: Extensible Data Type Registry
The system SHALL maintain a registry of `DataType` definitions identified by unique `Key` identifiers, supporting both atomic simple types (`croppy:integer`, `croppy:decimal`, `croppy:boolean`, `croppy:string`) and composite compound types (`croppy:compound`, custom addon structures).

#### Scenario: Register standard primitive data types
- **WHEN** the typed variable system initializes
- **THEN** core data types for integers, decimals, booleans, strings, and compounds are registered and retrievable by their canonical keys

#### Scenario: Register custom addon data type
- **WHEN** an external addon registers a custom data type with binary serializer and supported operations
- **THEN** the custom type is registered in the data type registry and available for variable instantiation

### Requirement: Type-Safe Variable Mutation Operations
The system SHALL enforce strict type safety during variable mutations by requiring each data type to declare supported `VariableOperation` implementations, rejecting unsupported operations without mutating state.

#### Scenario: Apply valid numeric addition operation
- **WHEN** an `ADD` operation with positive increment is applied to an integer variable
- **THEN** the variable value is updated by the increment and marked dirty

#### Scenario: Apply valid boolean toggle operation
- **WHEN** a `TOGGLE` operation is applied to a boolean variable
- **THEN** the boolean value is inverted to its opposite truth value

#### Scenario: Reject unsupported operation fail-fast
- **WHEN** a numeric `ADD` or `SUBTRACT` operation is attempted on a boolean or string variable
- **THEN** the operation is rejected with an informative validation exception and the variable retains its original value

### Requirement: Typed Entity Variable Storage and Initial Defaults
The system SHALL attach a `TypedVariableStore` to domain entities, populating initial variables from definition specifications and exposing them under the `var.` namespace prefix for context evaluation.

#### Scenario: Initialize default variables from definition
- **WHEN** a station or entity is instantiated with configured `default_variables`
- **THEN** a typed variable instance is created for each declared variable with its validated default value

#### Scenario: Export typed variables to evaluation context
- **WHEN** an evaluation context is built for an entity with stored variables
- **THEN** all simple variables are exported under `var.<name>` and compound properties under `var.<name>.<property>`, alongside formatted representations

### Requirement: Resilient Binary Persistence
The system SHALL serialize and deserialize typed variable stores to binary payloads for SQLite and PersistentDataContainer backends, preserving unrecognized addon types safely without data loss.

#### Scenario: Round-trip binary serialization
- **WHEN** an entity with stored integer, boolean, and compound variables unloads and reloads
- **THEN** all variable keys, types, and values are faithfully restored from binary storage

#### Scenario: Safe preservation of unresolved addon types
- **WHEN** a variable store containing an unregistered addon data type is loaded from persistence
- **THEN** the raw binary payload is preserved in memory as an unresolved variable and written back without throwing exceptions or corrupting adjacent chunk data
