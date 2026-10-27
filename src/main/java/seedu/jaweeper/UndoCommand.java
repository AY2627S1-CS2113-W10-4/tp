// @@author ProNoob116
package seedu.jaweeper;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Restores the board saved before the most recent reveal, flag, or unflag.
 */
public class UndoCommand extends Command {
    public static final String USAGE = "Usage: undo";

    private static final Logger logger = Logger.getLogger(UndoCommand.class.getName());

    private final GameSession session;

    /**
     * Creates an undo command for the given session. The session may be null in parser-only use.
     */
    public UndoCommand(GameSession session) {
        this.session = session;
    }

    /**
     * Creates an undo command, rejecting any extra arguments.
     *
     * @param arguments Tokens after the command name. This command takes none.
     * @param session The session whose board history is restored.
     * @return A command that pops one saved board.
     */
    public static UndoCommand parse(String[] arguments, GameSession session) {
        assert arguments != null : "Undo arguments must not be null";
        if (arguments.length != 0) {
            throw new IllegalArgumentException(USAGE);
        }
        return new UndoCommand(session);
    }

    /**
     * Pops one saved board and copies it onto the live board.
     *
     * @param board The board currently shown to the player.
     */
    @Override
    public void execute(Board board) {
        assert board != null : "Board must not be null";
        logger.log(Level.INFO, "Undo requested");
        if (session == null || !session.undoLastMove(board)) {
            logger.log(Level.INFO, "Undo rejected: history is empty");
            System.out.println("Nothing to undo.");
            return;
        }
        logger.log(Level.INFO, "Restored the previous board");
        System.out.println("Undid the last move.");
        board.printBoard();
    }
}
// @@author
