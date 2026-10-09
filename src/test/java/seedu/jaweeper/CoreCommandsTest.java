package seedu.jaweeper;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Scanner;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests Zack's core commands, parser and application loop.
 */
public class CoreCommandsTest {

    private PrintStream originalOutput;
    private PrintStream capturedOutput;
    private ByteArrayOutputStream output;

    @BeforeEach
    public void setUp() {
        originalOutput = System.out;
        output = new ByteArrayOutputStream();
        capturedOutput = new PrintStream(output, true, StandardCharsets.UTF_8);
        System.setOut(capturedOutput);
    }

    @AfterEach
    public void tearDown() {
        System.setOut(originalOutput);
        capturedOutput.close();
    }

    @Test
    public void parse_coreCommands_correctTypes() {
        assertInstanceOf(HelpCommand.class, Parser.parse("help"));
        assertInstanceOf(ExitCommand.class, Parser.parse("exit"));
        assertInstanceOf(NewGameCommand.class, Parser.parse("new"));
        assertInstanceOf(PrintBoardCommand.class, Parser.parse("board"));
    }

    @Test
    public void parse_caseAndWhitespace_accepted() {
        assertInstanceOf(HelpCommand.class, Parser.parse("  HeLp\t"));
        assertInstanceOf(ExitCommand.class, Parser.parse("\tEXIT  "));
    }

    @Test
    public void parse_invalidCoreInput_rejected() {
        String[] invalidInputs = {
            "", "   ", "unknown", "help extra", "exit extra",
            "new extra", "board extra"
        };

        for (String input : invalidInputs) {
            assertThrows(IllegalArgumentException.class, () -> Parser.parse(input), input);
        }

        assertThrows(IllegalArgumentException.class, () -> Parser.parse(null));
    }

    @Test
    public void parse_coordinateCommands_zeroBasedIndices() {
        Map<String, BiFunction<Integer, Integer, Command>> commands = Map.of(
                "r", TestCoordinateCommand::new,
                "f", TestCoordinateCommand::new,
                "u", TestCoordinateCommand::new);

        for (String name : new String[]{"r", "f", "u"}) {
            TestCoordinateCommand command = assertInstanceOf(
                    TestCoordinateCommand.class,
                    Parser.parse(name + " 1 " + Board.SIZE, commands));

            assertEquals(0, command.row);
            assertEquals(Board.SIZE - 1, command.column);
        }

        TestCoordinateCommand command = assertInstanceOf(
                TestCoordinateCommand.class,
                Parser.parse("  R\t2   3  ", commands));

        assertEquals(1, command.row);
        assertEquals(2, command.column);
    }

    @Test
    public void parse_invalidCoordinates_rejected() {
        Map<String, BiFunction<Integer, Integer, Command>> commands =
                Map.of("r", TestCoordinateCommand::new);

        String[] invalidInputs = {
            "r", "r 1", "r 1 2 3", "r x 1", "r 1 2.5",
            "r 0 1", "r 1 0", "r -1 1",
            "r " + (Board.SIZE + 1) + " 1",
            "r 1 " + (Board.SIZE + 1),
            "r 99999999999999999999 1"
        };

        for (String input : invalidInputs) {
            assertThrows(IllegalArgumentException.class,
                    () -> Parser.parse(input, commands), input);
        }
    }

    @Test
    public void parse_unregisteredCommand_clearMessage() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> Parser.parse("r 1 1"));

        assertTrue(exception.getMessage().contains("not integrated"));
    }

    @Test
    public void execute_newGame_freshGrid() {
        Board board = new Board();
        Cell[][] previousGrid = board.grid;
        previousGrid[0][0].isFlagged = true;
        previousGrid[0][1].isRevealed = true;

        NewGameCommand command = new NewGameCommand();
        command.execute(board);

        assertNotSame(previousGrid, board.grid);
        assertFalse(command.isExit());

        int mineCount = 0;
        for (int row = 0; row < Board.SIZE; row++) {
            for (int column = 0; column < Board.SIZE; column++) {
                Cell cell = board.grid[row][column];
                assertNotSame(previousGrid[row][column], cell);
                assertFalse(cell.isFlagged);
                assertFalse(cell.isRevealed);
                if (cell.isMine) {
                    mineCount++;
                }
            }
        }

        assertEquals(Board.MINES, mineCount);
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("New game started."));
    }

    @Test
    public void execute_helpAndBoard_preserveGrid() {
        Board board = new Board();
        Cell[][] previousGrid = board.grid;
        board.grid[0][0].isFlagged = true;

        new HelpCommand().execute(board);
        new PrintBoardCommand().execute(board);

        assertSame(previousGrid, board.grid);
        assertTrue(board.grid[0][0].isFlagged);
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("r ROW COL"));
        assertFalse(new HelpCommand().isExit());
        assertFalse(new PrintBoardCommand().isExit());
    }

    @Test
    public void execute_exit_requestsTermination() {
        ExitCommand command = new ExitCommand();
        command.execute(new Board());

        assertTrue(command.isExit());
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("Goodbye!"));
    }

    @Test
    public void run_invalidInput_continuesUntilExit() {
        try (Scanner input = new Scanner("unknown\nhelp\nexit\nnew\n")) {
            Jaweeper.run(input, new Board(), Map.of());
        }

        String text = output.toString(StandardCharsets.UTF_8);
        assertTrue(text.contains("Unknown command"));
        assertTrue(text.contains("Jaweeper commands:"));
        assertTrue(text.contains("Goodbye!"));
        assertFalse(text.contains("New game started."));
    }

    @Test
    public void run_endOfInput_exitsGracefully() {
        try (Scanner input = new Scanner("")) {
            Jaweeper.run(input, new Board(), Map.of());
        }

        assertTrue(output.toString(StandardCharsets.UTF_8).contains("End of input."));
    }

    @Test
    public void run_registeredCommand_executesOnCurrentBoard() {
        Board board = new Board();
        Map<String, BiFunction<Integer, Integer, Command>> commands =
                Map.of("f", TestCoordinateCommand::new);

        try (Scanner input = new Scanner("f 2 3\nexit\n")) {
            Jaweeper.run(input, board, commands);
        }

        assertTrue(board.grid[1][2].isFlagged);
    }

    /**
     * A test-only command that records parsed coordinates and marks a cell.
     */
    private static class TestCoordinateCommand extends Command {

        private final int row;
        private final int column;

        private TestCoordinateCommand(int row, int column) {
            this.row = row;
            this.column = column;
        }

        @Override
        public void execute(Board board) {
            board.grid[row][column].isFlagged = true;
        }
    }
}
