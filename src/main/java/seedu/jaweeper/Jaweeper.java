package seedu.jaweeper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
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
            run(input, session, createCoordinateCommands(), createFeatureCommands(session));
        } catch (IOException exception) {
            LOGGER.log(Level.WARNING, "Could not load the profile registry", exception);
            System.out.println("Unable to open profiles: " + exception.getMessage());
            System.out.println("Check data/profiles.txt. No replacement registry has been created.");
        }
    }

    /**
     * Registers gameplay commands with zero-based row and column arguments.
     */
    static Map<String, BiFunction<Integer, Integer, Command>> createCoordinateCommands() {
        Map<String, BiFunction<Integer, Integer, Command>> commands = new HashMap<>();

        commands.put("r", RevealCommand::new);
        commands.put("f", FlagCommand::new);
        commands.put("u", UnflagCommand::new);

        return commands;
    }

    /**
     * Central integration point for feature factories sharing this session.
     * Register lowercase command names here once each feature implementation exists.
     * Do not construct another ProfileManager inside individual commands.
     */
    static Map<String, Function<String[], Command>> createFeatureCommands(GameSession session) {
        Map<String, Function<String[], Command>> commands = new HashMap<>();
        // Feature owners supply factories; Zack coordinates their registration here.
        // @@author ProNoob116
        commands.put("hint", HintCommand::create);
        // @@author
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

            Command command;
            try {
                command = parseCommand.apply(input.nextLine());
            } catch (IllegalArgumentException exception) {
                LOGGER.fine("Rejected invalid command input");
                System.out.println(exception.getMessage());
                continue;
            }

            assert command != null : "Parsed command should not be null";

            LOGGER.log(Level.FINE, "Executing command: {0}", command.getClass().getSimpleName());
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
                return;
            }
        }
    }
}
