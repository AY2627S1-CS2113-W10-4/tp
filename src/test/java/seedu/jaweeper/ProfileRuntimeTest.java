package seedu.jaweeper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks startup and session-aware command routing without changing real player files.
 */
public class ProfileRuntimeTest {

    @TempDir
    Path temporaryDirectory;

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
    public void startup_create_selectsAndPersistsProfile() throws IOException {
        ProfileManager manager = new ProfileManager(temporaryDirectory);
        try (Scanner input = new Scanner("create Zack New\nexit\n")) {
            assertTrue(ProfileStartup.selectProfile(input, manager));
            assertEquals("exit", input.nextLine());
        }
        Profile active = manager.getActiveProfile().orElseThrow();
        assertEquals("Zack New", active.getName());
        assertEquals("Zack New", new ProfileManager(temporaryDirectory).getProfile(active.getId()).getName());
    }

    @Test
    public void startup_selectExisting_usesDisplayedNumber() throws IOException {
        ProfileManager manager = new ProfileManager(temporaryDirectory);
        manager.createProfile("Zack");
        Profile other = manager.createProfile("Pranav");
        try (Scanner input = new Scanner(" SELECT 2 \n")) {
            assertTrue(ProfileStartup.selectProfile(input, manager));
        }
        assertEquals(other.getId(), manager.getActiveProfile().orElseThrow().getId());
    }

    @Test
    public void startup_invalidInputs_recoversWithoutCreatingDuplicate() throws IOException {
        ProfileManager manager = new ProfileManager(temporaryDirectory);
        Profile existing = manager.createProfile("Zack");
        try (Scanner input = new Scanner("\nselect zero\nselect 0\nselect 9\ncreate ZACK\ncreate ../x\nselect 1\n")) {
            assertTrue(ProfileStartup.selectProfile(input, manager));
        }
        assertEquals(existing.getId(), manager.getActiveProfile().orElseThrow().getId());
        assertEquals(1, manager.getProfiles().size());
    }

    @Test
    public void startup_exitOrEof_leavesRegistryUnchanged() throws IOException {
        ProfileManager manager = new ProfileManager(temporaryDirectory);
        for (String text : List.of("exit\n", "")) {
            try (Scanner input = new Scanner(text)) {
                assertFalse(ProfileStartup.selectProfile(input, manager));
            }
        }
        assertTrue(manager.getActiveProfile().isEmpty());
        assertFalse(Files.exists(temporaryDirectory.resolve("profiles.txt")));
    }

