package seedu.jaweeper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Displays a ranked leaderboard of all profiles based on their win counts and win rates.
 */
public class LeaderboardCommand extends Command {
    private final GameSession session;

    /**
     * Constructs a LeaderboardCommand with access to the current game session.
     *
     * @param session The active game session containing profile and manager states.
     */
    public LeaderboardCommand(GameSession session) {
        this.session = session;
    }

    /**
     * Executes the leaderboard command, sorting and printing all profiles.
     *
     * @param board The current game board (unused by this command).
     */
    @Override
    public void execute(Board board) {
        // Wrap in ArrayList to make the list mutable for sorting
        List<Profile> profiles = new ArrayList<>(session.getProfileManager().getProfiles());

        profiles.sort(Comparator.comparingInt((Profile p) -> p.getStats().getWins()).reversed()
                .thenComparingDouble(p -> p.getStats().getWinRate()).reversed());

        System.out.println("================ LEADERBOARD ================");
        System.out.printf("%-4s | %-15s | %-6s | %-6s | %-8s\n", "Rank", "Profile", "Wins", "Losses", "Win Rate");
        System.out.println("---------------------------------------------");

        int rank = 1;
        for (Profile p : profiles) {
            System.out.printf("%-4d | %-15s | %-6d | %-6d | %.1f%%\n",
                    rank++,
                    p.getName(),
                    p.getStats().getWins(),
                    p.getStats().getLosses(),
                    p.getStats().getWinRate());
        }
        System.out.println("=============================================");
    }
}
