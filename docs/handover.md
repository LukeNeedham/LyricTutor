# LyricTutor: handover (2026-10-30)

Read this first. It replaces `docs/mvp-handover.md` as the entry point: that document is the original plan (still right about the
rules of the repo and the data sources, and now partly out of date). This one is the state of the app after the first build session,
what was learned, and the work that remains.

- [1. What the app is, and its state](#1-what-the-app-is-and-its-state)
- [2. How it works](#2-how-it-works)
- [3. Decisions made](#3-decisions-made)
- [4. Verified and unverified](#4-verified-and-unverified)
- [5. Rules and lessons for working here](#5-rules-and-lessons-for-working-here)
- [6. Remaining work](#6-remaining-work)
- [7. Open questions for the user](#7-open-questions-for-the-user)

## 1. What the app is, and its state

The user gives the app a Spotify song link (typed, or shared from Spotify's Share sheet). The app reads the song's details, finds its
lyrics, shows them line by line with an English translation, and lets the user tap any word to see what Wiktionary says about it. It is a
personal app, built for learning from songs. **First languages: Spanish and Dutch**, with no language-specific code (generic).

Everything of the MVP plan is built, merged and used on a device by the user (PRs #4 to #10): the track resolver, Home with recents,
lyrics from LRCLIB with a paste fallback, the Song page, Gemini translations, tappable words with a Wiktionary word sheet, saved
translations, a "Play on Spotify" button, "Translate again", a model picker, and token logging. `main` builds and its previews render.

Not built: saved vocabulary, highlighting the line that is playing, `spotify.link` short links, the Spotify account source. See
[6](#6-remaining-work).

## 2. How it works

Kotlin Multiplatform, Android target only: shared code in `commonMain`, Android-only code (Room, DataStore, Keystore, `App`,
`MainActivity`) in `androidMain`. Koin: `appModule` (`commonMain/di`) and `androidModule` (`androidMain/di`, the storage).

### Pages
`Home` (link box, recents, Settings), `Song(trackId)` (cover, title, artists, Play on Spotify, lyrics, translations, word sheet),
`Settings` (Gemini key and model, model picker, credits, Debug button). Each is `XPage` + internal `XPageContent` + `XViewModel` +
`XUiState`, with `Route.X`. The word sheet is `WordSheet` (container) + `WordSheetContent`, part of the Song page.

### The flow of a song (`SongViewModel`)
1. **Track**: the cache (`SongCache`), else `SpotifyTrackResolver` fetches `open.spotify.com/track/<id>` and `SpotifyPageParser` reads the
   `og:` and `music:` meta tags. `TitleCleaner` makes the search title (no "(with …)", "(feat. …)", remaster suffixes).
2. **Lyrics**: `LyricsRepository`: the cache, else `LrclibLyricsSource` (exact lookup, then search by closest duration within 8 s, each
   with the first artist and then the whole artist string; one request at a time; 3 attempts for 5xx/429/network failures). Timed (LRC)
   lyrics are preferred over plain. A miss is not cached. The user can paste lyrics, kept with the song.
3. **Translations**: `AnnotationRepository`: repeated lines (same after trim, case, spaces) are sent once and the result copied to all.
   One request for the whole song (up to 80 distinct lines), so the model has the whole song as context. A failed request, a missing line
   or an incomplete answer is asked for once more. Saved per line index in Room.
   The AI gives **only**, per line: `translation`, `language` (English name, e.g. "Spanish") and `otherLanguages` (words in another
   language). `LineAnnotation.isComplete` needs a translation and a language: older saved annotations without a language are asked again.
4. **Tapping a word**: every word of a line is tappable. `loadWiktionary`: the English Wiktionary REST endpoint
   `page/definition/<word>` (user-agent set), candidates = the word as written, then lower case; the language is the line's (or the word's
   own in `otherLanguages`); `WiktionaryProtocol.senses` reads the entries (see below); a form of another word costs one more request for
   the meanings of its dictionary form. Results are kept per word and language while the page is open (not on disk).
5. **Which part of speech**: if the page has several, `PartOfSpeechChooser` (Gemini, enum output `text/x.enum`, options = the page's own
   labels) says which one the word is used as in its line; only that sense is shown. With no key, a failure or no match, every sense is shown.
6. The sheet has a fixed height (half the screen), the tapped word is highlighted and the song scrolls so it stays above the sheet. The
   credit at the bottom ("From Wiktionary, under the CC BY-SA license. Open the page.") is the link to the page at the language section
   (a Wiktionary search when there is no page).

### How a Wiktionary entry is read (`WiktionaryProtocol`, real data in its test)
Answer: object by language code (`es`, `nl`, `other`) of entries `{partOfSpeech, language, definitions[].definition (HTML)}`.
A definition is a **meaning**, a **form of** another word (it has a `form-of-definition-link` span; the first link's `title` is the
dictionary form), or a **detail** line of a form (class `form-of-definition` without the link span; the nested list of an
"inflection of x:" is repeated as such lines, so the nested copy is cut off). Rules: an entry with meanings shows them in the order of the
page; an entry that is only forms shows a headline ("first-person singular present indicative of ir", "inflection of jouen"), the form
lines, then the meanings of the dictionary form; **"misspelling of …" definitions are hidden unless nothing else is left**.

### Storage and settings
Room (`AppDatabase`, version 2, `MIGRATION_1_2` added the annotation table): `song`, `lyrics` (serialized `Lyrics`), `annotation`
(serialized `LineAnnotation` per line). Settings in DataStore; **the Gemini key is encrypted** (AES-GCM, Android Keystore, `KeystoreCipher`);
if it cannot be decrypted it reads as empty and the user enters it again. Release builds are minified: `proguard-rules.pro` keeps the
serialization and Room rules (**never tried in a release build**).

### Gemini requests (`GeminiProtocol`, `GeminiAnnotator`, `GeminiPartOfSpeechChooser`, `GeminiModelCatalog`)
REST `generateContent` with the user's key (`x-goog-api-key`). JSON response schema for translations; enum for the part of speech; safety
settings `BLOCK_NONE` (lyrics can be crude). **No temperature** (Gemini 3 looped on low values). Thinking: translations `low`, the part
of speech question `minimal`, only for models starting with `gemini-3`. Answer caps: 16,384 tokens (translations), 512 (the question).
Timeouts: the HTTP client has 30 s request, 15 s connect and 30 s socket by default; translations ask for 120 s. Each answer logs its time
and tokens (`in/out/thinking`) plus the running total of the song. Errors (HTTP status and the start of the body) are logged and shown on
the page.

## 3. Decisions made

| Decision | Answer |
|---|---|
| Input | A Spotify link. No Spotify account access yet (`TrackSource` interface later) |
| Lyrics | LRCLIB, with a paste fallback. Personal app: lyrics are fetched on demand, never bundled or put in tests, previews or docs (use made-up placeholder lines) |
| AI | Gemini only, the user's own key, encrypted. **A Claude provider was considered and dropped** (a Claude subscription does not cover the API anyway) |
| What the AI does | Translate each line and name its language. **Everything about a single word comes from Wiktionary** (free), to keep tokens low |
| Cognates, AI etymology | Dropped. The credit link to the Wiktionary page replaces "where it comes from" |
| Meaning of a word | The word's Wiktionary senses, filtered to the part of speech it is used as. Not "meaning in this line" (possible later, on demand) |
| Languages | Generic code; tested with Spanish and Dutch |
| Hiding | Misspellings are hidden unless alone. Other labels ("nonstandard", "obsolete", "alternative form of") are **not** hidden |
| Default model | `gemini-3.8-flash` (the user's choice; the user mostly runs `gemini-3.1-flash-lite`). Changed in Settings, with a picker listing what the key can use |

**Money (the user's account):** the free tier of the model had **20 requests a day** (429 "PerDay"), which ran out during testing. The user
then enabled billing; it is **prepaid credit** (a 402 "prepayment credits are depleted" with a balance of 0). The key must belong to the
project that has billing. Costs seen: the part-of-speech question is about 50 tokens in, 1 out, 0 thinking, 0.75 s. **The tokens of a
whole-song translation after the slimming were never measured**: read the "so far" line in Debug → Logs for a song and record it here.

## 4. Verified and unverified

**Checked on the user's device:** Spotify page reading, LRCLIB lookup (a 503 from LRCLIB happens now and then; retried), translations,
tappable words and the word sheet for Spanish and Dutch words, the Wiktionary reading for `voy`, `gewoon`, `pa'` and `jou` (from real
answers the user pasted, now unit-test fixtures), Play on Spotify (it plays), Translate again, the model picker, token logging, the
part-of-speech question with minimal thinking, the fixed sheet height, and scrolling.

**Not verified:**
- The **unit tests were never seen to run in CI**: the build log showed `./gradlew assembleDebug`. Find out whether CI runs `commonTest`
  (it is in the managed `ci-workflows`); if not, they only compile. Do not claim they pass until seen.
- Wiktionary pages of other languages and unusual templates (a "form of" worded differently may show only meanings or nothing). Ask the
  user for the REST answer of any word that looks wrong, and add it as a test fixture.
- The Share sheet path and `spotify.link` short links (the latter is **not supported**: only `open.spotify.com/track/…`, `spotify:track:` and bare ids).
- A **release (minified) build**: Room, serialization, Ktor.
- The upgrade from Room version 1 to 2 on a device with old data (it ran for the user, but it was not examined).
- LRCLIB coverage of the user's music (needs their song links).

## 5. Rules and lessons for working here

- **Claude cannot build or run the app.** CI is the build. The sandbox's network policy blocks `lrclib.net`, `en.wiktionary.org`,
  `open.spotify.com` and Google's API hosts, so **real data has to be pasted by the user** (that is how the Wiktionary parser got its
  fixtures). Write carefully, push, read the PR build comment and, on failure, the job log (`get_job_logs`: the compiler errors are
  mid-log, not in the tail).
- **PR flow:** a branch (`claude/mvp-implementation-plan-hq7myt`, restart it from `main` with `git checkout -B … origin/main` after each
  merge), a PR to `main`, then **paste `📦 [Open in AppHub](https://www.lukeneedham.com/apphub/pr/LyricTutor/<number>)` on a line of its
  own** and the same at the top of the PR description. **Read the number from the result of creating the PR**: once it was guessed
  wrong. Subscribe to the PR; never poll, never schedule check-ins (the user's rule).
- **Pages:** `XPage` + internal `XPageContent` + `XViewModel` + `XUiState`, each in its own file, `Route.X` in the one `Route`, a branch
  in `Router` and an entry in the `SavedStateConfiguration` of `AppNavigation`. Own `AppTheme`, no Material, 48 dp touch targets, all
  text in `strings.xml`, nothing Android-only in `commonMain`, the library's `Logger` (`com.lukeneedham.common.logger.Logger`), tests in
  `commonTest` with names without spaces.
- **Screenshots** are private `@Preview`s in the `screenshot` package (`androidDebug`), one file per page named after it, listed in
  `order.txt`; CI renders them. `README.md` is generated from `readme.yml`: never edit it.
- **Never put real lyrics anywhere** (tests, previews, docs). Use invented placeholder lines. Song titles and single words are fine.
- **Lessons:** (1) after rewriting a part of a file with a script, **compare its function list with `main`'s**: two functions were deleted
  by accident and broke the build. (2) A change to what the AI returns needs old saved results to count as incomplete (`isComplete`) so
  they are asked again; a change to Room needs a version bump and a migration. (3) Gemini 3: a temperature below its default made it
  loop; its thinking tokens count against `maxOutputTokens`; ask for the least thinking the task needs. (4) The OkHttp engine's default
  socket timeout is 10 s: set `socketTimeoutMillis` for slow calls. (5) A failure must be **logged with its HTTP status and body** and
  shown with its reason, or it is invisible (an unlogged 404 for a retired model cost an hour). (6) Large Compose edits: count braces.
- The Stop hook's warnings about "unpushed" or "Unverified" commits by `github-actions[bot]` are CI's own commits: `git fetch origin`
  and do nothing else.

## 6. Remaining work

In the order I would do it. Each is its own PR.

1. **Saved vocabulary** (the next feature; the design was agreed in outline). A Save button on the word sheet (once Wiktionary's answer
   is in; "Saved" when saved, tapping removes). An entry keeps the page title, language, the senses as shown (so it works offline), the
   line it came from and the song; **one entry per word and language, listing the songs**. A Room table (version 3 + migration), a
   `Route.Vocabulary` + `VocabularyPage` (list, newest first, filter by language, delete, tap to reopen its sheet), a link from Home, a
   screenshot, tests (saving, removing, order). **No flashcards in the first version** (a "hide the meaning until tapped" mode can follow).
2. **Find out whether CI runs the unit tests**; if it does not, run them (there is no Gradle here: ask the user, or change the CI
   configuration in `ci-workflows`, which is managed there, not here).
3. **`spotify.link` short links**: follow one redirect (Ktor follows redirects; read the final URL for the track id), then resolve as usual.
4. **A release build check**: ask the user to run a minified build and look at the logs/crashes pages; fix the keep rules.
5. **Cost**: record the tokens of a whole song; try `minimal` thinking for the translations on one or two songs (quality matters more
   there); consider a "meaning in this line" button that asks the AI about **one word on demand** (cheap, only when tapped).
6. **Model retired or wrong**: a 404 from `generateContent` for the model name should say "model not found" and point to the picker.
7. **Wiktionary**: keep results on disk (offline words), hide other low-value labels if the user asks, handle entries with no section for
   the language better, and add fixtures per language as the user finds odd words. Spanish verbs ("come", "vino") and Dutch separable
   verbs are good tests.
8. **Lyrics coverage test**: the user gives 20 to 30 song links; read each as the app does, ask LRCLIB, report found or missing and
   timed or plain. If coverage is poor, add a second `LyricsSource` (plain text only) behind `LyricsRepository`.
9. **Highlight the playing line**: needs Spotify's **App Remote SDK**, a registered client id, and the Spotify app installed (a link
   cannot tell the playback position). The synced lyrics already carry `startMs`. Investigate before promising it.
10. **Later:** the Spotify account source behind `TrackSource` (liked songs, playlists; development-mode limits apply), languages written
    without spaces (the word split is by spaces), and UI polish (the 48 dp rows make annotated lines airy; the sheet is half the screen).

## 7. Open questions for the user

- Saved vocabulary: confirm one entry per word and language, with the list of songs; flashcards later?
- Should "nonstandard", "obsolete" or "alternative form of" definitions be hidden like misspellings?
- Which model to keep as the default (`gemini-3.8-flash`, or the lite model they use)?
- Do they want the playing-line highlight enough to take on the Spotify SDK?
- Their song links for the LRCLIB coverage test.
