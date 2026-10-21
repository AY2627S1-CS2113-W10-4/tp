package seedu.jaweeper;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

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
        return parse(input, coordinateCommands, Map.of(), Board.SIZE, Board.SIZE);
    }

    /**
     * Parses commands using feature factories and the current board dimensions.
     * Registry keys must be lowercase. A feature factory receives argument tokens
     * without the command name; their original case is preserved. Feature factories
     * take precedence over built-ins, allowing extensions such as new easy.
     * Quoted arguments and semicolon-separated commands are not handled here.
     */
    public static Command parse(String input,
                                Map<String, BiFunction<Integer, Integer, Command>> coordinateCommands,
                                Map<String, Function<String[], Command>> featureCommands,
                                int rows, int columns) {
        if (rows <= 0 || columns <= 0) {
            throw new IllegalArgumentException("Board dimensions must be positive.");
        }
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Enter a command. Type help for usage.");
        }

        String[] words = input.strip().split("\\s+");
        String name = words[0].toLowerCase(Locale.ROOT);

        Function<String[], Command> factory = featureCommands.get(name);
        if (factory != null) {
            Command command = factory.apply(Arrays.copyOfRange(words, 1, words.length));
            assert command != null : "Registered feature factory must not return null";
            return command;
        }

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
            return parseCoordinates(name, words, coordinateCommands, rows, columns);
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
                                            Map<String, BiFunction<Integer, Integer, Command>> coordinateCommands,
                                            int rows, int columns) {
        if (words.length != 3) {
            throw new IllegalArgumentException(
                    "Usage: " + name + " ROW COL (rows 1-" + rows + ", columns 1-" + columns + ").");
        }

        int row = parseCoordinate(words[1], rows);
        int column = parseCoordinate(words[2], columns);

        // User input has already been validated and converted to array indices.
        assert row >= 0 && row < rows : "Validated row must be within the board";
        assert column >= 0 && column < columns : "Validated column must be within the board";

        BiFunction<Integer, Integer, Command> factory = coordinateCommands.get(name);
        if (factory == null) {
            throw new IllegalArgumentException(
                    "Command '" + name + "' is not integrated in this build yet.");
        }

        Command command = factory.apply(row, column);
        assert command != null : "Registered command factory must not return null";
        return command;
    }

    /**
     * Converts a one-based coordinate into a zero-based array index.
     */
    private static int parseCoordinate(String value, int limit) {
        int coordinate;

        try {
            coordinate = Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Coordinates must be integers from 1 to " + limit + ".");
        }

        if (coordinate < 1 || coordinate > limit) {
            throw new IllegalArgumentException(
                    "Coordinates must be from 1 to " + limit + ".");
        }

        return coordinate - 1;
    }
}
