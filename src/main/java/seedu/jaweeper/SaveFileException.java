package seedu.jaweeper;


public class SaveFileException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Creates an exception with a player-facing message.
     */
    public SaveFileException(String message) {
        super(message);
    }

    /**
     * Creates an exception with a player-facing message and its underlying cause.
     */
    public SaveFileException(String message, Throwable cause) {
        super(message, cause);
    }
}
