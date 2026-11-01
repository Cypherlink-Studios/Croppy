# heat-generation Specification

## Purpose
Provides active thermal generation, temperature regulation, fuel consumption scaling, and visual thermal feedback for dedicated heat source stations and burners.

## Requirements

### Requirement: Heat Source Station Component
The system SHALL attach an active heat source component to stations configured with heat generation properties, tracking operational status, target temperature, current output temperature, fuel burn ticks, and station thermal limits.

#### Scenario: Heat source activation and convergence
- **WHEN** a heat source station with available fuel is activated
- **THEN** it burns fuel and converges its output temperature towards its target temperature according to its configured heating rate

#### Scenario: Inactive or depleted heat source cooling
- **WHEN** an active heat source station runs out of fuel or is deactivated
- **THEN** it ceases active heat generation and cools down towards ambient temperature according to its configured cooling rate

#### Scenario: Empty air thermal dissipation
- **WHEN** an active heat source station operates with no station or cookware placed above it
- **THEN** it continues to consume fuel and maintain its output temperature in open air

### Requirement: Scaled Fuel Consumption and Modular Providers
The system SHALL resolve fuel burn duration and properties through modular fuel providers and scale fuel consumption rate proportionally to the operating temperature relative to the station thermal limits.

#### Scenario: Scaled fuel burn rate
- **WHEN** a heat source station operates at maximum configured temperature
- **THEN** fuel burn ticks are consumed at an accelerated rate compared to operation at minimum simmer temperature

#### Scenario: Fuel replenishment from storage
- **WHEN** the current fuel combustion cycle finishes and valid fuel items remain in the station fuel input slot
- **THEN** the next fuel item is consumed, replenishing remaining burn ticks

#### Scenario: Unsupported fuel item rejection
- **WHEN** an item not recognized by registered fuel providers is placed into the fuel slot
- **THEN** the station does not consume the item and reports no fuel available

### Requirement: Temperature Control and Chat Selection Prompt
The system SHALL provide temperature adjustment via control menu buttons and a dedicated chat prompt service allowing exact degree numerical input within configured station bounds.

#### Scenario: Stepwise temperature adjustment
- **WHEN** a player clicks temperature increment or decrement buttons in the station GUI
- **THEN** the target temperature changes by the configured step delta without exceeding minimum or maximum station limits

#### Scenario: Chat prompt valid input
- **WHEN** a player clicks the chat temperature button, receives a chat prompt, and enters a valid number within station limits
- **THEN** the station target temperature updates to the entered value, feedback is provided, and the station GUI reopens immediately

#### Scenario: Chat prompt invalid input or cancellation
- **WHEN** a player enters a temperature outside configured bounds or types cancel
- **THEN** the station target temperature remains unchanged and an informative error message is displayed

### Requirement: In-World Colored Dust Feedback
The system SHALL emit colored DUST particles at active heat source stations reflecting the current output temperature.

#### Scenario: Warm simmer particle display
- **WHEN** an active heat source operates below 90°C
- **THEN** amber-orange dust particles emit from the burner surface

#### Scenario: High temperature particle display
- **WHEN** an active heat source operates at high searing or alchemical temperatures
- **THEN** bright incandescent red or high-energy particles emit from the burner surface
