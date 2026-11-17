# :feature:settings

The Settings tab (step 13B, revised from Functional Analysis v0.4 SET-01..06 — see `docs/
requirements-notes.md` for the exact wording change and the new SET-07/SET-08). Account (optional
Google/Apple sign-in), Appearance (theme, language), Map ("Show labels", data update), Spotify
(flagged off, not implemented), About, Danger zone (delete account, signed-in only).

## Dependencies

`:core:model`, `:core:data` (`AccountService`, `SettingsRepository`, `ContentSyncer`,
`SpotifyConnection`, `FeatureFlags`), `:core:design`, `:core:database` (`PlatformContext`, for
`appVersionName(...)` — resolved once in `di/SettingsFeatureModule.kt`, not re-read by the
`ViewModel` itself, see below), Compose, lifecycle-viewmodel, Koin — same shape as `:feature:news`'s
`build.gradle.kts`.

## Theme and Google/Apple sign-in setup (read this before testing on a device)

This module only *renders* the Appearance/Account sections — the actual theme system
(`IloveViTheme`, the Light palette, `IloveViThemeMode`) lives in `:core:design`, and the actual
sign-in flows (Credential Manager, `AuthenticationServices`, the Supabase `Auth` plugin) live in
`:core:data`/`:core:network`. See:

- `documentation/ilovevi-design-system 2/README.md`'s "Color" section for the Light theme and its
  contrast verification.
- `docs/auth-notes.md` for the full Google/Apple sign-in research, the exact `local.properties` keys
  (`GOOGLE_WEB_CLIENT_ID`/`GOOGLE_IOS_CLIENT_ID`, read by `:core:network/build.gradle.kts`'s
  `generateNetworkBuildConfig` task — same mechanism as `SUPABASE_URL`/`SUPABASE_ANON_KEY`), and the
  Apple Developer Portal / Supabase Dashboard steps that still need doing by hand before
  Google/Apple sign-in can actually be tested against a real account.

## `SettingsUiState` / `SettingsViewModel`

One `SettingsUiState` for the whole screen (every section's data in one place, per this step's own
"one UI state per screen" requirement) — `AccountUiState` (mirrors `:core:data`'s `AccountState`,
flattened), `ThemeModeOption`/`LanguageOption` (the typed enums this screen uses; `:core:data`'s
`Preferences.themeMode`/`.language` are plain `String`s, mapped only here), `showLabels`,
`lastUpdateAt`, `isRefreshingData`/`dataRefreshError`, `spotifyState`, `appVersion`.

`SettingsViewModel` combines five flows: `AccountService.state`, `SettingsRepository.preferences()`,
`ContentSyncer.syncState`, `SpotifyConnection.state`, and one local `MutableStateFlow` for the two
one-shot, non-repository-backed fields (an in-flight sign-in's result, an in-flight delete-account's
result) — `AccountService.state` only ever reports an `Error` for a *session-refresh* failure, never
for a failed *sign-in attempt* (which was never signed in to begin with, so there's no session to
report against); that result only ever reaches this screen through `signIn()`'s own `Result`, which
is why it needs its own local state. See the class's own KDoc for the full reasoning.

**`appVersion` is a plain `String` constructor parameter, not a `PlatformContext`**: the ViewModel
itself never touches `:core:database`'s `PlatformContext` type — `di/SettingsFeatureModule.kt`
resolves `appVersionName(get())` once and passes the resulting string in. This was a deliberate
change from an earlier draft that injected `PlatformContext` directly: `PlatformContext` has no
public constructor outside each platform's own `androidMain`/`iosMain`/`jvmMain` (same
`expect class PlatformContext` shape `:core:database`/`:core:data` already establish), so a
`commonTest` file testing this `ViewModel` could never construct one — passing the already-resolved
`String` instead keeps `SettingsViewModelTest` fully in `commonTest`, no per-platform duplication.

