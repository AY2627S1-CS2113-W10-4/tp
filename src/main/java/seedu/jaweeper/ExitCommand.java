package seedu.jaweeper;

/**
 * Requests application termination.
 */
public class ExitCommand extends Command {

    private final SaveTracker tracker;
    private boolean isAccepted;

    /**
     * Creates an exit command that always quits (used where no save tracking exists).
     */
    public ExitCommand() {
        this(null);
    }

    
    public ExitCommand(SaveTracker tracker) {
        this.tracker = tracker;
        this.isAccepted = tracker == null;
    }

    @Override
    public void execute(Board board) {
        if (tracker != null && !tracker.confirmDiscard(board, "exit")) {      // ← ADD
            isAccepted = false;                                                 // ← ADD
            System.out.println("You have unsaved progress in this game.");     // ← ADD
            System.out.println("Use 'save NAME' to keep it, or type 'exit' again to quit without saving."); // ← ADD
            return;                                                             // ← ADD
        }                                                                       // ← ADD
        isAccepted = true;                                                      // ← ADD
        System.out.println("Goodbye!");
    }


    

    @Override
    public boolean isExit() {
        return isAccepted;
    }
}
