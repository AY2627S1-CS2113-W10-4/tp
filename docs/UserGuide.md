# User Guide

## Introduction

Jaweeper is a command-line Minesweeper game. You reveal cells, flag mines, and start a new board at easy, medium, or hard size. This guide covers profile selection, basic commands, starting a sized game and asking for a hint.

## Quick Start

1. Install Java 25 or above.
1. In the `tp` project directory, start Jaweeper with `./gradlew run` (Windows: `gradlew.bat run`).
1. At the `profile>` prompt, create or select a profile. For a first run, enter `create Zheng Yiting`.
1. After `Selected profile: NAME`, the game prompt is `>`. The opening board is easy: 9 rows, 9 columns, and 10 mines.
1. Type a command and press Enter. Row and column numbers start at 1.

## Features

### Creating or selecting a player

At `profile>`, enter `create Zheng Yiting` to create and select a profile, or `select 1` to
choose the number shown next to an existing profile. Enter `exit` to cancel startup.
You choose again each time the application starts.

Names may contain 1–30 letters/digits, spaces, underscores or hyphens, and must start
with a letter or digit. Names differing only in case are duplicates. For example,
`create Zheng Yiting` and `create zheng yiting` cannot create two separate profiles.

Profiles and their saves/settings are stored in the `data` folder in your current
working directory. Always launch from the same folder to find the same profiles.
If the registry cannot be read, the application reports the problem without replacing
it. Preserve the existing files before attempting recovery.

### Managing profiles during a game

At the game prompt `>`, use these commands:

| Command | Example | Result |
|---|---|---|
| `profile list` | `profile list` | Lists profiles in creation order; `*` marks the current player. |
| `profile create NAME` | `profile create Guest` | Creates a profile without switching away from your current game. |
| `profile select NUMBER` | `profile select 2` | Switches to that player and starts a fresh easy game. |
| `profile rename NUMBER NAME` | `profile rename 2 Guest New` | Changes the name while keeping the player's identity, saves, settings and current statistics. |
| `profile delete NUMBER` | `profile delete 2` | Shows a warning; nothing is deleted yet. |
| `profile delete NUMBER confirm` | `profile delete 2 confirm` | Removes that inactive profile from the list. |

Numbers refer to the current `profile list`, starting at 1. Deleting an entry renumbers
later entries, so check the list again before selecting, renaming or confirming deletion.
Names can include spaces without quotes; the same name restrictions as startup apply.
Command/action words ignore case. Missing arguments, invalid numbers and duplicate names
are rejected without changing profiles.

Save unfinished progress before switching. If the current board has unsaved revealed
or flagged cells, `profile select` asks you to use `save NAME` first. Selecting the
current player does nothing. A successful switch clears the previous game's undo
history, starts a fresh timer and uses the selected player's settings. Use `load NAME`
to resume one of that player's saved games; it is not resumed automatically.

Example from a fresh directory after creating Zheng Yiting at startup:

```text
profile create Guest
f 1 1
profile select 2
save first-game
profile select 2
profile rename 2 Guest New
profile select 1
load first-game
```

The first selection is blocked until the game is saved. The second starts a fresh game
for Guest. The final load restores Zheng Yiting's saved flag at row 1, column 1.

You cannot delete the active profile; select another first. Deletion removes the registry
entry but retains its data files. Recreating the same name creates a new identity and
does **not** recover those files. Statistics survive renaming/switching in the current
run but are not persisted across application restarts.

### Viewing help and the board

Enter `help` to list available commands and the current coordinate range. Enter
`board` to print the current board. Neither command changes the board or the selected
profile. Both commands take no additional arguments.

### Leaving the application

Enter `exit` to quit. The current session-aware exit command checks for unsaved
progress. If it warns, save the game or repeat `exit` without an intervening board
change to confirm discarding that progress. At the startup `profile>` prompt,
`exit` simply cancels startup because no game has begun.

Closing the terminal or input stream does not invoke this confirmation. Save explicitly
before closing the application that way.

### Saving a game: `save`

Saves your unfinished game so you can continue it later, including the time already spent. Each profile has its own saves.

Format: `save NAME` or `save`

* `NAME` has 1–30 letters, digits, `_` or `-`, with no spaces. It is not case-sensitive: `Trip` and `trip` are the same save.
* `save` on its own re-saves to the name this game was last saved to or loaded from.
* Saving to an existing name replaces that save.