**`FeatureFlags.spotify` is checked only by `SettingsScreenContent`, not by the `ViewModel`**: the
flag is a compile-time `const val`, so a test can't vary it the way it can a constructor dependency.
`uiState.spotifyState` always reflects the real `SpotifyConnection` (today, always
`SpotifyConnectionState.Unavailable`), fully testable against a fake regardless of the flag's
current value; hiding the Spotify card when the flag is off is the screen's own
`if (FeatureFlags.spotify)`
check, architecturally guaranteed (dead code when `false`) rather than state-driven.

## `SettingsScreen` / `SettingsScreenContent`

`SettingsScreen(viewModel = koinViewModel())` is the only real caller of `SettingsScreenContent` — a
stateless composable (no `ViewModel`/Koin dependency) taking the `SettingsUiState` and one callback
per action directly, so it's usable from a `@Preview` with no fake Koin setup. Every section is a
small `private` composable in the same file (`AccountSection`, `AppearanceSection`,
`MapSection`, `SpotifySection`, `AboutSection`, `DangerZoneSection`) built from existing
`:core:design` primitives (`IloveViButton`, `IloveViSwitch`, `IloveViChip`/`IloveViChipGroup`, a
plain M3 `AlertDialog` styled with `IloveViColors` tokens for the delete-account confirmation — no
dialog component exists in `:core:design` yet and this is the only call site, so one wasn't added
there). The 3-way theme selector and 2-way language selector both reuse one generic
`RadioChipGroup<T>` helper (`Role.RadioButton` + `selected` semantics on each `IloveViChip`, single-
selection enforced by `onSelect` always replacing the whole value) rather than a Settings-specific
component.

**Account section states**: signed-out (one "Continue with Google" button, "Continue with Apple" on
iOS only — `availableProviders` drives which buttons render — plus the required "the app works fully
without an account" sentence), loading (a spinner, shown both for `AccountState.Loading` and for an
in-flight `signIn()` call), signed-in (initials-circle avatar — no image loading/Coil dependency
added to this module just for an optional Google photo, display name, email, "Sign out"), error (the
mapped `AccountErrorReason` message + Retry).

**The whole Account section is hidden unless `ACCOUNT_LOGIN_ENABLED=true` is set** (root
`local.properties`/environment, read via `AccountFeatureConfig.isLoginEnabled()` in `:core:network`,
exposed as `FeatureFlags.accountLogin` in `:core:data` — same shape as `FeatureFlags.spotify`,
except build-config-driven rather than hardcoded, so trying Google/Apple sign-in needs a
`local.properties` line, not a code change). Unset/blank means off, matching this section's behavior
before the flag existed. Danger zone is gated on `FeatureFlags.accountLogin &&
state.account is AccountUiState.SignedIn` for the same reason — it can only ever show when Account
can.

**Google is hidden, not just platform-gated, when `GOOGLE_WEB_CLIENT_ID` isn't set**:
`AccountServiceImpl.availableProviders` (`:core:data`) checks both the platform *and*
`GoogleAuthConfig.isConfigured()` (`:core:network`) before including `AuthProviderId.GOOGLE` — a
real bug found by actually tapping the button on a machine without that key configured: the button
used to always render and always fail `signInWithGoogle()`'s own `require()` the moment it was
tapped, a confusing dev-time failure for something the user never did wrong. `isConfigured()` exists
specifically because `GOOGLE_WEB_CLIENT_ID` itself is `internal` to `:core:network` — this is the
only way `:core:data` can check it without the full `require()`-throwing `fromBuildConfig()`.

**Accessibility**: the theme/language selectors use `Role.RadioButton` + `selected` semantics (see
above); "Show labels" is a `Role.Switch` row with merged semantics covering the whole row, not just
the switch thumb, so a screen reader announces the full label; every row has a 44dp+ tap target,
matching `IloveViChip`/`IloveViSegmentedSwitch`'s own established convention.

## Resources (strings)

`composeResources/values(-it)/strings.xml` — every label, section header, error message and the
"Data update" relative-time text (`settings_map_data_update_just_now`/`_minutes_ago`/`_hours_ago`/
`_days_ago`, formatted in the screen, not the `ViewModel`, since only the screen has
`stringResource` access) are resource-backed, English and Italian, no hardcoded text — including the
relative-time formatting, which `:feature:news`'s own `NewsFormatting.kt` notably does *not* do
(hardcoded English "m ago"/"h ago"); this module doesn't repeat that gap.

