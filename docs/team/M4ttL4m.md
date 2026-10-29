# Matthew Lam - Project Portfolio Page

## Overview

Jaweeper is a command-line Minesweeper application for CS2113. Players select a profile, then reveal and flag cells on a shared board. I implemented the flood-fill reveal, the safe first click, the game timer, per-profile statistics, and the `stats`, `stats clear` and `leaderboard` commands. I also connected the timer and statistics to the game loop so that a finished game updates the active profile.

## Summary of Contributions

* **Flood-fill reveal**: Added `Board#floodFill`, which opens a blank cell and spreads to its neighbours. The spread stops at the board edge, at cells that are already revealed or flagged, and at mines. A numbered cell is revealed but does not spread further, so the player sees the blank region and its numbered border. I also fixed the empty-cell expansion in `RevealCommand` so a blank reveal calls the flood fill, and added the `BoardTest` cases `floodFill_outOfBounds_doesNotThrowException`, `floodFill_blankBoard_revealsEntireGrid` and `floodFill_cellWithAdjacentMines_revealsSelfAndStops`.

* **Safe first click**: Added `Board#ensureSafeFirstClick(row, col)` and the `isFirstMove` flag. On the first reveal, any mine in the 3×3 area around the chosen cell is moved to a random free cell outside that area, then adjacency counts are recalculated. The total mine count stays the same, and the chosen cell always has zero adjacent mines, so the first reveal opens an area instead of ending the game. Assertions check both rules. `copyStateFrom` carries `isFirstMove` across, so `new` makes the next first click safe again. `RevealCommand` does not use up the safe first move when the player tries to reveal a flagged cell. Tests: four `ensureSafeFirstClick_*` cases and `copyStateFrom_afterFirstClick_restoresFirstMove` in `BoardTest`, and `execute_firstRevealOnMine_mineIsMovedAndGameContinues`, `execute_firstReveal_floodFillOpensNeighbours`, `execute_firstRevealOnFlaggedCell_doesNotUseUpSafeFirstMove` and `execute_afterNewGame_firstRevealIsSafeAgain` in `RevealCommandTest`.

* **Game timer**: Added `GameTimer`, which measures elapsed time with wall-clock timestamps (`System.currentTimeMillis()`). `start`, `stop` and `reset` control it, and `getElapsedMillis` includes any interval that is still running. `GameSession` owns one timer. `NewGameCommand` starts it, and `RevealCommand` stops it when the game ends. The save/load feature later extended the class so a saved duration can be resumed.

* **Statistics model**: Added `Stats`, held by each `Profile`. It records wins, losses, the current win streak, the longest win streak and a list of completion times. `recordLoss` resets the current streak, `getWinRate` returns 0 when no games have been played, and `getFastestTime` returns -1 when there is no win. `Stats#reset` clears everything.

* **Statistics and leaderboard commands**: Added `StatsCommand`, `StatsClearCommand` and `LeaderboardCommand`, and registered `stats` and `leaderboard` in `Jaweeper.createFeatureCommands`. `stats` prints the active profile's wins, losses, win rate, streaks and fastest time (`N/A` when there is no win). `stats clear` resets the active profile. `leaderboard` prints a table of every registered profile with rank, wins, losses and win rate. Wrong arguments print `Usage: stats or stats clear` or `Usage: leaderboard`, and the game loop continues.

* **Session integration**: `NewGameCommand(session, difficulty)` and `RevealCommand(session, row, col)` now take the shared `GameSession`. Hitting a mine stops the timer and calls `recordLoss` on the active profile. Revealing the last safe cell stops the timer and calls `recordWin`. The older constructors without a session remain, so existing tests and code that do not use profiles are unaffected.

* **Documentation**:
  * Developer Guide: wrote the "Statistics and Leaderboard Feature" section. It describes the `Stats` model, `GameTimer`, the command syntax table and the persistence boundary (statistics are kept in memory and are not saved to `profiles.txt`). I also added the maintainer note for the statistics component.

* **Code contributed**: Functional changes are in `Board`, `RevealCommand`, `NewGameCommand`, `Profile`, `GameSession`, `Jaweeper`, `GameTimer`, `Stats`, `StatsCommand`, `StatsClearCommand` and `LeaderboardCommand`. Tests are in `BoardTest` and `RevealCommandTest`.

## Contributions to the Developer Guide

Given below is a summary of the section I added to the Developer Guide.

### Statistics and Leaderboard Feature

The statistics module tracks performance in the active profile's in-memory `Stats` object. It does not save statistics between launches.

* **`Stats` model**: counts wins, losses, current and maximum win streaks, and keeps a list of completion times.
* **`GameTimer`**: tracks elapsed time with millisecond timestamps. Commands start, stop or restore it, and creating a session does not start it.
* **Persistence boundary**: `profiles.txt` stores profile IDs and names only. A new or loaded profile starts with fresh statistics, and renaming a profile keeps its current `Stats` object. A future statistics file would need its own format, validation and load/save integration.

| Command | Format | Description |
| :--- | :--- | :--- |
| `stats` | `stats` | Shows wins, losses, win rate, streaks and the fastest time for the active profile. |
| `stats clear` | `stats clear` | Resets the active profile's wins, losses, streaks and times. |
| `leaderboard` | `leaderboard` | Shows every registered profile with its wins, losses and win rate. |