Example:

`save trip`

Expected output:

```text
> save trip
Game saved as 'trip'.

> save
Overwrote save 'trip'.
```

These inputs are rejected and nothing is saved:

```text
> save
This game has not been saved yet. Usage: save NAME (letters, digits, '_' or '-'), or save to re-save this game.

> save ../x
Save names use 1-30 letters, digits, '_' or '-' (no spaces).

> save con
'con' is reserved by the system; choose another name.
```

A finished game cannot be saved, because there is nothing left to continue.

### Loading a game: `load`

Replaces the current game with a saved one and continues its timer.

Format: `load NAME`

Example:

`load trip`

Expected output:

```text
> load trip
Loaded save 'trip'.
Remaining Mines: 9
```

The saved board is then printed. If your current game has moves you have not saved, Jaweeper warns you first. Type the same `load` again to load anyway:

```text
> load trip
Your current game has unsaved progress that will be lost.
Use 'save NAME' to keep it first, or type 'load trip' again to load anyway.
> load trip
Loaded save 'trip'.
```

If the save cannot be loaded, your current game is not changed:

```text
> load nothing
Could not load 'nothing': No save named 'nothing'.
Your current game has not been changed.
```

### Where saves are kept

Saves are text files in `data/profiles/<profile ID>/saves/`, inside the folder you launched Jaweeper from. Each file ends with a checksum. If a save is edited by hand or damaged, Jaweeper refuses to load it:

```text
> load edited
Could not load 'edited': The save file was changed outside Jaweeper or is damaged (checksum mismatch).
Your current game has not been changed.
```

> **Caution:** Do not edit save files. Copy the whole `data` folder if you want to back up your saves.

### Unsaved progress when exiting

If your game has moves you have not saved, `exit` warns you instead of quitting. Type `exit` again to quit without saving:

```text
> exit
You have unsaved progress in this game.
Use 'save NAME' to keep it, or type 'exit' again to quit without saving.
> exit
Goodbye!
```

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

### Starting a custom game: `new custom`

Starts a new game with the number of rows, columns and mines you choose. The previous board is discarded and the undo history is cleared.

Format: `new custom ROWS COLS MINES`

* `ROWS` and `COLS` must be whole numbers from 5 to 50.
* `MINES` must be at least 1 and fewer than `ROWS` × `COLS`.

Example:

`new custom 5 6 3`

Expected output:

```text
> new custom 5 6 3
New custom game started (5x6, 3 mines).
Remaining Mines: 3
      1   2   3   4   5   6
    +---+---+---+---+---+---+
  1 | . | . | . | . | . | . |
```

The full board has 5 rows. These inputs are rejected, and the current board stays as it is:

```text
> new custom 4 6 3
Custom boards must be at least 5 by 5.

> new custom 5 51 3
Custom boards cannot exceed 50 by 50.

> new custom 5 5 25
Mines must be fewer than the number of cells.

> new custom 5 5 0
A custom board needs at least 1 mine.

> new custom 5 x 3
Rows, columns, and mines must be integers.

> new custom 5 6
Usage: new custom ROWS COLS MINES
```

### Undoing a move: `undo`

Takes back your last reveal, flag or unflag. You can type `undo` several times to step further back. The timer keeps running.

Format: `undo`

Example: flag two cells, then undo the second flag.

```text
> f 1 1
Cell (1, 1) flagged.
Remaining Mines: 2
...
> f 1 2
Cell (1, 2) flagged.
Remaining Mines: 1
...
> undo
Undid the last move.
Remaining Mines: 2
```

The board is printed again after each line marked `...`. When there is nothing left to undo:

```text
> undo
Nothing to undo.
```

`undo` takes no extra words:

```text
> undo please
Usage: undo
```

`new custom` and switching profiles clear the undo history.

### Revealing a cell: `r`

Opens a cell. A safe cell shows how many of its 8 neighbours hold mines, and a blank cell also opens the blank area around it. Revealing a mine ends the game. You win when every safe cell is revealed.

Format: `r ROW COL`

* `ROW` and `COL` start at 1. `ROW` is counted from the top and `COL` from the left.
* A flagged cell cannot be revealed. Jaweeper prints `Cell is flagged! Unflag it first.`; remove the flag with `u ROW COL`.

Example:

`r 3 3`

Expected output when the cell is safe (your numbers will differ):

