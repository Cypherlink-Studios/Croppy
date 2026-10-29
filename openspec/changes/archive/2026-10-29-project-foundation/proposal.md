# Proposal: Project Foundation & Core Architecture

## Why

Croppy requires a clean, extensible architectural foundation before implementing complex agricultural simulations, ingredient tracking, or cooking recipes. To prevent coupling domain logic to Minecraft server internals (Paper/Bukkit) and enable fast headless unit testing, we must establish a strictly enforced multi-module build, core contracts, in-memory registries, and a Paper 1.21.8 platform runtime adapter.

## What Changes

- Establish a Gradle Kotlin DSL multi-module project targeting Java 21:
  - `croppy-api`: Pure Java contracts, namespace identifiers (`Key`), registry interfaces, and service abstractions with zero external dependencies.
  - `croppy-core`: Pure Java implementation of core registries and domain lifecycle management, strictly decoupled from Bukkit and testable via JUnit 5.
  - `croppy-paper`: Paper 1.21.8 runtime adapter, `paper-plugin.yml` descriptor, `CroppyPlugin` entry point, and fatjar/shadow distribution packaging.
- Implement decoupled identifiers (`Key` records like `croppy:wheat`) to replace direct reliance on Bukkit `NamespacedKey` across domain models.
- Implement an in-memory `SimpleRegistry<T>` providing deterministic registration, lookup, and unmodifiable views.
- Implement a unified lifecycle orchestrator (`CroppyCore`) coordinating startup, service registration, and clean shutdown.
- Configure JUnit 5 headless test suite in `croppy-core` verifying registry mechanics and lifecycle states without needing a running server or Bukkit mocks.

## Capabilities

### New Capabilities
- `core-registry`: Namespaced key identifiers and generic registry contracts for deterministic registration and lookup of domain objects without platform dependencies.
- `plugin-lifecycle`: Decoupled core service lifecycle management and Paper 1.21.8 runtime initialization bridge.

### Modified Capabilities
<!-- None: Initial project foundation -->

## Impact

- Scaffolds root repository structure with `settings.gradle.kts` and subprojects.
- Configures Paper 1.21.8 dependency via Paperweight/userdev in `croppy-paper`.
- Establishes `dev.darkblade.croppy` as the root package.
- All future changes (species, growth, persistence, cooking) will build upon these clean module boundaries.
