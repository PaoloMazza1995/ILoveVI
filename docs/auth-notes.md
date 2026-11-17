# Auth notes (step 13B)

Research done 2026-11-06, against: supabase-kt's own docs/source (BOM `3.1.4`, already the version
this repo's `gradle/libs.versions.toml` pins and `:tools:map-editor` already depends on), Apple's
current App Store Review Guidelines, and Google Play's current Developer Program Policies. Written
before any auth code in this step, per the step's own instruction.

## 1. Google sign-in: the method chosen, per platform

**Android — native, Credential Manager + ID token (as the step asked for).**

- Dependencies: `androidx.credentials:credentials`,
  `androidx.credentials:credentials-play-services-auth`,
  `com.google.android.libraries.identity.googleid:googleid`. None of these are in
  `gradle/libs.versions.toml` yet — added as part of implementation.
- Flow: build a `GetGoogleIdOption` with `setServerClientId(GOOGLE_WEB_CLIENT_ID)` (the **Web**
  client ID, not the Android one — this is the one value Google's Credential Manager actually needs;
  the Android client ID only has to exist in the Google Cloud project, already done, so the package
  name + debug SHA-1 are recognized), request it through `CredentialManager.getCredential`, extract
  the ID token, then call supabase-kt:
  ```kotlin
  supabase.auth.signInWith(IDToken) {
      idToken = googleIdToken
      provider = Google
      nonce = rawNonce
  }
  ```
- **Nonce**: generate a random raw nonce, SHA-256-hash it (hex) for the value sent to Google's
  `GetGoogleIdOption.setNonce`, and pass the **raw** (unhashed) nonce to supabase-kt's `nonce`
  parameter — Supabase hashes it the same way server-side and compares. This means **"Skip nonce
  checks" must stay OFF** for the Android/ID-token method — it exists only as an escape hatch for
  clients that can't do this dance, which supabase-kt can.
- Supabase Dashboard → Authentication → Sign In / Providers → Google needs the **Android** client ID
  added to the "Authorized Client IDs" list (comma-separated after the Web one, which is already
  there per your setup) — the Android ID token's `aud` claim is the Web client ID, but its signer
  verification also checks the list of allowed client IDs matches what Google issued. **Action for
  you: add the Android client ID to that list; nothing else changes in your existing Supabase
  config.**

**iOS — web OAuth (PKCE), not the native `GoogleSignIn-iOS` SDK.** supabase-kt documents both as
valid; this repo's iOS target has no CocoaPods/SPM integration today (`iosApp` is a bare SwiftUI
shell calling straight into the Compose Multiplatform `MainViewController()`, confirmed by reading
`iosApp/iosApp/iOSApp.swift` — no `Podfile`, no `Package.swift`). Adding `GoogleSignIn-iOS` would
mean introducing a new native-dependency mechanism (CocoaPods or SPM + a cinterop bridge) this
project has never needed, just for one button. The web OAuth flow needs none of that: supabase-kt's
`Auth` plugin already knows how to drive it from `commonMain` on every platform.

- Flow: `supabase.auth.signInWith(Google) { redirectTo = "com.pamaz.vi://login-callback" }` opens
  the system browser (`ASWebAuthenticationSession` under the hood on iOS, Custom Tabs on Android —
  though Android uses the native method above instead); Supabase redirects back to
  `com.pamaz.vi://login-callback` with the session in the URL fragment; the app's deep-link handler
  calls `supabase.auth.handleDeeplinks(url)` (or the exact method supabase-kt 3.1.4 exposes —
  verified against the real jar at implementation time, this module's own established convention per
  `core:network/README.md`) to complete the sign-in.
- This is plain OAuth, not the ID-token method, so **no nonce, no "Skip nonce checks" setting, and
  no iOS client ID needs to be listed in Supabase's Google provider config for this path** — the Web
  client ID (already configured in Supabase per your message) and the already-registered Supabase
  callback + `com.pamaz.vi://login-callback` redirect URL are the only things this flow needs, and
  you've said both are already done.
- Needs `com.pamaz.vi` registered as a URL scheme in `iosApp/iosApp/Info.plist`
  (`CFBundleURLTypes`) and a `.onOpenURL { url in ... }` (or `UIApplicationDelegateAdaptor`) in
  `iOSApp.swift` forwarding the URL into the shared Kotlin code — this is new, since today's
  `iOSApp.swift` has no URL handling at all. **No other registration is needed from you** — you
  don't need a separate iOS redirect URI in Supabase; the Web client ID + the one deep link you've
  already registered cover this flow.
