package seedu.jaweeper;

/**
 * Resets all recorded statistics for the currently active profile.
 */
public class StatsClearCommand extends Command {
    private final GameSession session;

    /**
     * Constructs a StatsClearCommand with access to the current game session.
     *
     * @param session The active game session.
     */
    public StatsClearCommand(GameSession session) {
        this.session = session;
    }

    /**
     * Resets the active profile's stats counters and completion history.
     *
     * @param board The current game board (unused by this command).
     */
    @Override
    public void execute(Board board) {
        Profile activeProfile = session.getActiveProfile();
        activeProfile.getStats().reset();
        System.out.println("Statistics have been successfully cleared for profile: " + activeProfile.getName());
    }
}
