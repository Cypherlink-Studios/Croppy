# Spec Delta

## ADDED Requirements

### Requirement: Complete Starter Pack Model Binding
The system SHALL ensure every bundled starter-pack item definition in `items/` declares an explicit, valid `item-model` or `item_model` key corresponding to a dedicated native model definition in the companion resource pack, avoiding unconfigured fallback materials.

#### Scenario: Verify item model binding across starter pack
- **WHEN** all bundled starter-pack YAML item definitions are parsed and registered by `ItemRegistry`
- **THEN** every item definition (including fertilizers, seeds, produce, tools, and watering cans) possesses a non-null `itemModel` key targeting a valid `croppy:*` model definition

#### Scenario: Support green and purple grape disambiguation
- **WHEN** the item registry loads fruit ingredients
- **THEN** `croppy:green_grapes` and `croppy:purple_grapes` are registered as independent item definitions with their respective item models