```text
> r 3 3
Remaining Mines: 1
      1   2   3   4   5
    +---+---+---+---+---+
  1 | F |   |   | 1 | . |
    +---+---+---+---+---+
  2 |   |   |   | 1 | 1 |
```

Expected output when the last safe cell is revealed:

```text
========================
        VICTORY!        
========================
```

Expected output when the cell is a mine:

```text
> r 3 3
BOOM! You hit a mine at (3, 3).
...
========================
       GAME OVER        
========================
```

Once the game is over, `r`, `f`, `u` and `hint` print `The game is over. Type 'new' to start a new game.`

> **Tip:** Your first reveal of each game never hits a mine, unless you have turned this off with `settings safeclick off`.

### Flagging a cell: `f`

Marks a hidden cell where you think a mine is. A flagged cell cannot be revealed by mistake. `Remaining Mines` goes down by 1 for each flag.

Format: `f ROW COL`

Example:

`f 1 1`

Expected output:

```text
> f 1 1
Cell (1, 1) flagged.
Remaining Mines: 9
      1   2   3   4   5   6   7   8   9
    +---+---+---+---+---+---+---+---+---+
  1 | F | . | . | . | . | . | . | . | . |
```

A flag is only your guess. Jaweeper does not tell you whether it is correct until the game ends.

These inputs are rejected and the board is not changed:

```text
> f 1 1
Cell (1, 1) is already flagged.

> f 0 1
Coordinates must be from 1 to 9.

> f 1
Usage: f ROW COL (rows 1-9, columns 1-9).
```

Flagging a revealed cell prints `Cell (ROW, COL) is already revealed.`

### Removing a flag: `u`

Removes a flag you placed.

Format: `u ROW COL`

Example:

`u 1 1`

Expected output:

```text
> u 1 1
Cell (1, 1) unflagged.
Remaining Mines: 10
```

If the cell has no flag, Jaweeper prints `Cell (ROW, COL) is not flagged.`

### Seeing the mines after a loss

When you hit a mine, the board shows where every mine was and which flags were wrong:

```text
> r 3 3
BOOM! You hit a mine at (3, 3).
Remaining Mines: 1
      1   2   3   4   5
    +---+---+---+---+---+
  1 | X | . | . | . | * |
    +---+---+---+---+---+
  2 | . | . | . | . | . |
    +---+---+---+---+---+
  3 | . | . | * | . | . |
    +---+---+---+---+---+
  4 | . | . | . | . | . |
    +---+---+---+---+---+
  5 | F | . | . | . | . |
    +---+---+---+---+---+
========================
       GAME OVER        
========================
```

| Symbol | Meaning |
|---|---|
| `*` | A mine you did not flag |
| `X` | A flag on a cell that was safe |
| `F` | A flag on a mine (correct) |

`board` keeps showing this view until you start a new game.

### Changing settings: `settings`

Shows or changes the settings of the current profile. Settings are saved, so they stay the same the next time you choose this profile.

Format: `settings` or `settings safeclick on|off`

* `safeclick on` (default): the first reveal of each game is never a mine and always opens an area.
* `safeclick off`: the first reveal can hit a mine.

Example:

`settings safeclick off`

Expected output:

```text
> settings safeclick off
Safeclick is now OFF. It takes effect on the next first reveal.

> settings
Settings for Tester:
  safeclick  OFF  (your first reveal is never a mine)
Change with: settings safeclick on|off
```

The change applies to the next first reveal; a game you have already started is not affected. Any other input is rejected:

```text
> settings safeclick maybe
Usage: settings, or settings safeclick on|off
```

### Running several commands at once: `;`

Separate commands with `;` to run them one after another from a single line.

Format: `COMMAND; COMMAND; ...`

Example:

`f 1 1; f 1 2; u 1 1`

Expected output:

```text
> f 1 1; f 1 2; u 1 1
Cell (1, 1) flagged.
...
Cell (1, 2) flagged.
...
Cell (1, 1) unflagged.
Remaining Mines: 9
```

The board is printed after each line marked `...`.

* If one command fails, its error is shown and the rest still run. For example, `unknown; board` prints `Unknown command: unknown. Type help for usage.` and then the board.
* Each command uses the board as the earlier commands left it, so `new hard; f 16 30` works.
* If a command wins or loses the game, the rest of the line is skipped:

