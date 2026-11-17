# Requirements notes — Settings (§6.6 of the Functional Analysis)

The Functional Analysis (`documentation/ilovevi-design-system 2/IloveVI-Functional-Analysis.pdf`,
v0.4 draft) is a binary PDF and can't be edited in place. This file is the plain-text record of the
changes step 13B's prompt made to its §6.6 "Settings" requirements table (SET-01..06), per that
prompt's own section 6 instruction — read this alongside the PDF, not as a replacement for it.

| ID     | Requirement                                                                                                                                                                                                                                        | Priority | Status vs. v0.4 PDF                                                                                                                                                                                                                                                                                                |
|--------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| SET-01 | Spotify card: connect and disconnect the account, with the connection status. (*) for disconnect and status.                                                                                                                                       | Must     | **Unchanged in wording, but not implemented this step.** `SpotifyConnection` (`core/data/.../spotify/`) and `FeatureFlags.spotify = false` exist as the extension point step 13A will implement for real; the card itself only renders when the flag is on. See `docs/ROADMAP.md`'s "13A - feature:spotify" entry. |
| SET-02 | Switch "Show labels": names next to map pins.                                                                                                                                                                                                      | Could    | Unchanged. Implemented — `SettingsRepository.showLabels`, already read by `:feature:map`'s `MapViewModel` (wired since before this step).                                                                                                                                                                          |
| SET-03 | Show the date of the last content update, with a manual refresh. (*) for refresh.                                                                                                                                                                  | Should   | Unchanged. Implemented via `ContentSyncer.syncState`.                                                                                                                                                                                                                                                              |
| SET-04 | ~~Theme row: the app has a single dark theme, so the row is informational.~~ **Revised: "Theme: System, Light, Dark"** — a real, interactive 3-way choice (System/Light/Dark), not an informational row; System follows the device's live setting. | Could    | **Revised this step**, per the step's own instruction. Implemented — `IloveViThemeMode` (`core:design`), stored in `SettingsRepository.themeMode`, applied at `App()`'s root with no restart.                                                                                                                      |
| SET-05 | Language selection: Italian and English. (*)                                                                                                                                                                                                       | Should   | Unchanged. Implemented, applied immediately.                                                                                                                                                                                                                                                                       |
| SET-06 | Always show the disclaimer: unofficial app, not affiliated with Rockstar Games or Take-Two Interactive.                                                                                                                                            | Must     | Unchanged. Implemented, always visible in the About section.                                                                                                                                                                                                                                                       |
| SET-07 | **New this step:** "Optional Google account: sign in and out, the app works without it."                                                                                                                                                           | Must     | Implemented — `AccountService`/`AuthProviderId.GOOGLE`, Android native (Credential Manager) + web-OAuth elsewhere. The app never requires an account for anything.                                                                                                                                                 |
| SET-08 | **New this step:** "Delete account from the app."                                                                                                                                                                                                  | Must     | Implemented — Danger zone section (signed-in only), confirmation dialog, `backend/supabase/migrations/*_delete_own_account_rpc.sql`.                                                                                                                                                                               |

## Beyond the prompt's own SET-07/SET-08 wording

Two things the step's prompt asked for that go beyond what SET-07/SET-08's literal wording says,
recorded here rather than silently folded into those two IDs:

- **Apple sign-in on iOS** (not in the original SET-07 wording, added by an explicit mid-step
  decision — see `docs/auth-notes.md` §2 for why: Apple App Store Guideline 4.8 means a Google-only
  sign-in option on iOS is a real rejection risk). `AuthProviderId.APPLE`, iOS-only
  (`availableProviders`
  filters by platform).
- **Licenses and credits, app version** (About section) — not a numbered SET requirement in the v0.4
  PDF at all, but explicitly asked for in this step's prompt text alongside SET-06's disclaimer.

## Open decision this step did *not* resolve

`docs/ROADMAP.md`'s existing "Open decisions" section already tracks **Spotify access and limits**
(step 13A) as unresolved — this step doesn't change that; it only adds the flag/interface extension
point SET-01's eventual real implementation will need.

## Music / Compilation — intentionally dropped for v1 (decided 2026-11-17)

The Functional Analysis's Music requirements (MUS-01..06, mostly Must priority) and Compilation
requirements (CMP-01..08, mostly Must priority) are **not implemented for the first release**. This
was raised as a BLOCKER by the 2026-11-17 pre-submission audit (`docs/RELEASE-AUDIT.md`, RA-07/
FUNC-1) because a Must-priority feature set was missing without the requirements doc saying so. This
entry is the formal status update that closes that gap: the decision is to ship v1 without Music/
Compilation, not to build it before submission.

| ID | Requirement | Priority | Status vs. v0.4 PDF |
|----|-------------|----------|----------------------|
| MUS-01..05 | Song list, search, station filter, add song, compilation card | Must | **Intentionally dropped for v1.** No `feature:music` module exists (`docs/ROADMAP.md` step 10). |
| MUS-06 | Per-song "Open in Spotify" | Could | Dropped for v1, same reason. |
| CMP-01..07 | Compilation screen, reorder, Spotify playlist export | Must | **Intentionally dropped for v1.** |
| CMP-08 | Rename compilation | Could | Dropped for v1, same reason. |

The Music tab is already hidden from the bottom nav (`a01031f`, "Hides for first release the music
tab") and `FeatureFlags.spotify = false`, so nothing half-built is reachable in the shipped app —
this entry just makes the *requirements-tracking* status match what the code already does. Revisit
MUS-*/CMP-* for a post-v1 release; `docs/ROADMAP.md`'s "Spotify access and limits" open decision
still gates it either way.
