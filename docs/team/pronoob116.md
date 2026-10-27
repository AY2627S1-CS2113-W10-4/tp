# Pranav - Project Portfolio Page

## Overview

Jaweeper is a command-line Minesweeper application for CS2113. Players select a profile, then reveal and flag cells on a shared board. I implemented variable board sizes, the easy, medium, and hard presets, and a hint command that suggests one logically safe cell without changing the board.

## Summary of Contributions

* **Dynamic board sizing**: `Board` now keeps `rows`, `cols`, and `totalMines`. Mine placement, adjacency counts, flood fill, victory checking, and printing use those dimensions. `copyStateFrom` copies the new grid into the board object `GameSession` already holds, so a new size does not require a new session. The old `Board.SIZE` and `Board.MINES` constants remain for existing easy-board tests.

* **Difficulty presets**: Added the `Difficulty` enum. `EASY` is 9×9 with 10 mines, `MEDIUM` is 16×16 with 40 mines, and `HARD` is 16×30 with 99 mines. `Parser.parseNewCommand` accepts `new`, `new easy`, `new medium`, and `new hard`. `NewGameCommand` builds the matching board and prints `New easy game started.`, `New medium game started.`, or `New hard game started.` An unknown difficulty prints `Usage: new [easy|medium|hard]` and leaves the current board unchanged.

* **Hint command**: Added `HintCommand` and registered `hint` in `Jaweeper.createFeatureCommands`. The command scans revealed numbers whose surrounding flags already match the number, then prints the first hidden unflagged neighbour in one-based coordinates. It does not reveal or flag that cell. Extra arguments, a finished game, and a missing deduction each have their own message. A broken grid is caught and reported so the command loop continues. The class JavaDoc carries the RepoSense tag `@@author ProNoob116`.

* **Documentation**:
  * User Guide: `new easy`, `new medium`, `new hard`, and `hint`, including format and terminal output.
  * Developer Guide: implementation of dynamic boards and `new hard`, including the class diagram and the sequence diagram.

* **Code contributed**: Functional changes are in `Board`, `Difficulty`, `Parser`, `NewGameCommand`, and `HintCommand`, with the RepoSense author tag `ProNoob116` on `HintCommand`. The diagrams are `docs/diagrams/new-hard-class-diagram.puml` and `docs/diagrams/new-hard-sequence-diagram.puml`.

## Contributions to the User Guide

Given below are sections I contributed to the User Guide. They show how a player starts each preset and how to read a hint.

### Starting a hard game: `new hard`

Starts a new 16 by 30 game with 99 mines.

Format: `new hard`

Expected output:

```text
> new hard
New hard game started.
Remaining Mines: 99
```

Jaweeper then prints a board with 16 rows and 30 columns.

### Asking for a hint: `hint`

Suggests one hidden cell that is safe if your flags are correct. The command prints the cell and does not reveal it. You still enter `r ROW COL` yourself.

Format: `hint`

Expected output when a safe cell can be deduced:

```text
> hint
Try revealing cell (4, 7).
```

## Contributions to the Developer Guide

Given below is the implementation section I added to the Developer Guide. It explains `new hard` and includes both PlantUML diagrams.

### Difficulty levels and dynamic boards

`Board` stores `rows`, `cols`, and `totalMines`. `Difficulty.HARD` is 16 rows, 30 columns, and 99 mines. `new hard` is handled by `Parser.parseNewCommand`, which returns `new NewGameCommand(Difficulty.HARD)`. Execution builds `new Board(16, 30, 99)` and `copyStateFrom` copies that grid into the session's existing board. The command prints `New hard game started.` and the 16×30 board. `isExit()` is false, so the input loop continues and later coordinates are checked against the new size.

The class diagram is [diagrams/new-hard-class-diagram.puml](../diagrams/new-hard-class-diagram.puml) (Developer Guide Figure 3). It shows `Parser` creating `NewGameCommand`, that command using one `Difficulty`, and `GameSession` keeping a single `Board`.

The sequence diagram is [diagrams/new-hard-sequence-diagram.puml](../diagrams/new-hard-sequence-diagram.puml) (Developer Guide Figure 4). It shows `parse("new hard")`, construction of `Board(16, 30, 99)`, `copyStateFrom`, printing, and `isExit()` returning false.
