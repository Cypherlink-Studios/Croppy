# Tasks: Project Foundation & Core Architecture

## 1. Project & Gradle Multi-Module Scaffolding

- [x] 1.1 Create root `settings.gradle.kts`, `build.gradle.kts`, and Gradle wrapper properties for Java 21 multi-module structure (`croppy-api`, `croppy-core`, `croppy-paper`) and verify Gradle project evaluation.
- [x] 1.2 Configure `croppy-api` build script targeting pure Java 21 with zero external dependencies.
- [x] 1.3 Configure `croppy-core` build script depending on `croppy-api` and JUnit 5 with pure Java classpath.
- [x] 1.4 Configure `croppy-paper` build script with Paper API 1.21.8 repository, dependencies on `croppy-core`, and Shadow/fatjar packaging.

## 2. API Contracts & Identifiers

- [x] 2.1 Implement `Key` record in `dev.darkblade.croppy.api.identifier.Key` with namespace/value validation and `Keyed` interface.
- [x] 2.2 Implement `Registry<T extends Keyed>` and `Service` interfaces in `dev.darkblade.croppy.api.registry`.

## 3. Core Registry & Lifecycle Implementation

- [x] 3.1 Implement thread-safe `SimpleRegistry<T extends Keyed>` in `dev.darkblade.croppy.core.registry` disallowing duplicate keys and returning unmodifiable views.
- [x] 3.2 Implement `CroppyCore` in `dev.darkblade.croppy.core` orchestrating internal registries and service lifecycle transitions (`initialize`, `shutdown`).
- [x] 3.3 Add JUnit 5 unit tests in `croppy-core` covering `Key` validation, duplicate registration prevention, and `CroppyCore` lifecycle transitions.

## 4. Paper Platform Adapter & Build Verification

- [x] 4.1 Create `paper-plugin.yml` in `croppy-paper/src/main/resources/paper-plugin.yml` targeting Paper 1.21.8.
- [x] 4.2 Implement `CroppyPlugin` extending `JavaPlugin` in `dev.darkblade.croppy.platform.paper` delegating lifecycle hooks to `CroppyCore`.
- [x] 4.3 Run full Gradle build (`gradle build`) and verify compilation, JUnit 5 test execution, and plugin jar packaging.
