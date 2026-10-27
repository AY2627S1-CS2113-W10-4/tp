# User Guide

## Introduction

Jaweeper is a command-line Minesweeper game. You reveal cells, flag mines, and start a new board at easy, medium, or hard size. This guide covers starting a sized game and asking for a hint.

## Quick Start

1. Install Java 25 or above.
1. In the `tp` project directory, start Jaweeper with `./gradlew run` (Windows: `gradlew.bat run`).
1. At the `profile>` prompt, create or select a profile. For a first run, enter `create NAME`.
1. After `Selected profile: NAME`, the game prompt is `>`. The opening board is easy: 9 rows, 9 columns, and 10 mines.
1. Type a command and press Enter. Row and column numbers start at 1.

## Features

### Starting an easy game: `new easy`

Starts a new 9 by 9 game with 10 mines. `new` with no difficulty word does the same thing. The previous board is discarded.

Format: `new easy`

Example:

`new easy`

Expected output:

```text
> new easy
New easy game started.
Remaining Mines: 10
      1   2   3   4   5   6   7   8   9
    +---+---+---+---+---+---+---+---+---+
  1 | . | . | . | . | . | . | . | . | . |
```

The full board has 9 rows. A `.` is a hidden cell. Mine positions change from game to game, so the later rows will not match this example until you reveal cells.

### Starting a medium game: `new medium`

Starts a new 16 by 16 game with 40 mines.

Format: `new medium`

Example:

`new medium`

Expected output:

```text
> new medium
New medium game started.
Remaining Mines: 40
```

Jaweeper then prints a 16 by 16 board. Column labels run from 1 to 16, and row labels run from 1 to 16. The border uses the same `+---+` style as the easy board.

### Starting a hard game: `new hard`

Starts a new 16 by 30 game with 99 mines.

Format: `new hard`

Example:

`new hard`

Expected output:

```text
> new hard
New hard game started.
Remaining Mines: 99
```

Jaweeper then prints a board with 16 rows and 30 columns. Column labels run from 1 to 30. `Remaining Mines` decreases when you place a flag and increases when you remove one.

`NEW HARD` is accepted. The command name and the difficulty word are not case-sensitive. These inputs are rejected, and the current board stays as it is:

```text
> new expert
Usage: new [easy|medium|hard]

> new hard extra
Usage: new [easy|medium|hard]
```

### Asking for a hint: `hint`

Suggests one hidden cell that is safe if your flags are correct. Jaweeper looks at each revealed number. When the flags around that number already match it, an unflagged hidden neighbour is a safe place to reveal. The command prints the first such cell and does not reveal it, flag it, or reprint the board. You still enter `r ROW COL` yourself.

Format: `hint`

Example:

`hint`

Expected output when a safe cell can be deduced. The numbers are the cell's row and column, starting at 1:

```text
> hint
Try revealing cell (4, 7).
```

Expected output when the revealed numbers and flags do not force a safe cell yet:

```text
> hint
No safe cell can be deduced yet. Reveal more cells.
```

Expected output after the game has already been won or lost:

```text
> hint
The game is over. Type 'new' to start a new game.
```

`hint` takes no extra words:

```text
> hint please
Usage: hint
```

A hint can point at a mine if a flag is on the wrong cell. Correct the flags and ask again.

## FAQ

**Q**: Which board do I get if I type `new`?

**A**: The same board as `new easy`: 9 rows, 9 columns, and 10 mines. The message is `New easy game started.`

**Q**: Does `hint` reveal the cell for me?

**A**: No. It only prints a suggestion. Reveal that cell with `r ROW COL`, using the same row and column numbers.

## Command Summary

| Command | What it does |
|---|---|
| `new` | Starts an easy game. |
| `new easy` | Starts a 9×9 game with 10 mines. |
| `new medium` | Starts a 16×16 game with 40 mines. |
| `new hard` | Starts a 16×30 game with 99 mines. |
| `hint` | Prints one suggested safe cell, or says that none can be deduced. |
