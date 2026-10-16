# Theatrle requirements audit — 2026-10-15

This audit covers every request in the project conversation. Later requests supersede earlier conflicting behavior: hyphens are word gaps, and feedback priority is green, then same-word yellow, then other-word purple, then gray.

| Requirement | Implementation and verification |
| --- | --- |
| Android application written in Java | Native Android Java application, package `com.theatrle.app`; Java 17 pinned for Gradle. |
| Separate UI and application responsibilities | `MainActivity` coordinates; `GameEngine` scores; `MovieTitle` parses title structure; `FilmCsv` parses CSV strings; `FilmRepository` loads resources; `TitleLayout` wraps; `GuessBoardView` draws; `KeyboardView` handles buttons; `ResultView` displays results; `SharePattern` formats output; `GameStateStore` persists. |
| Exactly 300 popular films in a CSV | `films.csv` contains a single title column and 300 feature-film rows, ranked by IMDb vote count. Actual titles, spaces, punctuation, hyphens, and digits are retained. The ranking evidence is in `film-ranking.tsv`. |
| Spaces and hyphens are not guesses | Both delimit words, have no guess index, and produce a 24dp gap when the words share a row. A wrap separates words on different rows. |
| Free punctuation | Apostrophes, colons, and other punctuation are displayed automatically. Digits remain playable; the `123` keyboard supports titles such as `1917`. |
| Larger phone UI, safe camera and home-bar regions | 48dp letter tiles, 23dp tile labels, 50dp-high keys inside 56dp rows; Android native controls; all four system/cutout insets plus an 8dp margin. Narrow phone keyboards necessarily divide the available width among ten keys. |
| Long titles wrap | Whole words move to the next row. A single word wider than the display wraps its characters without reducing tile size. |
| Portrait and landscape | Portrait stacks board and keyboard; landscape puts them beside one another. Native scroll containers support short displays. The puzzle persists across recreation and rotation. |
| Only current and completed guesses | Board derives row count from submitted guesses plus the current attempt. No future blank attempts are drawn. |
| Scroll previous guesses | Native board scrolling; automatic scrolling keeps the active input row visible. |
| Six guesses | `GameEngine.MAX_GUESSES` is 6; incomplete submissions do not count; input is blocked after completion. |
| Green exact, yellow same word, purple other word | Three scoring passes reserve all greens, then all yellows, then purples, using remaining counts per word. Extra occurrences are gray. Counts reset for each submitted guess, as in Wordle. |
| Win/loss and Wordle-style result | Separate scrollable result screen shows outcome, answer, guess count, wrapped color pattern, copy-results button and explicit new-game button. |
| Launch from VS Code, launch correct app | Default build task (`Ctrl+Shift+B`) builds, starts/waits for the emulator, installs and explicitly launches `com.theatrle.app/.MainActivity`. Existing other apps are left installed. |
| Fresh manual-test APK in requested location | `assembleDebug` includes a task that copies the successfully packaged APK to `app/build/intermediates/apk/debug/app-debug.apk`. Standard output remains `app/build/outputs/apk/debug/app-debug.apk`. Failed builds do not install an old APK. |

## Catalogue definition and provenance

“Most popular of all time” has no single universal ranking. This project defines popularity as the number of IMDb user votes, descending, across non-adult feature films (`titleType=movie`). This is a popularity measure, not an average-rating or box-office ranking. The packaged snapshot was generated on October 15, 2026. The 300th film has 577,569 votes.

