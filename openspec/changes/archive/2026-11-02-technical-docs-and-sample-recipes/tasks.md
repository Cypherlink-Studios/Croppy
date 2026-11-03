# Tasks

## 1. Bundled Sample Recipes & Food Items

- [x] 1.1 Author default food item YAML files (`croppy:tomato_soup`, `croppy:vegetable_stew`, `croppy:fried_egg`) under `croppy-paper/src/main/resources/items/ingredients/` and verify they conform to `YamlItemLoader`.
- [x] 1.2 Author default recipe YAML files (`tomato_soup.yml`, `vegetable_stew.yml`, `fried_egg.yml`) under `croppy-paper/src/main/resources/recipes/` and verify they parse cleanly via `YamlRecipeLoader`.
- [x] 1.3 Update `CroppyPlugin.java` to invoke `saveResourceIfNotExists` for the new default recipes and food items during initialization and verify compilation with `./gradlew compileJava`.

## 2. Technical Documentation Pages (English)

- [x] 2.1 Author `docs/en/recipes-configuration.md` covering `YamlRecipeLoader` YAML schema, inputs/results, discovery hints, and step-by-step authoring tutorial.
- [x] 2.2 Author `docs/en/workstations-configuration.md` covering `YamlStationLoader` schema, PacketEvents virtual 3D display layers (`process_display`, `text_display`), and thermal properties.
- [x] 2.3 Author `docs/en/rules-and-interactions.md` covering `YamlRuleLoader` (Pratt AST condition grammar and actions) and `YamlInteractionLoader` (triggers and filters).
- [x] 2.4 Update `docs/en/items-and-tools.md` and `docs/en/soil-and-fertilizers.md` with complete YAML schema references for custom items, modern `item_model` data components, and fertilizers.

## 3. Technical Documentation Pages (Spanish)

- [x] 3.1 Author `docs/es/recipes-configuration.md` in Spanish with identical schema breakdowns and tutorial.
- [x] 3.2 Author `docs/es/workstations-configuration.md` in Spanish with identical station, display, and thermal specifications.
- [x] 3.3 Author `docs/es/rules-and-interactions.md` in Spanish with identical rule AST and interaction documentation.
- [x] 3.4 Update `docs/es/items-and-tools.md` and `docs/es/soil-and-fertilizers.md` in Spanish with matching item and fertilizer schema references.

## 4. Navigation Re-indexing & Verification

- [x] 4.1 Re-index sidebar order attributes across all 18 markdown files in `docs/en/` and `docs/es/` (1 to 18) to ensure consistent Starlight navigation.
- [x] 4.2 Execute `./gradlew test` and verify that all unit test suites in `croppy-core` pass with 0 failures.
