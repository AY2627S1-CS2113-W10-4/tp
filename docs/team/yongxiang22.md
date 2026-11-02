# Yong Xiang's Project Portfolio Page

### Project: Jaweeper

Jaweeper is a command-line Minesweeper game used to practise software engineering in CS2113. The user plays by typing commands to reveal and flag cells, choose a board size, keep a profile with statistics, and save and resume games. It is written in Java, and has about 4 kLoC.

Given below are my contributions to the project.

* **New Feature**: Added the ability to save a game and load it later (`save NAME`, `load NAME`).
    * What it does: saves the unfinished game, including the time already spent, under a name in the player's own saves folder. `load NAME` replaces the current game with a saved one and continues its timer, and a plain `save` re-saves to the name the game came from. If the current game has unsaved progress, `load` and `exit` warn first and only go ahead when the same command is typed again.
    * Justification: This feature improves the product significantly because a game, especially on a hard or large custom board, can take longer than one sitting. Without it, closing the app loses the game in progress.
    * Highlights: This enhancement affects existing commands and features added later. `exit`, `help`, the board and the timer were extended for it, and teammates later reused its parts: profile switching checks for unsaved progress with `SaveTracker`, and `saves` and `delete NAME` use `SaveStore`. It required an in-depth analysis of design alternatives, such as detecting unsaved progress by comparing the board with a snapshot instead of making every command set a "dirty" flag. The implementation too was challenging because a save file comes from outside the program. Every line is validated (header, checksum, sizes, mine count, symbols, and each revealed number against the recomputed board) before anything changes, so a bad file is rejected with a message naming the line and never destroys the current game. Save names allow only letters, digits, `_` and `-`, which blocks path traversal (`../`) and Windows-reserved names such as `con`.
    * Credits: The save-file checksum uses Java's built-in `java.util.zip.CRC32`.

* **New Feature**: Added revealing cells (`r ROW COL`) and the win/loss game state.
    * What it does: reveals the chosen cell. Revealing a mine ends the game, and revealing a safe cell hands over to the team's flood fill. `GameState` (`IN_PROGRESS`, `WON`, `LOST`) makes reveal, flag and unflag refuse to act once a game is over, and `new` puts the board back in progress.
    * Highlights: `RevealCommand` checks for a finished game, out-of-bounds coordinates and a flagged cell separately, each with its own message, and uses assertions and logging for the win and loss states.

* **Code contributed**: [RepoSense link](https://nus-cs2113-ay2627-s1.github.io/tp-dashboard/?search=yongxiang22)

* **Enhancements to existing features**:
    * Added `Board.restore`, which rebuilds a board from saved cells without placing random mines, and `GameTimer.resumeFrom`, which continues a saved game's timer (Pull request [\#42](https://github.com/AY2627S1-CS2113-W10-4/tp/pull/42))
    * Restructured `RevealCommand` with assertions, exceptions and logging (Pull request [\#29](https://github.com/AY2627S1-CS2113-W10-4/tp/pull/29))
    * Wrote 32 JUnit tests: 10 in `RevealCommandTest`, and 22 for saving and loading in `GameSaveCodecTest` and `SaveLoadCommandsTest`. They cover a round trip of a random 16×30 board, checksum tampering, truncated files and a full run through the application loop, using JUnit temporary folders (Pull requests [\#20](https://github.com/AY2627S1-CS2113-W10-4/tp/pull/20), [\#42](https://github.com/AY2627S1-CS2113-W10-4/tp/pull/42))

* **Documentation**:
    * User Guide:
        * Added documentation for the features `r`, `save` and `load`, where saves are kept, and the unsaved-progress warning on exit [\#58](https://github.com/AY2627S1-CS2113-W10-4/tp/pull/58)
    * Developer Guide:
        * Added the "Saving a game" and "Loading a game" sections: the save-file format, the validation order, the unsaved-progress rule and the design alternatives [\#42](https://github.com/AY2627S1-CS2113-W10-4/tp/pull/42)
        * Added class, object and sequence diagrams for saving and loading, and an activity diagram for the unsaved-progress check
        * Restructured the Developer Guide into the AB3 format and added its UML diagrams ([d93fc74](https://github.com/AY2627S1-CS2113-W10-4/tp/commit/d93fc746eb64b0ff013562f4bb38216d2bcf860b), [2e1b57c](https://github.com/AY2627S1-CS2113-W10-4/tp/commit/2e1b57c477da547ce0b038438bfe88308099c184))
    * About Us: added my entry [\#9](https://github.com/AY2627S1-CS2113-W10-4/tp/pull/9)
