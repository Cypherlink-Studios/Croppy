# crop-persistence Specification

## Purpose
Provides durable persistence for crop instances across server restarts and chunk unloads, supporting hybrid backends (Paper Chunk PDC and SQLite via HikariCP).

## Requirements

### Requirement: Configurable Storage Backend Selection
The system SHALL support configurable storage engine backends selected via configuration.

#### Scenario: PDC storage engine selection
- **WHEN** configuration specifies storage type as pdc
- **THEN** the system SHALL initialize and use Chunk PersistentDataContainer storage

#### Scenario: SQLite storage engine selection
- **WHEN** configuration specifies storage type as sqlite
- **THEN** the system SHALL initialize and use SQLite storage with HikariCP connection pooling

### Requirement: Chunk PersistentDataContainer Storage
The system SHALL serialize and deserialize crop instances within the Chunk PersistentDataContainer using compact binary encoding supporting schema versioning.

#### Scenario: Saving crops to chunk PDC
- **WHEN** crops in a chunk are saved
- **THEN** their states including hydration day and active fertilizer SHALL be encoded using Version 2 binary format and written to the chunk PDC

#### Scenario: Loading crops from chunk PDC
- **WHEN** crops in a chunk are loaded
- **THEN** the byte array SHALL be read from the chunk PDC and deserialized into crop instances

#### Scenario: Backwards compatibility for legacy binary format
- **WHEN** reading legacy Version 1 binary crop data
- **THEN** the system SHALL deserialize all fields, defaulting lastWateredDay to unwatered and fertilizer to null without data corruption

#### Scenario: Empty chunk cleans PDC key
- **WHEN** a chunk has zero remaining crops
- **THEN** the storage key SHALL be removed from the chunk PDC

### Requirement: SQLite Database Storage with HikariCP
The system SHALL persist crop instances to an indexed SQLite database using HikariCP connection pooling supporting hydration and fertilizer columns.

#### Scenario: Database table and index initialization
- **WHEN** the SQLite storage backend initializes
- **THEN** the croppy_crops table SHALL include columns for last_watered_day and fertilizer and the chunk spatial index SHALL be verified

#### Scenario: Atomic chunk batch save
- **WHEN** crops in a chunk are saved
- **THEN** records SHALL be inserted or updated with hydration and fertilizer state using batch operations

### Requirement: Dynamic Runtime Library Management via Libby
The system SHALL utilize Libby Paper to dynamically download database driver libraries only when the SQLite storage backend is active.

#### Scenario: SQLite enabled triggers library download
- **WHEN** the SQLite backend is selected in configuration
- **THEN** Libby SHALL download and inject HikariCP and SQLite JDBC into the plugin classloader

#### Scenario: PDC backend skips database library download
- **WHEN** the PDC backend is selected in configuration
- **THEN** database libraries SHALL not be downloaded or loaded
