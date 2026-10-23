package seedu.jaweeper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Scanner;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

/**
 * Shared setup for tests that run commands through a real session with a profile stored in a
 * temporary folder: captured console output, a selected profile, board helpers and a script runner.
 */
abstract class SessionTestBase {

    @TempDir
    Path temporaryDirectory;

    GameSession session;

    private PrintStream originalOutput;
    private ByteArrayOutputStream output;

    @BeforeEach
    public void setUp() throws IOException {
        originalOutput = System.out;
        output = new ByteArrayOutputStream();
        System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
        session = newSession(new Board());
    }

    @AfterEach
    public void tearDown() {
        System.setOut(originalOutput);
    }

    /**
     * Opens the profile registry in the temporary folder (creating one profile on first use),
     * selects that profile and wraps it with the given board in a fresh session. Calling this
     * again simulates restarting the application.
     */
    GameSession newSession(Board board) throws IOException {
        ProfileManager manager = new ProfileManager(temporaryDirectory);
        if (manager.getProfiles().isEmpty()) {
            manager.createProfile("Tester");
        }
        manager.selectProfile(manager.getProfiles().get(0).getId());
        return new GameSession(board, manager);
    }

    /** Returns and clears everything printed to System.out so far, with Unix line endings. */
    String takeOutput() {
        String text = output.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");
        output.reset();
        return text;
    }

    /** A 9x9 board whose only mines are at the given zero-based positions; first move still pending. */
    static Board boardWithMines(int... rowColPairs) {
        Board board = new Board();
        for (Cell[] row : board.grid) {
            for (Cell cell : row) {
                cell.isMine = false;
            }
        }
        for (int i = 0; i < rowColPairs.length; i += 2) {
            board.grid[rowColPairs[i]][rowColPairs[i + 1]].isMine = true;
        }
        return board;
    }

    /** Feeds the script to the application loop as if typed, one command line per line. */
    void runScript(String script) {
        try (Scanner input = new Scanner(script)) {
            Jaweeper.run(input, session, Jaweeper.createCoordinateCommands(session),
                    Jaweeper.createFeatureCommands(session));
        }
    }
}
