# `shared` — the app module

This is the module that holds `App()` and is consumed by every platform shell (`androidApp`'s
`MainActivity`, this module's own iOS `MainViewController`, `webApp`'s `main`). It is **not**
called `:app` anywhere in this repo — don't create a separate `:app` module; this is it.

## App shell (step 8)

Five bottom-bar tabs (Map, Catalog, Music, News, Settings) plus three secondary destinations (Map
Filters, Detail, Compilation) opened on top of a tab with a back control. Stub screens only (a title
and placeholder text) built from `:core:design` components — no data/network/database yet (that's
steps 6/7A/7B/9).

### Why `src/appMain`, not `commonMain`

`:core:design` and `:core:mapview` only target JVM/Android/iOS (see their own `build.gradle.kts`)
— they have no `js`/`wasmJs` variant. The real app shell depends on both, so it can't live in
`commonMain`: `webApp`'s `js`/`wasmJs` targets would fail to resolve those dependencies for their
own compilations (confirmed by trying it: `:shared:compileKotlinJs` fails with a "no matching
variant" error as soon as `core:design`/`core:mapview` are commonMain dependencies).

The fix is **not** a custom intermediate Kotlin source set between `commonMain` and
`androidMain`/`iosMain`. Kotlin's default hierarchy template has no built-in grouping for exactly
"Android + iOS, not JS/wasmJs", and introducing a custom node for it via manual `dependsOn` calls
makes Kotlin disable the *entire* default template's automatic wiring for this module — confirmed
the hard way: doing so silently disconnected the pre-existing `iosMain`/`Platform.ios.kt` from the
iOS compilation entirely (it stopped satisfying `Platform.kt`'s `expect fun getPlatform()`, breaking
iOS). Reconstructing the whole hierarchy by hand (including the test source sets
`androidHostTest`/`iosTest`/`webTest`, which also rely on the default template) was judged too large
and risky a change for this step.

Instead, `shared/src/appMain/kotlin` is a plain directory on disk, added as an **extra source
directory** of both the (template-managed, otherwise-untouched) `androidMain` and `iosMain` source
sets via `kotlin.srcDir(...)` in `build.gradle.kts` — not a new Kotlin source set, no `dependsOn`
edge, so the default template keeps working exactly as before. The tradeoff: every file under
`src/appMain` is compiled twice (once into the Android target, once into both iOS targets via the
existing `iosMain`), instead of once. For this step's size (a handful of files, no heavy logic),
that's cheap. The same trick is used for this code's tests: `src/appTest/kotlin` is aliased into
`androidHostTest` only (see "What isn't tested" below — iOS doesn't run these today).

`App()` itself stays `expect` in `commonMain` (zero platform-specific code in `androidApp`/this
module's iOS entry point/`webApp` — all three already just call `App()`, unchanged). Its real
`actual` lives in `src/appMain/kotlin/com/pamaz/vi/App.kt`. `jsMain`/`wasmJsMain` each get their own
tiny placeholder `actual` (plain `MaterialTheme` + a "coming later" `Text`) so `webApp` keeps
building — `webApp` was not part of this step's scope.

### Navigation library: Navigation Compose

The task asked to pick Decompose or Navigation Compose if neither was already chosen.
`navigation-compose` (`org.jetbrains.androidx.navigation`, JetBrains' own Compose Multiplatform
fork) was already a declared dependency of `:core:design` (unused there) and already in the version
catalog — the closer of the two to "already chosen". It's also the lower-risk pick for this exact
stack: official JetBrains support for Compose Multiplatform (Android/iOS/desktop/web), and its
bottom-tab-with-preserved-back-stack idiom (`popUpTo(startDestination){saveState=true}` +
`restoreState=true`) is a documented, standard recipe rather than something to design from scratch.
Decompose is arguably more testable/lifecycle-explicit for very large apps, but that benefit wasn't
worth the extra new dependency + unfamiliar paradigm for a step that's still all stub screens.

One flat `NavHost` (not one nested `NavHost` per tab) holds all eight destinations — the five tab
roots plus the three secondary ones — so a secondary destination pushes onto whichever tab opened
it, with the bottom bar still visible underneath. See `navigation/AppNavHost.kt`'s KDoc for exactly
how per-tab back stacks and "system back on Map exits the app" work (the latter is Navigation
Compose's own default behavior at the graph's start destination — not re-implemented).

### The feature-registration contract

`navigation/FeatureRegistration.kt` + `navigation/AppDestination.kt` are the "small contract" the
task asked for: a `FeatureRegistration` is a destination plus the composable for it, and a future
`feature:catalog`/`feature:music`/etc. module is expected to expose a `List<FeatureRegistration>`
built the exact same way `navigation/AppFeatureRegistrations.kt` builds its (currently
app-module-owned, stub) one — so the app module never needs to import a feature module's internals,
only this one shape.

### Koin

Not configured anywhere before this step — `build_logic`'s `kmm-koin-plugin` convention plugin
exists but applies `com.google.devtools.ksp` for `koin-annotations`' compiler, and (confirmed by
trying it) that combination currently fails on a module using the new
`com.android.kotlin.multiplatform.library` Android target (a `ClassCastException` between the new
AGP KMP Android compilation type and the one the KSP Gradle plugin still expects) — the same target
type `shared` uses. Rather than fight that incompatibility for an "empty aggregate is fine now"
module, `shared` uses **plain Koin** (`koin-core` + `koin-compose`, no annotations/KSP):
`di/AppKoinModules.kt`'s `allAppKoinModules` aggregates every real module's Koin module, started
from `App()` itself (guarded by catching `KoinApplicationAlreadyStartedException`, so a
recomposition/configuration change never crashes) — so `MainActivity`/the iOS
`MainViewController`/`webApp`'s `main` need no Koin-specific code of their own. Revisit
`kmm-koin-plugin`/koin-annotations once there's a real DI graph worth the annotation-scanning
ceremony, or once that AGP/KSP incompatibility is resolved upstream.

**Updated in step 9**: the original empty-placeholder `module { }` per Gradle module is gone,
replaced by the real `databaseModule`/`networkModule`/`dataModule`/`catalogFeatureModule` — each of
those three `core:*` modules' own README said "not wired in until a consuming step needs it";
`:feature:catalog` is that step. The one thing this needed that nothing before it did: a real
Android `Context` for `databaseModule`'s `PlatformContext` single. `di/PlatformContextModule.kt`
(`expect` in *true* `commonMain`, `actual` in *true* `androidMain`/`iosMain`/`jsMain`/
`wasmJsMain` — not the `src/appMain` alias above, since `expect`/`actual` need genuinely different
compilations) is `@Composable` specifically so its Android `actual` can read
`androidx.compose.ui.platform.LocalContext.current` — the only place in this app to get a real
`Context` from, since there's no custom `Application` subclass and `App()`'s own signature stays
untouched (same "zero platform-specific code in the platform entry points" invariant this file's
intro already states). `App()` reads it once per composition and folds it into
`allAppKoinModules(platformContextModule)`.

### Debug-only Map sample

The Map tab shows `:core:mapview`'s `MapViewerSampleScreen` (new in this step — promoted from that
module's private preview code) whenever `MapViewBuildConfig.singleImageDevModeEnabled` is true,
which is every build except one invoked with `-Pilovevi.release=true` — the exact same compile-time
flag `:core:mapview` already uses to gate its own bundled dev image (see that module's README's
"Release safety" section), reused here rather than inventing a second one. Every other tab, and the
Map tab in a release build, gets the same plain stub.

### What isn't tested here

This project has no Compose UI test harness set up (no `compose-ui-test` dependency, no
Robolectric/instrumented Compose test infra) and this sandboxed environment has no Android emulator
or iOS simulator running. So:

- `navigation/AppDestinationTest.kt` (aliased into `androidHostTest`, run via
  `./gradlew :shared:testAndroidHostTest`) covers what's pure structure: five tabs in the right
  order, unique routes, `Compilation` absent from the tab set, `Detail`'s route formatting.
- Actually tapping tabs, pressing system back, and watching "Show on map" return to Map with the
  right pin highlighted were verified by reading the Navigation Compose recipe this follows and by
  confirming every target compiles/links (`:shared:compileAndroidMain`,
  `:shared:compileKotlinIosSimulatorArm64`, `:shared:linkDebugFrameworkIosSimulatorArm64`,
  `:androidApp:assembleDebug`, `:webApp:build`) — not by driving the real UI. Do that on a real
  device/emulator before calling this step done end-to-end.

## What was missing from step 0B

Per this step's instructions, found but not created (out of scope for step 8):

- No `CLAUDE.md` anywhere in the repo.
- No CI workflow (`.github/workflows` doesn't exist).
- No "How to add a module" section in the root `README.md`.

`docs/ROADMAP.md` does exist (created in step P).