## Tests

`SettingsViewModelTest` (23 cases, `commonTest`, fakes in `fake/`): theme/language/show-labels
persist and round-trip (including that changing one field never clobbers another, e.g. `language`/
`selectedNewsTopic`/`lastContentUpdate`, which this screen doesn't own); data-refresh
progress/error; signed-out/loading/signed-in/error account states; Google sign-in success,
cancelled, developer-error, offline; sign out; delete-account success and failure; every
`SpotifyConnectionState`
reflected in `uiState` regardless of the (always-off) feature flag. "System theme follows the device
live" is *not* re-tested at this layer — that's `:core:design`'s `IloveViTheme`/
`isSystemInDarkTheme()`, architecturally guaranteed, not something this `ViewModel` controls; this
module's own tests only assert that a `"SYSTEM"` stored preference passes through unchanged.

No screenshot-testing tooling exists in this repo (confirmed, same as every other module) —
`SettingsScreenContent` has four `@Preview`s instead (signed-out/signed-in × Light/Dark), the same
substitute `:core:design`'s own component gallery already established.

## Wiring into the real app (step 8's contract)

`shared/src/appMain/kotlin/com/pamaz/vi/di/AppKoinModules.kt` adds `settingsFeatureModule`;
`navigation/AppFeatureRegistrations.kt`'s `AppDestination.Tab.Settings` registration now calls
`SettingsScreen()` instead of the old `SettingsStubScreen()` (removed from
`screens/TabScreens.kt`, along with its now-unused `app_tab_settings_title` string). `App()` itself
(`shared/src/appMain/kotlin/com/pamaz/vi/App.kt`) now reads the stored `themeMode` — a blocking
`runBlocking` first read on the first composition (fast: one local SQLite row, same database
`SeedImporter`/`ContentSyncer` already open) so the very first frame already renders in the right
theme, then a reactive `collectAsStateWithLifecycle` for every change after that, so a theme change
from this screen applies immediately with no restart. The stored `language` is applied the same way,
through `core:design`'s `ApplyLanguagePreference` (`expect`/`actual`, next to
`ApplySystemBarsAppearance`): immediately on Android (`android.os.LocaleList.setDefault(...)` — the
actual value `stringResource(...)` reads on every API level this app supports, confirmed by reading
the real `androidx.compose.ui:ui-text-android` sources, not just `AppCompatDelegate`'s own API,
which alone doesn't reliably apply without an `AppCompatActivity` — plus an `Activity.recreate()`),
but only on the *next* cold launch on iOS — Compose Multiplatform has no public, same-process way to
override resource locale (a real, currently-unresolved upstream limitation, not a gap here — see
`ApplyLanguagePreference`'s own KDoc).

## What wasn't verified here

Same sandboxed-environment limitations as every other GUI/auth step in this roadmap: no real
Google/Apple sign-in round trip, no real device to confirm the on-screen contrast/readability or the
system-bar color switch by eye. `./gradlew :feature:settings:jvmTest` (23/23) and
`:feature:settings:iosSimulatorArm64Test` (23/23, same 23 — this module doesn't have `:core:data`'s
known iOS-link limitation, see that module's own README) both pass; `:androidApp:assembleDebug`,
`:shared:testAndroidHostTest` and `:webApp:build` were all re-run clean after every change in this
step, not assumed. The one genuinely unresolved item is **not** in this module: see `docs/
auth-notes.md`'s closing note on the `iosApp` Xcode scheme's `embedAndSignAppleFrameworkForXcode`
Gradle task being skipped under this sandbox's installed Xcode/SDK combination — every Kotlin source
file this step touches (including this module's) compiles clean on `iosSimulatorArm64`, confirmed
repeatedly; the gap is in the Xcode-side framework embed step, not in any `.kt`/`.swift` file.
