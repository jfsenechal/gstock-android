# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

Gstock is an Android tool-checkout app (package `be.marche.gstock`) built with Kotlin and Jetpack Compose. Workers borrow and return tools; the app authenticates against a Laravel backend (default `https://gstock.marche.be/`), scans QR codes with the camera, and caches data offline in Room. It is an online-first client over a REST API, not a standalone app.

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

If `./gradlew` fails with "JAVA_HOME is not set", point it at Android Studio's bundled JBR, e.g. `JAVA_HOME=/opt/android-studio/jbr ./gradlew ...`.

## Architecture

Single-activity Compose app with Hilt DI and an offline-first repository layer. Layers under `app/src/main/java/be/marche/gstock/`:

- **`MainActivity` + `GstockApplication`** — `MainActivity` is the only `ComponentActivity`; `GstockApplication` is `@HiltAndroidApp`. `MainActivity` resolves the dark/light scheme from `SettingsViewModel` and wraps the UI in `GstockTheme { GstockApp() }` with `enableEdgeToEdge()`.
- **`ui/navigation/`** — `GstockApp` gates on `AuthState` (Loading → `LoadingBox`, Unauthenticated → `LoginScreen`, Authenticated → `MainScaffold`). `MainScaffold` hosts the bottom `NavigationBar` + `NavHost`. The `Destination` enum in `Destinations.kt` is the single source of truth for bottom-bar routes/labels/icons; the start destination is `Checkouts`. Tabs: **Checkouts**, **Catalog** (Workers + Tools behind a `TabRow`), **Check out**, **Account**.
- **`ui/<feature>/`** — one package per screen (`auth`, `checkout`, `checkouts`, `catalog`, `tools`, `workers`, `account`, `scan`), each a `@Composable` Screen + a `@HiltViewModel`. Screens get their VM via `hiltViewModel()` and collect state with `collectAsStateWithLifecycle()`. Shared widgets (`LoadingBox`, `MessageBox`, `SearchField`) live in `ui/common/`.
- **`data/repository/`** — `@Singleton` repositories injected with a `GstockApi` and a Room DAO. The offline-first pattern: the UI observes Room (`observeXxx(): Flow`) as the single source of truth, and `refresh()` fetches from the API and writes through to Room (`dao.replaceAll(...)`). Network calls are wrapped in `safeApiCall { }` → `ApiResult<T>` (`core/ApiResult.kt`); `AuthRepository` does its own try/catch to parse Laravel error bodies.
- **`data/remote/`** — `GstockApi` (Retrofit interface), `dto/` (kotlinx.serialization `@Serializable` DTOs), and `AuthInterceptor` which adds the bearer token to every request by reading it synchronously from `SessionManager`.
- **`data/local/`** — Room `GstockDatabase` (entities + DAOs in `entity/Entities.kt`, `dao/Daos.kt`). `data/mapper/Mappers.kt` converts DTO ↔ entity.
- **`data/auth/`** — `SessionManager` (`@Singleton`) holds the in-memory token + `AuthState` flow so the interceptor never touches the DB on the network thread; `AuthRepository` persists the session in Room and keeps `SessionManager` in sync.
- **`data/settings/`** — `ThemePreferences` (`@Singleton`, SharedPreferences-backed) exposes a `StateFlow<ThemeMode>` (SYSTEM/LIGHT/DARK); the Account screen writes it and `MainActivity` reads it to pick the scheme.
- **`core/`** — `ApiResult` result wrapper and `GstockQr` QR parsing helpers.
- **`di/`** — Hilt modules: `NetworkModule` (Json, OkHttp, Retrofit, `GstockApi`) and `DatabaseModule` (Room database + DAO providers), both `@InstallIn(SingletonComponent::class)`.

## Conventions

- **UI:** 100% Jetpack Compose, no XML layouts. New screens follow the Screen + `@HiltViewModel` pairing and expose a single immutable UI-state data class via `StateFlow`.
- **Theme:** lives in `ui/theme/` (`Theme.kt`, `Color.kt`, `Type.kt`). Wrap composables in `GstockTheme { }`. It supports Material You dynamic color on Android 12+; the in-app Account toggle controls only the dark/light dimension (dynamic color still overrides the static purple palette on 12+).
- **DI:** prefer constructor injection with `@Inject` / `@Singleton`. Add bindings to the existing `di/` modules rather than creating ad-hoc factories.
- **Networking:** add endpoints to `GstockApi` and DTOs to `data/remote/dto/`; never build Retrofit/OkHttp by hand. Route new data through a repository, not directly from a ViewModel.
- **Secrets/config:** the API base URL comes from `gstock.base.url` in `local.properties` (falls back to `https://gstock.marche.be/`), surfaced as `BuildConfig.GSTOCK_BASE_URL`. Keep tokens/URLs out of source control.
- **Dependencies** are declared in the version catalog `gradle/libs.versions.toml` and referenced as `libs.*` aliases from `app/build.gradle.kts`. Add or bump dependencies there, not as inline coordinates. Compose libraries are versioned via the Compose BOM (`androidx-compose-bom`), so individual Compose artifacts are listed without versions.

## SDK / Toolchain

- `minSdk = 35`, `targetSdk = 36`, `compileSdk = 36` (with `minorApiLevel = 1`) — this targets very recent Android only.
- Kotlin 2.2.x with the Compose compiler plugin and KSP (Room + Hilt code generation); Android Gradle Plugin 9.x.
- Java source/target compatibility is 11.
- Camera/QR scanning uses CameraX + ML Kit barcode scanning.
- `local.properties` (git-ignored) holds the local `sdk.dir` (and optional `gstock.base.url`); it is required for builds and not committed.
