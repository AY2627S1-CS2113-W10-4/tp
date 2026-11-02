# Project Portfolio Page - Matthew Lam (M4ttL4m)

## Overview

**Project: Jaweeper**

[Jaweeper](https://github.com/AY2627S1-CS2113-W10-4/tp) is a command-line Minesweeper game written in Java for CS2113. Players choose a profile, then reveal and flag cells, start boards of different sizes, undo a move, and save games under that profile. The product uses a shared `GameSession` so every command sees the same board.

I built the core reveal mechanics (flood-fill and the safe first click), the game timer, and the per-profile statistics and leaderboard features, and connected them to the game loop so that finishing a game updates the active profile.

## Summary of Contributions

### Code contributed

[RepoSense contribution view](https://nus-cs2113-ay2627-s1.github.io/tp-dashboard/?search=m4ttl4m). My main files are `Board`, `Stats`, `GameTimer`, `StatsCommand`, `StatsClearCommand`, `LeaderboardCommand`, parts of `RevealCommand`, `NewGameCommand`, `GameSession` and `Jaweeper`, and the tests `BoardTest`, `RevealCommandTest` and `RemainingBugRegressionTest`.

### Enhancements implemented

* **New Feature**: Added statistics and a leaderboard (`stats`, `stats clear`, `leaderboard`).
    * What it does: `stats` shows the active profile's wins, losses, win rate, current and longest win streak, and fastest win time (`N/A` before the first win). `stats clear` resets only the active profile. `leaderboard` ranks every registered profile by wins, with win rate breaking ties.
    * Justification: players share one computer and several profiles, so a result that is recorded per profile gives them a reason to play again and compare. Recording happens automatically when a game ends, so the player never has to enter a result.
    * Highlights: `Stats` is held by each `Profile` and keeps counters plus a list of completion times. Edge cases are handled in the model: the win rate is 0 when no game has been played, the fastest time is -1 when there is no win, and a loss resets the current streak but not the longest one. `LeaderboardCommand` sorts a copy of the profile list so the registry's creation order, which profile numbers depend on, is never changed. Wrong arguments print `Usage: stats or stats clear` or `Usage: leaderboard` and the game loop continues.
    * Credits: none.

* **New Feature**: Added the safe first click (`Board#ensureSafeFirstClick`).
    * What it does: the first reveal of a game never hits a mine and always opens a blank area.
    * Justification: losing on the first move is not a real game. Classic Minesweeper avoids it, and players expect the same here.
    * Highlights: on the first reveal, every mine in the 3×3 area around the chosen cell is moved to a random free cell outside that area, then all adjacency counts are recalculated. The total mine count never changes, and assertions check both that rule and that the chosen cell ends up with zero adjacent mines. The `isFirstMove` flag is carried by `copyStateFrom`, so `new` makes the next first click safe again, and revealing a flagged cell does not use up the safe move. Nine tests cover this in `BoardTest` and `RevealCommandTest`.
    * Credits: none. The rule follows classic Minesweeper.

* **New Feature**: Added the flood-fill reveal (`Board#floodFill`) and fixed empty-cell expansion in `RevealCommand`.
    * What it does: revealing a blank cell opens every connected blank cell and the numbered border around them, stopping at the board edge, at flagged or revealed cells, and at mines.
    * Highlights: after a later bug review I rewrote it from recursion to an explicit stack. Saved boards can be up to 99×99, and a large open area could overflow the call stack. A regression test reveals the largest saved board and checks that numbers and flags still stop the spread.

* **New Feature**: Added the game timer and session integration.
    * What it does: `GameTimer` measures elapsed time from wall-clock timestamps and includes any interval that is still running. `GameSession` owns one timer. A mine hit stops the timer and records a loss; revealing the last safe cell stops the timer and records a win with its time.
    * Highlights: `RevealCommand` and `NewGameCommand` take the shared `GameSession`, while the older constructors without a session remain, so existing tests and code that do not use profiles are unaffected.

* **Bug fixes and code quality (PR #66, "patchfixes")**:
    * Removed the default console log handler at startup (`Jaweeper.removeConsoleLogging`). Java's default setup printed lines such as `INFO: Cell flagged at (0, 0)` between game messages.
    * Replaced the copy constructor used by undo. `Board.deepCopy()` used to build a random new board, place mines and then overwrite it on every move; it now copies the source directly.
    * The mine counter never prints a negative number, even for a hand-edited save with extra flags.
    * A finished game now deletes the saves that belong to it, so a won or lost game cannot be replayed from a save and counted twice (`GameSession#deleteFinishedGameSaves`). Reloading a save no longer deletes saves of earlier games.
    * `delete NAME` now asks for a fresh confirmation if the save was overwritten after the first warning, by comparing the file contents, instead of only the name.
    * Added `RemainingBugRegressionTest` with 15 tests covering all of the above.

## Contributions to the UG

I wrote the `stats`, `stats clear` and `leaderboard` sections of the User Guide, with expected output and error messages. I also added the FAQ entry on renaming a profile and the matching command-summary rows. The sections are reproduced in the UG extracts below.

## Contributions to the DG

I wrote the "Recording a game result" and "Viewing the leaderboard" sections of the Developer Guide, including the design alternatives for where results are recorded and how the leaderboard is sorted. I also added the maintainer note for the statistics component. The sections are reproduced in the DG extracts below.

## Contributions to team-based tasks

* Pull requests #16, #24, #28, #39, #56, #59 and #66 were merged into the team repository.
* In PR #39, connected `RevealCommand` and `NewGameCommand` to the shared `GameSession`, timer and `Stats`, so the same session object is used by the commands that other teammates added.
* Fixed Checkstyle violations and failing assertions in my own branches before merging.

## Extracts from the User Guide

Given below are sections I contributed to the User Guide. They showcase my ability to write documentation targeting end-users.

### Viewing your statistics: `stats`

Shows the current profile's wins, losses, win rate, streaks and fastest time. A win or loss is recorded when a game ends.

Format: `stats`

Expected output after one loss:

```text
> stats
=== Statistics for: Tester ===
Wins: 0
Losses: 1
Win Rate: 0.0%
Current Streak: 0
Max Streak: 0
Fastest Time: N/A
=========================================
```

`Current Streak` counts wins in a row and resets to 0 after a loss. `Max Streak` is your longest streak. `Fastest Time` is `N/A` until you win a game.

> **Note:** Statistics are kept only while Jaweeper is running. They are reset when you exit.

### Clearing your statistics: `stats clear`

Resets all statistics of the current profile. Other profiles are not affected.

Format: `stats clear`

```text
> stats clear
Statistics have been successfully cleared for profile: Tester
```

Any other extra word is rejected:

```text
> stats now
Usage: stats or stats clear
```

### Viewing the leaderboard: `leaderboard`

Shows every profile's wins, losses and win rate, so players on the same computer can compare results.

Format: `leaderboard`

```text
> leaderboard
================ LEADERBOARD ================
Rank | Profile         | Wins   | Losses | Win Rate
---------------------------------------------
1    | Tester          | 0      | 1      | 0.0%
=============================================
```

`leaderboard` takes no extra words:

```text
> leaderboard x
Usage: leaderboard
```

## Extracts from the Developer Guide

Given below are sections I contributed to the Developer Guide. They showcase my ability to write technical documentation and the technical depth of my contributions to the project.

### Recording a game result

When a reveal ends the game, the timer stops and the active profile's `Stats` are updated.

Given below is an example usage scenario.

Step 1. The player reveals a cell. If it is a mine, `revealMine` sets the state to `LOST`, stops the timer and calls `recordLoss()`, which resets the current streak.

Step 2. If it is safe, `revealSafeCell` flood-fills from the cell. If every safe cell is now revealed, the state becomes `WON`, the timer stops, and `recordWin(timeInMillis)` increments wins and the streak and stores the time.

> **Note:** Statistics are kept in memory only and are lost when the app exits. `profiles.txt` stores profile IDs and names only, so a loaded profile starts with fresh statistics. A future statistics file would need its own format, validation and load/save integration.

#### Design considerations:

**Aspect: Where results are recorded**

* **Alternative 1 (current choice):** `RevealCommand` records the result when it ends the game.
  * Pros: The result is recorded exactly once, at the moment the game ends.
  * Cons: `RevealCommand` depends on `Stats` and `GameTimer`.
* **Alternative 2:** The input loop checks for a finished game after every command.
  * Pros: Commands stay simpler.
  * Cons: The loop must remember whether a result was already recorded.

### Viewing the leaderboard

`stats` shows the active profile's statistics, `stats clear` resets them, and `leaderboard` compares all profiles.

| Command | Format | Description |
| :--- | :--- | :--- |
| `stats` | `stats` | Shows wins, losses, win rate, streaks and the fastest time for the active profile. |
| `stats clear` | `stats clear` | Resets the active profile's statistics. |
| `leaderboard` | `leaderboard` | Shows every registered profile with its wins, losses and win rate. |

Step 1. The command copies the profile list from the `ProfileManager`, so it can be sorted without changing the registry.

Step 2. The list is sorted by wins, with win rate breaking ties.

Step 3. One row is printed per profile: rank, name, wins, losses and win rate.

#### Design considerations:

**Aspect: Sorting the leaderboard**

* **Alternative 1 (current choice):** Sort a copy of the profile list each time.
  * Pros: The registry's creation order, used for profile numbers, is never changed.
  * Cons: Sorts on every call (negligible for a few local profiles).
* **Alternative 2:** Keep a sorted list updated after every game.
  * Pros: No sorting when displaying.
  * Cons: Must be kept in step with every win, loss and `stats clear`.
