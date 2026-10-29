# Tasks

## 1. Dependencies and Configuration Setup

- [x] 1.1 Add `org.incendo:cloud-paper:2.0.1` and `org.incendo:cloud-annotations:2.0.0` to `croppy-paper/build.gradle.kts` and verify with `./gradlew compileJava`.
- [x] 1.2 Update `config.yml` with `inspection-tool` configuration (material, name, lore) and customizable MiniMessage feedback messages.

## 2. Argument Parsers and Inspection Service

- [x] 2.1 Implement `SpeciesParser` for Incendo Cloud 2.0, providing type-safe `Species` parsing by `Key` and suggestion provider auto-completing registered species.
- [x] 2.2 Implement `CropInspectorService` providing MiniMessage status card generation (species info, stage index/total, progress bar, light requirements vs ambient, water fluid level vs required, and growth readiness).
- [x] 2.3 Implement unit tests in `croppy-paper` validating inspection card generation, progress bar math, and condition formatting.

## 3. Command Handlers and Cloud Manager Setup

- [x] 3.1 Initialize `PaperCommandManager<CommandSourceStack>` using `PaperCommandManager.builder().executionCoordinator(...).buildOnEnable(plugin)` and configure `AnnotationParser`.
- [x] 3.2 Implement `CroppyCommands` with `/croppy inspect [x y z]` supporting both raycast target block lookup within 5 blocks and explicit coordinate parameters.
- [x] 3.3 Implement `/croppy tool` in `CroppyCommands` creating an ItemStack with PDC key `croppy:tool` from `config.yml` settings.
- [x] 3.4 Implement `/croppy reload` in `CroppyCommands` re-reading `config.yml` and reloading species YAML files into `SpeciesRegistry`.
- [x] 3.5 Implement debug commands `/croppy setstage <stageIndex>` and `/croppy setprogress <progress>` in `CroppyCommands` modifying the targeted crop and updating the block display.
- [x] 3.6 Implement `/croppy give <player> <species> [amount]` in `CroppyCommands` to deposit species items into the target player's inventory.

## 4. Interactive Inspection Listener and Lifecycle Registration

- [x] 4.1 Implement `InspectionToolListener` detecting block right-clicks with the PDC-tagged tool item, cancelling interaction, and triggering inspection.
- [x] 4.2 Wire and register `InspectionToolListener`, `CropInspectorService`, and `CroppyCommands` in `CroppyPlugin.onEnable()`.

## 5. Build and End-to-End Verification

- [x] 5.1 Run `./gradlew test` across all modules to verify that all unit tests pass without regressions.
- [x] 5.2 Run `./gradlew build` to verify full compilation and proper `shadowJar` artifact assembly.

## Workflow follow-up

- Archive the change after verification is complete.
- Verify the archived result and commit to git branch `main`.
