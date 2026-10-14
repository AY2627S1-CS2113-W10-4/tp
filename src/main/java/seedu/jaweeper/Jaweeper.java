package seedu.jaweeper;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.function.BiFunction;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Runs the Jaweeper command-line application.
 */
public class Jaweeper {

    private static final Logger logger = Logger.getLogger(Jaweeper.class.getName());

    /**
     * Starts Jaweeper with a fresh board.
     *
     * @param args Command-line arguments, currently unused.
     */
    public static void main(String[] args) {
        logger.info("Starting Jaweeper application");
        try (Scanner input = new Scanner(System.in)) {
            run(input, new Board(), createCoordinateCommands());
        }
        logger.info("Exiting Jaweeper application");
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

        System.out.println("Welcome to Jaweeper! Type help for commands.");
        board.printBoard();
        logger.info("Game loop started successfully");

        while (true) {
            System.out.print("> ");

            if (!input.hasNextLine()) {
                System.out.println("End of input. Goodbye!");
                logger.info("Input stream ended, terminating game loop");
                return;
            }

            Command command;
            try {
                command = Parser.parse(input.nextLine(), coordinateCommands);
            } catch (IllegalArgumentException exception) {
                System.out.println(exception.getMessage());
                logger.warning("Invalid user input caught: " + exception.getMessage());
                continue;
            }

            assert command != null : "Parsed command should not be null";
            command.execute(board);

            if (command.isExit()) {
                logger.info("Exit command executed");
                return;
            }
        }
    }
}