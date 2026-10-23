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

For each command on an input line (a line may chain several commands, see [Gameplay quality-of-life features](#gameplay-quality-of-life-features)), the session-aware loop reads the current board dimensions and passes them to `Parser.parse`. Command names are lowercased with `Locale.ROOT`; argument case is preserved.

Dispatch follows this order:

1. If a matching lowercase key exists in the feature registry, invoke its `Function<String[], Command>` factory with the argument tokens, excluding the command name.
2. Otherwise, handle built-in `help`, `exit`, `new` and `board` commands, rejecting extra arguments. (`clear` is also a built-in, but its case is currently commented out; see [The clear command](#the-clear-command-temporarily-disabled).)
3. For `r`, `f` and `u`, validate two integer coordinates against separate row/column limits, convert from one-based input to zero-based indices, and invoke the registered coordinate factory.

Unknown commands and invalid arguments produce `IllegalArgumentException` messages. The loop displays the message and continues. Factories construct commands; they must not mutate gameplay or perform persistence as a parsing side effect.

![Sequence diagram for parsing and executing the existing flag command](images/zack-command-sequence.svg)

*Figure 2. Successful processing of `f 2 3`. The board has at least two rows and three columns. Scanner calls, logging, lambda adapters and the internals of flagging are omitted to keep the diagram focused. The call to parse represents the invocation through the loop's parsing function. Dashed arrows are returns; the command instance begins at its creation message.*

The parser converts the coordinates to `(1, 2)` before calling the flag factory. It returns the resulting command to the loop, which obtains the current board immediately before `execute(board)`. After successful execution, the loop checks `isExit()`. A false result continues the loop; a true result ends it. An `IllegalArgumentException` thrown during execution is also reported and the loop continues, but the loop does not roll back any mutations already made by that command.

Existing commands keep the `Command.execute(Board)` contract. A feature that also needs profiles can accept `GameSession` in its constructor. Registration in `Jaweeper.createFeatureCommands(session)` supplies that same session to its factory. The feature registry is intentionally empty in this baseline: the extension mechanism is implemented, but teammates' new commands have not been registered yet.

Feature registrations take precedence over built-ins. For example, registering `new` would take over both `new easy` and plain `new`, so its factory must define both cases. Whitespace tokenization does not implement quoting. Semicolon-separated chains are split by the execution loop before parsing, so `Parser` itself still handles exactly one command per call.

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
- The `saves` and `delete NAME` commands call `SaveStore.list()` and `SaveStore.delete(name)` rather than building paths themselves; see [Gameplay quality-of-life features](#gameplay-quality-of-life-features).

### Gameplay quality-of-life features

**Maintainer:** Zhe Wen (zzzwun).

This section covers five additions that make the game nicer to play: showing every mine after a loss, a per-profile safeclick setting, chaining several commands on one line, listing and deleting saved games, and an updated `help`. It also documents `clear`, which is implemented but currently disabled.

#### Command summary & syntax

| Command | Format | Description | Example |
| :--- | :--- | :--- | :--- |
| **Settings** | `settings` | Shows the active profile's settings. | `settings` |
| **Safeclick** | `settings safeclick on\|off` | Turns the guaranteed-safe first reveal on or off for the active profile. Saved across launches. | `settings safeclick off` |
| **Saves** | `saves` | Lists the active profile's saved games alphabetically. | `saves` |
| **Delete** | `delete NAME` | Deletes a saved game. Must be typed twice to confirm. | `delete trip` |
| **Chaining** | `COMMAND; COMMAND; ...` | Runs several commands from one input line. | `f 1 1; f 2 3` |
| **Clear** *(disabled)* | `clear` | Clears the terminal. | `clear` |

#### Showing all mines after a loss

`RevealCommand.revealMine` sets the game state to `LOST` and then calls `board.printBoard()`. While the state is `LOST`, `Board.iconFor(cell)` chooses what each cell looks like, instead of always using `Cell.getIcon()`:

| Cell | Shown as |
|---|---|
| Mine, not flagged | `*` |
| Flag on a safe cell | `X` |
| Flag on a mine | `F` (unchanged) |
| Anything else | Whatever `Cell.getIcon()` returns |

This is display only: no `isRevealed` flag is changed. That keeps `calculateCompletionPercentage`, the statistics and any later snapshot of the grid unaffected by the loss screen. The `board` command shows the same view for as long as the game stays lost, and `new` resets the state, so mines are hidden again in the next game.

#### Safeclick setting

| Class | Responsibility |
|---|---|
| `SettingsCommand` | Parses `settings` and `settings safeclick on\|off` when created (no file access) and displays or changes the setting in `execute`. |
| `ProfileSettings` | Reads and writes `data/profiles/<UUID>/settings.txt`. Static methods only; knows nothing about commands. |
| `GameSession` | `isSafeClickEnabled()` and `setSafeClickEnabled(boolean)`. Caches the value for the active profile so a reveal does not read a file. |
| `Board`, `RevealCommand` | On the first reveal, `RevealCommand` calls `ensureSafeFirstClick` if safeclick is on, or the new `skipSafeFirstClick` if it is off. |

The file is two lines: the header `jaweeper-settings-v1`, then `safeclick on` or `safeclick off`. It is kept apart from `profiles.txt` on purpose, because the registry is strictly validated and a change to its format would affect every profile.

How the value is read and written:

- A missing, unreadable or malformed file means the default (on). The file is not overwritten until the player changes a setting, so a hand-edited file is not silently destroyed.
- `setSafeClickEnabled` writes the file first and only then updates the cached value, the same policy as the profile registry. If the write fails, `SettingsCommand` reports the error and nothing changes.
- The cache is keyed by the active profile's ID, so selecting another profile in the same session reads that profile's own value.
- With no session (legacy test usage) or no active profile, safeclick is on, which matches the behaviour before this feature.

`skipSafeFirstClick` clears the board's first-move flag without moving any mines. Without it, turning safeclick off would leave the flag set, and a later reveal could still trigger the mine relocation. The setting is checked when the first reveal happens, so a game that has not been revealed yet follows the current value, while a game already in progress is unaffected.

#### Command chaining

`Jaweeper.runLoop` now hands each input line to `runLine`, which uses `splitChain` to split it on `;` and then runs each segment in turn:

1. `splitChain` trims each segment and drops empty ones, so `f 1 1;; f 2 3;` works. A line with no commands at all is passed through unchanged so the parser still reports "Enter a command".
2. Each segment is parsed **just before it runs**, not all up front. This matters because the board can change partway through a line: in `new hard; f 15 29`, the coordinate `15 29` is only valid on the 16×30 board that `new hard` creates. Parsing lazily lets `Parser` validate against the board as it is at that moment.
3. If parsing or executing a segment throws `IllegalArgumentException`, the message is printed and the **next segment still runs**.
4. If a segment is an accepted `exit`, the loop ends and later segments are not run.
5. If the game ends during a segment (a win or a loss) and more segments remain, they are skipped and one line says how many. Without this, every remaining `r`, `f` or `u` would print its own "The game is over" message.

`Parser` is unchanged and still handles one command per call. Doing the split in the loop means both `run` overloads, including the legacy test entry point, get chaining.

Two consequences to be aware of. First, `exit; exit` counts as the confirming repeat for the unsaved-progress warning, because `SaveTracker.confirmDiscard` compares the action and the board position, not how many input lines were typed. `delete x; delete x` behaves the same way. Second, `;` cannot appear inside a command's arguments; this is already true because save names allow only letters, digits, `_` and `-`.

#### Listing and deleting saves

`SavesCommand` calls `SaveStore.list()` and prints the names, or a hint to use `save NAME` if there are none. `DeleteCommand` uses `SaveStore.exists` and `SaveStore.delete`. Both work only inside the active profile's saves folder, as `SaveStore` already guarantees.

`delete NAME` follows the same "type it twice" convention as `exit` and `load`:

1. If no save has that name, a message is printed and any pending confirmation is cleared.
2. Otherwise, the first request prints a permanent-deletion warning and remembers the name.
3. Typing the same `delete NAME` again deletes the file. Requesting a different name replaces the remembered one, so each save needs its own confirmation.

Command objects are created fresh for every input line, so the remembered name lives in a `DeleteCommand.Confirmation` object. `Jaweeper.createFeatureCommands` creates one and passes it to every `delete` command, in the same way it shares one `SaveTracker`. `SaveTracker.confirmDiscard` is not reused here because it asks "does the current board have unsaved moves?", which is unrelated to deleting a file.

`delete` also calls the new `SaveTracker.forgetSave(name)`. If `name` is the save that the current game was last saved to or loaded from, the tracker forgets it. The game then counts as unsaved again, so `exit` warns, and a plain `save` has no name to reuse and asks for one. `forgetSave` returns whether it did this, and `DeleteCommand` prints a note when it did. Because deleting does not decode the file, a corrupted save that cannot be loaded can still be deleted.

#### The clear command (temporarily disabled)

`ClearCommand` prints the ANSI sequence `ESC[H ESC[2J ESC[3J` (move the cursor home, erase the screen, erase the scrollback) and flushes. It does not touch the game state and does not reprint the board.

The command is currently disabled in three places. To re-enable it, uncomment all three:

- the `case "clear"` branch in `Parser.parse`,
- the `clear` line in `HelpCommand`,
- `@Disabled` on `clear_printsAnsiClearSequence_andKeepsBoard` in `ClearAndHelpCommandTest`.

Terminals that understand ANSI codes clear correctly. Some IDE consoles ignore the codes, in which case nothing visible happens.

#### The help command

`HelpCommand` groups commands by purpose (playing, saved games, player, other) and prints them through a small `printEntry` helper so the descriptions line up. It also explains chaining. The coordinate range is read from the `Board` passed to `execute`, so on a 16×30 board it shows "rows 1 to 16, columns 1 to 30" rather than the deprecated `Board.SIZE`. When a command is added or removed, its `printEntry` line must be updated too.

#### Design decisions and alternatives

| Decision | Alternative considered | Reason and trade-off |
|---|---|---|
| Draw mines in `printBoard` while the state is `LOST` | Set `isRevealed` on every mine when the game is lost | The display change cannot affect completion percentage, statistics or snapshots. The cost is that the rule lives in `Board.iconFor` rather than in `Cell`. |
| Store safeclick in a separate `settings.txt` per profile | Add a column to `profiles.txt` | The registry stays strictly validated and its format unchanged. A second file per profile has to be read and written separately. |
| Fall back to the default and never overwrite a bad settings file | Fail startup on a malformed file | A damaged setting should not stop the game, and the player's file is kept until they choose to change it. A typo in the file is silently ignored. |
| Split chains in `Jaweeper`, parse each segment when it runs | Teach `Parser` to return a list of commands | Commands can change the board (`new`, `load`), so later segments must be validated against the updated board, and both `run` overloads get chaining. Parsing errors appear only when the segment is reached. |
| Keep running after a failed command in a chain | Stop at the first error | The player sees every error and the valid commands still apply. A mistake early in a line does not cancel later commands that may depend on it. |
| Skip the rest of a line when the game ends | Run every command and let each report "game over" | One note replaces a wall of repeated messages. The player cannot chain a `new` after a game-ending move on the same line. |
| Share one `DeleteCommand.Confirmation` between delete commands | Store the pending name in `SaveTracker` or a static field | A command object lives for only one input line, so the state must be shared. A dedicated object keeps delete logic out of `SaveTracker` and avoids global state. |
| `forgetSave` on delete | Leave the tracker unchanged | Otherwise `exit` would stay silent for a game whose save file no longer exists. One more method on `SaveTracker`. |

#### Testing and known limitations

The tests share setup (a temporary profile folder, captured output, a script runner) through the abstract class `SessionTestBase`:

- `LossRevealTest` checks `*`, `X` and `F` after a loss, that cell state is unchanged, and that an in-progress board hides mines.
- `SettingsCommandTest` covers safeclick on and off, persistence across a simulated restart, separate values per profile, invalid arguments and a malformed settings file.
- `CommandChainingTest` covers `splitChain`, errors in the middle of a line, `exit`, parsing against a board changed by `new hard`, the game-ending skip and blank lines.
- `SavesAndDeleteCommandTest` covers listing, the two-step delete, a different name needing a fresh confirmation, missing or invalid names, and the effect of deleting the current game's save on `exit`.
- `ClearAndHelpCommandTest` covers the ANSI output and the help text. The `clear` test is currently `@Disabled`.

**Known limitations:**

- `exit; exit` and `delete x; delete x` on a single line count as confirmed repeats, as described under command chaining.
- `clear` depends on the terminal understanding ANSI codes, and is disabled for now.
- Safeclick changes apply at the next first reveal, not to a game already in progress.
- `UserGuide.md` does not yet describe these commands.

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
