# Developer Guide

## Acknowledgements

{list here sources of all reused/adapted ideas, code, documentation, and third-party libraries -- include links to the original source as well}

## Design & implementation

### Statistics and Leaderboard Feature

The statistics module tracks player performance metrics per profile, ensuring that personal scores are preserved independently across sessions.

#### Architecture and Components
- **`Stats` Model**: Maintains counts for wins, losses, current win streaks, maximum win streaks, and a historical list of completion times.
- **`GameTimer`**: Records high-precision elapsed game duration using system timestamps (`System.currentTimeMillis()`), which is automatically triggered on game start and processed upon victory.
- **Persistence**: Profile-specific statistics are serialized and stored independently under each player's designated directory (e.g., `data/profiles/<UUID>/stats.txt`) via the `Storage` subsystem.

#### Command Summary & Syntax

| Command | Format | Description | Example |
| :--- | :--- | :--- | :--- |
| **Stats** | `stats` | Displays performance metrics for the currently active profile (win rate, streaks, recent times). | `stats` |
| **Stats Clear** | `stats clear` | Resets all recorded wins, losses, streaks, and times for the active profile. | `stats clear` |
| **Leaderboard** | `leaderboard` | Compiles and ranks all registered profiles by win count and completion times. | `leaderboard` |

### Command processing and profile sessions

