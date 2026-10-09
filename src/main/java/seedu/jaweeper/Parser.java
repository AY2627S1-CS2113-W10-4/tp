package seedu.jaweeper;

import java.util.Locale;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Converts user input into commands.
 */
public class Parser {

    /**
     * Parses a core command without gameplay command registrations.
     */
    public static Command parse(String input) {
        return parse(input, Map.of());
    }

    /**
     * Parses input using registered gameplay command factories.
     * Each factory receives a zero-based row and column, in that order.
     */
    public static Command parse(String input,
                                Map<String, BiFunction<Integer, Integer, Command>> coordinateCommands) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Enter a command. Type help for usage.");
        }

        String[] words = input.strip().split("\\s+");
        String name = words[0].toLowerCase(Locale.ROOT);

        switch (name) {
        case "help":
            requireNoArguments(words);
            return new HelpCommand();
        case "exit":
            requireNoArguments(words);
            return new ExitCommand();
        case "new":
            requireNoArguments(words);
            return new NewGameCommand();
        case "board":
            requireNoArguments(words);
            return new PrintBoardCommand();
        case "r":
        case "f":
        case "u":
            return parseCoordinates(name, words, coordinateCommands);
        default:
            throw new IllegalArgumentException(
                    "Unknown command: " + words[0] + ". Type help for usage.");
        }
    }

    /**
     * Rejects arguments supplied to a command that takes none.
     */
    private static void requireNoArguments(String[] words) {
        if (words.length != 1) {
            throw new IllegalArgumentException(
                    "Usage: " + words[0].toLowerCase(Locale.ROOT));
        }
    }

    /**
     * Checks coordinates and creates the corresponding gameplay command.
     */
    private static Command parseCoordinates(String name, String[] words,
                                            Map<String, BiFunction<Integer, Integer, Command>> coordinateCommands) {
        if (words.length != 3) {
            throw new IllegalArgumentException(
                    "Usage: " + name + " ROW COL (each from 1 to " + Board.SIZE + ").");
        }

        int row = parseCoordinate(words[1]);
        int column = parseCoordinate(words[2]);

        BiFunction<Integer, Integer, Command> factory = coordinateCommands.get(name);
        if (factory == null) {
            throw new IllegalArgumentException(
                    "Command '" + name + "' is not integrated in this build yet.");
        }

        return factory.apply(row, column);
    }

    /**
     * Converts a one-based coordinate into a zero-based array index.
     */
    private static int parseCoordinate(String value) {
        int coordinate;

        try {
            coordinate = Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Coordinates must be integers from 1 to " + Board.SIZE + ".");
        }

        if (coordinate < 1 || coordinate > Board.SIZE) {
            throw new IllegalArgumentException(
                    "Coordinates must be from 1 to " + Board.SIZE + ".");
        }

        return coordinate - 1;
    }
}
