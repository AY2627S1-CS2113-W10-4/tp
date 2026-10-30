package seedu.jaweeper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Runs the Jaweeper command-line application.
 */
public class Jaweeper {

    private static final Logger LOGGER = Logger.getLogger(Jaweeper.class.getName());

    /**
     * Selects a profile and starts Jaweeper with a fresh board.
     *
     * @param args Command-line arguments, currently unused.
     */
    public static void main(String[] args) {
        try (Scanner input = new Scanner(System.in)) {
            ProfileManager profiles = new ProfileManager(Path.of("data"));
            if (!ProfileStartup.selectProfile(input, profiles)) {
                return;
            }
            GameSession session = new GameSession(new Board(), profiles);
            run(input, session, createCoordinateCommands(session), createFeatureCommands(session));
        } catch (IOException exception) {
            LOGGER.log(Level.WARNING, "Could not load the profile registry", exception);
            System.out.println("Unable to open profiles: " + exception.getMessage());
            System.out.println("Check data/profiles.txt. No replacement registry has been created.");
        }
    }

    /**
     * Registers gameplay commands for legacy/test usage without a session.
     */
    static Map<String, BiFunction<Integer, Integer, Command>> createCoordinateCommands() {
        return createCoordinateCommands(null);
    }

    /**
     * Registers gameplay commands with zero-based row and column arguments.
     */
    static Map<String, BiFunction<Integer, Integer, Command>> createCoordinateCommands(GameSession session) {
        Map<String, BiFunction<Integer, Integer, Command>> commands = new HashMap<>();

        commands.put("r", (row, col) -> new RevealCommand(session, row, col));
        // @@author ProNoob116
        commands.put("f", (row, col) -> new FlagCommand(session, row, col));
        commands.put("u", (row, col) -> new UnflagCommand(session, row, col));
        // @@author

        return commands;
    }

    /**
     * Central integration point for feature factories sharing this session.
     * Register lowercase command names here once each feature implementation exists.
     * Do not construct another ProfileManager inside individual commands.
     */
    static Map<String, Function<String[], Command>> createFeatureCommands(GameSession session) {
        Map<String, Function<String[], Command>> commands = new HashMap<>();
        // Feature owners supply factories; Zheng Yiting coordinates registration here.
        // @@author ProNoob116
        commands.put("hint", HintCommand::create);
        commands.put("undo", args -> UndoCommand.parse(args, session));
        // @@author

        commands.put("stats", args -> {
            if (args.length == 1 && args[0].equalsIgnoreCase("clear")) {
                return new StatsClearCommand(session);
            } else if (args.length == 0) {
                return new StatsCommand(session);
            } else {
                throw new IllegalArgumentException("Usage: stats or stats clear");
            }
        });

        commands.put("leaderboard", args -> {
            if (args.length != 0) {
                throw new IllegalArgumentException("Usage: leaderboard");
            }
            return new LeaderboardCommand(session);
        });

        SaveTracker saveTracker = new SaveTracker();
        commands.put("save", args -> SaveCommand.parse(args, session, saveTracker));
        commands.put("load", args -> LoadCommand.parse(args, session, saveTracker));

        commands.put("settings", args -> SettingsCommand.parse(args, session));
        commands.put("saves", args -> {
            if (args.length != 0) {
                throw new IllegalArgumentException("Usage: saves");
            }
            return new SavesCommand(session);
        });
        // One shared confirmation, so the second 'delete NAME' is recognised as a repeat.
        DeleteCommand.Confirmation deleteConfirmation = new DeleteCommand.Confirmation();
        commands.put("delete", args -> DeleteCommand.parse(args, session, saveTracker, deleteConfirmation));
        commands.put("profile", args -> ProfileCommand.parse(args, session, saveTracker, deleteConfirmation));

        commands.put("exit", args -> {
            if (args.length != 0) {
                throw new IllegalArgumentException("Usage: exit");
            }
            return new ExitCommand(saveTracker);
        });

        commands.put("new", args -> {
            // @@author ProNoob116
            if (args.length > 0 && "custom".equalsIgnoreCase(args[0])) {
                return CustomGameCommand.parse(args, session);
            }
            // @@author
            Difficulty difficulty = Difficulty.EASY;
            if (args.length == 1) {
                String diffStr = args[0].toLowerCase(Locale.ROOT);
                switch (diffStr) {
                case "easy":
                    difficulty = Difficulty.EASY;
                    break;
                case "medium":
                    difficulty = Difficulty.MEDIUM;
                    break;
                case "hard":
                    difficulty = Difficulty.HARD;
                    break;
                default:
                    throw new IllegalArgumentException("Usage: new [easy|medium|hard]");
                }
            } else if (args.length > 1) {
                throw new IllegalArgumentException("Usage: new [easy|medium|hard]");
            }
            return new NewGameCommand(session, difficulty);
        });

        return commands;
    }

