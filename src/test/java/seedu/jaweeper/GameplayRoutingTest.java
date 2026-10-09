package seedu.jaweeper;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Scanner;
import java.util.function.BiFunction;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that the application routes input to the team's real commands.
 */
public class GameplayRoutingTest {

    private PrintStream originalOutput;
    private PrintStream capturedOutput;
    private ByteArrayOutputStream output;
    private Board board;
    private Map<String, BiFunction<Integer, Integer, Command>> commands;

    @BeforeEach
    public void setUp() {
        originalOutput = System.out;
        output = new ByteArrayOutputStream();
        capturedOutput = new PrintStream(output, true, StandardCharsets.UTF_8);
        System.setOut(capturedOutput);

        board = new Board();
        commands = Jaweeper.createCoordinateCommands();
    }

    @AfterEach
    public void tearDown() {
        System.setOut(originalOutput);
        capturedOutput.close();
    }

    @Test
    public void parse_gameplayInput_returnsRealCommands() {
        assertInstanceOf(RevealCommand.class, Parser.parse("r 1 1", commands));
        assertInstanceOf(FlagCommand.class, Parser.parse("f 2 3", commands));
        assertInstanceOf(UnflagCommand.class, Parser.parse("u 2 3", commands));
    }

    @Test
    public void execute_flagAndUnflag_usesCorrectCoordinates() {
        Parser.parse("f 2 3", commands).execute(board);

        assertTrue(board.grid[1][2].isFlagged);
        assertFalse(board.grid[2][1].isFlagged);

        Parser.parse("u 2 3", commands).execute(board);

        assertFalse(board.grid[1][2].isFlagged);
    }

    @Test
    public void run_flagRevealUnflag_routesCommandsInOrder() {
        try (Scanner input = new Scanner("f 2 3\nr 2 3\nu 2 3\nexit\n")) {
            Jaweeper.run(input, board, commands);
        }

        assertFalse(board.grid[1][2].isFlagged);
        assertFalse(board.grid[1][2].isRevealed);

        String text = output.toString(StandardCharsets.UTF_8);
        assertTrue(text.contains("Cell is flagged! Unflag it first."));
        assertTrue(text.contains("unflagged."));
        assertTrue(text.contains("Goodbye!"));
    }
}
