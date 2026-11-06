# economy-service Specification

## Purpose

Provides transactional currency operations and financial requirement checks for culinary crafting and progression via decoupled economy ports and Vault integration.

## Requirements

### Requirement: Economy Transaction Operations
The system SHALL support verifying player balances, withdrawing funds, depositing funds, and formatting monetary amounts through an abstract economy port.

#### Scenario: Successful balance check
- **WHEN** the system verifies whether a player has sufficient currency for a transaction
- **THEN** it queries the active economy provider and returns true if the balance meets or exceeds the required amount

#### Scenario: Sufficient funds withdrawal
- **WHEN** a player authorizes a purchase with sufficient balance
- **THEN** the system withdraws the specified amount and returns true

#### Scenario: Insufficient funds withdrawal
- **WHEN** a player attempts a transaction without adequate currency
- **THEN** the withdrawal is rejected, no balance is deducted, and the system returns false

### Requirement: Vault Economy Adapter Integration
The system SHALL bridge transactions to the server's registered Vault Economy service provider when Vault integration is enabled.

#### Scenario: Vault service available
- **WHEN** a financial operation executes on a server with an active Vault economy provider
- **THEN** the system delegates balance queries and transfers directly to the Vault Economy provider

### Requirement: Fallback No-Op Economy Handling
The system SHALL provide a no-op economy adapter when Vault is absent or disabled in configuration, treating all currency queries as non-funded and denying paid operations without errors.

#### Scenario: Vault is not present
- **WHEN** an economy operation is invoked and Vault is not installed
- **THEN** the system gracefully rejects fee-based transactions and logs no exceptions
