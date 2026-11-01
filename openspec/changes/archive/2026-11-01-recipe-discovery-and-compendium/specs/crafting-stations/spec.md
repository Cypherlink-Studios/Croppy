# Spec Delta: crafting-stations

## ADDED Requirements

### Requirement: Station Experimental Discovery Support
The system SHALL support experimental discovery in crafting stations, detecting valid undiscovered recipe combinations upon action interaction, granting discovery without consuming ingredients, and updating action state.

#### Scenario: Successful experimental discovery in station
- **WHEN** a player places ingredients matching an undiscovered recipe into a station and triggers the experiment action
- **THEN** the recipe is granted to the player, celebratory audiovisual feedback plays, ingredients remain unconsumed, and the action button updates to cooking execution

#### Scenario: Invalid experimental combination
- **WHEN** a player attempts experimentation with inputs that do not match any discoverable recipe
- **THEN** an informative feedback message is displayed, no ingredients are consumed, and no process starts
