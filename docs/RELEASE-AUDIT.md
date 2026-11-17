# ILoveVI — Pre-Submission Release Audit

- **Date:** 2026-11-17
- **Branch / commit:** `develop` @ `a01031f` ("Hides for first release the music tab")
- **Scope:** Apple App Store (primary), Google Play (secondary). Read-only audit — no code, config, or
  database was changed as part of this pass.
- **Method:** 7 parallel investigation passes (build/tests, functional/UX, content & IP, privacy/security,
  Supabase backend, App Store/Android readiness, performance) plus direct verification by the lead agent
  of the single highest-risk claim (the map image — see RA-01). Builds were actually run (`./gradlew`,
  `xcodebuild`), the app was actually launched on an iOS 18.5 simulator against the real backend, and the
  Supabase schema was read statically (no Docker available in this environment, so `supabase db lint`
  /`db reset` could not be run locally — see §5).

## 0. One-Page Summary

### Verdict: **NOT READY TO SUBMIT**

Two independent, hard blockers are visual/content problems that a code fix alone won't solve (they need a
product decision), and several more are build/config gaps that take minutes to fix but are currently
unresolved. None of this is exotic — most items below map directly to things this repo's own
`docs/ROADMAP.md` "Open decisions" and step 15/16 entries already flagged as outstanding. This audit's job
was to confirm which of those are now release-blocking, with evidence, two days before GTA VI itself ships
(19 Nov 2026).

### Findings by severity (approximate — see per-section tables for the authoritative list)

| Severity | Count | Meaning |
|---|---|---|
| **BLOCKER** | **11** | Would likely cause App Store/Play rejection, a crash, or a leak. Ship-blocking. |
| **SHOULD** | ~26 | Fix before release; not individually fatal but adds up. |
| **NICE** | ~15 | Polish, can follow in a fast-follow update. |

### The 11 BLOCKERs, most important first

1. **RA-01 — Unlicensed "Map of Leonida" image ships inside the app by default.**
   `core/mapview/src/commonMain/composeResources/drawable/map_dev.png` (git-tracked, added in commit
   `2633cd3`) is a detailed, labeled map titled **"Map of Leonida"** with place names (Port Gellhorn,
   Mount Kalaga National Park, Lake Leonida, St. Joseph, Vice City, Gunsmoke Key) in the style of the
   widely-circulated fan-made "GTA VI map" renders — **not** the watermarked grid placeholder
   (`map_dev_fallback.png`) the module's own README describes. I generated a thumbnail and viewed both
   images directly to confirm this myself (the content-IP sub-audit and the functional sub-audit
   initially disagreed on what this file contained — I resolved the discrepancy by looking at the actual
   pixels). Whether this image is used is controlled by `SINGLE_IMAGE_DEV_MODE_ENABLED`, a build-time
   `const val` set to `!releaseBuild` in `core/mapview/build.gradle.kts`, where `releaseBuild` is only
   `true` if the build is invoked with `-Pilovevi.release=true`. **No build run during this audit — Android
   bundleRelease, iOS Release archive, or iOS Release simulator build — passed that flag**, and nothing in
   the repo's Gradle/Xcode configuration passes it automatically. The functional sub-audit confirmed this
   image actually renders live on an iOS simulator after a real sync against the project's real Supabase
   backend. **This is a direct violation of this project's own documented policy**
   (`docs/ROADMAP.md` → Open Decisions → "Map image rights": *"no official or fan-made GTA VI map image
   may ever be embedded, downloaded, or re-hosted by this project"*).
   Separately, and likely related: files of unverified origin at `/tmp/audit_map/leonida_v1.png` and
   `v2_source.png` (not part of the git repo, left over from an earlier, unrelated session on this
   machine — a permission-log entry in `.claude/settings.local.json` shows that session already
   investigated these same files) depict a *different* fan-made "Leonida" map (different county/city
   labels: Lummox/Leonard/Kelly/Vice Dale/Mariana County). The filename `v2_source` suggests a connection
   to `map_config.source_image_url`/`map_version` — i.e. this may be a candidate or actual **production**
   map source image. **This could not be confirmed or ruled out from the repo alone** — the live Supabase
   project (`fgkoxqovgammjunlmhqn`) was not reachable through the tools available in this session.
   *→ See §2 (MAP-1), §3 (IP-01/IP-02).*
   **Fix:** Before doing anything else — (a) directly query the live `map_config` table and open whatever
   `source_image_url`/tile bucket it points to, to see exactly what real users would see; (b) replace
   `map_dev.png` with the already-existing safe placeholder or delete it outright; (c) wire
   `-Pilovevi.release=true` into whatever actually produces your App Store/Play build so this can never
   silently regress; (d) decide, for real, where the production map art will come from (commissioned
   original art / procedural generation from `tools/tilegen` / a properly licensed asset) — this is listed
   as an **unresolved open decision in your own roadmap**, not a new problem this audit invented.

2. **RA-02 — App icon imitates GTA VI's own visual branding.**
   The app icon (`iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/app-icon-1024.png`, mirrored on
   Android) is a heart containing palm-tree silhouettes on a purple→pink→orange vertical sunset gradient
   with a neon-outlined "VI" — the Vice-City/Miami neon-sunset-and-palms treatment Rockstar has used as
   GTA VI's own marketing identity. This is a visual trade-dress risk independent of the text disclaimer,
   under both App Store 2.3.1/4.1 (metadata/icon implying official status) and Take-Two's trade dress
   rights. *→ See §3 (IP-01).*
   **Fix:** Re-skin the icon with an original color story before submission, or get this specific design
   reviewed by someone with trademark expertise.

3. **RA-03 — Production seed data still contains literal placeholder text.**
   `backend/supabase/seed.sql` ships rows like `'[Weapon 1]'` / `'[Placeholder description for Weapon
   1.]'`. Real content entry (`docs/ROADMAP.md` step 16) is "Not started. Blocked until GTA VI ships (19
   Nov 2026)" — two days after this audit. Submitting now means a reviewer sees bracketed placeholder
   text, a textbook App Store 2.1 rejection. *→ See §6 (ASR-02).*
   **Fix:** Don't submit until step 16 loads real (non-bracketed) content, or hold submission until just
   after 19 Nov 2026.

4. **RA-04 — Android release build produces a completely unsigned AAB.**
   `./gradlew :androidApp:bundleRelease` succeeds and produces an 18.1 MB AAB, but
   `androidApp/build.gradle.kts` has no `signingConfigs` block at all — `jarsigner -verify` confirms the
   AAB is unsigned. Cannot be uploaded to Play Console as-is. *→ See §1 (BUILD-01).*
   **Fix:** Add a release signing config (upload keystore via env vars/`keystore.properties`, never
   committed) before building for real submission.

5. **RA-05 — iOS Release archive cannot be code-signed.**
   `xcodebuild archive -sdk iphoneos` fails: *"Signing for 'iosApp' requires a development team."*
   `iosApp/Configuration/Config.xcconfig`'s `TEAM_ID` is blank. This is an account-setup action, not a
   code bug, but it is currently a hard blocker to producing an upload-ready archive. Also already
   flagged in your own `docs/ROADMAP.md` step 15. *→ See §1 (BUILD-02), §6 (ASR-12).*
   **Fix:** Fill in your real Apple Developer Team ID and confirm the matching App ID/provisioning setup.

