# Zhe Wen's Project Portfolio Page

### Project: Jaweeper

Jaweeper is a command-line Minesweeper game written in Java, with about 11 kLoC of production code. Players interact and type commands via a CLI to reveal and flag cells, choose a difficulty, keep per-player profiles with statistics and a leaderboard, and save and resume games.

Given below are my contributions to the project.

- **New Feature**: Added the ability to flag and unflag cells (`f ROW COL`, `u ROW COL`). (PR #15 and PR #19)
    - What it does: lets the player mark a hidden cell they believe is a mine, and remove that mark later. The board is reprinted after every attempt so the player always sees the result.
    - Justification: flagging is the core way a Minesweeper player keeps track of suspected mines, and the remaining-mines counter and win logic rely on it. Clear feedback on every rejected action keeps a command-line game easy to learn.
    - Highlights: the commands reject every invalid case with a specific message instead of failing silently: game already over, coordinates out of bounds, cell already revealed, cell already flagged, and cell not flagged. A flagged cell can never be revealed, and the commands keep that invariant with assertions. Coordinates are typed one-based but stored zero-based, so error messages convert back to the numbers the player typed. I wrote 12 JUnit tests in `FlagUnflagCommandTest` covering these cases.
    - Credits: none. Both commands follow the standard Minesweeper flagging rules.
- **New Feature**: Added the victory check (`Board.checkVictory`). (PR #22)
    - What it does: after every reveal, the game checks whether every safe cell has been uncovered. If so, the player wins.
    - Justification: this is the condition that ends a successful game. Flags are deliberately not part of it, so a player is never blocked from winning by a wrongly placed flag, matching classic Minesweeper.
    - Highlights: the check is wired into `RevealCommand` so it runs after the flood-fill that reveals empty regions, which can uncover many cells at once. My teammates later built the win/lose game state, timer and statistics on top of it, and adapted it to rectangular boards for the difficulty levels.
    - Credits: none.
- **New Feature**: Added a batch of gameplay quality-of-life features (PR #44).
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
- **Code contributed**: [RepoSense link](https://nus-cs2113-ay2627-s1.github.io/tp-dashboard/#/widget/?search=&sort=groupTitle&sortWithin=title&timeframe=commit&mergegroup=&groupSelect=groupByRepos&breakdown=true&checkedFileTypes=docs~functional-code~test-code~other&since=2026-09-18T00%3A00%3A00&filteredFileName=&chartGroupIndex=11&chartIndex=2)
- **Project management**:
    - Tagged and published release `v2.0` on GitHub.
    - Managed milestones `v2.0` - `v2.1` on GitHub, ensuring PRs and Issues are correctly tagged
- **Enhancements to existing features**:
    - Added assertions and logging to `FlagCommand` and `UnflagCommand` (PR #32).
    - Wrote 26 JUnit tests for the quality-of-life features (25 active, 1 disabled with `clear`), split into one test class per feature with shared setup in an abstract `SessionTestBase` class, to follow the existing one-class-per-feature test layout.
- **Bug fixes** (PR #64): found by testing the integrated game, and covered by 20 regression tests in `LogicBugRegressionTest`.
    - **Custom boards and flood fill**: a custom board could be created with so many mines that the safe first click had no room to clear its 3×3 opening. The code that relocates mines then retried random positions and could loop forever. `new custom` now allows at most `ROWS × COLS − 9` mines, so the first reveal always opens a blank area for the flood fill to expand from, and mines are relocated by shuffling a list of free cells instead of retrying at random. I updated the User Guide and Developer Guide to state the new limit and error message.
    - **Statistics saved per profile**: statistics were lost when the game closed. Each profile's wins, losses, streaks and completion times are now written to `stats.txt` in its own folder after every win, loss and `stats clear`, and loaded at startup. The file reuses the existing `Storage` helper, which writes to a temporary file first, so a failed write keeps the previous file. A write error is reported without interrupting the game.
    - **Safeguards against hand-edited data files**: `stats.txt` is validated on load (header and line count, digits-only numbers with a length cap, and win, streak and completion-time counts that agree with each other). A damaged or edited file is ignored with a log warning and the profile starts from zero, instead of crashing or showing impossible statistics. The file is only replaced the next time a result is recorded. The first-click mine relocation also has a fallback for hand-made saves whose board is too dense: it clears only the clicked cell, rather than searching forever for free cells that do not exist.
    - **Improved `hint` command**: the hint used the player's flags to find a safe cell, so a wrongly placed flag could point the player at a mine. It now ignores flags and works only from the revealed numbers: a hidden cell is a known mine when a number has exactly as many hidden neighbours as its value, and a number already touching that many known mines makes its other hidden neighbours safe. The board is left unchanged.
    - **Undo history** (the `undo` feature is a teammate's; I fixed how it interacted with the rest of the game):
        - The history is cleared on `new` and `load`, so `undo` can no longer step back into a different game.
        - The history is cleared after a win or a loss. Before, a loss could be undone and the same game then won, so one game counted as both.
        - Rejected actions no longer add an undo step, such as revealing an already revealed cell or a flag that is refused. Only a change to the board does.
        - Undo now restores the board in place (`Board.restoreFrom`), so the grid keeps its identity. Unsaved-progress detection and plain `save` still recognise the game after undoing back to a saved position.
    - **Timer and completion times**:
        - `GameTimer.start()` now resets the elapsed time, so a new game no longer inherits time from the previous one.
        - The opening game at startup is now timed like a game started with `new`.
        - Wins are recorded in milliseconds, which is the unit `Stats` stores, instead of seconds. Fastest times and the leaderboard now use one consistent unit.
    - **Leaderboard ordering**: the sort comparator was reversed twice, which undid the ordering on wins. It now ranks by most wins, then by higher win rate.
    - **Completion percentage after a loss**: the mine that ended the game counted as a revealed safe cell in the game summary. It no longer does, so "Safe Cells Revealed" is correct after a loss.
- **Documentation**:
    - Team: filled in my entry on the About Us page (PR #2).
    - User Guide:
        - Added documentation for the features `flagging a cell`, `removing a flag`, `seeing the mines after a loss`, `Changing settings`, `Running several commands at once`, `Listing saved games`, `Deleting a saved game` (PR #60)
        - Also added more to `FAQ` portion with relation to this features, along with adding them to the `Command Summary` (PR #60)
    - Developer Guide:
        - Added a "Gameplay quality-of-life features" section covering the design and implementation of each feature above, the alternatives considered, testing and known limitations.
        - Updated existing sections that the new features made out of date: command chaining, the built-in command list, and the planned `saves` and `delete` commands.
- **Community**:
    - Reviewed PRs of other teams, leaving feedback that some current implementations were not working correctly
    - Contributed to forum issues discussions
    - Reported bugs and suggestions for other teams in the class


