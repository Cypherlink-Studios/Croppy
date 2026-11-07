# Tasks: Resource Pack Pipeline & Commands

## 1. API Ports & Contracts (`croppy-api`)

- [x] 1.1 Define `PackBuildResult` immutable record (`Path zipPath`, `String sha1Hex`, `byte[] sha1Digest`, `long sizeBytes`, `int assetCount`, `long durationMs`) in `dev.darkblade.croppy.api.resourcepack` and verify compilation.
- [x] 1.2 Define `ResourcePackCompiler` and `ResourcePackService` interfaces in `croppy-api` with methods for synchronous and asynchronous compilation and player distribution.

## 2. Compilation & Merging Engine (`croppy-core`)

- [x] 2.1 Implement `DefaultResourcePackCompiler` in `croppy-core` using streaming `ZipOutputStream` with `DigestOutputStream` (SHA-1), merging base assets and enabled addon `assets/<namespace>/` subdirectories with regex path validation.
- [x] 2.2 Create JUnit 5 test suite `DefaultResourcePackCompilerTest` in `croppy-core` covering single-source pack generation, multi-addon asset merging, and SHA-1 checksum verification.

## 3. Embedded HTTP Server & Paper Adapter (`croppy-paper`)

- [x] 3.1 Implement `EmbeddedPackHttpServer` in `croppy-paper` using `com.sun.net.httpserver.HttpServer`, serving the compiled pack at `/pack/croppy-unified-pack.zip` with `Content-Type: application/zip`, `Content-Length`, and `ETag` headers.
- [x] 3.2 Implement `PaperResourcePackService` managing asynchronous builds, writing exports to `plugins/Croppy/exports/croppy-unified-pack.zip`, and invoking Paper's `Player.setResourcePack`.
- [x] 3.3 Register `PlayerJoinEvent` listener for optional automatic pack dispatch and `PlayerResourcePackStatusEvent` for connection telemetry.

## 4. Commands, Configuration & Localization

- [x] 4.1 Add `resource-pack` section to `croppy-paper/src/main/resources/config.yml` and update `PluginConfig.java` to parse server host, port, public URL, auto-send, and prompt settings.
- [x] 4.2 Add localized message keys for pack build start, build success, build error, pack sent, and unauthorized access in `en-US.yml` and `es-ES.yml`.
- [x] 4.3 Register `/croppy pack build` and `/croppy pack send [player]` commands in `CroppyCommands.java` annotated with Cloud Paper 2.0 and permission `croppy.admin.pack`.

## 5. End-to-End Verification

- [x] 5.1 Execute `.\gradlew.bat test` across all subprojects (`croppy-api`, `croppy-core`, `croppy-paper`) to guarantee zero compilation errors and 100% test pass rate.