- Trade-off, stated plainly: this shows Safari's OAuth consent screen rather than Apple's native
  one-tap Google account picker. If that UX gap matters later, swapping in `GoogleSignIn-iOS`
  natively is a contained change behind the same `AuthProvider` interface — see §3.

Both platforms end up with a real Supabase session either way; `AccountService` in `:core:data`
doesn't know or care which method was used underneath.

**iOS also gets Sign in with Apple (native), per your 2026-11-06 decision — see §2.** Apple's own
`AuthenticationServices` framework is a system framework (ships with every iOS SDK, unlike
`GoogleSignIn-iOS`), so — matching this repo's existing convention of writing every iOS platform
`actual` straight in Kotlin against Apple's system frameworks (`DatabaseDriverFactory.ios.kt`,
`ConnectivityMonitor.ios.kt`, `NetworkHttpEngine.ios.kt` — none of them touch Swift) — Apple sign-in
is implemented as Kotlin/Native `cinterop` against `platform.AuthenticationServices.*`
directly in `core:data`'s `iosMain`, with **no CocoaPods, no SPM, no Swift code added**. Flow:
`ASAuthorizationAppleIDProvider().createRequest()` (requesting `.fullName`/`.email` scopes, with a
SHA-256-hashed nonce set via `requestedScopes`/`nonce`), run through `ASAuthorizationController`
(a Kotlin class implementing `ASAuthorizationControllerDelegateProtocol` via Obj-C interop), then
the returned `identityToken` + raw nonce go to:

```kotlin
supabase.auth.signInWith(IDToken) {
    idToken = appleIdentityToken
    provider = Apple
    nonce = rawNonce
}
```

Apple sign-in is **iOS-only** — Apple's own SDK doesn't exist on Android, and Android has no policy
reason to need it (Guideline 4.8 only governs App Store submissions), so the Settings screen's
Account section renders a Google button on Android, and both Google + Apple buttons on iOS, via
`AuthProvider.availableOn(platform)` rather than a single hardcoded list — see §3.

## 2. Store policy findings

**Apple App Store Guideline 4.8 (Login Services) — this is the one that needs your decision.**

> Apps that use a third-party or social login service (Google Sign-In, Facebook Login, etc.) to set
> up or authenticate the user's **primary account** with the app must also offer, as an equivalent
> option, another login service that (a) limits data collection to name + email, (b) lets the user
> keep their email private, (c) doesn't collect ad-tracking interactions without consent. Sign in
> with Apple is the obvious (not the only) service meeting all three; your own email/password system
> would also qualify, but this app has none.

This step's spec has the app offer **only** Google as a sign-in method (no email/password). The
guideline's exemptions don't apply here (not an enterprise/education app, not "client for one
specific third-party service" — Google here *is* your own account, not content the user is
fetching). The fact that signing in is **optional** (the app works fully signed out) does not exempt
you: the rule triggers on "offers a third-party login for the account," not on whether it's
mandatory. **Net: if `feature:settings`'s "Continue with Google" ships on iOS without Sign in with
Apple, it is a realistic App Store rejection risk under 4.8, not a hypothetical one.**

**2026-11-06 decision: implement Sign in with Apple in this same step** (your choice, over the
step's original "don't implement Apple unless I ask" instruction — flagging the change explicitly
rather than silently expanding scope). iOS ships both Google and Apple from day one; Android ships
Google only (Apple has no Android SDK and no Android policy requires it — see §1's native-Apple
note). This closes the 4.8 risk entirely rather than deferring it.

**Action for you — Apple Developer Portal + Supabase, before this step's iOS auth code can be tested
against a real account** (same shape as what you already did for Google). **Important correction
found while wiring this**: the iOS app's real Bundle ID is **`com.pamaz.vi.ILoveVI`**, not
`com.pamaz.vi` — confirmed by reading `iosApp/Configuration/Config.xcconfig`'s
`PRODUCT_BUNDLE_IDENTIFIER = com.pamaz.vi.ILoveVI$(TEAM_ID)` ( `$(TEAM_ID)` is currently blank in
that file, so it resolves to exactly `com.pamaz.vi.ILoveVI`, not something with a suffix). Android's
`applicationId` really is `com.pamaz.vi` (`androidApp/build.gradle.kts`) — the two platforms'
identifiers are genuinely different strings, confirm this matches what you already registered for
the Google Cloud "iOS" OAuth client's bundle id field, and use `com.pamaz.vi.ILoveVI` below:

