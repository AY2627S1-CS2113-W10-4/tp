# Jaweeper v2.0 shared runtime contract

This document describes the foundation and runtime wiring supplied by Zack. It does not mean that save/load, statistics, variable-size boards, chained commands, or unsaved-game protection are implemented.

## Shared state

Production startup constructs exactly one `ProfileManager(Path.of("data"))`. `ProfileStartup.selectProfile(input, manager)` handles `create NAME`, `select NUMBER`, and `exit` before any game is started. EOF cancels startup. Creating a profile through this startup UI also selects it; `ProfileManager.createProfile` itself still does not select.

The application then constructs one `GameSession(board, manager)` and passes that same session to feature command factories.

- `session.getBoard()` returns the current board.
- `session.replaceBoard(validatedBoard)` changes the board used by following commands. This method does not check unsaved progress, restore timers, or reset statistics.
- `session.getProfileManager()` returns the shared manager, not a new instance.
- `session.getActiveProfile()` returns the selected `Profile`, or throws `IllegalStateException` if selection was omitted. Do not store a permanent copy of the active profile in a long-lived service; obtain the current value when needed.
- `session.getProfileDirectory()` returns `data/profiles/<UUID>/` as an absolute `Path`; it does not create that directory.
- The underlying `ProfileManager.getActiveProfile()` returns `Optional<Profile>`. This differs intentionally from the session's selected-profile accessor.

Profile IDs do not change on rename. Runtime profile switching is not implemented yet. Until progress-handling rules are integrated, commands must not switch the active profile while a game is underway.

## Storage ownership

`Storage.readLines(Path)` reads UTF-8 text and throws `IOException` on missing or unreadable files. The feature owner decides whether a missing file means first-time use or an error. Do not treat every I/O error as an empty file.

`Storage.writeLines(Path, List<String>)` creates parents and writes through a temporary file. Atomic replacement is attempted; the fallback is not guaranteed atomic. It throws `IOException` on failure.

Recommended independent locations:

- Registry: `data/profiles.txt` -- Zack.
- Named saves: `session.getProfileDirectory().resolve("saves")` -- Yong Xiang.
- Statistics: `session.getProfileDirectory().resolve("stats.txt")` -- Matthew.
- Profile settings, if persisted: `session.getProfileDirectory().resolve("settings.txt")` -- ZW.

Only the UUID is safe as a profile path component. Each feature must validate its own user-provided save names/paths and its own file contents. Renaming a profile must not rename its UUID directory.

Inactive-profile deletion currently removes the registry record and retains feature files. Active-profile deletion is blocked. Recreating the same display name creates a new UUID and does not reconnect retained files. User confirmation and any future cleanup policy belong in the later profile UI work.

## Command contract and registration

`Command.execute(Board board)` is unchanged. A command requiring profile or session state accepts `GameSession` in its constructor and stores it in an instance field. Do not use static/global profile state or construct another manager per command. The `board` argument is the current board at execution time.

The registration point is `Jaweeper.createFeatureCommands(GameSession session)`. It initially contains no registrations because teammates' v2.0 classes do not exist in this branch yet. Supply a factory with this shape:

```java
Function<String[], Command>
```

For example, once the corresponding implementation exists:

```java
commands.put("save", arguments -> SaveCommand.parse(arguments, session));
```

`SaveCommand.parse` above is an example of the feature owner's factory, not an existing API. Supply the actual class/factory signature to Zack for registration.

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
- The parser currently splits on whitespace only: no quoting, semicolon splitting, or escaping. Names containing spaces can be reconstructed by a profile factory from its remaining tokens. ZW owns any later chained-command behaviour and should coordinate the common line-dispatch hook with Zack.
- EOF currently ends the game loop directly. An exit command override does not intercept EOF; coordinate any EOF warning policy separately.

Zack coordinates changes to the central registry and parser. Feature owners can test immediately by passing their own feature map to:

```java
Jaweeper.run(input, session, coordinateFactories, featureFactories);
```

Existing `r`, `f`, and `u` factories continue to receive zero-based `(row, column)` through the coordinate registry. Do not also register them in the feature map unless deliberately taking over their parsing and validation.

## Variable-size boards

The production loop validates coordinates using the current session's row and column counts on every input. After `replaceBoard`, it uses the replacement's dimensions. The current adapter reads `board.grid.length` and `board.grid[0].length` and assumes a nonempty rectangular grid. If Pranav encapsulates grid, update only these two GameSession adapter methods to use his public dimension getters.

Legacy `Parser.parse` overloads retain `Board.SIZE` for compatibility with existing tests. Production uses the dimension-aware overload. This change alone does not make the existing Board algorithms support custom sizes; that remains Pranav's feature.

## Responsibilities still belonging to feature owners

- Yong Xiang: saved-game format/validation, save/load, unsaved-state tracking, and exit/progress-loss rules. Coordinate hooks for `new`, `load`, profile switching and EOF; the shared session is not itself dirty tracking.
- Matthew: timers, game-result accounting, statistics persistence and leaderboard logic. Coordinate the post-command/new-game hooks with Zack when the required service methods are available; merely having a session does not record results automatically.
- Pranav: difficulty/custom boards and remaining-mine display.
- ZW: safe-click options, show-mines-on-loss, chained commands, clear, and save listing/deletion through Yong Xiang's save service.
- Zack: profile CLI operations, startup, central registration, integration and documentation.

These boundaries allow implementation and unit testing in parallel. Integration still requires matching each feature's actual factory signature and agreed cross-feature behaviour.