    /**
     * Executes commands until exit or the end of input.
     * The caller is responsible for closing the scanner.
     *
     * @param input The source of command lines.
     * @param board The current board.
     * @param coordinateCommands Registered gameplay command factories.
     */
    public static void run(Scanner input, Board board,
                           Map<String, BiFunction<Integer, Integer, Command>> coordinateCommands) {
        assert input != null : "Scanner input must not be null";
        assert board != null : "Board must not be null";
        assert coordinateCommands != null : "Coordinate commands map must not be null";

        runLoop(input, () -> board, line -> Parser.parse(line, coordinateCommands));
    }

    /**
     * Runs registered features with one shared session and the current board.
     * Command factories may capture the session in their constructors. Existing
     * execute(Board) commands remain compatible. The caller owns the scanner.
     */
    public static void run(Scanner input, GameSession session,
                           Map<String, BiFunction<Integer, Integer, Command>> coordinateCommands,
                           Map<String, Function<String[], Command>> featureCommands) {
        assert input != null : "Scanner input must not be null";
        assert session != null : "Game session must not be null";
        assert coordinateCommands != null : "Coordinate commands map must not be null";
        assert featureCommands != null : "Feature commands map must not be null";
        session.getActiveProfile();
        runLoop(input, session::getBoard, line -> Parser.parse(line, coordinateCommands,
                featureCommands, session.getRowCount(), session.getColumnCount()));
    }

    /**
     * Shares execution behaviour between the legacy test entry point and profile sessions.
     */
    private static void runLoop(Scanner input, Supplier<Board> currentBoard,
                                Function<String, Command> parseCommand) {
        LOGGER.info("Jaweeper session started");
        System.out.println("Welcome to Jaweeper! Type help for commands.");
        currentBoard.get().printBoard();

        while (true) {
            System.out.print("> ");

            if (!input.hasNextLine()) {
                LOGGER.info("Jaweeper session ended: end of input");
                System.out.println("End of input. Goodbye!");
                return;
            }

            if (runLine(input.nextLine(), currentBoard, parseCommand)) {
                return;
            }
        }
    }

    /**
     * Runs one input line, which may chain several commands separated by ';' (for example
     * {@code f 1 1; f 2 3}). Each command is parsed only when its turn comes, so it is checked
     * against the board as the earlier commands left it (for instance after {@code new hard}).
     * A command that fails prints its error and the rest of the line still runs. The one
     * exception is a game that ends partway through the line (a win or a loss): the remaining
     * commands are skipped with a single note instead of each complaining that the game is over.
     *
     * @return True if a command asked the application to exit; later commands are then skipped.
     */
    private static boolean runLine(String line, Supplier<Board> currentBoard,
                                   Function<String, Command> parseCommand) {
        List<String> segments = splitChain(line);
        for (int index = 0; index < segments.size(); index++) {
            String segment = segments.get(index);
            Command command;
            try {
                command = parseCommand.apply(segment);
            } catch (IllegalArgumentException exception) {
                LOGGER.fine("Rejected invalid command input");
                System.out.println(exception.getMessage());
                continue;
            }

            assert command != null : "Parsed command should not be null";

            LOGGER.log(Level.FINE, "Executing command: {0}", command.getClass().getSimpleName());
            boolean wasGameOver = currentBoard.get().isGameOver();
            try {
                command.execute(currentBoard.get());
            } catch (IllegalArgumentException exception) {
                LOGGER.fine("Rejected command execution");
                System.out.println(exception.getMessage());
                continue;
            }
            LOGGER.log(Level.FINE, "Completed command: {0}", command.getClass().getSimpleName());

            if (command.isExit()) {
                LOGGER.info("Jaweeper session ended: exit command");
                return true;
            }

            int remaining = segments.size() - index - 1;
            if (!wasGameOver && currentBoard.get().isGameOver() && remaining > 0) {
                System.out.println("The game ended, so the remaining " + remaining
                        + (remaining == 1 ? " command" : " commands") + " on this line "
                        + (remaining == 1 ? "was" : "were") + " skipped.");
                return false;
            }
        }
        return false;
    }

    /**
     * Splits a line on ';' into trimmed, non-empty commands, so "f 1 1;; f 2 3;" works.
     * A line with no commands at all is returned as-is so the parser reports "Enter a command".
     */
    static List<String> splitChain(String line) {
        List<String> commands = new ArrayList<>();
        for (String part : line.split(";")) {
            if (!part.isBlank()) {
                commands.add(part.strip());
            }
        }
        if (commands.isEmpty()) {
            commands.add(line);
        }
        return commands;
    }
}