1. **Apple Developer Portal → Certificates, Identifiers & Profiles → Identifiers → your App ID**
   (`com.pamaz.vi.ILoveVI`): enable the **"Sign in with Apple"** capability.
2. **Identifiers → "+" → Services IDs**: create one (e.g. `com.pamaz.vi.signin`) — this is **not**
   the app's Bundle ID, a separate reverse-DNS identifier used only for the web/OAuth side of Apple
   sign-in (Android's `signInWithOAuth` path). Configure its "Sign in with Apple" website URLs:
   primary domain = your Supabase project's domain, return URL =
   `https://<your-project-ref>.supabase.co/auth/v1/callback` (the same Supabase auth callback
   already registered for Google).
3. **Keys → "+"**: create a new key with "Sign in with Apple" enabled, download the `.p8` file once
   (Apple only lets you download it once) — note its **Key ID**, and your account's **Team ID**
   (top-right of the Developer Portal). **You'll also need this Team ID in
   `iosApp/Configuration/Config.xcconfig`'s `TEAM_ID=` line (currently blank) for code signing to
   work at all** — unrelated to Apple sign-in specifically, but you'll hit it immediately when
   building for a real device.
4. **Supabase Dashboard → Authentication → Sign In / Providers → Apple**: enable it, enter **Team
   ID**, **Key ID**, the `.p8` file's contents, and in **Client IDs** put the Services ID from step
   2 **first**, then the app's Bundle ID (`com.pamaz.vi.ILoveVI`) second, comma-separated — the
   Services ID covers Android's web-OAuth path, the Bundle ID covers iOS's native
   `signInWithIdToken` path (its token's `aud` claim is the Bundle ID, not the Services ID).
5. **Xcode** (`iosApp.xcodeproj`): add the "Sign in with Apple" capability under Signing &
   Capabilities — this is a project-file change I'll make as part of implementation (a new
   `iosApp.entitlements` with `com.apple.developer.applesignin`), but it only takes effect once step
   1 above has been done on your account; flag to me if `assembleDebug`-equivalent iOS builds start
   failing on a missing provisioning profile entitlement and I'll re-check this.

Google Play has no equivalent "offer another provider" rule — Android shipping Google-only is fine
there regardless.

**Google Play account deletion policy** — in effect since 2023, still current:

- An app offering account creation must offer both: (a) an **in-app path** to request deletion of
  the account and its data, and (b) a **web URL** (entered in Play Console's data-safety form,
  separate from the app binary) where deletion can also be requested, reachable without installing
  the app. Deactivation/freezing does not count as deletion.
- (a) is this step's job (`Danger zone → Delete account`, §4 below). (b) is a Play Console listing
  field, not app code — it belongs with step 15 (store readiness), not here; I've added it to that
  step's checklist in `docs/ROADMAP.md` rather than silently dropping it. **Action for you at step
  15 time**: have a reachable page describing the request-deletion process (can be a single static
  page since the in-app flow already does the real deletion) and paste its URL into Play Console.
- Apple has the same in-app-deletion requirement (Guideline 5.1.1 (v)), already satisfied by this
  step's "Delete account" flow — no separate web URL is required by Apple.

## 3. Architecture: `AuthProvider`, so Apple can be added later without touching the screen

```
AccountService (core:data, interface)
  state: Flow<AccountState>   // SignedOut, SignedIn(user), Loading, Error(message)
  signInWithGoogle(): Result<Unit>
  signOut()
  deleteAccount(): Result<Unit>

AuthProvider (core:data, interface) — one per sign-in method
  id: AuthProviderId            // GOOGLE, APPLE
  isAvailableOnThisPlatform: Boolean   // Apple -> false on Android, true on iOS; Google -> true everywhere
  signIn(): Result<AuthSession>

AccountServiceImpl (core:network) — holds the Supabase Auth client + a List<AuthProvider>
  (GoogleAuthProvider always; AppleAuthProvider's iOS actual does the real
  AuthenticationServices/signInWithIdToken flow, its Android actual returns
  isAvailableOnThisPlatform = false and a signIn() that never gets called). SettingsViewModel asks
  AccountService for `availableProviders`, filters already-unavailable ones out of the UI itself —
  so a *third* provider later (neither Google nor Apple) is still just "implement AuthProvider,
  register it," no change to AccountService's public surface or to SettingsViewModel's shape.
```

`SettingsViewModel` depends only on `AccountService`, never on `AuthProvider` or supabase-kt types —
matches this repo's established "feature depends on `:core:data`'s interface, never the
`:core:network` implementation" rule (see `core/data/README.md`, `NewsViewModel`'s own dependency on
`NewsRepository`/`ContentSyncer` interfaces).

