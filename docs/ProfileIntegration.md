# Jaweeper v2.0 shared runtime contract

This document describes the profile foundation and runtime wiring maintained by Zheng Yiting. Save/load, statistics, variable-size boards, settings and chained commands now use the shared session; their domain logic belongs to the corresponding feature owners.

## Shared state

Production startup constructs exactly one `ProfileManager(Path.of("data"))`. `ProfileStartup.selectProfile(input, manager)` handles `create NAME`, `select NUMBER`, and `exit` before any game is started. EOF cancels startup. Creating a profile through this startup UI also selects it; `ProfileManager.createProfile` itself still does not select.

The application then constructs one `GameSession(board, manager)` and passes that same session to feature command factories.

- `session.getBoard()` returns the current board.
- `session.replaceBoard(validatedBoard)` changes the board used by following commands. This method does not check unsaved progress, restore timers, or reset statistics.
- `session.getProfileManager()` returns the shared manager, not a new instance.
- `session.getActiveProfile()` returns the selected `Profile`, or throws `IllegalStateException` if selection was omitted. Do not store a permanent copy of the active profile in a long-lived service; obtain the current value when needed.
- `session.getProfileDirectory()` returns `data/profiles/<UUID>/` as an absolute `Path`; it does not create that directory.
- The underlying `ProfileManager.getActiveProfile()` returns `Optional<Profile>`. This differs intentionally from the session's selected-profile accessor.

Profile IDs do not change on rename, and `Profile.withName` retains the existing in-memory `Stats` object. Runtime switching is handled by `ProfileCommand`: it refuses a different player while `SaveTracker.hasUnsavedChanges` is true. A successful switch creates a fresh easy board, resets/restarts the timer, clears undo history, calls `SaveTracker.reset()` and clears saved-game deletion confirmation. Do not call `ProfileManager.selectProfile` directly during play and bypass these steps. Selecting the current profile is a no-op.

## Storage ownership

`Storage.readLines(Path)` reads UTF-8 text and throws `IOException` on missing or unreadable files. The feature owner decides whether a missing file means first-time use or an error. Do not treat every I/O error as an empty file.

`Storage.writeLines(Path, List<String>)` creates parents and writes through a temporary file. Atomic replacement is attempted; the fallback is not guaranteed atomic. It throws `IOException` on failure.

Recommended independent locations:

- Registry: `data/profiles.txt` -- Zheng Yiting.
- Named saves: `session.getProfileDirectory().resolve("saves")` -- Yong Xiang.
- Statistics: `session.getProfileDirectory().resolve("stats.txt")` -- proposed location for Matthew's future persistence; current statistics are in memory only.
- Profile settings, if persisted: `session.getProfileDirectory().resolve("settings.txt")` -- ZW.

Only the UUID is safe as a profile path component. Each feature must validate its own user-provided save names/paths and its own file contents. Renaming a profile must not rename its UUID directory.

Inactive-profile deletion removes the registry record and retains feature files. Active-profile deletion is blocked. Recreating the same display name creates a new UUID and does not reconnect retained files. `profile delete NUMBER` explains this without deleting; `profile delete NUMBER confirm` confirms removal. Numbers refer to the current `profile list`. Runtime `profile create NAME` creates without switching; startup `create NAME` still selects the created profile.

## Command contract and registration

`Command.execute(Board board)` is unchanged. A command requiring profile or session state accepts `GameSession` in its constructor and stores it in an instance field. Do not use static/global profile state or construct another manager per command. The `board` argument is the current board at execution time.

The registration point is `Jaweeper.createFeatureCommands(GameSession session)`. Existing feature routes are registered here. Factories have this shape:

```java
Function<String[], Command>
```

For example, the save route shares one tracker with load, exit and profile commands:

```java
commands.put("save", arguments -> SaveCommand.parse(arguments, session, saveTracker));
```

Reuse the existing tracker; do not create a tracker for each parsed command.

- Keys must be lowercase.
- Factories receive argument tokens only, without the command name; argument case is preserved.
- `save MyGame` gives `["MyGame"]`.
- `stats clear` gives `["clear"]` to the `stats` factory.
- `new custom 9 12 15` gives `["custom", "9", "12", "15"]` to the `new` factory.
- Feature factories run before built-ins. Registering `new` or `exit` deliberately replaces the built-in handling of that entire command, including its no-argument form.
- Invalid syntax should throw `IllegalArgumentException` with a helpful message. The loop displays it and continues.
- Factories must not perform gameplay or disk mutations; return a command to perform the operation in `execute`.
- `execute` still cannot declare checked `IOException`. Catch file errors inside the feature command, report the failure, and do not report a successful save/load or update success flags afterward.
- A cancelled exit must return false from `isExit()`. Return true only after exit was actually accepted.
- The main loop splits semicolon-separated commands, then parses each segment immediately before execution. The parser splits on whitespace; quoting and escaping are not supported. The profile factory reconstructs a name from its remaining tokens using single spaces.
- EOF currently ends the game loop directly. An exit command override does not intercept EOF; coordinate any EOF warning policy separately.

Zheng Yiting coordinates changes to the central registry and parser. Feature owners can test by passing their own feature map to:

```java
Jaweeper.run(input, session, coordinateFactories, featureFactories);
```

Existing `r`, `f`, and `u` factories continue to receive zero-based `(row, column)` through the coordinate registry. Do not also register them in the feature map unless deliberately taking over their parsing and validation.

## Variable-size boards

The production loop validates coordinates using the current session's row and column counts on every input. After `replaceBoard`, it uses the replacement's dimensions. The current adapter reads `board.grid.length` and `board.grid[0].length` and assumes a nonempty rectangular grid. If Pranav encapsulates grid, update only these two GameSession adapter methods to use his public dimension getters.

Legacy `Parser.parse` overloads use `Difficulty.EASY.rows` and `Difficulty.EASY.cols`. Production uses the dimension-aware overload, including after a profile switch installs an easy board.

## Responsibilities still belonging to feature owners

- Yong Xiang: saved-game format/validation, save/load, unsaved-state tracking, and exit/progress-loss rules. Coordinate hooks for `new`, `load`, profile switching and EOF; the shared session is not itself dirty tracking.
- Matthew: timers, game-result accounting, statistics persistence and leaderboard logic. Coordinate shared hooks with Zheng Yiting; merely having a session does not record results automatically.
- Pranav: difficulty/custom boards and remaining-mine display.
- ZW: safe-click options, show-mines-on-loss, chained commands, clear, and save listing/deletion through Yong Xiang's save service.
- Zheng Yiting: profile CLI operations, startup, central registration, integration and documentation.

These boundaries allow implementation and unit testing in parallel. Integration still requires matching each feature's actual factory signature and agreed cross-feature behaviour.