    @Test
    public void startup_writeFailure_reportsErrorAndAllowsExit() throws IOException {
        ProfileManager manager = new ProfileManager(temporaryDirectory);
        Path blocker = Files.createDirectory(temporaryDirectory.resolve("profiles.txt"));
        Files.writeString(blocker.resolve("keep.txt"), "keep");
        try (Scanner input = new Scanner("create Zack\nexit\n")) {
            assertFalse(ProfileStartup.selectProfile(input, manager));
        }
        assertTrue(manager.getProfiles().isEmpty());
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("Could not save the profile"));
    }

    @Test
    public void session_missingSelection_reportsError() throws IOException {
        GameSession session = new GameSession(new Board(), new ProfileManager(temporaryDirectory));
        assertThrows(IllegalStateException.class, session::getActiveProfile);
        assertThrows(IllegalStateException.class, session::getProfileDirectory);
    }

    @Test
    public void session_renameAndBoardReplacement_keepProfileDirectory() throws IOException {
        GameSession session = selectedSession();
        ProfileManager manager = session.getProfileManager();
        String id = session.getActiveProfile().getId();
        Path directory = session.getProfileDirectory();
        manager.renameProfile(id, "Zack New");
        Board replacement = new Board();
        session.replaceBoard(replacement);
        assertSame(replacement, session.getBoard());
        assertEquals("Zack New", session.getActiveProfile().getName());
        assertEquals(directory, session.getProfileDirectory());
        assertFalse(Files.exists(directory));
        assertThrows(NullPointerException.class, () -> session.replaceBoard(null));
        assertSame(replacement, session.getBoard());
    }

    @Test
    public void parser_featureFactory_receivesArgumentsWithoutChangingTheirCase() {
        Command expected = new HelpCommand();
        Map<String, Function<String[], Command>> features = Map.of("save", arguments -> {
            assertArrayEquals(new String[]{"MyGame", "Extra"}, arguments);
            return expected;
        });
        assertSame(expected, Parser.parse(" SaVe MyGame Extra ", Map.of(), features, 9, 9));
    }

    @Test
    public void parser_featureOverride_precedesBuiltIn() {
        Command expected = new HelpCommand();
        Map<String, Function<String[], Command>> features = Map.of("new", arguments -> {
            assertArrayEquals(new String[]{"hard"}, arguments);
            return expected;
        });
        assertSame(expected, Parser.parse("new hard", Map.of(), features, 9, 9));
        assertInstanceOf(NewGameCommand.class, Parser.parse("new", Map.of(), Map.of(), 9, 9));
    }

    @Test
    public void parser_nullFeatureResult_violatesInvariant() {
        Map<String, Function<String[], Command>> features = Map.of("save", arguments -> null);
        assertThrows(AssertionError.class, () -> Parser.parse("save", Map.of(), features, 9, 9));
    }

    @Test
    public void parser_rectangularBoard_validatesRowsAndColumnsSeparately() {
        Command expected = new HelpCommand();
        Map<String, BiFunction<Integer, Integer, Command>> coordinates = Map.of("r", (row, column) -> {
            assertEquals(2, row);
            assertEquals(11, column);
            return expected;
        });
        assertSame(expected, Parser.parse("r 3 12", coordinates, Map.of(), 3, 12));
        assertThrows(IllegalArgumentException.class,
                () -> Parser.parse("r 4 12", coordinates, Map.of(), 3, 12));
        assertThrows(IllegalArgumentException.class,
                () -> Parser.parse("r 3 13", coordinates, Map.of(), 3, 12));
        assertThrows(IllegalArgumentException.class,
                () -> Parser.parse("r 3 12", coordinates, Map.of(), 0, 12));
    }

    @Test
    public void run_featureCommands_shareActiveProfileAndBoard() throws IOException {
        GameSession session = selectedSession();
        AtomicInteger calls = new AtomicInteger();
        String id = session.getActiveProfile().getId();
        Map<String, Function<String[], Command>> features = Map.of("inspect", arguments -> action(board -> {
            assertSame(session.getBoard(), board);
            assertEquals(id, session.getActiveProfile().getId());
            calls.incrementAndGet();
        }));
        try (Scanner input = new Scanner("inspect\ninspect\nexit\n")) {
            Jaweeper.run(input, session, Map.of(), features);
        }
        assertEquals(2, calls.get());
    }

    @Test
    public void run_boardReplacement_isUsedByFollowingCommandsAndParser() throws IOException {
        GameSession session = selectedSession();
        Board replacement = new Board();
        replacement.grid = new Cell[3][12];
        AtomicInteger calls = new AtomicInteger();
        Map<String, Function<String[], Command>> features = Map.of(
                "load", arguments -> action(board -> session.replaceBoard(replacement)));
        Map<String, BiFunction<Integer, Integer, Command>> coordinates = Map.of("r", (row, column) -> action(board -> {
            assertSame(replacement, board);
            assertEquals(2, row);
            assertEquals(11, column);
            calls.incrementAndGet();
        }));
        try (Scanner input = new Scanner("load\nr 3 12\nr 4 12\nexit\n")) {
            Jaweeper.run(input, session, coordinates, features);
        }
        assertEquals(1, calls.get());
    }

    @Test
    public void run_invalidFeatureArgumentsAndExecution_recovers() throws IOException {
        GameSession session = selectedSession();
        Map<String, Function<String[], Command>> features = Map.of(
                "badargs", arguments -> {
                    throw new IllegalArgumentException("Bad feature arguments");
                },
                "badexecute", arguments -> action(board -> {
                    throw new IllegalArgumentException("Feature rejected operation");
                }));
        try (Scanner input = new Scanner("badargs\nbadexecute\nexit\n")) {
            Jaweeper.run(input, session, Map.of(), features);
        }
        String text = output.toString(StandardCharsets.UTF_8);
        assertTrue(text.contains("Bad feature arguments"));
        assertTrue(text.contains("Feature rejected operation"));
        assertTrue(text.contains("Goodbye!"));
    }

    @Test
    public void run_cancelledExit_doesNotEndLoop() throws IOException {
        GameSession session = selectedSession();
        AtomicInteger calls = new AtomicInteger();
        Map<String, Function<String[], Command>> features = Map.of(
                "exit", arguments -> action(board -> calls.incrementAndGet()));
        try (Scanner input = new Scanner("exit\nexit\n")) {
            Jaweeper.run(input, session, Map.of(), features);
        }
        assertEquals(2, calls.get());
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("End of input."));
    }

    /**
     * Creates a session selected through the same manager passed to feature commands.
     */
    private GameSession selectedSession() throws IOException {
        ProfileManager manager = new ProfileManager(temporaryDirectory);
        Profile profile = manager.createProfile("Zack");
        manager.selectProfile(profile.getId());
        return new GameSession(new Board(), manager);
    }

    /**
     * Adapts test behaviour to the unchanged legacy command contract.
     */
    private static Command action(Consumer<Board> action) {
        return new Command() {
            @Override
            public void execute(Board board) {
                action.accept(board);
            }
        };
    }
}