## 4. Account deletion: DB migration function, not an Edge Function

Chosen because it's simpler **and** needs no secret at all, not despite it:

- `backend/supabase/functions/` already exists in this repo but is empty — no Edge Function has ever
  been deployed here (the one earlier attempt, step 12A's fetch-news job, was reverted in step 12D
  back to client-side fetching). Introducing Edge Functions now means new deploy tooling
  (`supabase functions deploy`, Deno runtime, a function-local secret for the service-role key) for
  a single one-line delete.
- A `security definer` Postgres function, in a new migration, following the exact precedent already
  in this codebase (`commit_map_edits`,
  `backend/supabase/migrations/20261021120003_commit_map_edits_rpc.sql`)
  for "one more RPC migration, pgTAP-tested": `public.delete_own_account()` — `security definer`,
  `set search_path = ''`, no arguments (always acts on `auth.uid()`, so it can never delete anyone
  else's account even if called directly), `delete from auth.users where id = auth.uid()`.
  Supabase's own `auth.users` → `auth.identities`/`auth.sessions`/`auth.refresh_tokens` foreign keys
  are
  `ON DELETE CASCADE`, so this one delete removes the whole account, not just a profile row — no app
  content tables exist yet to also clean up (per this step's "no user data is synchronized" scope).
  The app calls it via `supabase.postgrest.rpc("delete_own_account")`, authenticated with the
  caller's own JWT (the function runs as its owner, not the caller, for the one statement that needs
  elevated rights to touch `auth.users`; the caller still has to be signed in to call it at all,
  normal Postgrest RLS/auth applies to the RPC call itself) — **the service-role key never appears
  in the app or in this function**, which an Edge Function approach would have needed to touch
  `auth.admin.deleteUser`.
- Add a pgTAP test file following `backend/supabase/tests/database/004_commit_map_edits.test.sql`'s
  shape: a signed-in user calling it deletes their own row and nothing else; an anonymous/different
  user cannot delete someone else's account by any path.
- **Command for you to run** (same Docker-less limitation every backend step in this roadmap has
  flagged — I can't execute this here): `cd backend && supabase db reset && supabase test db` to
  apply the new migration and run the pgTAP tests, or `supabase db push` against your real project
  once you've reviewed the migration file.

## 5. Common failure messages (no secret ever logged)

`AccountService`/`AuthProvider` map every known supabase-kt/Credential Manager exception to a
specific, secret-free UI string + matching log line (log the exception *type* and Supabase's own
error `code`/`message` fields only, never a header, token or key):

- Android `GetCredentialException` subtypes: `NoCredentialException` → "No Google account on this
  device"; a Credential Manager `DEVELOPER_ERROR`-shaped failure (wrong SHA-1/package name
  registered) → "Google sign-in isn't configured correctly for this app build" (points at this doc,
  not raw Google error text).
- Supabase `AuthRestException`/`AuthWeakPasswordException`-style errors: redirect URI mismatch,
  "Unsupported provider" (Google provider disabled), user not allowed (consent screen still in
  Testing mode and this Google account isn't an added test user) — each gets its own short sentence.
- A missing `GOOGLE_WEB_CLIENT_ID`/`GOOGLE_IOS_CLIENT_ID` in `local.properties` fails the build
  loudly at config-read time (same `require(...)` pattern as `SupabaseConfig.fromBuildConfig()`),
  never a silent blank sign-in button.
- Offline (no network) during sign-in/sign-out/delete → the existing `NetworkResult.Offline`-shaped
  "No connection, try again" message this app already uses elsewhere, not a generic error.

## 6. local.properties keys this step reads

`GOOGLE_WEB_CLIENT_ID` (used by both Android Credential Manager's server client ID and iOS's OAuth
provider config) and `GOOGLE_IOS_CLIENT_ID` (used only if/when the native `GoogleSignIn-iOS` path
from §1 is adopted later — not read by the web-OAuth iOS path this step actually ships, but
documented/read now so the extension point is real, not just promised). Read the same way
`core/network/build.gradle.kts` already reads `SUPABASE_URL`/`SUPABASE_ANON_KEY`: real environment
variable first, falling back to the git-ignored root `local.properties`; missing → build fails
loudly naming the missing key, never a silent empty client ID.

**`ACCOUNT_LOGIN_ENABLED`** (2026-11-08, added after the step was first marked done): a third key,
same read mechanism, but a plain `"true"`/anything-else toggle, not a secret — whether the Settings
screen's whole Account section (and, as a consequence, Danger zone, which can only ever show when
signed in) renders at all. Unset/blank means off, the exact behavior before this flag existed, so no
existing `local.properties` needs touching to keep working. This exists because the Account section
used to always render unconditionally, which (combined with `GOOGLE_WEB_CLIENT_ID` being genuinely
optional) meant the Google/Apple buttons were visible to everyone even before anyone had set up real
credentials to try them — set `ACCOUNT_LOGIN_ENABLED=true` to actually try the sign-in flows
locally. `AccountFeatureConfig.isLoginEnabled()` (`:core:network`) exposes it;
`FeatureFlags.accountLogin` (`:core:data`) is what `:feature:settings`'s `SettingsScreenContent`
actually checks — same "the screen checks it, not the `ViewModel`" shape `FeatureFlags.spotify`
already established, except this one is build-config-driven, not hardcoded, specifically so trying
it needs no code change.

## 7. iOS Xcode build: a real, specific finding (2026-11-06/07)

This environment has a working `xcodebuild` and real iOS Simulators installed — the first time any
step in this roadmap has had either. That made it possible to actually build `iosApp` end to end,
not just compile the Kotlin side, and it turned up one genuine, narrow, reproducible problem worth
your attention before you try building this yourself:

**Every `.kt` and `.swift` file this step touches compiles cleanly** — `./gradlew
:shared:compileKotlinIosSimulatorArm64` (and every module underneath it, including the
`AuthenticationServices`/Keychain cinterop in `core/network/.../auth/SecureSessionManager.ios.kt`
and `core/data/.../auth/AppleIdTokenRequester.ios.kt`) was re-run clean after every change in this
step, dozens of times. The one piece that does **not** currently work is `xcodebuild`'s own
"Compile Kotlin Framework" script phase: the `Shared.framework` Xcode links `iOSApp.swift` against
ends up missing this step's newest Kotlin symbols (`AuthDeepLinksKt.handleIosAuthDeepLink`,
specifically — a function that exists, compiles, and is otherwise correct), because the underlying
Gradle task (`:shared:embedAndSignAppleFrameworkForXcode`) reports `SKIPPED` even on a genuine, real
`xcodebuild` invocation with a fresh `DerivedData` and a real, deployment-target-matching Simulator
destination.

