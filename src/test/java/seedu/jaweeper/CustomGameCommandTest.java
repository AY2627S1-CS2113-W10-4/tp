// @@author ProNoob116
package seedu.jaweeper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests custom board sizes, including every rejected boundary.
 */
public class CustomGameCommandTest {

    @TempDir
    Path temporaryDirectory;

    private PrintStream originalOutput;
    private ByteArrayOutputStream output;

    @BeforeEach
    public void setUp() {
        originalOutput = System.out;
        output = new ByteArrayOutputStream();
        System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    public void tearDown() {
        System.setOut(originalOutput);
    }

    @ParameterizedTest
    @MethodSource("invalidCustomGames")
    public void parse_invalidCustomGame_rejected(String input, String expectedMessage) {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> Parser.parse(input));
        assertTrue(exception.getMessage().contains(expectedMessage), exception.getMessage());
    }

    @ParameterizedTest
    @MethodSource("validCustomGames")
    public void execute_validCustomGame_buildsRequestedBoard(int rows, int columns, int mines)
            throws IOException {
        CustomGameCommand command = CustomGameCommand.parse(
                new String[]{"custom", Integer.toString(rows), Integer.toString(columns), Integer.toString(mines)},
                null);
        Board board = new Board();
        command.execute(board);

        assertEquals(rows, board.getRows());
        assertEquals(columns, board.getCols());
        assertEquals(mines, countMines(board));
        assertTrue(output.toString(StandardCharsets.UTF_8).contains(
                "New custom game started (" + rows + "x" + columns + ", " + mines + " mines)."));
    }

    @Test
    public void parse_upperCaseCustom_accepted() {
        Command command = Parser.parse("NEW CUSTOM 8 9 4");
        CustomGameCommand custom = assertInstanceOf(CustomGameCommand.class, command);
        assertEquals(8, custom.getRows());
        assertEquals(9, custom.getColumns());
        assertEquals(4, custom.getMines());
    }

    @Test
    public void factory_customArguments_routesThroughSession() throws IOException {
        GameSession session = newSession();
        Command command = Jaweeper.createFeatureCommands(session)
                .get("new")
                .apply(new String[]{"custom", "6", "7", "3"});
        CustomGameCommand custom = assertInstanceOf(CustomGameCommand.class, command);
        custom.execute(session.getBoard());

        assertEquals(6, session.getBoard().getRows());
        assertEquals(7, session.getBoard().getCols());
        assertEquals(3, countMines(session.getBoard()));
    }

    @ParameterizedTest
    @MethodSource("invalidCustomArguments")
    public void factory_invalidCustomArguments_rejected(String[] arguments, String expectedMessage)
            throws IOException {
        GameSession session = newSession();
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> Jaweeper.createFeatureCommands(session).get("new").apply(arguments));
        assertTrue(exception.getMessage().contains(expectedMessage), exception.getMessage());
    }

    private static Stream<Arguments> invalidCustomGames() {
        return Stream.of(
                Arguments.of("new custom -1 10 5", "at least 5 by 5"),
                Arguments.of("new custom 10 -8 5", "at least 5 by 5"),
                Arguments.of("new custom 0 10 5", "at least 5 by 5"),
                Arguments.of("new custom 4 10 5", "at least 5 by 5"),
                Arguments.of("new custom 10 4 5", "at least 5 by 5"),
                Arguments.of("new custom 51 10 5", "cannot exceed 50 by 50"),
                Arguments.of("new custom 10 51 5", "cannot exceed 50 by 50"),
                Arguments.of("new custom 50 51 1", "cannot exceed 50 by 50"),
                Arguments.of("new custom 10 10 0", "at least 1 mine"),
                Arguments.of("new custom 10 10 -3", "at least 1 mine"),
                Arguments.of("new custom 5 5 25", "fewer than the number of cells"),
                Arguments.of("new custom 5 5 26", "fewer than the number of cells"),
                Arguments.of("new custom 10 10 100", "fewer than the number of cells"),
                Arguments.of("new custom x 5 1", "must be integers"),
                Arguments.of("new custom 5 y 1", "must be integers"),
                Arguments.of("new custom 5 5 z", "must be integers"),
                Arguments.of("new custom 5 5 1.5", "must be integers"),
                Arguments.of("new custom", "Usage: new custom"),
                Arguments.of("new custom 5 5", "Usage: new custom"),
                Arguments.of("new custom 5 5 1 2", "Usage: new custom")
        );
    }

    private static Stream<Arguments> validCustomGames() {
        return Stream.of(
                Arguments.of(5, 5, 1),
                Arguments.of(5, 5, 24),
                Arguments.of(6, 7, 3),
                Arguments.of(10, 12, 15),
                Arguments.of(50, 50, 1)
        );
    }

    private static Stream<Arguments> invalidCustomArguments() {
        return Stream.of(
                Arguments.of(new String[]{"custom", "-5", "10", "1"}, "at least 5 by 5"),
                Arguments.of(new String[]{"custom", "10", "100", "1"}, "cannot exceed 50 by 50"),
                Arguments.of(new String[]{"custom", "8", "8", "0"}, "at least 1 mine"),
                Arguments.of(new String[]{"custom", "8", "8", "64"}, "fewer than the number of cells"),
                Arguments.of(new String[]{"custom", "8", "8"}, "Usage: new custom")
        );
    }

    private GameSession newSession() throws IOException {
        return new GameSession(new Board(), new ProfileManager(temporaryDirectory));
    }

    private static int countMines(Board board) {
        int mines = 0;
        for (int row = 0; row < board.getRows(); row++) {
            for (int column = 0; column < board.getCols(); column++) {
                if (board.grid[row][column].isMine) {
                    mines++;
                }
            }
        }
        return mines;
    }
}
// @@author
