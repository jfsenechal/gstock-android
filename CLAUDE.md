# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

Gstock is an Android application (package `be.marche.gstock`) built with Kotlin and Jetpack Compose. As of this writing the codebase is a fresh Android Studio scaffold — a single `MainActivity` rendering a Compose `Greeting` — so most feature work starts from a near-empty base.

## Commands

The project uses the Gradle wrapper (`./gradlew`). All commands run from the repository root.

```bash
./gradlew assembleDebug              # Build the debug APK
./gradlew installDebug               # Build and install on a connected device/emulator
./gradlew test                       # Run JVM unit tests (app/src/test)
./gradlew testDebugUnitTest          # Run unit tests for the debug variant only
./gradlew connectedAndroidTest       # Run instrumented tests (app/src/androidTest) — needs a device/emulator
./gradlew lint                       # Run Android Lint
./gradlew clean                      # Clean build outputs
```

Run a single unit test class or method:

```bash
./gradlew test --tests "be.marche.gstock.ExampleUnitTest"
./gradlew test --tests "be.marche.gstock.ExampleUnitTest.addition_isCorrect"
```

## Architecture & Conventions

- **UI:** 100% Jetpack Compose (no XML layouts). `MainActivity` is the single `ComponentActivity` entry point and sets content via `setContent { GstockTheme { ... } }`. It uses `enableEdgeToEdge()`.
- **Theme:** Lives in `app/src/main/java/be/marche/gstock/ui/theme/` (`Theme.kt`, `Color.kt`, `Type.kt`). Wrap composables in `GstockTheme { }`. The theme supports dynamic color (Material You) on Android 12+.
- **Dependencies** are declared in the version catalog `gradle/libs.versions.toml` and referenced as `libs.*` aliases from `app/build.gradle.kts`. Add or bump dependencies there, not as inline coordinates. Compose libraries are versioned via the Compose BOM (`androidx-compose-bom`), so individual Compose artifacts are listed without versions.

## SDK / Toolchain

- `minSdk = 35`, `targetSdk`/`compileSdk = 36` — this targets very recent Android only.
- Kotlin 2.2.x with the Compose compiler plugin (`org.jetbrains.kotlin.plugin.compose`); Android Gradle Plugin 9.x.
- Java source/target compatibility is 11.
- `local.properties` (git-ignored) holds the local `sdk.dir`; it is required for builds and not committed.
