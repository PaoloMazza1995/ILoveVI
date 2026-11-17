# ILoveVI — Store Listing Draft (v1.0)

> **DRAFT.** Pending real support/marketing/privacy URLs (see `docs/RELEASE-AUDIT.md`'s manual actions
> checklist) and a product decision on the app name/subtitle tradeoff below. The disclaimer sentence
> appears verbatim in both the description and the App Review notes, per App Store metadata policy.
> Apple's App Store Review Guidelines were last read for this draft on 2026-11-17.

---

## App Name (≤30 chars)

- **EN:** `ILoveVI – Companion & Map` (25 chars)
- **IT:** `ILoveVI – Guida e Mappa` (23 chars)

No "GTA", "Grand Theft Auto", or "Rockstar" anywhere in the name — avoids Guideline 2.3.7 ("don't pack
metadata with trademarked terms") and avoids the name itself implying official affiliation.

## Subtitle (≤30 chars) — two options, pick one

**Option A — safest (recommended):**
- EN: `Unofficial Game Companion` (26 chars)
- IT: `App non ufficiale per gamer` (27 chars)

**Option B — descriptive, mentions "GTA VI" (higher discoverability, nonzero trademark risk — a
judgement call, not a default choice):**
- EN: `Unofficial GTA VI Companion` (28 chars)
- IT: `Compagno non ufficiale GTA VI` (30 chars)

*Recommendation: ship Option A for the initial submission to minimize Guideline 2.3/5.2 risk. Option B is
a defensible fallback if Option A underperforms on discoverability and you're willing to accept the
IP-risk tradeoff documented in `docs/RELEASE-AUDIT.md` (RA-02/ASR-08).*

## Promotional Text (≤170 chars)

- **EN:** `Your pocket atlas for the GTA universe fan community: browse weapons, vehicles and items, explore an interactive map, and track the latest news.` (147 chars)
- **IT:** `Il tuo atlante tascabile per la community di fan GTA: sfoglia armi, veicoli e oggetti, esplora una mappa interattiva e segui le ultime notizie.` (146 chars)

## Full Description

**EN:**
```
ILoveVI is an unofficial, fan-made companion app for fans of the GTA universe. It is not affiliated with,
endorsed by, or sponsored by Rockstar Games or Take-Two Interactive.

FEATURES
• Catalog — browse weapons, vehicles and items with detailed attributes, images, and locations
• Interactive Map — explore pins for every catalog entry, with filters and clustering for busy areas
• News — a curated feed of community and media coverage, kept up to date
• Settings — choose System/Light/Dark theme, English or Italian, and an optional account

ACCOUNT IS OPTIONAL
Every feature above works fully without signing in. If you choose to sign in (with Google or, on iOS,
Sign in with Apple), you can manage and delete your account at any time from Settings.

This is an unofficial app, not affiliated with Rockstar Games or Take-Two Interactive. All trademarks
belong to their respective owners.
```

**IT:**
```
ILoveVI è un'app companion non ufficiale, creata da fan, per gli appassionati dell'universo GTA. Non è
affiliata, approvata o sponsorizzata da Rockstar Games o Take-Two Interactive.

FUNZIONALITÀ
• Catalogo — sfoglia armi, veicoli e oggetti con attributi dettagliati, immagini e posizioni
• Mappa interattiva — esplora i pin di ogni voce del catalogo, con filtri e raggruppamento nelle zone più
  dense
• Notizie — una rassegna curata di notizie e copertura mediatica, sempre aggiornata
• Impostazioni — scegli il tema Sistema/Chiaro/Scuro, Italiano o Inglese, e un account opzionale

L'ACCOUNT È OPZIONALE
Tutte le funzionalità sopra elencate funzionano completamente senza accesso. Se scegli di accedere (con
Google o, su iOS, con Sign in with Apple), puoi gestire ed eliminare il tuo account in qualsiasi momento
dalle Impostazioni.

Questa è un'app non ufficiale, non affiliata a Rockstar Games o Take-Two Interactive. Tutti i marchi
appartengono ai rispettivi proprietari.
```

> **Note from the audit:** this description currently advertises account sign-in as a real, reachable
> feature. Before submitting, confirm `docs/RELEASE-AUDIT.md`'s RA-08 is resolved one way or the other —
> either the feature is genuinely switched on in the release build, or this paragraph (and the App Review
> notes below) need to be cut.

## Keywords (≤100 chars, comma-separated, no trademarks)

- **EN:** `companion app,interactive map,weapons guide,vehicles guide,open world,item tracker,wiki,atlas` (96 chars)
- **IT:** `app compagna,mappa interattiva,guida armi,guida veicoli,open world,tracker oggetti,wiki,atlante` (98 chars)

## URLs (placeholders — fill in once real values exist)

- Support URL: `<SUPPORT_URL>`
- Marketing URL: `<MARKETING_URL>`
- Privacy Policy URL: `<PRIVACY_POLICY_URL>`
- Account-deletion request page (Google Play Data Safety field): `<ACCOUNT_DELETION_URL>`

## What's New (v1.0 — initial release)

- **EN:** `Welcome to ILoveVI 1.0! Browse the catalog, explore the interactive map, and follow the news — with an optional account to sync your preferences. This is an unofficial fan companion app.`
- **IT:** `Benvenuto in ILoveVI 1.0! Sfoglia il catalogo, esplora la mappa interattiva e segui le notizie — con un account opzionale per sincronizzare le preferenze. Questa è un'app companion non ufficiale creata da fan.`

## App Review Notes

```
This is an unofficial, fan-made companion app for the GTA universe. It is not affiliated with
Rockstar Games or Take-Two Interactive.

LOGIN IS OPTIONAL. No demo account is needed: every reviewable feature is reachable without
signing in. Reviewer path, no account required:
1. Launch the app — the Catalog tab is the default screen. Tap any weapon/vehicle/item to see its
   detail page, including its location on the mini-map.
2. Switch to the Map tab — browse pin clusters, apply filters (category/zone), and tap a pin to
   jump to its catalog detail.
3. Switch to the News tab — browse the curated news feed; tapping an article opens it in the
   system browser (this is expected, intentional behavior, not a broken link).
4. Switch to the Settings tab — toggle theme (System/Light/Dark), language (English/Italian), and
   "Show labels" on map pins — all without an account.
5. Optional: the Settings > Account section offers "Continue with Google" (all platforms) and
   "Sign in with Apple" (iOS only, added specifically to satisfy Guideline 4.8 alongside Google).
   Signing in only unlocks account management (sign out, delete account) — no catalog, map, or
   news content is gated behind it.

All catalog/map/news content is served from our own backend (Supabase); no third-party game data
or assets are embedded in the binary.

This is an unofficial app, not affiliated with Rockstar Games or Take-Two Interactive.
```

> **Note from the audit:** step 5 above assumes RA-08 is resolved and sign-in genuinely ships. If you
> decide to ship v1 fully anonymous instead, delete step 5 and the account bullet from the description.

## Age Rating Self-Assessment (reasoning to carry into App Store Connect's questionnaire)

- Cartoon/Fantasy Violence: None
- Realistic Violence: None — the app displays text/image descriptions of a separately-published game's
  content; it renders no gameplay or violent imagery itself
- Sexual Content/Nudity: None
- Profanity/Crude Humor: None in app-authored content; the News feed aggregates external articles, which
  could occasionally include mild language — answer "Infrequent/Mild" honestly if asked about
  aggregated/linked content
- Alcohol, Tobacco, or Drug Use/References: None/Infrequent — only insofar as cataloged items might
  reference such themes abstractly (re-check once real GTA VI content loads)
- Mature/Suggestive Themes: Infrequent/Mild — the app is explicitly about a mature-rated game by name,
  even though it shows no mature content directly
- Horror/Fear Themes: None
- Gambling (simulated): None
- Contests: None
- Unrestricted Web Access: Likely "Yes" — News articles open in the system browser to external,
  uncontrolled sites
- User-Generated Content / User Communication: None — no forum/chat/posting feature ships in the mobile
  app
- Medical/Treatment Information: None

## App Privacy — Form Skeleton

```
Data Types to declare:
  Contact Info
    - Name — collected only if user signs in (Google/Apple) — Linked to user — Purpose: App functionality
    - Email Address — collected only if user signs in — Linked to user — Purpose: App functionality
  Identifiers
    - User ID (Supabase account ID) — collected only if signed in — Linked to user — Purpose: App
      functionality
  (No) Location data collected
  (No) User Content collected (no UGC feature ships)
  (No) Usage Data / Analytics collected — no analytics SDK is currently active (re-check this section if
       that changes before submission)

Tracking: "No, we do not track users" (no ad-attribution/cross-app tracking SDK present)
Data used to track you: None
Data linked to you: Name, Email, User ID (only if account created)
Data not linked to you: None currently identified
```

## Export Compliance

**Answer: No — standard encryption exemption applies.**

Reasoning: the app uses only standard HTTPS/TLS for network calls to Supabase and Apple's native
`AuthenticationServices` for Sign in with Apple. It implements no proprietary/non-standard cryptographic
algorithms and uses encryption for no purpose beyond securing network transport and credentials via
platform-standard APIs — qualifies for Apple's standard exemption (self-classification report, no
additional export documentation needed under the mass-market/standard-crypto exemption).

---

*See `docs/RELEASE-AUDIT.md` for the full pre-submission audit this draft was prepared alongside — in
particular, do not finalize this listing until RA-01 (map art), RA-02 (app icon), RA-03 (placeholder
seed content), and RA-08 (account feature on/off) are resolved, since all four directly affect what this
listing can truthfully say.*
