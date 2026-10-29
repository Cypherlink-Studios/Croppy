# core-registry Specification

## Purpose
Provides an in-memory, namespaced registry contract and key abstraction for deterministic registration, retrieval, and isolation of domain objects without platform dependencies.

## Requirements

### Requirement: Namespaced Key Representation
The system SHALL represent domain resource identifiers using an immutable key composed of a namespace and a value.

#### Scenario: Valid key creation
- **WHEN** a key is constructed with namespace "croppy" and value "wheat"
- **THEN** the key string representation SHALL be "croppy:wheat"
- **THEN** equality comparison between two keys with identical namespace and value SHALL evaluate to true

#### Scenario: Invalid key formatting
- **WHEN** a key is constructed with null, empty, or whitespace-only components
- **THEN** the system SHALL reject key creation with an IllegalArgumentException

### Requirement: Generic In-Memory Registry
The system SHALL provide an in-memory registry allowing domain objects to be registered and queried by their unique namespaced key.

#### Scenario: Successful entry registration and lookup
- **WHEN** an entry is registered under a unique key "croppy:test"
- **THEN** querying the registry for "croppy:test" SHALL return the registered entry
- **THEN** the entry count SHALL increment by one

#### Scenario: Duplicate registration prevention
- **WHEN** an entry is registered with a key that already exists in the registry
- **THEN** the registry SHALL reject registration with an IllegalStateException and preserve the original entry

#### Scenario: Querying non-existent key
- **WHEN** querying the registry for an unregistered key
- **THEN** the registry SHALL return an empty result without throwing an exception
