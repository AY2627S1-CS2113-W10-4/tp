package seedu.jaweeper;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.function.BiFunction;

/**
 * Runs the Jaweeper command-line application.
 */
public class Jaweeper {

    /**
     * Starts Jaweeper with a fresh board.
     *
     * @param args Command-line arguments, currently unused.
     */
    public static void main(String[] args) {
        try (Scanner input = new Scanner(System.in)) {
            run(input, new Board(), createCoordinateCommands());
        }
    }

    /**
     * Registers gameplay commands with zero-based row and column arguments.
     */
    private static Map<String, BiFunction<Integer, Integer, Command>> createCoordinateCommands() {
        Map<String, BiFunction<Integer, Integer, Command>> commands = new HashMap<>();

        // Enable each registration when the corresponding command class is available.
        // commands.put("r", RevealCommand::new);
        commands.put("f", FlagCommand::new);
        // commands.put("u", UnflagCommand::new);

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
        System.out.println("Welcome to Jaweeper! Type help for commands.");
        board.printBoard();

        while (true) {
            System.out.print("> ");

            if (!input.hasNextLine()) {
                System.out.println("End of input. Goodbye!");
                return;
            }

            Command command;
            try {
                command = Parser.parse(input.nextLine(), coordinateCommands);
            } catch (IllegalArgumentException exception) {
                System.out.println(exception.getMessage());
                continue;
            }

            command.execute(board);

            if (command.isExit()) {
                return;
            }
        }
    }
}