6. **RA-06 — `core:data`'s iOS test target fails to link.**
   `:core:data:linkDebugTestIosSimulatorArm64` fails (`ld: symbol(s) not found` for `sqlite3_*` symbols).
   The `-lsqlite3` flag is only wired into the Xcode app target's `Config.xcconfig`, not into this
   module's own Gradle/Kotlin-Native test compilation — so this module's iOS unit tests cannot currently
   be run at all via Gradle, meaning you have no real test coverage signal for `core:data` on iOS.
   *→ See §1 (BUILD-03).*
   **Fix:** Add the `sqlite3` linker flag to `:core:data`'s `iosSimulatorArm64()` test compilation in its
   own `build.gradle.kts`.

7. **RA-07 — Music/Compilation is a Must-priority feature that's entirely missing, and the gap isn't
   documented.**
   `feature:music` doesn't exist; the Music tab was hidden from the nav bar in the most recent commit
   (`a01031f`). Multiple Must-priority requirement IDs (MUS-01..05, CMP-01..07) have no implementation.
   *→ See §2 (FUNC-1).*
   **Fix:** Either build it before submission, or explicitly descope it for v1 in the Functional Analysis
   / `docs/requirements-notes.md` so "missing" becomes a documented, intentional decision rather than a
   silent gap — Apple reviewers don't care about your roadmap, but *you* need the record straight for the
   next item.

8. **RA-08 — Sign-in and account deletion are fully built and tested, but switched off in the current
   release config — and your draft store listing currently advertises them as available.**
   `ACCOUNT_LOGIN_ENABLED` is commented out in `local.properties`; iOS `TEAM_ID` and
   `GOOGLE_WEB_CLIENT_ID` are blank. As configured today, the shipped app has **no visible way to sign in
   or delete an account**, despite the code existing and being unit-tested. The App Review notes drafted
   in `docs/store-listing.md` (§6 of this audit) describe "Continue with Google" / "Sign in with Apple" as
   reachable — if you submit with the feature actually off, that text is inaccurate, which is itself a
   2.3 (accurate metadata) risk on top of just confusing the reviewer. *→ See §2 (FUNC-2).*
   **Fix:** Decide, before writing the final App Review notes: either turn the feature on for real
   (fill in `TEAM_ID`, `GOOGLE_WEB_CLIENT_ID`, set `ACCOUNT_LOGIN_ENABLED=true`) and re-test the full
   sign-in → delete-account flow end to end, or strip every mention of sign-in from the store listing and
   review notes and ship v1 as fully anonymous.

9. **RA-09 — An unguarded force-unwrap in the Settings data pipeline is a real crash risk.**
   `core/data/.../repository/AccountServiceImpl.kt:48`: `status.session.user!!.toAccountUser()` inside a
   `Flow.map{}` with no `.catch{}`, combined directly into `SettingsViewModel`'s state. If `session.user`
   is ever null while the status is `Authenticated` — a real nullable field upstream — this crashes
   Settings with no recovery. *→ See §2 (SCRN-1).*
   **Fix:** Replace the `!!` with a safe fallback to an error state, or wrap with `.catch{}`.

10. **RA-10 — No `PrivacyInfo.xcprivacy`, and the app actually calls a Required-Reason API.**
    No such file exists anywhere in the repo, yet `core/design/src/iosMain/.../Locale.ios.kt:17` calls
    `NSUserDefaults.standardUserDefaults.setObject(...)` — "User Defaults APIs" is one of Apple's five
    Required-Reason API categories (enforced since May 2024). App Store Connect's binary scan can flag or
    block processing of new submissions missing this. *→ See §4 (PRIV-01).*
    **Fix:** Add `iosApp/iosApp/PrivacyInfo.xcprivacy` declaring the UserDefaults API usage with an
    approved reason code (note: writing the system `AppleLanguages` key may not cleanly match any
    pre-approved reason — this specific usage may need to be reworked, not just declared, so get it
    reviewed before relying on a reason code), plus `NSPrivacyTracking: false` and empty collected-data
    arrays.

11. **RA-11 — Admin-privilege safety in the backend depends entirely on manual discipline, not code
    (contingent — not a confirmed defect, flagged because nothing in the repo lets you verify it).**
    There is no `profiles`/role table; "admin" is `auth.jwt() -> 'app_metadata' ->> 'role' = 'admin'`,
    which ordinary users genuinely cannot self-grant (verified — no RLS gap). This is a *sound design*,
    but it means the only thing standing between "normal user" and "admin, can commit map edits" is
    whoever has service-role access remembering never to grant it carelessly. Nothing to fix in code; flagged
    so you know this is a process, not a property the codebase enforces. *→ See §5 (BE-02).*
    **Fix:** Document and follow a specific, narrow grant procedure; nothing else required.

### Why "not ready" rather than "ready with caveats"

Items 1–3 (RA-01/02/03) are content/IP problems that **cannot be fixed by code alone** and are exactly the
kind of thing Apple reviewers and Take-Two's legal team both notice first — an icon and a map are the two
things every single reviewer sees in the first ten seconds. Items 4–6 (RA-04/05/06) mean **no one has ever
actually produced an uploadable build of this app** — that alone means "not ready" regardless of anything
else. The rest compound the risk. None of these are far away (most are hours, not weeks, of work plus one
real product decision about Music and one about the map art), but shipping today would fail.

---

## 1. Build and Tests

Commands actually run: `./gradlew allTests testAndroidHostTest --continue`, `./gradlew lintRelease`,
`./gradlew :androidApp:bundleRelease`, `xcodebuild … -configuration Release -sdk iphonesimulator build`,
`xcodebuild … -configuration Release -sdk iphoneos archive` (expected-fail, see BUILD-02), plus `jarsigner
-verify`, `nm`, and manual `grep` sweeps for debug code and secrets.

