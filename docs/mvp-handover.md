# LyricTutor: MVP handover

Read this first. It is the plan and the state of the app, written when the repo was set up, for the session that builds the MVP.
Steps 0 to 3 of [section 10](#10-delivery-plan) are built and merged (state on 2026-10-30); section 3 describes the skeleton the app started from. Everything marked **verified** was checked
(by running something or reading CI); everything marked **unverified** is a claim from a search or from memory, and is to be tested
before it is relied on.

- [1. The app](#1-the-app)
- [2. Decisions already made](#2-decisions-already-made)
- [3. What exists now](#3-what-exists-now)
- [4. Rules of this repo](#4-rules-of-this-repo)
- [5. How to build on the skeleton](#5-how-to-build-on-the-skeleton)
- [6. Getting the song: Spotify link to metadata](#6-getting-the-song-spotify-link-to-metadata)
- [7. Getting the lyrics: LRCLIB](#7-getting-the-lyrics-lrclib)
- [8. The learning content: Gemini](#8-the-learning-content-gemini)
- [9. Pages, data and structure](#9-pages-data-and-structure)
- [10. Delivery plan](#10-delivery-plan)
- [11. Risks and open questions](#11-risks-and-open-questions)
- [12. Sources](#12-sources)

## 1. The app

The user gives the app a Spotify song, as a link. The app:

1. reads the song's metadata (title, artists, album art, duration) from the link;
2. finds the lyrics;
3. shows them line by line, each with an English translation;
4. lets the user tap any word to see its meaning in context, its dictionary form (lemma) and how it was conjugated (tense, person,
   mood), and its etymology with English cognates, to help remember it.

The purpose is learning a language from real songs: vocabulary, conjugation and grammar in use. It is a **personal app**, so the
copyright of lyrics is not a concern the user wants handled (lyrics are fetched on demand for the owner's own use and never
bundled or redistributed).

Example: a Spanish song gives the Spanish lyrics; tapping a conjugated verb shows the infinitive and how it was formed.

## 2. Decisions already made

| # | Decision | Answer |
|---|---|---|
| 1 | Spotify account access (liked songs, playlists) | **Not in the MVP.** Input is a Spotify link. Account access is a later, separate feature behind a `TrackSource` interface |
| 2 | Lyrics source | **LRCLIB** (free, no key, line-timed lyrics), with a paste-lyrics fallback |
| 3 | AI provider | **Gemini** first (free tier, personal Google account). The code is provider-neutral (`LyricsAnnotator`), but **no other provider is planned**: a Claude implementation was considered and dropped |
| 4 | Languages | **Generic**: no language-specific logic. English is the target language |
| 5 | Wiktionary grounding of conjugation and etymology | **Later** (after the MVP flow works) |
| 6 | The shared library's debug pages | **Included** (already in the skeleton) |
| 7 | LRCLIB coverage test | **Wanted before the lyrics PR**: needs 20 to 30 Spotify links from the user (section 11) |
| 8 | Platform | **Kotlin Multiplatform, Android target only for now**: code in `commonMain` must stay free of anything Android-only |
| 9 | Provider key | The user enters their own Gemini key in Settings. Never embed a key in the APK |

## 3. What exists now

The repo was created on 2026-10-29 by the **New App** workflow of `LukeNeedham/ci-workflows` (`docs/create-app.md` there), which
pushed the skeleton and added the app to that repo's `apps.json`. The package is `com.lukeneedham.lyrictutor`.

**Verified** (CI, PR #2): the skeleton **compiles and assembles**, the previews render, and CI's icon, README and screenshot jobs run.
The first build failed on an adaptive icon in the wrong folder (fixed in #2 and in the template). The settings screenshot
(`docs/screenshots/01-SettingsPage.png`) shows the title "Settings", so Compose resource strings do render in CI screenshots.
It shows no Debug button because the library's debug mode is off in previews; the button shows in a debug build.

**Not verified**: the app has never been run on a device; whether the unit test passes was not read from CI; the state of the `main`
build after #2 should be looked at in the Actions tab (it should publish the first `main-build` release and the first previews).

What is in the repo, in `app/src`:

| Source set | Contents |
|---|---|
| `commonMain` | `DebugKitSetup.kt` (`initDebugKit(isDebug)`); `di/AppModule.kt` (Koin); `ui/`: `Root`, `AppNavigation` (back stack saved with a `SavedStateConfiguration` that registers every route), `Router` (exhaustive, no `else`), `Route` (`Settings`, `Debug`, `Logs`, `Crashes`), `SettingsPage`, `SettingsPageContent` (the title and the library's `DebugButton`), `SettingsViewModel`, `SettingsUiState`; `ui/theme/`: `AppTheme` (own theme, no Material), `AppThemeValues`, `ToDebugTheme`; `composeResources/values/strings.xml` |
| `androidMain` | `App` (`initDebugKit(BuildConfig.DEBUG)` first, then Koin), `MainActivity` (only `setContent { Root() }`), the manifest (`INTERNET`), resources (launcher icon: adaptive in `mipmap-anydpi-v26`, plain layer-list in `mipmap-anydpi`; the app name; the theme) |
| `androidDebug` | The `screenshot` package: `SettingsPageScreenshot` and `order.txt` |
| `commonTest` | `SettingsViewModelTest` (`kotlin.test`) |

Build files: `gradle/libs.versions.toml` (AGP 8.13.2, Kotlin 2.2.21, Compose Multiplatform 1.10.3, shared library `common` 0.8.0, KSP
`2.2.21-2.0.4`, Room 2.8.4, DataStore 1.1.1, Media3 1.5.1, Koin 4.2.2, Ktor 3.3.1, Coil 3.3.0, `kotlinx-datetime` 0.7.1) and
`app/build.gradle.kts`. The dependencies of the whole architecture are **already added**, so a new feature needs no new dependency
for these: Compose, Navigation 3 (the JetBrains artifacts), lifecycle, Koin, Coil 3, `kotlinx.serialization`, Ktor, coroutines,
`kotlinx-datetime`, and in `androidMain` Room (KSP), DataStore, Media3, OkHttp engines, activity-compose.

**Still placeholders, to replace in the first PR** (see section 10):

- `readme.yml` has `TODO:` text for the tagline and the description. CI renders `README.md` from it: never edit `README.md`.
- The launcher icon is a white diamond on indigo (`ic_launcher_foreground.xml`, `ic_launcher_background`). CI renders
  `app-icon.png` from it.
- The settings page has only a title and the Debug button.

## 4. Rules of this repo

The app follows the conventions of `LukeNeedham/ci-workflows` (`docs/new-app.md`, `app-shell.md`, `pages.md`,
`app-conventions.md`, `using-the-library.md`, `screenshots.md`, `readme.md`; the skill `.claude/skills/lukeneedham-conventions` carries
them here). The ones that matter most when adding features:

- **Claude can never build or run the app.** The sandbox has no Gradle or Android SDK. Write carefully, push, read the PR build
  comment and the log, and fix what it says. Only pure logic with no Android dependency is worth running locally.
- **Work on a branch and open a PR to `main`** (CI builds PRs into `main` only). As soon as the PR is open, paste
  `📦 [Open in AppHub](https://www.lukeneedham.com/apphub/pr/LyricTutor/<number>)` into the chat on a line of its own, and put the same
  line at the top of the PR description.
- **Pages**: each page is `XPage` (public, gets `XViewModel` through Koin, collects `XUiState`, only calls the content) and an `internal`,
  stateless `XPageContent` (state down, events up) with a `private fun Preview()` in its file. `XPage`, `XPageContent`, `XViewModel` and
  `XUiState` each in its own file. The route is `Route.X`, a member of the one `sealed interface Route : NavKey`. A new route needs a branch
  in `Router` (no `else`) **and an entry in the `SavedStateConfiguration` of `AppNavigation`**.
- **Screenshots**: a file per page in the `screenshot` package (`src/androidDebug`), each screenshot a `private @Preview` named
  `XPageScreenshot` that owns its mock data, listed in `order.txt`; nothing shared. CI renders them into `docs/screenshots/`.
- **Own theme, no Material**: read `AppTheme.current`; set font size and line height inline; no `MaterialTheme`. The app has no Material
  dependency, so build what is needed from `foundation` (for example the word sheet below).
- **All user-visible text in `strings.xml`**: in `commonMain` as Compose resources (`composeResources/values/strings.xml`,
  `stringResource(Res.string.x)`, with `Res` in `com.lukeneedham.lyrictutor.generated.resources`). The app name for the launcher is in
  `androidMain/res/values/strings.xml`. Lyrics, translations and annotations are content, not UI strings.
- **Kotlin Multiplatform**: nothing Android-only in `commonMain` (no `android.*`, `BuildConfig`, `java.*`, `R`, Room, DataStore, Media3).
  Put Android implementations of an interface in `androidMain` and bind them in an Android Koin module started in `App`. Tests go in
  `commonTest` with `kotlin.test` and names without spaces. Never add `iosX64`; see `docs/app-shell.md` of ci-workflows to add a target.
- **MVVM**, Koin constructor injection (classes never call Koin), coroutines and Flow, one-off events through a `Channel` (never in
  `UiState`), touch targets at least 48dp, the library's `Logger` instead of `Log`/`println` (log the failures you catch).
- **Previews must render** (CI fails the PR otherwise) and be deterministic (fixed data, no time or randomness).
- The stop hook's warnings about "unpushed" or "Unverified" commits by `github-actions[bot]` are CI's own commits: run `git fetch origin`
  and do nothing else. Never amend, rebase or force-push over them.

## 5. How to build on the skeleton

Concrete shapes that fit the rules, for the features below:

- **Interfaces in `commonMain`, implementations where they belong.** `TrackResolver`, `LyricsRepository`, `LyricsAnnotator`,
  `SettingsStore`, `SongCache`. Network code (Ktor, HTML parsing with plain string and regex work, JSON with `kotlinx.serialization`)
  can be fully common: `HttpClient()` without an engine picks OkHttp on Android. Storage (Room for songs and annotations, DataStore for
  settings) is Android-only for now: interface in `commonMain`, implementation and Koin module in `androidMain`.
- **The shared text from Spotify's Share sheet** arrives in `MainActivity` (an `ACTION_SEND`, `text/plain` intent filter in the
  manifest). `MainActivity` may hand `Root` what only the activity can provide, as a parameter (`docs/app-shell.md`): pass the shared text
  down and let `Root` give it to the navigation, like AppHub does with its deep link.
- **A sheet for the word breakdown** is part of `SongPageContent`, not a page. Without Material, draw it with `foundation` (a scrim and
  a bottom-aligned surface in a `Box`, with `AnimatedVisibility`), using `AppTheme.current`.
- **Images** (the cover art): Coil 3 `AsyncImage` with a `contentDescription` and a size. In screenshots, wrap with Coil's
  `LocalAsyncImagePreviewHandler` so a render never needs the network (`docs/screenshots.md`).
- **Fixtures for tests**: put sample track-page HTML (section 6) and sample LRCLIB and Gemini JSON in `commonTest` and parse them there.

## 6. Getting the song: Spotify link to metadata

Spotify's Web API cannot be the source for the MVP: the lyrics are not in it, and since February 2026 a development-mode app needs a
Premium account, is limited to five users and has a reduced endpoint set. So the app reads the **public track page**.

**Verified** for one song (the user ran `curl -A "Mozilla/5.0" <track url>` and kept the `og:` and `music:` tags): the page
`https://open.spotify.com/track/<id>` carries these tags, without logging in.

| Need | Tag | Example ("Si Me Voy (with The Marías)", track `6GiCszfL4D2GlCU8tFU3sR`) |
|---|---|---|
| Title | `og:title` | `Si Me Voy (with The Marías)` |
| Artists | `music:musician_description` | `Cuco, The Marías` (comma separated, first is the main artist) |
| Cover art | `og:image` (640 by 640) | `https://i.scdn.co/image/ab67616d0000b27314a012da5638f895eaefffa5` |
| Duration, seconds | `music:duration` | `203` |
| Release date | `music:release_date` | `2023-03-10` |
| Track number | `music:album:track` | `1` |
| Album | `music:album` | a URL only (`https://open.spotify.com/album/5TZm9qi0223t20ypmJevEq`), **not the name** |
| 30 s preview | `og:audio` | an mp3 link (may disappear: do not depend on it) |
| Fallback | `og:description` | `Cuco, The Marías · Si Me Voy (with The Marías) · Song · 2023` |

The page also has dozens of `og:restrictions:country:allowed` tags: ignore them. Use this song as the first parser fixture. It is also
a good test song for the annotation: it mixes Spanish and English.

Rules for the resolver (`TrackResolver`, behind an interface):

- Parse the track ID from `open.spotify.com/track/<id>` (with or without `?si=…`); a `spotify.link` short link needs one redirect
  followed (**unverified**). Fetch the page with a browser-like `User-Agent`.
- Clean the title for the lyrics lookup: strip a trailing `(with …)`, `(feat. …)`, `- Remastered …`, and similar; keep the original
  for display.
- The first artist of `music:musician_description` is the one to search with; retry with the whole string. A name with a comma
  ("Tyler, The Creator") splits wrongly, which the retry covers.
- The album name is only needed for display; LRCLIB's search works without it. A second request to the album page gets it, if wanted.
- If a field is missing, fall back to `og:description`; if the artist is still missing, ask the user to type it.
- This is scraping of a public page for personal use: it can change or be blocked without notice. Keep it behind the interface, and
  keep the manual fallback.
- The oEmbed endpoint (`open.spotify.com/oembed`) was not needed; whether it returns an artist is **unverified**.

Later feature, researched but deferred: Spotify account access (OAuth with PKCE, scopes `user-library-read`,
`playlist-read-private`, `playlist-read-collaborative`). Reported but **unverified**: `/playlists/{id}/tracks` now returns 403 for development-mode
apps (`/playlists/{id}/items` replaces it), and whether `/me/tracks` still works there is unknown: test it first.

## 7. Getting the lyrics: LRCLIB

Spotify has no public lyrics endpoint (its lyrics come from Musixmatch; unofficial access needs a session cookie and breaks the
terms), Genius gives no lyrics through its API, and Musixmatch's free tier returns part of each lyric. **LRCLIB** is the source:

- `GET https://lrclib.net/api/get` with `track_name`, `artist_name`, `album_name` and `duration` (seconds; matched within about two
  seconds), and a search endpoint (`/api/search`). Lyrics come as plain text and as line-timed LRC. Crowd-sourced, about three million
  entries (**unofficial figure**), no key.
- It rate-limits and does not allow parallel requests (numbers unpublished): **one request at a time**, and cache everything.
- **Unverified** and to be tested against the live API before the lyrics PR: whether `album_name` is required on `/api/get`
  (client libraries disagree), the exact response fields, and the coverage of Latin music (the coverage test, section 11).
- Lookup order: `/api/get` with the cleaned title, the first artist and the duration; then `/api/search`, taking the result whose
  duration is closest; then retry with the whole artist string; then the paste-lyrics box (the user pastes the lyrics, and the app keeps
  them with the song).
- A second free provider (lyrics.ovh and similar: plain text only, reliability unknown) goes behind the same `LyricsRepository`
  interface only if the coverage test shows LRCLIB misses too much.
- Cache the lyrics with the song, and keep the line timing for a later "highlight the current line" feature.

## 8. The learning content: Gemini

For each line the app needs the translation and, for each word: its language, lemma, part of speech, morphology (tense, mood,
person, number, gender), its meaning in context, a note for slang, idioms and elisions ("pa'" is "para"), and its etymology with
English cognates.

**Approach: an LLM for the annotation, Wiktionary later for the facts.** An LLM copes with slang, elisions, code-switching and
context-dependent meanings in lyrics, which classical NLP does not. Its etymologies and conjugations can be wrong, so until Wiktionary
grounding exists, **the UI marks them as AI-generated**.

- **Gemini API with a personal Google account**: the key is made in Google AI Studio (it belongs to a Cloud project AI Studio creates;
  no billing account is needed to start). Call the REST API with Ktor and ask for structured JSON (a response schema).
- **Unverified**: the free-tier limits (per model and per project: requests per minute, tokens per minute, requests per day) are
  reported differently by different sources and change often: read them in the AI Studio dashboard. Some sources say the free tier is
  unavailable in some regions including the EU: check for the user's region. Free-tier content may be used by Google to improve its
  products (lyrics are public text, so this matters little).
- **Batch the lines** of a song into few requests, and **cache** by song and by lemma, so repeated words and songs cost nothing.
- **The key is the user's**, entered in the Settings page (add a field there), stored in DataStore in `androidMain` (private to the app,
  not encrypted: say so on the page). Do not embed a key in the APK.
- **Provider-neutral**: `LyricsAnnotator` is an interface in `commonMain`; Gemini is the only implementation, and no other is planned.

Output shape for one batch (sketch):

```json
{
  "lines": [
    {
      "index": 0,
      "translation": "...",
      "words": [
        {
          "surface": "voy",
          "language": "es",
          "lemma": "ir",
          "partOfSpeech": "verb",
          "morphology": "present indicative, 1st person singular",
          "meaning": "I go",
          "note": null,
          "etymology": "...",
          "cognates": ["..."]
        }
      ]
    }
  ]
}
```

Prompt rules: detect the language of each word and never assume one source language (songs mix languages); never invent an
etymology (say "unknown"); keep each word's text short enough that a song fits the limits. Parse with `kotlinx.serialization`
leniently (optional fields may be missing); a batch that fails to parse is retried once, then its lines show as not annotated with a retry.

Later, **Wiktionary** for conjugation tables and etymologies (for example the wiktextract data at kaikki.org; CC BY-SA, so it needs
attribution; its bulk download location was not confirmed), with the LLM only writing the memory hook that links the real etymology to
English cognates.

### 8b. What the AI does now, and what Wiktionary does (step 4)

To keep the AI cost low, the AI is asked for the translation of each line and almost nothing else. Everything about a single word comes from
Wiktionary when the user taps it (free, and only for the words tapped).

| Information | Source |
|---|---|
| Translation of the line | AI (one answer per line) |
| Language of the line, and the words of another language | AI (a short tag per line) |
| Splitting a line into words | the app |
| Dictionary form, part of speech, how the word is formed ("first-person singular present indicative of ir") | Wiktionary: the REST `page/definition/<word>` endpoint; an inflected word's entry holds a "form of" definition with a link to its dictionary form |
| Meanings | Wiktionary: the first meanings of the word, or of its dictionary form (one more request) |
| Slang and elisions ("pa'" is an apocopic form of "para") | Wiktionary, when the word has an entry |
| Where it comes from | a link to the Wiktionary page, at the language section |
| English cognates | dropped |

Not covered: the meaning in this particular line (the AI no longer says), and phrases or idioms of several words. A "meaning in this line" button
that asks the AI about one word on demand is possible later.

Verified on 2026-10-30 against real answers of the endpoint for `voy`, `gewoon` and `pa'` (pasted by the user): the answer is an object by
language code of entries with `partOfSpeech`, `language` and `definitions[].definition` (HTML); a form of another word has a
`form-of-definition-link` span whose first link's `title` is the dictionary form. Other templates (other languages) are unverified.

## 9. Pages, data and structure

| Page | Route | Job |
|---|---|---|
| `HomePage` | `Route.Home` | Paste a Spotify link (and receive one from the Share sheet), the recent songs, a way to Settings |
| `SongPage` | `Route.Song(trackId)` | Cover art, title and artists, the lyrics by line, the translation of each line, tap a word for its sheet |
| `SettingsPage` | `Route.Settings` (exists) | The Gemini key and the AI provider, the Debug button (exists), the data attributions |

`Route.Home` becomes the start destination of `AppNavigation` (it is `Route.Settings` now). The library's `Debug`, `Logs` and `Crashes`
routes stay.

Room (Android, in `androidMain` for now), sketch:

- `Song`: track id, title, artists, cover URL, duration, last opened.
- `LyricsLine`: song id, index, text, start time (nullable).
- `LineAnnotation`: song id, line index, translation, the annotated words (serialized JSON), provider, model, created at.
- later `WordEntry`: lemma, language, part of speech, etymology, source (Wiktionary or AI).

## 10. Delivery plan

Each step is a PR to `main`, small enough to fix from the CI log alone. Step 0 can ride along with step 1.

| Step | Contents |
|---|---|
| **0. Housekeeping** (done) | Replace the `TODO:` text of `readme.yml`. Draw a real launcher icon. Look at the `main` build in the Actions tab |
| **1. Track resolver and Home** (done) | `TrackResolver` with parser tests on the fixture of section 6; `HomePage` (a link box, recents); the Share intent; the Song route as a stub; `Route.Home` as the start; a screenshot of Home |
| **2. Lyrics and the Song page** (done) | The LRCLIB client and cache, the paste fallback, title cleaning; `SongPage` with the cover art, the title and the lyrics by line; screenshot |
| **3. Annotation with Gemini** (done) | The key field in Settings (stored encrypted), `LyricsAnnotator` with Gemini, batching, JSON parsing, the cache; the translation of each line; the word sheet with lemma, morphology, meaning and cognates (marked AI-generated), and a link to Wiktionary for where the word comes from, never an AI answer |
| **3b. Making it dependable** | "Translate again" for a song, the tapped word kept in view above the sheet, a model picker in Settings, the token cost of a song in the logs |
| **4. Grounding** (done) | Wiktionary for the dictionary form, the form and the meanings of a tapped word ([section 8b](#8b-what-the-ai-does-now-and-what-wiktionary-does-step-4)); the AI only translates and names the language of each line. A Claude implementation of `LyricsAnnotator` is **dropped**: not needed |
| **5. Polish** | Saved vocabulary, timed highlighting while the song plays, more languages' special cases (languages written without spaces), the Spotify account source |

## 11. Risks and open questions

- **LRCLIB coverage of the user's music is unknown.** Before step 2, test it: take 20 to 30 Spotify links the user listens to
  (including lesser-known songs), read each link as in section 6, query LRCLIB, and report found or missing, with and without line
  timing. **Needs the links from the user.** The sandbox could not reach `open.spotify.com` (it was outside the allowed network
  hosts), so this runs on the user's machine, or those hosts must be allowed in the session's network settings.
- **Spotify's page can change or block the request**: hence the interface, the fallback fields and the manual artist.
- **LLM etymology and conjugation errors**: marked AI-generated until Wiktionary grounding; "unknown" is allowed.
- **Free-tier limits and region of the Gemini API**: unverified; batch and cache.
- **Languages without spaces** (Japanese, Chinese): out of the MVP.
- **A private repo** has no preview images in PR comments and is not listed in AppHub; this repo was made public.
- **Questions for the user before step 3**: which languages first (generic code, but the prompt is tested on one or two); whether the
  Gemini key may live unencrypted in DataStore (the plan says yes, with a note on the page).

## 12. Sources

- Spotify and its lyrics: [rspotify issue on a lyrics endpoint](https://github.com/ramsayleung/rspotify/issues/550),
  [community answer](https://community.spotify.com/t5/Spotify-for-Developers/Update-endpoint-for-lyrics/m-p/5355493),
  [developer access update, Feb 2026](https://developer.spotify.com/blog/2026-02-06-update-on-developer-access-and-platform-security),
  [quota modes](https://developer.spotify.com/documentation/web-api/concepts/quota-modes),
  [oEmbed reference](https://developer.spotify.com/documentation/embeds/reference/oembed).
- LRCLIB: [client docs](https://lrclibapi.readthedocs.io/en/stable/lrclib.html),
  [rate limiting note](https://flathub.org/en/apps/io.github.weiteck.Lyricade).
- Other lyrics providers: [lyricsgenius, how it works](https://lyricsgenius.readthedocs.io/en/latest/how_it_works.html),
  [unofficial Musixmatch client](https://github.com/Strvm/MusicXMatchAPI).
- Gemini: [rate limits](https://ai.google.dev/gemini-api/docs/rate-limits), [billing](https://ai.google.dev/gemini-api/docs/billing).
- Wiktionary data: [kaikki.org example entry](https://kaikki.org/dictionary/Spanish/meaning/f/fo/formato.html).
- This app's setup: `docs/create-app.md` and `docs/new-app.md` in [LukeNeedham/ci-workflows](https://github.com/LukeNeedham/ci-workflows).
