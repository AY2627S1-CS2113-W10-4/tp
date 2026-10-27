package seedu.jaweeper;

/**
 * Displays the statistics (wins, losses, streaks, and completion times) for the active profile.
 */
public class StatsCommand extends Command {
    private final GameSession session;

    /**
     * Constructs a StatsCommand with access to the current game session.
     *
     * @param session The active game session.
     */
    public StatsCommand(GameSession session) {
        this.session = session;
    }

    /**
     * Retrieves and prints performance statistics for the active profile.
     *
     * @param board The current game board (unused by this command).
     */
    @Override
    public void execute(Board board) {
        Profile activeProfile = session.getActiveProfile();
        Stats stats = activeProfile.getStats();

        System.out.println("=== Statistics for: " + activeProfile.getName() + " ===");
        System.out.println("Wins: " + stats.getWins());
        System.out.println("Losses: " + stats.getLosses());
        System.out.println("Win Rate: " + String.format("%.1f", stats.getWinRate()) + "%");
        System.out.println("Current Streak: " + stats.getCurrentStreak());
        System.out.println("Max Streak: " + stats.getMaxStreak());

        long fastest = stats.getFastestTime();
        if (fastest == -1) {
            System.out.println("Fastest Time: N/A");
        } else {
            long seconds = (fastest / 1000) % 60;
            long minutes = (fastest / (1000 * 60)) % 60;
            System.out.println("Fastest Time: " + String.format("%02d:%02d", minutes, seconds));
        }
        System.out.println("=========================================");
    }
}
