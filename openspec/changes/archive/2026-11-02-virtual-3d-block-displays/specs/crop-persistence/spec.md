# Spec Delta

## MODIFIED Requirements

### Requirement: Chunk PersistentDataContainer Storage
The system SHALL serialize and deserialize crop instances within the Chunk PersistentDataContainer using compact binary encoding supporting schema versioning.

#### Scenario: Saving crops to chunk PDC
- **WHEN** crops in a chunk are saved
- **THEN** their states including hydration day, active fertilizer, and orientation yaw SHALL be encoded using Version 3 binary format and written to the chunk PDC

#### Scenario: Loading crops from chunk PDC
- **WHEN** crops in a chunk are loaded
- **THEN** the byte array SHALL be read from the chunk PDC and deserialized into crop instances

#### Scenario: Backwards compatibility for legacy binary format
- **WHEN** reading legacy Version 1 or Version 2 binary crop data
- **THEN** the system SHALL deserialize all fields, defaulting missing fields (yaw to 0.0, lastWateredDay to unwatered, fertilizer to null) without data corruption

#### Scenario: Empty chunk cleans PDC key
- **WHEN** a chunk has zero remaining crops
- **THEN** the storage key SHALL be removed from the chunk PDC

### Requirement: SQLite Database Storage with HikariCP
The system SHALL persist crop instances to an indexed SQLite database using HikariCP connection pooling supporting hydration, fertilizer, and orientation columns.

#### Scenario: Database table and index initialization
- **WHEN** the SQLite storage backend initializes
- **THEN** the croppy_crops table SHALL include columns for last_watered_day, fertilizer, and yaw, and the chunk spatial index SHALL be verified

#### Scenario: Atomic chunk batch save
- **WHEN** crops in a chunk are saved
- **THEN** records SHALL be inserted or updated with hydration, fertilizer, and yaw state using batch operations