Source: [IMDb non-commercial datasets](https://developer.imdb.com/non-commercial-datasets/), specifically `title.ratings.tsv.gz` and `title.basics.tsv.gz` from [IMDb's dataset host](https://datasets.imdbws.com/). `tools/RefreshFilms.java` streams the official datasets, joins by IMDb ID, sorts by vote count (ID breaks ties), and generates both the title-only CSV and the ranking evidence. A 100,000-vote candidate threshold reduces memory; generation verifies the 300th result is strictly above that threshold. No TV series, episodes or short films are included.

IMDb's source data is provided for personal and non-commercial use; review its [licensing conditions](https://help.imdb.com/article/imdb/track-movies-tv/using-imdb-data/G8365SKK4YKW4HZ5) before publishing commercially. A newer snapshot can be generated with Java 17: `java tools/RefreshFilms.java`.

## Repeatable checks

- `powershell -NoProfile -ExecutionPolicy Bypass -File tools/Test-Rules.ps1`: exact feedback regressions, duplicate-letter budgets, 10,000 randomized scoring cases, six-guess loss, win/reset/input lock, saved-state restoration, quoted CSV strings, and 1,500 full-catalogue layouts at five display widths.
- `gradlew.bat lintDebug`: Android static analysis.
- `gradlew.bat assembleDebug`: compile and package, then synchronize the manual-testing APK.
- VS Code: **Terminal → Run Task → Theatrle: Run on Emulator**, **Ctrl+Shift+B**, or select **Theatrle: Run Android App** in Run and Debug and press **F5**. Set `theatrle.avdName` to another installed AVD if desired; the default is `Medium_Phone`. F5 runs the build/install/launch workflow; it does not attach a Java debugger.

For manual checks, rotate the phone during a partly entered title; check that typed letters and earlier guesses remain. Complete six incorrect guesses, check the failure answer/pattern, copy the results, then start a new game. Check a multiword and hyphenated title for visible word gaps and a long title for wrapping and scrolling.

## Completed validation

- Rules/layout suite: **289,952 assertions passed**, including 10,000 randomized scoring cases and 1,500 complete-catalogue layouts.
- Catalogue: 300 rows, 300 distinct IMDb film IDs, descending vote counts verified.
- Android lint: **0 errors**, with two advisory warnings (target SDK 36 rather than the newer SDK known to lint, and a pluralization suggestion for the English incomplete-guess message).
- Debug build, installation, and explicit launch on `Medium_Phone` (`emulator-5554`): successful.
- Inspected actual portrait and landscape screenshots; input persisted through rotation. Verified a six-guess loss, a one-guess `SPIDER-MAN` win, a 39-character Lord of the Rings puzzle after multiple guesses, and the 24dp physical gap in `A QUIET PLACE`.
- Temporary captures, UI hierarchies and one-off fixtures from the initial verification were discarded during project cleanup. The touchscreen-number test was completed after concurrent manual testing stopped: the `123` toggle and the actual number buttons solved `1917`. Copy-results and new-game buttons were exercised successfully. Automatic rotation was restored, and the emulator was left on a fresh puzzle with no submitted guesses or typed input. No Theatrle Java crash was reported during those checks.
- APK signature verification passed. Both APK locations contain identical 870,001-byte files, newer than every app source/resource file. SHA-256: `3080B98E1351375534950139E4CD3AE9C382CB29D523670FE66C0FE60F673AA1`.

## Project cleanup

The cleanup removed the unused, project-local SDK copy (the actual installed SDK remains at the path in `local.properties`), old one-off test artifacts, obsolete Gradle-version caches, an IDE device-list cache, and unused Java-upgrade tool-recording scripts. File contents, incoming references, and resolved paths were checked before deletion. No application class was removed: each is referenced by the app entry point, another component, or its resources.

Keep the wrapper scripts/JAR, Gradle configuration, local SDK configuration, VS Code tasks, application sources/resources, test sources, catalogue refresh tool and provenance, signing keys, and the phone demonstration APK. Current Gradle caches and IDE settings remain useful and were retained. Rule-check classes now go under `app/build/rule-checks`, so `gradlew.bat clean` removes them with other generated build outputs.

Post-cleanup verification passed: a clean rebuild, Android lint (zero errors), all 289,952 rule/layout assertions, the VS Code build/install/launch script, and emulator checks for six-guess loss, rotation with partial input, a numeric-title win, and result controls. The user's original saved emulator puzzle was backed up and restored with matching SHA-256 hashes. One-off verification files were removed afterward. Both debug APK locations and `Theatrle-demo.apk` still have the SHA-256 recorded above, confirming that cleanup did not change the packaged application.
