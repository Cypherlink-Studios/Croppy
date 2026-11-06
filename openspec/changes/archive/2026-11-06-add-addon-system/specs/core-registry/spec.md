# Spec Delta

## MODIFIED Requirements

### Requirement: Generic In-Memory Registry
The system SHALL provide an in-memory registry allowing domain objects to be registered, queried, and unregistered by their unique namespaced key in a thread-safe manner.

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

#### Scenario: Unregistering an existing entry by key
- **WHEN** an entry registered under "croppy:test" is unregistered
- **THEN** the registry removes the entry, returns true, and subsequent lookups for "croppy:test" return empty

#### Scenario: Unregistering a non-existent key
- **WHEN** unregistering a key that is not registered
- **THEN** the registry returns false without throwing an exception

## ADDED Requirements

### Requirement: Predicate-Based Bulk Registry Removal
The system SHALL allow bulk unregistration of registry entries matching a provided predicate, returning the count of removed entries.

#### Scenario: Bulk removal by namespace
- **WHEN** unregistering all entries matching namespace "addon_example"
- **THEN** all entries registered under "addon_example" are removed atomically while entries with other namespaces remain unaffected
