# Tasks: Addon Core Prerequisites (M0)

- [x] 1. `croppy-api`: Add `DatabaseService`, `ProcessListener`, `ItemMetadataContributor`, `ItemMetadataRegistry`, and extend `AddonContext`.
- [x] 2. `croppy-core`: Add `ProcessListener` dispatch to `ProcessEngine`, `DefaultItemMetadataRegistry`, and `StationBinaryCodec` v3 serialization for `StoredItem.properties()` and `ProcessInstance.contextVariables`.
- [x] 3. `croppy-paper`: Implement `SqliteDatabaseService`, wire into `CroppyPlugin` and `PaperAddonContext` (with scoped prefix & resource cleanup), and update `ItemFactory` + `StationGuiListener` for `StoredItem` metadata/quality round-trip.
- [x] 4. Tests: Add unit tests for `ProcessEngine` listener dispatch, `StationBinaryCodec` v3 round-trip, `DatabaseService` table-prefix validation/scoping, and `ItemFactory` metadata round-trip.
- [x] 5. Verify full `./gradlew.bat test` passes with 0 failures.