| ID | Severity | Evidence | Fix |
|---|---|---|---|
| BUILD-01 | **BLOCKER** | `jarsigner -verify androidApp-release.aab` → "jar is unsigned". `androidApp/build.gradle.kts` has no `signingConfigs` block; `release { isMinifyEnabled = false }` assigns no signing config. `./gradlew :androidApp:bundleRelease` succeeds and produces `androidApp/build/outputs/bundle/release/androidApp-release.aab` (18.1 MB), but the AAB has no `MANIFEST.MF` signature. | Add a release `signingConfig` (upload keystore via `keystore.properties`/env vars, never committed) before this can be uploaded to Play Console. |
| BUILD-02 | **BLOCKER** | `xcodebuild archive -sdk iphoneos` (Release) fails: *"Signing for 'iosApp' requires a development team. Select a development team in the Signing & Capabilities editor."* Root cause: `iosApp/Configuration/Config.xcconfig`'s `TEAM_ID=` is blank, so `DEVELOPMENT_TEAM` resolves empty (`CODE_SIGN_STYLE = Automatic`). The simulator build (no signing required) succeeds — see BUILD-10. | Set `TEAM_ID` to the real Apple Developer Team ID and confirm a matching App ID/provisioning profile exists before archiving. Account setup, not a code bug. |
| BUILD-03 | **BLOCKER** | `:core:data:linkDebugTestIosSimulatorArm64 FAILED` — `ld: symbol(s) not found for architecture arm64` for `_sqlite3_bind_blob`, `_sqlite3_step`, `_sqlite3_open_v2` (SQLDelight's `sqliter` cinterop). The `-lsqlite3` flag is only added via `Config.xcconfig`'s `OTHER_LDFLAGS` for the Xcode **app** target — `:core:data`'s own `iosSimulatorArm64Test` compilation never gets it, so this module's iOS unit tests cannot run via Gradle at all. | Add the `sqlite3` link flag to `:core:data`'s own `iosSimulatorArm64()` test compilation in its `build.gradle.kts`. |
| BUILD-04 | SHOULD | `feature:map:iosSimulatorArm64Test`: 29 tests, 2 failed — `PinClustererTest.fiveHundredPinsClusterUnderSixteenMilliseconds` and `...twoThousandPinsClusterUnderFiftyMilliseconds` (`feature/map/src/commonTest/.../PinClustererTest.kt:207,225`), both wall-clock performance-threshold assertions, likely flaky/machine-dependent rather than a real regression. | Move hard millisecond assertions out of `commonTest` into a benchmark-only suite, or make them environment-tolerant. |
| BUILD-05 | SHOULD | Plain `./gradlew test`/`check` at the repo root do **not** exercise the real KMP module tests (wired to `allTests`/`jvmTest`/`testAndroidHostTest`/`iosSimulatorArm64Test`, not the `test` lifecycle task) — `./gradlew test` only ran trivial tasks then failed on an unrelated `:tools:map-editor:compileKotlin` error (`@Composable` invoked outside a `@Composable` context, `tools/map-editor/.../MapArea.kt:345`) before reaching any real module test. | Standardize on `./gradlew allTests testAndroidHostTest` for CI. Fix the `tools/map-editor` compile error separately (dev tool only, not shipped). |
| BUILD-06 | SHOULD | The release gate `-Pilovevi.release=true` (controls `SINGLE_IMAGE_DEV_MODE_ENABLED` — see RA-01 above) is **not passed by any build run during this audit**. `docs/ROADMAP.md` line 290 already flags this: "the release gate is still not automatically passed by any build — step 15 needs to wire it into real release packaging." | Wire `-Pilovevi.release=true` into whatever actually produces your release Gradle/Xcode build (CI config, `gradle.properties` for release, or an Xcode build setting) before shipping. **This is the direct mechanism behind RA-01 — fixing this one line is necessary but not sufficient; you also need the map art decision.** |
| BUILD-07 | NICE | Android `lintRelease`: 0 errors, 58 warnings — dependency-bump noise (AGP/Kotlin/Ktor version nags) and `MonochromeLauncherIcon`/`IconLauncherShape` (adaptive-icon polish). Full report: `androidApp/build/reports/lint-results-release.txt`. | Low priority; add a monochrome launcher icon layer for Android 13+ themed icons at some point. |
| BUILD-08 | NICE | No detekt/ktlint/SwiftLint configured anywhere. Android Lint is the only static analysis run. | Optional code-quality hygiene, not required for review. |
| BUILD-09 | NICE | `androidApp`'s release build type has `isMinifyEnabled = false` — no R8/ProGuard shrinking/obfuscation. | Optional: would shrink the ~18 MB AAB; not required for submission. |
| BUILD-10 | Verified, no action | Re-ran the iOS Release build for `iphonesimulator` from a clean DerivedData: **BUILD SUCCEEDED**. `embedAndSignAppleFrameworkForXcode` shows `SKIPPED` — expected/correct, since `Shared.framework` is a **static** framework, statically linked rather than embedded. Confirmed via `nm`: the binary contains 24,363 Kotlin `kfun:` symbols including the exact `AuthDeepLinks.ios.kt` symbols `docs/auth-notes.md` §7 worried about. **This resolves the build concern documented in auth-notes.md §7** — the "SKIPPED" line there is a red herring for a static framework, not a bug. Simulator app size: 52 MB. | None — just update the note in auth-notes.md §7 for the next reader. |
| BUILD-11 | Info, no action | Bundle ID difference between platforms (`com.pamaz.vi` Android vs `com.pamaz.vi.ILoveVI` iOS) is intentional per `docs/auth-notes.md` lines 125-131, which explicitly instructs registering both separately. versionName/versionCode (1.0/1) and MARKETING_VERSION/CURRENT_PROJECT_VERSION (1.0/1) are aligned. | Just confirm App Store Connect's registered bundle ID matches `com.pamaz.vi.ILoveVI` exactly once `TEAM_ID` is filled in. |
| BUILD-12 | Verified, no action | No signing secrets tracked in git (`git ls-files` clean of `local.properties`/`*.jks`/`*.p12`/`*.mobileprovision`/`*.p8`). `local.properties` is gitignored and contains only a Supabase URL/anon key and a Google OAuth **client ID** (not a secret). | None. |
| BUILD-13 | Verified, no action | Only debug-gated code found (`core/mapview`'s `TileDebugOverlay.kt`/`MapViewBuildConfig.kt`/`DevMapImage.kt`) is correctly compile-time gated — see BUILD-06 for why the gate itself isn't flipped yet. No verbose request/response logging interceptors, no mock services wired into production DI, no hidden debug menus reachable from the shipped app, no `TODO`/`FIXME` in non-test production code. | None beyond BUILD-06. |

**FeatureFlags inventory** (`core/data/src/commonMain/.../FeatureFlags.kt`):

| Flag | Value this build | Notes |
|---|---|---|
| `spotify` | `false` (hardcoded `const val`) | Gates the Spotify card in Settings until a real `SpotifyConnection` ships. **Confirmed OFF for release — correct.** |
| `accountLogin` | `false` (default; reads `ACCOUNT_LOGIN_ENABLED` from `local.properties`/build config, unset → safe default off) | Gates the entire Account/sign-in section. Currently off because `local.properties` has it commented out — see RA-08. |

No flag gating unfinished/fake functionality is ON by default.

---

## 2. Functional Check Against the Analysis

Checked against `documentation/ilovevi-design-system 2/IloveVI-Functional-Analysis.pdf` (v0.4) and the
app-relevant subset of the Map Editor analysis, cross-referenced with `docs/requirements-notes.md` and
actual source. Screen/state walkthrough included a live run on an iOS 18.5 simulator against the real
backend.

**Must-priority items missing or not safely shippable today:**
- MUS-01..05 / CMP-01..07 (Must) — Music/Compilation feature entirely unimplemented (see RA-07).
- SET-01 (Must) — Spotify connect/disconnect not implemented this phase (flag correctly off).
- SET-07/SET-08 (Must) — sign-in/delete-account coded and tested, but compile-time-disabled in the
  current config (see RA-08).
- MST-01 (Must: "no official/fan-made map without rights") — **violated**, see RA-01.

### Requirement coverage matrix (abridged — Must items and anything not "implemented, tested" shown)

| ID | Requirement (short) | Priority | Status |
|---|---|---|---|
| NAV-01 | 5-destination bottom bar | Must | Implemented, tested — only 4 visible (Music hidden) |
| NAV-04 | Compilation reachable only from Music card | Must | Missing (Music doesn't exist) |
| MAP-08 | No user location, no permission | Must | Implemented, verified (grep + manifest/plist + code) |
| MAP-10 | Clustering, >10 pins / 48px | Must | Implemented, tested — constants match spec exactly |
| MUS-01..05 | Song list/search/station filter/add/compilation card | Must | **Missing** |
| CMP-01..07 | Compilation screen, reorder, Spotify playlist | Must | **Missing** |
| NWS-05 | Background refresh at start + ~every 30 min while open | Should | Partial — start + reconnect only, no periodic timer |
| SET-01 | Spotify connect/disconnect | Must | Missing (flag correctly off, by design) |
| SET-06 | Disclaimer always visible | Must | Implemented, tested (see IP-03 re: scroll position) |
| SET-07 | Optional Google/Apple sign-in | Must | Implemented + tested in code, **disabled in current config** |
| SET-08 | Delete account in-app | Must | Implemented + tested in code, **disabled in current config** |
| DAT-01..05 | Offline-first, delta sync, no release-content updates, no location/account data captured, local storage | Must | Implemented, tested |
| MST-01 | No official/fan map without rights | Must | **Violated — see RA-01** |
| MST-04 | On-demand+cached tiles, never blank offline | Must | Implemented in code; contradicted by direct first-launch observation, see MAP-2 |
| MST-05 | Normalized pin coordinates | Must | Implemented, extensively tested |
| EDT-01..81 | Map Editor desktop tool requirements | — | Out of scope (desktop-only); the data contract it writes is covered by MST-* |

Everything not listed above (CAT-*, DET-*, NWS-01..04/06-09, SET-02..05, MAP-01..07/09/11) is
**implemented and tested**.

### Findings

| ID | Severity | Evidence | Fix |
|---|---|---|---|
| MAP-1 | **BLOCKER** | See RA-01 above — `core/mapview/.../map_dev.png` is a detailed, labeled "Map of Leonida" image confirmed rendering live on an iOS 18.5 simulator after a real sync against the project's real backend (`local.properties`'s `SUPABASE_URL=https://fgkoxqovgammjunlmhqn.supabase.co`). | See RA-01. |
| FUNC-1 | **BLOCKER** | `feature/music` doesn't exist; MUS-01..05/CMP-01..07 (mostly Must) entirely unimplemented; tab hidden via `NavBar.kt`'s `VisibleNavDestinations`. | Formally descope Music/Compilation for v1 in the Functional Analysis/requirements notes, or build it before submission. |
| FUNC-2 | **BLOCKER** | SET-07/SET-08 code exists and is tested, but `ACCOUNT_LOGIN_ENABLED` is commented out in `local.properties`, iOS `TEAM_ID`/`GOOGLE_WEB_CLIENT_ID` are blank. | See RA-08 — lock the release decision one way or the other before writing final review notes. |
| MAP-2 | SHOULD | First-launch screenshot (~3s after install) showed 4 correctly-colored pins floating on a pure white canvas, no map image/grid/placeholder visible; map appeared only after background sync completed. | Confirm a visible neutral placeholder (not plain white) covers the canvas from frame one, independent of theme background. |
| SCRN-1 | **BLOCKER** | `core/data/.../repository/AccountServiceImpl.kt:48`: `status.session.user!!.toAccountUser()` inside a `Flow.map{}` with no `.catch{}`; `SettingsViewModel.kt:74` combines this flow directly. A null `session.user` while `Authenticated` crashes Settings uncaught. | Replace the `!!` with a safe fallback to an error state, or wrap with `.catch{}`. |
| SCRN-2 | SHOULD | `ContentSyncerImpl.syncNews()` (~line 264) is the one sync function **not** routed through `TableSync.syncTable()`'s catch-all; `sync()` itself only has a `finally`, no `catch`. The launching coroutine scope (`App.kt:69-72`) has no `CoroutineExceptionHandler`. | Wrap `syncNews()`'s body the same way every other table sync is wrapped. |
| SCRN-3 | SHOULD | `TableSync.kt:47`: `change.deletedAt!!` fallback throws (caught harmlessly) when a row fails to map, but that table's sync cursor never advances past the bad row — it re-fetches the same stuck page forever. | Skip/report just the unmappable row instead of aborting the whole page's cursor advance. |
| SCRN-4 | SHOULD | NWS-05 wants a periodic (~30 min) refresh while the app is open; `ContentSyncerImpl.start()` only syncs at launch and on reconnect. | Add a periodic trigger alongside the two existing ones. |
| I18N-1 | SHOULD | `core/design/.../screens/NewsScreen.kt` (used live by `feature:news`) hardcodes ~9 English literals ("News", "Updated …", category chip labels, "No stories yet", offline banner text, "Retry", "Refresh" VoiceOver label) instead of `stringResource()`; `MapControls.kt:37-38` hardcodes "Zoom in"/"Zoom out" VoiceOver labels on the real zoom buttons. Every other production screen is clean of this pattern. | Move these into the existing `strings.xml`/`values-it` mechanism used everywhere else — also fixes the untranslated VoiceOver/TalkBack labels. |
| MAP-3 | NICE | MAP-10's cluster threshold (>10 pins, 48px) matches the spec's proposed default exactly, but both are still listed as unconfirmed in `docs/ROADMAP.md`'s Open Decisions, as is the mixed-cluster color. | Confirm these as final before relying on them for store screenshots. |

**Positive confirmations:** no location permission/API anywhere (clean grep across Kotlin/Swift/Manifest/
Info.plist); 44dp touch targets consistently applied; reduced motion respected on both platforms for tile
cross-fades; pin accessibility semantics correctly merged per-pin (no double announcement); Light/Dark/
System theme applies with no restart, visually confirmed on simulator; Map/Catalog/Detail/News all have
explicit sealed Loading/Error/Empty/Offline states with no unguarded force-unwraps found in those
particular paths (the one force-unwrap found, SCRN-1, is in the account/session path, not these).

---

## 3. Content and Intellectual Property

**Correction to this section's own sub-audit:** the content/IP pass initially described
`core/mapview/.../map_dev.png` as "confirmed-safe" (grid/watermark), which — after the functional
sub-audit flagged the opposite and I personally viewed both images — **is wrong**. `map_dev.png` is the
risky "Map of Leonida" asset; `map_dev_fallback.png` is the safe watermarked grid. See RA-01 / MAP-1. The
rest of this section's findings stand.

| ID | Severity | Evidence | Fix |
|---|---|---|---|
| IP-01 | **BLOCKER** | App icon imitates GTA VI's neon-sunset/palm trade dress — see RA-02 above. | See RA-02. |
| IP-02 | **BLOCKER (unresolved)** | `/tmp/audit_map/leonida_v1.png` and `v2_source.png` (outside the repo, not shipped, of unverified origin — see RA-01 for how these were found and why they weren't dismissed as noise) depict a detailed, unwatermarked "Leonida" map with county names (Lummox/Leonard/Kelly/Vice Dale/Mariana County) and place names (Port Gellhorn, Ambrosia, Welcome Sands, Hailea, Watson Isle, Gold Coast, Key Lento) matching widely-circulated fan "GTA VI map" renders — a *different* render from the git-tracked `map_dev.png`. These names don't match `seed.sql`'s fake `https://example.com/...` URL. | Directly query the production `map_config` table (`select source_image_url, tile_url_template from public.map_config order by map_version desc limit 1;`) against the real `ilovevi` Supabase project and open what it actually serves, to settle whether this specific image is (or was a candidate for) the production map. Regardless of the answer, treat "where does the real map art come from" as the single open decision to resolve before submission. |
| IP-03 | SHOULD | The "unofficial app" disclaimer (SET-06) is always rendered but `SettingsScreen.kt`'s `AboutSection` (line 203, disclaimer at line 624) comes after Appearance/Map/Account/Spotify sections in one scrollable column, styled `bodySmall`/`inkMuted` (low visual weight) — not visible without scrolling on a typical phone. | Move the disclaimer higher (directly under the Settings title), or add a lightweight persistent disclosure elsewhere (e.g. a Map/Catalog tab footer or first-launch notice). |
| IP-04 | NICE (superseded by ASR-02 for severity) | `backend/supabase/seed.sql` is explicitly "a small, obviously-fake dataset for local tests/previews"; every row uses bracketed placeholders (`[Weapon 1]`, `[Zone 1]`, `[Station 1]`, `[Song 1]`/`[Artist 1]`, `[Feed 1]`). Not an IP risk in isolation (no real names used) — but see RA-03/ASR-02 for why this is actually a BLOCKER for the 2.1 completeness guideline if shipped as-is. | See RA-03. |
| IP-05 | NICE | Music/Spotify: `stations`/`songs` tables store only `title`/`artist`/`spotify_track_id` text — no audio/lyrics columns. `SpotifyConnection.kt` is an unimplemented interface; `FeatureFlags.spotify = false`; the tab was removed from nav for v1. No embedded audio, no streaming, no live deep-link yet. | None required now. When Spotify ships, ensure it only deep-links out, never embeds playback. |

### Trademark/IP term scan (compact)

| Term | Occurrences | Representative evidence | Judgment |
|---|---|---|---|
| "Rockstar"/"Rockstar Games" | 13 | `README.md:47`, Settings `strings.xml` (EN/IT), `docs/requirements-notes.md:15`, RSS test fixtures | All legitimate disclaimer/nominative/test-fixture use. |
| "Take-Two"/"Take Two" | 5 | `README.md:47`, design-system README, `strings.xml` | All disclaimer/nominative use. |
| "GTA" | 54 | `seed.sql` (news search query), `tools/tilegen/README.md`, `docs/ROADMAP.md` | Overwhelmingly descriptive/nominative; no instance uses "GTA" as the app's own brand. |
| "Grand Theft Auto" | 10 | `seed.sql`, RSS test fixture | Same — descriptive/test use. |
| "Leonida" | 14 | `README.md:16` ("explore Leonida"), News preview sample text, test fixture | Mostly fine as nominative use of the setting name — the real risk is the *map art* (RA-01/IP-02), not the word. |
| "Vice City" | 2 | Sample item description, test fixture | Sample/preview copy and test fixture only — swap the sample string before release so no reviewer screenshot shows it out of context. |
| Character names (Trevor/Michael/Franklin/Lucia/Jason) | 0 | — | None found anywhere. |

The text-level disclaimer and nominative-use posture are solid and consistent. The two live risks are
visual, not textual — the icon (IP-01/RA-02) and the map art (IP-02, MAP-1/RA-01).

---

## 4. Privacy, Security and Account

**No leaked secrets found**, in the working tree or in git history (28 commits, pickaxe-searched for
`service_role`, client-secret, private-key, and keystore/`.env`-shaped filenames). `local.properties`
(Supabase URL + **anon/publishable** key + Google Web client **ID**, confirmed via JWT payload decode —
`{"role":"anon", ...}` — not a secret) is gitignored and genuinely untracked. No `service_role` key,
OAuth client secret, `.p8`/`.p12`/`.jks`/`.mobileprovision`/`.env` file, or private-key block exists
anywhere, past or present.

| ID | Severity | Evidence | Fix |
|---|---|---|---|
| PRIV-01 | **BLOCKER** | See RA-10 above — no `PrivacyInfo.xcprivacy`, yet `core/design/src/iosMain/.../Locale.ios.kt:17` calls a Required-Reason API (`NSUserDefaults`). | See RA-10. |
| PRIV-02 | SHOULD | No privacy policy or support URL string exists anywhere in app code — Settings' About section only shows the disclaimer, licenses, and version. | Confirm a real privacy policy URL and support URL/email are entered in App Store Connect and Play Console (both required there regardless of in-app linking); strongly consider an in-app link too, since the app collects account email/name via sign-in. |
| PRIV-03 | SHOULD | `TARGETED_DEVICE_FAMILY = "1,2"` ships iPhone **and** iPad, but no size-class/adaptive-layout code exists anywhere in the UI modules. | Either add iPad-adapted layouts before submission, or drop iPad from `TARGETED_DEVICE_FAMILY` (ship iPhone-only) until adaptive layouts exist. |
| PRIV-04 | SHOULD | `Info.plist` has no `ITSAppUsesNonExemptEncryption` key. The app only uses standard HTTPS/TLS (confirmed — no custom crypto found), so it almost certainly qualifies for the standard exemption, but its absence means this must be answered manually at each submission. | Add `ITSAppUsesNonExemptEncryption = false` to `Info.plist` to skip the manual prompt, or confirm the answer every release. |
| PRIV-05 | NICE | `Info.plist` has zero `NSxxxUsageDescription` keys — consistent with no location/camera/photo-library API usage anywhere in code. No ATS exceptions present. | No action; flagged only so a human confirms no permission-gated feature was silently added later. |
| PRIV-06 | NICE | Firebase/Crashlytics/Analytics entries exist in `gradle/libs.versions.toml` but are all commented out — no analytics/crash/tracking SDK is active anywhere today. | No action now; revisit the data inventory and `PrivacyInfo.xcprivacy`/Data-safety form if this is ever un-commented. |

### Account handling — traced end to end

- **Google Sign-In is fully optional** — `FeatureFlags.accountLogin` gates the entire Account/Danger-zone
  section; no other feature checks sign-in state.
- **Apple Sign-In is implemented, iOS-only, native** (`ASAuthorizationAppleIDProvider`, `.fullName` +
  `.email` scopes), added specifically to close the Guideline 4.8 risk of offering Google without an
  equivalent (per `docs/auth-notes.md` §1/§2).
- **Account deletion, full trace:** Settings "Danger zone" → `AccountServiceImpl.deleteAccount()` → RPC
  `delete_own_account` → `backend/supabase/migrations/20261106120001_delete_own_account_rpc.sql`:
  ```sql
  create or replace function public.delete_own_account()
  ... security definer ...
  begin
    if auth.uid() is null then
      raise exception 'delete_own_account requires a signed-in session';
    end if;
    delete from auth.users where id = auth.uid();
  end;
  $$;
  grant execute on function public.delete_own_account() to authenticated;
  revoke all on function public.delete_own_account() from anon;
  ```
  Takes **no arguments** and only ever acts on `auth.uid()` — a caller cannot delete anyone else's
  account. `auth.users → auth.identities/sessions/refresh_tokens` cascade-delete, so this is a real,
  complete deletion. Satisfies Apple 5.1.1(v).
- **Sign-out** calls `supabase.auth.signOut()`, which actively deletes the stored session (not just an
  in-memory flag).
- **Token storage at rest:** Android uses `EncryptedSharedPreferences` (AES-256-GCM, Android Keystore
  master key); iOS uses the real Keychain (`kSecClassGenericPassword`). Neither platform stores the
  session in plain prefs/`NSUserDefaults`/a flat file.
- **Logging:** no token/password/secret/auth-header value found in any `Log./println/NSLog` call anywhere
  in the codebase. Error mapping deliberately surfaces only an enum, never raw exception text.

*(Remember: per RA-08, all of the above is currently unreachable in a default build because
`accountLogin` is off — this is solid engineering sitting behind a flag that isn't switched on.)*

### Data inventory draft for App Privacy / Data Safety forms

| Data type | Collected by | Purpose | Linked to identity |
|---|---|---|---|
| Email address | Google/Apple Sign-In → `auth.users` | Account auth, shown in Settings | Y |
| Name | Google/Apple Sign-In | Shown in Settings | Y |
| Profile photo URL | Google Sign-In (if present) | Shown in Settings | Y |
| User ID (Supabase auth UID) | Supabase Auth | Session/RLS/account deletion target | Y |
| Auth session/refresh token | Supabase Auth client, Keychain/EncryptedSharedPreferences | Keep user signed in | Y |
| IP address | Implicit in every HTTPS request | Supabase infra logs (not app-captured) | Y (server-side) |
| Device locale/language | Platform locale APIs | In-app language override | N |
| Crash data / analytics / ad identifiers | None — no SDK active | N/A | N/A |
| Map/catalog/news content requests | First-party Supabase Postgrest calls | Fetching static app content | N (signed-out) / Y (bearer token present when signed in, content itself not personal) |

**Summary for the forms:** declare Email and Name as "linked to identity, used for account functionality,
not used for tracking." No analytics/advertising/tracking SDK is present today, so Location/Health/
Financial/Browsing History/Advertising-Data categories should be "not collected."

---

## 5. Backend (Supabase)

**Method actually used:** Docker wasn't available in this environment, so `supabase start`/`db reset`/
`db lint` could not be run — confirmed by `supabase db lint` failing with `ECONNREFUSED 127.0.0.1:54322`.
This section is a **pure static review** of the 11 migration files, `config.toml`, `seed.sql`, and the
pgTAP tests. The MCP Supabase connector only has an unrelated project ("Pickabot-Mobile") linked — the
real ILoveVI project ref (`fgkoxqovgammjunlmhqn`, from `backend/supabase/.temp/project-ref`) was **not
reachable** through any tool available in this session. **You need to manually check the dashboard** —
see the checklist below.

Schema (8 `public` tables: `weapons`, `vehicles`, `items`, `stations`, `songs`, `map_config`, `map_pins`,
`feed_sources`; 2 storage buckets: `map-tiles`, `entry-images`; 2 RPCs: `commit_map_edits`,
`delete_own_account`). No `profiles` table, no admin role column — "admin" is `public.is_admin()` reading
`auth.jwt() -> 'app_metadata' ->> 'role' = 'admin'`, which ordinary users cannot write themselves (only a
service-role call can set `app_metadata`) — a clean design with nothing for a user to self-escalate.

| ID | Severity | Evidence | Fix |
|---|---|---|---|
| BE-01 | NICE | Every content table has RLS enabled plus `select_anyone using (true)` and admin-only write via `public.is_admin()`. All 8 are intended to be publicly readable; confirmed correct, no table found missing RLS. | None; re-confirm via dashboard Advisors once the project is reachable. |
| BE-02 | **BLOCKER (contingent)** | See RA-11 above — the whole admin boundary reduces to "nobody carelessly sets `app_metadata.role=admin`." No code gap, no self-escalation path found; `functions/` is empty (no Edge Functions ship at all). | Document and follow a specific, narrow grant procedure. |
| BE-03 | — | `commit_map_edits(payload jsonb)` is **not** `security definer`, explicitly checks `public.is_admin()` and raises if not, and every insert still goes through the underlying tables' own admin-only RLS (defense in depth). pgTAP test exercises both admin-allowed and anon-denied. | None required; see BE-07 for a minor cleanup. |
| BE-04 | — | `delete_own_account()` is `security definer`, touches only `auth.uid()`, no parameter for another user's id, explicit `revoke ... from anon`. pgTAP proves user isolation. | None. |
| BE-05 | — | `map-tiles` and `entry-images` buckets: `public = true` with `select_anyone` + admin-only insert/update/delete policies, consistent with the bucket flag. | None. |
| BE-06 | SHOULD | **No scheduled job exists for "news."** News is fetched client-side (`:core:data`'s `ContentSyncerImpl.syncNews()`), confirmed intentional via `seed.sql`'s own comment. No backend cost risk from a job, but every device independently hits Google News RSS and third-party feed URLs on its own schedule. | No backend action required; confirm `syncNews()` has a sane poll interval and doesn't hammer `news.google.com`. |
| BE-07 | NICE | Anon can SELECT all 8 tables/buckets with no RLS-layer pagination cap — relies on `config.toml`'s `[api] max_rows = 1000`, adequate for the current (tiny) seed size. `commit_map_edits` isn't explicitly `revoke`d from `anon` (it errors inside instead — functionally safe, just a wasted round-trip). | Add `revoke all on function public.commit_map_edits(jsonb) from anon;` for consistency with `delete_own_account`'s pattern; consider dashboard rate limiting before launch. |
| BE-08 | SHOULD | No tile files are committed to the repo; actual stored bucket size depends entirely on the real map pyramid once generated. `SupabaseAdminClient.kt:87` sets `Cache-Control: max-age=31536000, immutable` on every tile upload — a strong, correct cache header. | Once the real tile pyramid is generated, check its total size against your Supabase plan's storage/egress limits in the dashboard (these change — don't trust a remembered number). |
| BE-09 | SHOULD | `config.toml` has no backup/PITR configuration (that's dashboard-managed, not CLI-controlled). No evidence backups have been verified. | Open **Dashboard → Database → Backups** for project `fgkoxqovgammjunlmhqn`; confirm daily backups are enabled and decide if PITR is needed before launch. |
| BE-10 | SHOULD | If `map-tiles` were emptied, the only regeneration path is re-running `tools/tilegen` against the original source map image — no second copy of the generated pyramid exists elsewhere in the repo. | Confirm the original high-res source image lives somewhere other than Supabase storage before relying on `tools/tilegen` as your "backup" plan. |

**Overall:** no BLOCKER-severity *code* defect in RLS/RPCs — the one BLOCKER row (BE-02) is a process
dependency, not a bug. The real unresolved items are operational: confirm the live project's migrations
match this repo, run the dashboard Advisors, verify the backup tier, and measure real tile-pyramid size.

---

## 6. App Store Readiness (iOS)

Apple's App Store Review Guidelines were fetched and read on **2026-11-17** (same day as this audit).

| ID | Severity | Evidence | Fix |
|---|---|---|---|
| ASR-01 (1.x Safety/UGC) | OK | Only UGC-adjacent feature anywhere is `commit_map_edits`, driven exclusively by the desktop-only `tools/map-editor` (explicitly documented as never shipped to players). No posting/commenting/sharing surface in the mobile app. | None. |
| ASR-02 (2.1 Completeness) | **BLOCKER** | See RA-03 above — `seed.sql` ships bracketed placeholder content; real content entry is blocked until GTA VI ships (19 Nov 2026). | See RA-03. |
| ASR-03 (2.1 Stability) | SHOULD | No crash-test/TestFlight results available to this audit beyond the simulator run in §2/§8. `docs/auth-notes.md` §7's build-tooling concern is resolved per BUILD-10. | Archive and run on a real device/TestFlight build with a stable (non-beta) Xcode release before submission; run a full manual smoke pass per screen. |
| ASR-04 (2.3 Accurate metadata) | AT RISK | No store listing existed before this audit (see §B / `docs/store-listing.md`); no privacy/support URL string found anywhere in the repo. | Use the draft in `docs/store-listing.md`: descriptive, non-trademarked keywords only. |
| ASR-05 (4.2 Minimum functionality) | OK | Catalog/Map/News/Settings are native Compose Multiplatform screens backed by local SQLDelight storage and ViewModels, not a WebView wrapper. `webApp/` is a separate product, not what ships as the iOS binary. | Mention native capabilities (offline catalog, pin clustering, map filters) in App Review notes to preempt a 4.2 question. |
| ASR-06 (4.8 Login services) | OK (code-level), contingent on manual setup | Google ships on iOS; Sign in with Apple was added specifically to close this risk (native `AuthenticationServices`, entitlement already present). None of 4.8's exemptions apply otherwise. | Finish the manual Apple Developer Portal steps (`docs/auth-notes.md` §2): enable the capability, create the Services ID, upload the key, configure Supabase — and resolve RA-08 first (no point finishing this if the feature ships switched off). |
| ASR-07 (5.1 Privacy) | AT RISK | In-app deletion satisfies 5.1.1(v) in code (see §4), but no privacy policy URL exists anywhere yet, and the detailed App Privacy answers haven't been entered in App Store Connect. | Publish a real privacy policy page, link it from Settings' About section and App Store Connect, before submission. |
| ASR-08 (5.2 IP) | AT RISK | No formal "fan app" exception exists in the guidelines. Mitigating: disclaimer is hardcoded and always-rendered; no Rockstar logo/trademark assets found in app resources. Aggravating: see RA-01 and RA-02. | Resolve RA-01/RA-02 first — disclaimers don't neutralize visual trade-dress/IP risk, they only address textual misrepresentation. |
| ASR-09 (App icon) | SHOULD | `app-icon-1024.png`: 1024×1024, RGB, no alpha channel (confirmed via `sips`) — correct re: the no-transparency rule. Dark/tinted appearance slots exist in `Contents.json` but have no distinct art assigned (falls back to default, not an error). Cannot visually confirm corner-rounding from file properties alone. Separately, see RA-02 for the branding content itself. | Visually inspect in Xcode/Preview for pre-baked rounded corners (Apple masks corners itself). Optional: add distinct dark/tinted artwork. |
| ASR-10 (Launch screen) | OK | `INFOPLIST_KEY_UILaunchScreen_Generation = YES` — Xcode auto-generates a blank launch screen. | NICE: a branded static launch screen is polish, not required. |
| ASR-11 (iOS deployment target) | SHOULD | `IPHONEOS_DEPLOYMENT_TARGET = 18.5` in both configs — a very high floor that materially shrinks the addressable install base, though not itself a guideline violation. | Confirm this is intentional (does any dependency genuinely require 18.5?); consider lowering it. |
| ASR-12 (Code signing readiness) | **BLOCKER** | Same root cause as BUILD-02/RA-05 — `TEAM_ID` blank, `CODE_SIGN_STYLE = Automatic`. | See RA-05. |
| ASR-13 (TARGETED_DEVICE_FAMILY) | Informational | `"1,2"` → targets iPhone **and** iPad — drives the mandatory screenshot set below, and ties into PRIV-03's "no iPad-adaptive layout exists" finding. | Confirm iPad is actually validated, or restrict to iPhone-only (see PRIV-03). |

**Technical requirements as of 2026-11-17:** App name ≤30 chars, subtitle ≤30 chars. Screenshots: 1–10 per
required device size, no alpha/transparency. Mandatory sizes for this app (per ASR-13): **6.1" iPhone**
and **13" iPad** (App Store Connect can auto-scale from your highest-resolution set if the UI is visually
consistent across sizes — recommended additions: 6.9"/6.7" iPhone, 12.9"/11" iPad). Minimum iOS version
actually targeted: **iOS 18.5**.

---

## 7. Android

| ID | Severity | Evidence | Fix |
|---|---|---|---|
| AND-01 (Target API level) | Informational — verify live | `compileSdk = 37`, `targetSdk = 37`, `minSdk = 24`. Comfortably ahead of any floor known as of training cutoff (Play required ≥ API 35 from Aug 2025, with an annual +1 expected). | Verify the live current minimum in Play Console before upload — this changes yearly. |
| AND-02 (Data safety form) | SHOULD | Optional Google sign-in collects name+email; no location permission; no active analytics/crash SDK; Spotify is a disabled flag with no SDK dependency yet; news articles open in the external browser. | Structure the form around: Collected — account email/name (optional, functionality only); Shared — none; Security — HTTPS, account deletion supported. Re-run once Spotify ships for real. |
| AND-03 (Content rating) | Informational | App renders no gameplay/violence/gambling/UGC itself — it's a reference/companion app describing a separately-rated third-party game by name. | Answer IARC honestly; disclose in free-text that the app references but doesn't depict third-party game content. Expect a low rating. |
| AND-04 (Play App Signing) | SHOULD | Same root cause as BUILD-01/RA-04 — no `signingConfig` assigned at all in `androidApp/build.gradle.kts`. | See RA-04; additionally enroll in Play App Signing (default for new Play Console apps). |
| AND-05 (OAuth Android client SHA-1) | Informational | Needs registering in Google Cloud Console once a real keystore exists; none exists in this repo to extract a value from. | Run `./gradlew :androidApp:signingReport` (or `keytool -list -v`) once signing is configured (AND-04), register the printed SHA-1. Note: Play App Signing re-signs the final APK with its own key — you'll need that SHA-1 from Play Console too, not just your local upload-key one. |
| AND-06 (Manifest permissions) | OK | Exactly two permissions declared: `INTERNET`, `ACCESS_NETWORK_STATE`, both justified inline. Confirmed no location permission. | None. |
| AND-07 (ProGuard/R8) | SHOULD | `isMinifyEnabled = false`; `proguard-rules.pro` is the untouched default template. | Safe as shipped (no obfuscation-crash risk) but misses size/protection benefits. If enabled later, test thoroughly — Supabase/Ktor/kotlinx.serialization model classes typically need explicit `-keep` rules. |
| AND-08 (64-bit requirement) | OK | No ABI filter restricting to `armeabi-v7a` anywhere; default AGP/KMP build includes `arm64-v8a`. | None; just confirm Play's pre-launch report doesn't flag anything. |
| AND-09 (Closed testing requirement) | Informational — verify live, frequently changes | Historically: 12 testers, 14 continuous days, before production access for new personal accounts. | Treat as potentially stale for Nov 2026 — check Play Console directly, and start closed testing 3+ weeks before any intended launch given this lead time. |
| AND-10 (Account-deletion web URL) | SHOULD | Play's account-deletion policy requires a **web URL** reachable without installing the app, separate from the in-app flow (already done). `docs/ROADMAP.md` step 15 lists this as still outstanding. | Publish a simple static page describing the deletion request process and enter its URL in Play Console. |

---

## 8. Performance and Stability

**Method:** built and ran `iosApp` (Debug) on an iPhone 16 Pro / iOS 18.5 simulator — cold-start
screenshots and idle memory are **real measurements**. Scripted multi-minute interaction (pinch-zoom, tab
switching, theme toggle) via Simulator automation **could not run** in this sandbox (Accessibility
automation permission isn't grantable here) — those items fall back to code review and are marked **NOT
VERIFIED** below.

| ID | Severity | Evidence | Fix |
|---|---|---|---|
| PERF-01 | NICE | Real + code-reasoned. App shell renders within ~1s of launch; the map background image is blank for ~2 more seconds while syncing. No hang/crash. `App.kt` does a blocking `runBlocking` read of one settings row on the composition path (intentional, to avoid a theme/language flash) — low risk today, but a slow query could stall the first frame. Seed import/sync both run off the composition path (`LaunchedEffect`), not a first-frame risk. | Consider a tiny splash/placeholder state instead of the blocking preference read. Re-measure cold start on a **Release** build on a real (older) device — these numbers are Debug/simulator only. |
| PERF-02 | NICE | Code-reasoned (live zoom not verified — automation blocked). `TileMemoryCache` is a bounded, byte-budgeted LRU (96 MB mobile cap) with low-zoom tiles pinned; wired to a real OS low-memory signal on iOS. Image decoding downsamples before allocating full-resolution bitmaps on both platforms. No unbounded cache or repeated-redecode pattern found. | No code change indicated. Manually pinch-zoom repeatedly on a real device and watch Instruments/Xcode's memory gauge before submission — the one check this audit couldn't perform. |
| PERF-03 | NICE | Code-reasoned. Current seed data is tiny (a few dozen rows total); real catalog content doesn't exist yet (blocked until 19 Nov 2026, same as RA-03). News items are capped at 200/topic, with soft-deleted overflow that accumulates indefinitely (low single-digit MB even after years). | No near-term action; re-run this estimate once real content is loaded post-launch. Consider a periodic hard-delete/VACUUM pass for old news tombstones later — not a submission blocker. |
| PERF-04 | NICE | Verified absence of background work — no `BGTaskScheduler`/`WorkManager` anywhere, no `UIBackgroundModes` declared. Sync runs only at app start and on offline→online reconnect, both only while the process is alive. | Effectively a pass for Guideline 2.4.2/battery concerns. No fix required. |
| PERF-05 | SHOULD | Confirmed: no crash reporter wired in on any platform (Firebase/Crashlytics entries exist in version catalogs but are all commented out; no Sentry/Bugsnag anywhere). | Wire in a minimal crash reporter (e.g. Sentry or Crashlytics) with PII collection disabled, before or shortly after submission — otherwise there's no visibility into field crashes right when traffic spikes around GTA VI's launch. |
| PERF-06 | NICE | Not verified (automation blocked) + one real data point: no crash on cold launch; process RSS settled at ~224 MB idling on the initial map screen (Debug/simulator only, not representative of Release/on-device). | Run a manual 5+ minute soak test (navigate all tabs, zoom repeatedly, toggle theme) on a Release build on a real, ideally older, device before submission, watching for memory growth/slowdown/crashes. Explicitly flagged as unverified by this audit. |

**Summary:** no BLOCKER-level performance/stability issue found — tile caching, image downsampling, and
sync triggering are all deliberately bounded and well-documented in code. The two real gaps are the total
absence of crash reporting (PERF-05) and the inability to complete a live soak test in this sandboxed
environment (PERF-02/PERF-06) — both need direct attention from the team before submission.

---

## Manual actions checklist (things only you can do)

- [ ] **Resolve the map art.** Query the live `map_config` table / `map-tiles` bucket directly; decide
      where production map art will actually come from (RA-01).
- [ ] **Redesign or legally clear the app icon** (RA-02).
- [ ] **Decide the Music/Compilation scope** for v1 — build it or formally descope it (RA-07).
- [ ] **Decide the account/sign-in scope** for v1 — turn it on for real (fill `TEAM_ID`,
      `GOOGLE_WEB_CLIENT_ID`, `ACCOUNT_LOGIN_ENABLED=true`) or strip it from the listing (RA-08).
- [ ] **Fill in your real Apple Developer Team ID** in `Config.xcconfig` (RA-05).
- [ ] **Set up Android release signing** (keystore outside the repo, enroll in Play App Signing) (RA-04).
- [ ] **Wire `-Pilovevi.release=true`** into your actual CI/release build pipeline (BUILD-06) — and verify
      the dev map image is gone from the resulting build.
- [ ] **TestFlight a real build on a real iPhone**, including an airplane-mode-at-launch test and a
      multi-minute soak test (map zoom, tab switching, theme toggle) — several checks in §2 and §8
      explicitly could not be run in this sandbox.
- [ ] **Do the same on a real low/mid-range Android device** if you intend to actually ship Android
      alongside iOS.
- [ ] **Publish a real privacy policy page and a support page**; enter both URLs in App Store Connect and
      Play Console; link them from Settings (PRIV-02, ASR-07).
- [ ] **Publish the Play account-deletion web page** required by Play's policy, separate from the in-app
      flow (AND-10).
- [ ] **Open the Supabase dashboard** for project `fgkoxqovgammjunlmhqn` → Database → Advisors (security +
      performance) and Database → Migrations, and compare against the 11 local migration files — this
      session could not reach that project through any available tool (§5).
- [ ] **Confirm your Supabase backup/PITR tier** in the dashboard (BE-09).
- [ ] **Run `./gradlew :androidApp:signingReport`** (or `keytool -list -v`) once signing is set up, and
      register the SHA-1 with the Google Cloud Android OAuth client (AND-05).
- [ ] **Check Play Console directly** for the current target API level floor and closed-testing
      requirements (AND-01, AND-09) — both are known to change yearly and this audit's knowledge may be
      stale for Nov 2026.
- [ ] **Load real content** into the backend once available (blocked until 19 Nov 2026 per your own
      roadmap) before submitting — placeholder seed data is a near-automatic 2.1 rejection (RA-03).
- [ ] **Take the real App Store screenshots** at the required sizes once the above are resolved (§6).
- [ ] **Decide the final app name/subtitle** from the two options drafted in `docs/store-listing.md` (safe
      vs. "GTA VI"-descriptive) — this is explicitly flagged as a tradeoff, not a default choice made for
      you.

## Questions only you can answer

1. Where does the real production map image actually come from, and do you have the rights to it? (RA-01)
   — this is the single most important open question in this whole audit.
2. Is the Music/Compilation feature shipping in v1 at all? If not, should the Functional Analysis be
   formally revised to drop MUS-*/CMP-* as Must items for this release?
3. Is optional sign-in shipping in v1? If yes, who owns getting the Apple Developer Portal / Google Cloud
   Console setup done before submission?
4. Which store-listing option do you want — the safe non-trademark subtitle, or the "GTA VI"-descriptive
   one with its attendant risk (see `docs/store-listing.md`)?
5. What is your actual Supabase plan tier (Free/Pro/Team), and does it include PITR/daily backups? This
   audit could not check your dashboard directly.
6. Do you want to submit iOS and Android together, or iOS first given Android's signing/testing gaps are
   larger?
7. What is your real target submission date, given real content can't be entered until after 19 Nov 2026?

