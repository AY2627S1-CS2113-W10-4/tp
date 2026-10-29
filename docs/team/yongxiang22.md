Yong Xiang — Project Portfolio Page
Overview

Jaweeper is a Java command-line Minesweeper game with local player profiles, configurable boards and named saves. I implemented revealing cells and the win/loss game state, and built the save and load feature that lets a player pause an unfinished game and continue it later with the time already spent.

Summary of contributions
Code: RepoSense contribution view.
Reveal command: implemented RevealCommand (r ROW COL) in #9 and #17. A flagged cell cannot be revealed, revealing a mine ends the game, and revealing a safe cell hands over to the team's flood fill.
Game state: added GameState (IN_PROGRESS, WON, LOST) in #29, so reveal, flag and unflag are rejected once a game is over, and new puts the board back in progress. In the same PR, I restructured RevealCommand into separate checks for a finished game, out-of-bounds coordinates and flagged cells, each with its own message, and added assertions for the win and loss states.
Save and load: implemented save NAME, load NAME and the unsaved-progress warning on exit in #42.
SaveStore owns save names and the per-profile folder data/profiles/<UUID>/saves/. Names are lowercased and restricted to letters, digits, _ and -, which prevents path traversal (../) and Windows-reserved names such as con.
GameSaveCodec writes a readable text format with a CRC32 checksum. On load it validates every line (header, checksum, sizes, mine count, symbols, revealed numbers) and rejects bad files with a message naming the line, without touching the current game.
SaveTracker detects unsaved progress by comparing the board with a snapshot taken at the last save or load, so no other command had to be changed. exit and load warn once and go ahead only when the same command is repeated.
Added small hooks to existing classes: Board.restore rebuilds a board without random mines, and GameTimer.resumeFrom continues a saved game's timer.
Testing: added RevealCommandTest in #20. For save/load, added GameSaveCodecTest (round trips including a random 16×30 board, checksum tampering, truncation, and each content check using files with a valid checksum) and SaveLoadCommandsTest (name validation, re-saving, refusing finished games, keeping the current game after a bad load, both confirmation flows, and a full run through Jaweeper.run). All file tests use JUnit temporary folders.
Developer Guide: wrote the save and load sections in #42: the save-file format, the validation order, the unsaved-progress rule and the design alternatives. Updated them to the AB3 format with a class, object and sequence diagram for both saving and loading, plus an activity diagram for the unsaved-progress check.
Team work: the shared SaveTracker and SaveStore are reused by teammates' features: runtime profile switching checks hasUnsavedChanges, and saves/delete NAME use SaveStore.list() and SaveStore.delete(name).
Documentation extracts

My Developer Guide sections are Saving a game and Loading a game.

The save class diagram, save object diagram and save sequence diagram show how save trip writes a file and marks the game as saved only after a successful write.
The load class diagram, load object diagram, load sequence diagram and unsaved-progress activity diagram show why a bad file or an unconfirmed load never changes the current game.