```text
========================
        VICTORY!        
========================
The game ended, so the remaining 2 commands on this line were skipped.
```

### Listing saved games: `saves`

Lists the current profile's saved games in alphabetical order.

Format: `saves`

Expected output:

```text
> saves
Saved games (1):
  trip
Use 'load NAME' to continue one, or 'delete NAME' to remove it.
```

If there are none: `No saved games yet. Use 'save NAME' to create one.`

### Deleting a saved game: `delete`

Permanently deletes a saved game. Type the same command twice to confirm.

Format: `delete NAME`

Example:

`delete trip`

Expected output:

```text
> delete trip
This will permanently delete save 'trip'.
Type 'delete trip' again to confirm.
> delete trip
Deleted save 'trip'.
```

If the save you deleted belonged to the game you are playing, Jaweeper adds `That was the save for your current game, which now counts as unsaved.` If no save has that name, it prints `No save named 'trip'. Type 'saves' to see your saved games.`

### Viewing your statistics: `stats`

Shows the current profile's wins, losses, win rate, streaks and fastest time. A win or loss is recorded when a game ends.

Format: `stats`

Example:

`stats`

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

Example:

`stats clear`

Expected output:

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

Example:

`leaderboard`

Expected output:

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

## FAQ

**Q**: Which board do I get if I type `new`?

**A**: The same board as `new easy`: 9 rows, 9 columns, and 10 mines. The message is `New easy game started.`

**Q**: Does `hint` reveal the cell for me?

**A**: No. It only prints a suggestion. Reveal that cell with `r ROW COL`, using the same row and column numbers.

**Q**: Can I undo after hitting a mine?

**A**: Yes. `undo` takes back the reveal that hit the mine, so you can keep playing that game. The loss is still counted in your statistics.

**Q**: How do I move my saved games to another computer?

**A**: Copy the whole `data` folder to the folder you launch Jaweeper from on the other computer. Then select the same profile.

**Q**: Why does `load` say my save was changed outside Jaweeper?

**A**: The file no longer matches its checksum, usually because it was edited or damaged. Jaweeper does not load it, to protect your current game.

**Q**: Do my statistics change when I rename my profile?

**A**: No. Renaming keeps your statistics. They are only reset by `stats clear` or by exiting Jaweeper.

**Q**: I flagged the wrong cell. What do I do?

**A**: Remove the flag with `u ROW COL`, or use `undo` if it was your last move.

**Q**: Can I put `;` inside a save name?

**A**: No. `;` always separates commands, and save names only allow letters, digits, `_` and `-`.

## Command Summary

| Command | What it does |
|---|---|
| `create NAME` / `select NUMBER` | Create/select a profile at the startup prompt. |
| `help` / `board` | Show command help / the current board. |
| `exit` | Quit; confirm first when warned about unsaved progress. |
| `profile list` | Show current profile numbers and the selected player. |
| `profile create NAME` | Create a profile without switching. |
| `profile select NUMBER` | Select a player after saving unfinished progress. |
| `profile rename NUMBER NAME` | Rename a profile without changing its identity. |
| `profile delete NUMBER [confirm]` | Warn first; add `confirm` to remove an inactive profile. |
| `save NAME` / `save` | Saves this game / re-saves it to the same name. |
| `load NAME` | Continues a saved game; repeat it to confirm when warned. |
| `new` | Starts an easy game. |
| `new easy` | Starts a 9×9 game with 10 mines. |
| `new medium` | Starts a 16×16 game with 40 mines. |
| `new hard` | Starts a 16×30 game with 99 mines. |
| `new custom ROWS COLS MINES` | Starts a custom game (5–50 rows and columns, at least 1 mine). |
| `hint` | Prints one suggested safe cell, or says that none can be deduced. |
| `undo` | Takes back your last reveal, flag or unflag. |
| `r ROW COL` | Reveals a cell. |
| `f ROW COL` | Flags a cell. |
| `u ROW COL` | Removes a flag. |
| `stats` | Shows your statistics. |
| `stats clear` | Resets your statistics. |
| `leaderboard` | Compares all profiles. |
| `settings` | Shows your settings. |
| `settings safeclick on\|off` | Makes your first reveal safe, or not. |
| `COMMAND; COMMAND` | Runs several commands from one line. |
| `saves` | Lists your saved games. |
| `delete NAME` | Deletes a saved game; type it twice to confirm. |