Traced this to a specific root cause, not a guess: that Gradle task's own `onlyIf` (decompiled from
the real Kotlin Gradle Plugin 2.4.20 jar via `javap`, the same empirical approach this repo already
uses for third-party APIs) is named, almost verbatim, `"Binary ... does not match Xcode-requested
build type or architecture"` — it compares the Kotlin-configured framework's build type/target
against what it parses out of Xcode's own environment variables (`CONFIGURATION`, `SDK_NAME`,
`ARCHS`, etc.) at build time, and skips if they don't line up. This machine's installed Xcode
reports `SDK_NAME=iphonesimulator27.2` — an unusually high/beta SDK version for a Kotlin Gradle
Plugin version (2.4.20) that likely predates it. The most probable explanation is a genuine version
mismatch between this sandbox's beta Xcode and the Kotlin Gradle Plugin's environment-parsing logic,
not a bug in this step's source code, and not something fixable by changing `.kt`/`.swift` files —
ruled out by elimination: fresh `DerivedData`, a real (non-generic) simulator destination matching
the deployment target, `ENABLE_PREVIEWS=NO`, and manually replicating Xcode's real env vars were all
tried and none changed the result.

**What this means for you**: build `iosApp` yourself in actual Xcode (or with a non-beta Xcode
version, if this sandbox's is in fact a beta) before concluding anything is wrong with this step's
iOS code — there's a good chance your own Xcode installation doesn't hit this at all. If it does,
upgrading the Kotlin Gradle Plugin past 2.4.20 (once a version recognizing this SDK string ships) or
building with a stable-channel Xcode are the two most likely fixes, in that order.