**Maintainer:** Zack (zackyesten).
**Implementation baseline:** the foundation and runtime integration merged in [PR #34](https://github.com/AY2627S1-CS2113-W10-4/tp/pull/34).

This section explains how Jaweeper selects a local player, turns text input into commands, and gives commands access to the same current board and profile. It documents the implemented foundation, not the complete planned v2.0 feature set. In-game profile management commands, save/load, statistics and progress-loss confirmation are not provided by this foundation.

#### Architecture and responsibilities

The implementation separates input coordination, command construction, shared state and file access. These are logical responsibilities within the same Java package, not separate processes or independently deployed components.

| Responsibility | Main classes | Boundary |
|---|---|---|
| Application startup and execution | `Jaweeper`, `ProfileStartup` | Own the input loop and startup selection; delegate feature logic to commands. |
| Input interpretation | `Parser` | Validate syntax and coordinates, then construct a command without executing it. |
| Shared runtime state | `GameSession` | Hold the current `Board` and one shared `ProfileManager`. |
| Profile management | `ProfileManager`, `Profile` | Maintain stable player identities, the registry and the session-only active selection. |
| Text persistence | `Storage` | Read/write UTF-8 text; do not interpret saved games or calculate statistics. |

`Jaweeper.main` creates a `ProfileManager` using the working-directory-relative path `data`. Only after startup selection succeeds does it create the initial board and `GameSession`. The session-aware `run` method then delegates to the common execution loop. The older `run(Scanner, Board, ...)` overload remains available for existing tests and does not perform profile selection.

#### Profile component structure

![Class diagram showing the shared session, profile registry and storage dependencies](images/zack-profile-classes.svg)

*Figure 1. Selected members and relationships of the profile component. Solid directed associations denote retained references; dashed dependencies denote use. ProfileStartup and Storage expose static operations. Multiplicities show one manager and one current board per session, and zero or more profiles in the registry. This is a focused diagram, not the complete application class model.*

A `Profile` is immutable: its ID and name are final. Renaming replaces the profile object in the manager's map while preserving its UUID. The active selection stores the ID rather than an old object reference, so subsequent lookups obtain the renamed profile. Consumers should similarly obtain the current profile when needed instead of caching an old display-name object.

`GameSession` holds one replaceable board reference. Following `replaceBoard(validatedBoard)`, the next command receives the replacement through `getBoard()`. Replacement itself does not validate saved-game contents, handle unsaved progress or update timers. Those operations remain the responsibility of the calling feature.

#### Startup selection and profile persistence

At the startup prompt, `create NAME` creates and explicitly selects a profile; `select NUMBER` selects an entry from the displayed list. The displayed number is a temporary user-facing index, not the persistent identity. `exit` or end-of-input cancels startup before a board is created. Invalid choices are reported and the prompt repeats.

`ProfileManager.createProfile` alone does not select its result. Keeping these operations separate lets another caller create a profile without changing the current player. Startup deliberately performs both operations. Active selection is not written to disk, so the next launch prompts again.

The registry is a human-readable UTF-8 file, `data/profiles.txt`. Its first line is `profiles-v1`; each following line contains a canonical UUID, a tab, and a display name. Names are trimmed, must contain 1–30 permitted characters, and must start with a letter or digit. Unicode letters/digits, spaces, underscores and hyphens are allowed; tabs and line breaks are not. Name uniqueness ignores case.

A missing registry means first-time use. An existing empty file, invalid header, malformed record, duplicate ID or duplicate name is instead an error. Startup must not silently replace such a file with an empty registry. Permission and other read errors are also reported, rather than interpreted as "no profiles".

For a registry change, the manager builds a candidate map, serializes it, and calls `Storage.writeLines` before replacing its in-memory map. Storage creates parent directories and writes a temporary file alongside the target, then attempts an atomic replacement. A non-atomic replacement is used if atomic moves are unsupported. This reduces exposure to partially written files but is not a universal crash-recovery guarantee or a multi-process transaction system.

A player's feature data belongs under `data/profiles/<UUID>/`. Changing a display name does not change this path. The directory accessor does not create it; a subsequent storage write creates parents as needed. The foundation's delete operation rejects the active profile and removes only an inactive profile's registry record. Associated feature files are retained. A future deletion UI must explain that behaviour rather than claim all data was erased.

#### Command parsing and execution

For each input line, the session-aware loop reads the current board dimensions and passes them to `Parser.parse`. Command names are lowercased with `Locale.ROOT`; argument case is preserved.

Dispatch follows this order:

1. If a matching lowercase key exists in the feature registry, invoke its `Function<String[], Command>` factory with the argument tokens, excluding the command name.
2. Otherwise, handle built-in `help`, `exit`, `new` and `board` commands, rejecting extra arguments.
3. For `r`, `f` and `u`, validate two integer coordinates against separate row/column limits, convert from one-based input to zero-based indices, and invoke the registered coordinate factory.

Unknown commands and invalid arguments produce `IllegalArgumentException` messages. The loop displays the message and continues. Factories construct commands; they must not mutate gameplay or perform persistence as a parsing side effect.

![Sequence diagram for parsing and executing the existing flag command](images/zack-command-sequence.svg)

*Figure 2. Successful processing of `f 2 3`. The board has at least two rows and three columns. Scanner calls, logging, lambda adapters and the internals of flagging are omitted to keep the diagram focused. The call to parse represents the invocation through the loop's parsing function. Dashed arrows are returns; the command instance begins at its creation message.*

The parser converts the coordinates to `(1, 2)` before calling the flag factory. It returns the resulting command to the loop, which obtains the current board immediately before `execute(board)`. After successful execution, the loop checks `isExit()`. A false result continues the loop; a true result ends it. An `IllegalArgumentException` thrown during execution is also reported and the loop continues, but the loop does not roll back any mutations already made by that command.

Existing commands keep the `Command.execute(Board)` contract. A feature that also needs profiles can accept `GameSession` in its constructor. Registration in `Jaweeper.createFeatureCommands(session)` supplies that same session to its factory. The feature registry is intentionally empty in this baseline: the extension mechanism is implemented, but teammates' new commands have not been registered yet.

Feature registrations take precedence over built-ins. For example, registering `new` would take over both `new easy` and plain `new`, so its factory must define both cases. Whitespace tokenization currently does not implement quoting or semicolon-separated command chains.

#### Defensive checks and error boundaries

User input is checked with ordinary conditionals and exceptions, not assertions. For example, invalid coordinate text is rejected even when Java assertions are disabled. Assertions instead document internal invariants, including validated coordinate bounds and non-null results from command factories. Gradle's test and run tasks enable assertions; a direct JAR launch can use `java -ea -jar jaweeper.jar`.

Session/profile preconditions also have distinct meanings: `ProfileManager.getActiveProfile()` returns an `Optional`, while `GameSession.getActiveProfile()` throws `IllegalStateException` if startup selection was omitted. Session construction and board replacement reject null references.

The application logs session start/end at INFO and command dispatch/rejected input at FINE. It does not log complete user command lines for these dispatch messages. Startup registry read failures are reported without replacing the registry. Because `execute(Board)` does not declare checked I/O exceptions, future file-based commands must handle their own `IOException` and must not report success after a failed write.

EOF currently ends the game loop directly; it does not execute an exit command. Therefore, a future override of `exit` alone will not implement EOF progress-loss handling.

#### Design decisions and alternatives

| Decision | Alternative considered | Reason and trade-off |
|---|---|---|
| Stable UUID identity, separate from display name | Use the name as a filename/key | Renames do not break save/statistics references; IDs are less convenient for users, so startup displays numbered names. |
| Pass one GameSession to feature commands | Static global state or a new manager per command | Explicit sharing is easier to isolate in tests and avoids independent active selections; constructors need an extra dependency. |
| Preserve execute(Board) and add feature factories | Rewrite every command to accept a new context | Existing commands/tests remain compatible; profile-aware commands carry session state in addition to receiving the board. |
| Save a candidate registry before replacing the in-memory map | Mutate the map and then write | A failed write does not make memory claim that a registry update succeeded; replacement requires a temporary copy. |
| Generic text storage | One storage class owning profiles, boards and statistics | Each owner controls their format without expanding one shared class; each feature must supply its own validation and error policy. |

#### Verification and remaining work

The automated tests separate parser behaviour, startup interaction and file-system state:

- `CoreCommandsTest` and `GameplayRoutingTest` cover the existing command routes and zero-based coordinate conversion.
- `ParserAssertionsTest` checks enabled assertions, a null command-factory result, and invalid-input exceptions.
- `ProfileStorageTest` uses JUnit temporary directories to exercise persistence, duplicate names, malformed registries, retained feature files and failed-write behaviour.
- `ProfileRuntimeTest` covers startup creation/selection, cancellation, recovery from invalid input, shared session access, feature overrides, rectangular coordinate limits and board replacement.

Run `./gradlew clean build` (Windows: `.\gradlew.bat clean build`) before submitting changes. The rectangular routing test checks the parser/session adapter; it is not evidence that all Board algorithms already support rectangular games.

This foundation does not yet supply in-game profile create/select/rename/delete commands, deletion confirmations, unsaved-game protection, saved-game restoration, statistics updates or difficulty presets. The dimension-aware production path currently reads the rectangular grid's dimensions through GameSession; legacy parser overloads still use `Board.SIZE`. Later features must update the relevant integration points and their documentation when behaviour changes.

See [the integration contract](ProfileIntegration.md) for the factory interface and ownership boundaries. Other team members should add their own implementation sections alongside this one.

### Difficulty Levels and Dynamic Boards
* The `Board` class was refactored to support dynamic dimensions, replacing the hardcoded 9x9 grid constants.
* A new `Difficulty` enum manages the standard presets: EASY (9x9, 10 mines), MEDIUM (16x16, 40 mines), and HARD (16x30, 99 mines).
* `Parser.java` and `NewGameCommand.java` were updated to route difficulty arguments (e.g., `new hard`) to generate the correct board.
* The `copyStateFrom()` method allows the active `GameSession` to seamlessly adopt the new board dimensions without breaking the main game loop.

### Saving and loading games

**Maintainer:** Yong Xiang (yongxiang22).

This feature lets a player save an unfinished game to a readable text file and continue it later with the time already spent. Before quitting or loading, the player is warned if the current game has unsaved moves. Corrupted or hand-edited save files are rejected without touching the game in progress.

#### Overview

The feature adds five classes and makes small additions to four existing ones. Figure 3 shows how they fit together.

![Class diagram of the save/load component](images/SaveLoadClassDiagram.png)

*Figure 3. Classes of the save/load component and the existing classes they use. Only the members discussed below are shown.*

| Class | Responsibility |
|---|---|
| `SaveCommand`, `LoadCommand` | Parse their arguments when the command is created (no file access) and carry out the operation in `execute`. |
| `SaveStore` | Owns save-name rules and file locations, `data/profiles/<UUID>/saves/<name>.txt`. Provides `save`, `load`, `list`, `delete` and `exists`. Uses the shared `Storage` helper for the actual reading and writing. |
| `GameSaveCodec` | Converts a `Board` plus elapsed time to save-file lines and back, validating every line on the way in. Knows nothing about files or paths. |
| `SaveTracker` | Remembers the position at the last save or load and decides whether the current game has unsaved progress. |
| `SaveFileException` | A checked exception whose message is shown to the player as-is. |

`Jaweeper.createFeatureCommands` creates **one** `SaveTracker` and passes it to the `save`, `load` and `exit` factories, so all three agree on what was last saved. The `exit` registration overrides the built-in `exit`; the no-argument `ExitCommand()` constructor still quits immediately, so existing tests are unaffected.

Two small hooks were added to existing classes: `Board.restore(...)` builds a board from given cells without placing random mines, and `GameTimer.resumeFrom(ms)` / `getElapsedMillis()` let the timer continue from a saved duration.

#### Saving a game

Figure 4 shows what happens when the player types `save trip`.

![Sequence diagram for save trip](images/SaveSequenceDiagram.png)

*Figure 4. Successful `save trip`. Logging, the overwrite check and the failed-write branch are omitted.*

1. The parser passes `["trip"]` to `SaveCommand.parse`, which checks and lowercases the name. The name may contain only letters, digits, `_` and `-`, so it can never contain `/` or `..` and escape the saves folder. Windows-reserved names such as `con` and `nul` are also rejected.
2. `execute` refuses finished games, since there is nothing left to continue and the result is already recorded in the statistics.
3. `SaveStore` asks `GameSaveCodec.encode` for the file contents, then writes them with `Storage.writeLines`. `Storage` writes to a temporary file first, so a failed write leaves any previous save intact.
4. Only after the write succeeds does `SaveCommand` call `tracker.markSaved`. If the write throws an `IOException`, the command prints the error and does **not** mark the game as saved, so the exit warning still protects the player's progress.

Plain `save` (no name) re-saves to the name this game was last saved to or loaded from, which `SaveTracker.getCurrentSaveName` provides.

#### Save file format

```
jaweeper-save-v1
size 3 4
mines 2
elapsed-ms 52340
board
00.x
11f.
1F..
checksum 623d2492
```

| Symbol | Meaning |
|---|---|
| `.` | Hidden cell, no mine |
| `x` | Hidden mine |
| `f` | Flag on a safe cell |
| `F` | Flag on a mine |
| `0`–`8` | Revealed safe cell, showing its adjacent-mine count |

The board section looks like the game board itself, so a developer can read a save while debugging. The last line is a CRC32 checksum of every line above it, written as 8 lowercase hex digits.

#### Loading a game

Figure 5 shows `load trip`, including the two places where loading can stop.

![Sequence diagram for load trip](images/LoadSequenceDiagram.png)

*Figure 5. `load trip`. The outer `alt` is the file check; the inner `alt` is the unsaved-progress check. `Storage.readLines` is shown as a self-call to keep the diagram narrow.*

The key design point is the **order**: `LoadCommand` reads and fully decodes the file *before* asking for confirmation or replacing anything. Only after both checks pass does it call `replaceBoard`, `resumeFrom` and `markSaved`. A failed load therefore leaves the current game exactly as it was.

`GameSaveCodec.decode` never trusts the file. It rejects the file with a message naming the line, where possible, if:

1. The header `jaweeper-save-v1` is missing or different.
2. The checksum line is missing or does not match. This catches truncated files, disk damage and hand edits.
3. The `size`, `mines` or `elapsed-ms` line is malformed, including signs, non-digits and over-long numbers.
4. Rows or columns are outside 1–99, or the mine count is outside 1 to (cells − 1).
5. A board row has the wrong length or an unknown symbol, or there are too many or too few rows.
6. The number of mines in the grid differs from the `mines` line.
7. A revealed number does not match the recalculated count. `Board.restore` always recalculates adjacency itself, so the file's numbers are only checked, never used.
8. Every safe cell is already revealed, which means the game is finished.

The checksum is checked first so a hand-edited file gets a clear "changed outside Jaweeper" message. Checks 3–8 still matter: they catch files written by a buggy future version, and they stop bad data from reaching `Board`'s assertions.

Whether a game was saved before its first reveal is worked out from the grid (no revealed cells), so the safe-first-click rule still applies after loading such a game.

#### Detecting unsaved progress

Rather than adding a `markDirty()` call to every command that changes the board, `SaveTracker` compares the board with a snapshot taken at the last save or load. Figure 6 shows the decision made by `confirmDiscard`, which both `exit` and `load` call.

![Activity diagram for confirmDiscard](images/ConfirmDiscardActivityDiagram.png)

*Figure 6. How `SaveTracker.confirmDiscard` decides whether `exit` or `load` may go ahead.*

- `markSaved(board, name)` stores a reference to the board's grid array and the grid's encoded rows.
- A board has unsaved changes if the game is unfinished, at least one cell is revealed or flagged, and either the grid is a **different array** from the snapshot (`new` or another load replaced it) or its rows differ.
- The first progress-losing request remembers the action (`"exit"` or `"load trip"`) and the current position, prints a warning and returns `false`. Repeating the **same** action with no move in between returns `true`. Any move, or a different action, needs a fresh confirmation.

Comparing the grid array by identity also stops plain `save` from overwriting the previous game's file after `new`, because `NewGameCommand` installs a fresh grid array through `copyStateFrom`. The comparison is one pass over the grid per `exit`, `load` or `save`, which is negligible for boards up to 99×99.

#### Design decisions and alternatives

| Decision | Alternative considered | Reason and trade-off |
|---|---|---|
| Readable text with a CRC32 checksum | Binary or Java serialisation | Readable files help debugging and suit the course's text-file requirement. CRC32 detects edits and damage but is not tamper-proof against someone who recomputes it; that is acceptable for a single-player game. |
| Recalculate adjacency on load | Trust the numbers in the file | One source of truth; the stored digits still keep the file readable and act as an extra consistency check. |
| Validate everything before replacing the board | Load cell by cell into the live board | No half-loaded game on error, at the cost of building a second board in memory. |
| Snapshot comparison for unsaved changes | A dirty flag set by each command | No changes to teammates' commands, and new commands are covered automatically. |
| Type the same command twice to confirm | A separate `y/n` prompt | Works within the existing one-line-per-command loop with no new input mode; the trade-off is a less familiar confirmation style. |
| Lowercase, restricted save names | Free-form names | Prevents path traversal (`../`), Windows-reserved names, and names that differ only in case colliding on Windows and macOS. |

#### Testing and known limitations

- `GameSaveCodecTest` covers round trips (including a random 16×30 board), checksum tampering and truncation. It also tests each content check above using files with a *valid* checksum, so only the content check can catch the problem.
- `SaveLoadCommandsTest` uses JUnit temporary folders to test the store, name validation, saving and loading, re-saving, refusing finished games, keeping the current game after a bad load, both confirmation flows, and a full run through `Jaweeper.run`.

**Limitations and planned work:**

- Pressing Ctrl-D (end of input) ends the loop without the unsaved-progress warning, because it bypasses commands. A future change could auto-save to a reserved slot on end of input.
- Runtime profile switching, once added, must call `SaveTracker.confirmDiscard` before changing profile, since saves belong to one profile.
- ZW's planned `saves list` and `saves delete` commands will call `SaveStore.list()` and `SaveStore.delete(name)` rather than building paths themselves.

## Product scope
### Target user profile

{Describe the target user profile}

### Value proposition

{Describe the value proposition: what problem does it solve?}

## User Stories

|Version| As a ... | I want to ... | So that I can ...|
|--------|----------|---------------|------------------|
|v1.0|new user|see usage instructions|refer to them when I forget how to use the application|
|v2.0|user|find a to-do item by name|locate a to-do without having to go through the entire list|

## Non-Functional Requirements

{Give non-functional requirements}

## Glossary

* *glossary item* - Definition

## Instructions for manual testing

{Give instructions on how to do a manual product testing e.g., how to load sample data to be used for testing}
