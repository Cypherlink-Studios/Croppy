# Tasks

## 1. Grape Model UV Normalization

- [x] 1.1 Update `grape_plant_stage_0.json` and `grape_plant_stage_1.json` element face UV coordinates to normalized `[0, 0, 16, 16]`, remove non-standard `"texture_size"` metadata, and verify valid JSON structure
- [x] 1.2 Update `grape_plant_stage_2.json` and `grape_plant_stage_3.json` element face UV coordinates to normalized `[0, 0, 16, 16]`, remove non-standard `"texture_size"` metadata, and verify valid JSON structure
- [x] 1.3 Update `grape_plant_stage_4.json` and `grape_plant_stage_5.json` element face UV coordinates to normalized `[0, 0, 16, 16]`, remove non-standard `"texture_size"` metadata, and verify valid JSON structure

## 2. Resource Pack Audit & Regression Verification

- [x] 2.1 Run an automated UV coordinate audit script across all JSON files in `resourcepack/assets/croppy/models/` and verify 0 occurrences of UV coordinates exceeding 16.0
- [x] 2.2 Execute `./gradlew test` and verify that `CropResourcesIntegrityTest` and the full multi-module test suite pass with 0 errors
