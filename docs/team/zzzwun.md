# Zhe Wen's Project Portfolio Page

### Project: Jaweeper

Jaweeper is a command-line Minesweeper game written in Java, with about 3 kLoC of production code. Players type commands to reveal and flag cells, choose a difficulty, keep per-player profiles with statistics and a leaderboard, and save and resume games.

Given below are my contributions to the project.

- **New Feature**: Added the ability to flag and unflag cells (`f ROW COL`, `u ROW COL`).
    - What it does: lets the player mark a hidden cell they believe is a mine, and remove that mark later. The board is reprinted after every attempt so the player always sees the result.
    - Justification: flagging is the core way a Minesweeper player keeps track of suspected mines, and the remaining-mines counter and win logic rely on it. Clear feedback on every rejected action keeps a command-line game easy to learn.
    - Highlights: the commands reject every invalid case with a specific message instead of failing silently: game already over, coordinates out of bounds, cell already revealed, cell already flagged, and cell not flagged. A flagged cell can never be revealed, and the commands keep that invariant with assertions. Coordinates are typed one-based but stored zero-based, so error messages convert back to the numbers the player typed. I wrote 12 JUnit tests in `FlagUnflagCommandTest` covering these cases.
    - Credits: none. Both commands follow the standard Minesweeper flagging rules.
- **New Feature**: Added the victory check (`Board.checkVictory`).
    - What it does: after every reveal, the game checks whether every safe cell has been uncovered. If so, the player wins.
    - Justification: this is the condition that ends a successful game. Flags are deliberately not part of it, so a player is never blocked from winning by a wrongly placed flag, matching classic Minesweeper.
    - Highlights: the check is wired into `RevealCommand` so it runs after the flood-fill that reveals empty regions, which can uncover many cells at once. My teammates later built the win/lose game state, timer and statistics on top of it, and adapted it to rectangular boards for the difficulty levels.
    - Credits: none.
- **New Feature**: Added a batch of gameplay quality-of-life features
    - **Mines revealed on loss**
        - What it does: after the player hits a mine, the board is printed with every unflagged mine shown as `*` and every wrongly placed flag shown as `X`; correct flags stay `F`.
        - Justification: showing what was missed lets the player learn from the loss.
        - Highlights: the board is drawn differently while the game state is `LOST`; no cell is actually revealed. That keeps the completion percentage and statistics unaffected by the loss screen.
    - **Safeclick setting** (`settings`, `settings safeclick on|off`)
        - What it does: lets the player turn the guaranteed-safe first reveal on or off. The choice is saved per profile and remembered across launches.
        - Justification: experienced players may want the standard risk of a mine on the first click, while beginners benefit from the safe start.
        - Highlights: the setting lives in its own `settings.txt` in each profile's folder, so the strictly validated profile registry format is untouched. A missing or damaged file falls back to the default instead of blocking the game, and the file is written before the in-memory value changes, so a failed write cannot leave the two out of step.
    - **Command chaining** (`f 1 1; f 2 3`)
        - What it does: runs several commands typed on one line, separated by `;`.
        - Justification: speeds up play, since a player can flag several known mines in one go.
        - Highlights: each command is parsed only when its turn comes, because earlier commands can change the board. For example, in `new hard; f 15 29` the coordinates are only valid on the board `new hard` creates. A failing command reports its error and the rest still run. If the game ends partway through a line, the remaining commands are skipped with one note instead of printing a "game over" message for each.
    - **Saved game management** (`saves`, `delete NAME`)
        - What it does: lists the player's saved games and deletes one, after the player types the same `delete` command twice to confirm.
        - Justification: saves would otherwise pile up with no way to see or remove them from inside the game.
        - Highlights: deleting uses the same "type it twice" convention as `exit` and `load`, which fits the one-line-per-command design without adding an input mode. The confirmation state is shared between commands because a command object only lives for one input line. If the deleted save belongs to the game currently in play, that game counts as unsaved again, so `exit` still warns. Corrupted saves that cannot be loaded can still be deleted.
    - **Updated `help`**: grouped by purpose, covers every command and chaining, and shows the coordinate range of the current board instead of a fixed 9×9.
    - **`clear` command**: implemented using ANSI escape codes but currently disabled, pending a fix for terminals that ignore these codes.
    - Credits: the `clear` command uses standard ANSI terminal escape sequences.
- **Code contributed**: [RepoSense link](<iframe src="https://nus-cs2113-ay2627-s1.github.io/tp-dashboard/#/widget/?search=&sort=groupTitle&sortWithin=title&timeframe=commit&mergegroup=&groupSelect=groupByRepos&breakdown=true&checkedFileTypes=docs~functional-code~test-code~other&since=2026-09-18T00%3A00%3A00&filteredFileName=&chartGroupIndex=11&chartIndex=2" frameBorder="0" width="800px" height="147px"></iframe>)
- **Project management**:
    - Tagged and published release `v2.0` on GitHub.
- **Enhancements to existing features**:
    - Added assertions and logging to `FlagCommand` and `UnflagCommand` (PR #32).
    - Wrote 26 JUnit tests for the quality-of-life features (25 active, 1 disabled with `clear`), split into one test class per feature with shared setup in an abstract `SessionTestBase` class, to follow the existing one-class-per-feature test layout.
- **Documentation**:
    - Team: filled in my entry on the About Us page (PR #2).
    - Developer Guide:
        - Added a "Gameplay quality-of-life features" section covering the design and implementation of each feature above, the alternatives considered, testing and known limitations.
        - Updated existing sections that the new features made out of date: command chaining, the built-in command list, and the planned `saves` and `delete` commands.
- **Pull requests merged**: #2 (About Us), #15 (flag), #19 (unflag and JUnit tests), #22 (victory check), #32 (assertions and logging for flag/unflag), #44 (quality-of-life features).