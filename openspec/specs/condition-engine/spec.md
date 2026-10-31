# condition-engine Specification

## Purpose
Provides a lightweight, zero-allocation condition and expression evaluation engine supporting compact string syntax, local contextual variables, and external PlaceholderAPI integration.

## Requirements

### Requirement: Compact Expression Parsing
The condition engine SHALL parse compact string expressions containing comparison operators (`==`, `!=`, `<`, `<=`, `>`, `>=`), logical combinators (`&&`, `||`, `!`), grouping parentheses, and basic arithmetic (`+`, `-`, `*`, `/`) into an in-memory Abstract Syntax Tree.

#### Scenario: Parse and evaluate valid comparison
- **WHEN** expression `"temperature >= 100"` is evaluated with `temperature = 105`
- **THEN** evaluation returns `true`

#### Scenario: Parse and evaluate compound logical expression
- **WHEN** expression `"tier >= 2 && (fuel > 0 || is_creative == true)"` is evaluated with `tier = 2`, `fuel = 0`, and `is_creative = true`
- **THEN** evaluation returns `true`

#### Scenario: Syntax error handling
- **WHEN** an invalid expression string with unclosed parentheses or unknown tokens is submitted
- **THEN** parsing fails fast throwing a descriptive `IllegalArgumentException` during configuration load

### Requirement: Evaluation Context and Variable Resolution
The evaluation engine SHALL resolve variables dynamically through an extensible `EvaluationContext` holding typed local scope variables and delegating to registered variable resolvers.

#### Scenario: Resolve local scope variable
- **WHEN** variable `"station.temperature"` is queried in a context holding local variable `120.0`
- **THEN** resolver returns the numeric value for comparison

#### Scenario: Missing variable fallback
- **WHEN** an unknown variable is referenced in an expression
- **THEN** the resolver returns empty and the condition evaluates according to null-safe equality rules without throwing an uncaught exception

### Requirement: External PlaceholderAPI Resolution
The engine SHALL support resolving external Minecraft placeholders enclosed in percent signs (`%...%`) when evaluated within an actor context on Paper servers.

#### Scenario: Resolve player placeholder on Paper
- **WHEN** expression `"%vault_eco_balance% >= 50.0"` is evaluated with a player actor who has 100 balance
- **THEN** PlaceholderAPI resolver provides the numerical value and expression evaluates to `true`

#### Scenario: PlaceholderAPI absent fallback
- **WHEN** a placeholder expression is evaluated on a server where PlaceholderAPI is not present
- **THEN** the placeholder returns empty or unmodified string without breaking engine simulation
