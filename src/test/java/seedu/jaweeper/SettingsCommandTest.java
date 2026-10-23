package seedu.jaweeper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests the settings command and the per-profile safeclick setting.
 */
public class SettingsCommandTest extends SessionTestBase {

    @Test
    public void safeClickOn_firstRevealOnMine_isMovedAndSafe() {
        session.replaceBoard(boardWithMines(0, 0));
        new RevealCommand(session, 0, 0).execute(session.getBoard());
        // With a single mine the safe opening may even win, so only "not lost" is guaranteed.
        assertNotEquals(GameState.LOST, session.getBoard().getGameState());
    }

    @Test
    public void safeClickOff_firstRevealOnMine_losesTheGame() throws IOException {
        session.setSafeClickEnabled(false);
        session.replaceBoard(boardWithMines(0, 0));
        new RevealCommand(session, 0, 0).execute(session.getBoard());
        assertEquals(GameState.LOST, session.getBoard().getGameState());
    }

    @Test
    public void settingsCommand_togglesAndPersistsPerProfile() throws IOException {
        assertTrue(session.isSafeClickEnabled(), "Default is on");
        runScript("settings safeclick off\nsettings\nexit\n");
        String text = takeOutput();
        assertTrue(text.contains("Safeclick is now OFF"), text);
        assertTrue(text.contains("safeclick  OFF"), text);

        GameSession reopened = newSession(new Board());
        assertFalse(reopened.isSafeClickEnabled(), "Setting must survive a restart");

        reopened.setSafeClickEnabled(true);
        assertTrue(newSession(new Board()).isSafeClickEnabled());
    }

    @Test
    public void settingsCommand_otherProfileKeepsItsOwnValue() throws IOException {
        session.setSafeClickEnabled(false);
        ProfileManager manager = session.getProfileManager();
        manager.selectProfile(manager.createProfile("Other").getId());
        assertTrue(session.isSafeClickEnabled(), "A different profile starts with the default");
    }

    @Test
    public void settingsCommand_badArguments_rejected() {
        var coordinateCommands = Jaweeper.createCoordinateCommands(session);
        var featureCommands = Jaweeper.createFeatureCommands(session);
        for (String line : List.of("settings safeclick", "settings safeclick maybe", "settings volume on",
                "settings safeclick on extra")) {
            assertThrows(IllegalArgumentException.class,
                () -> Parser.parse(line, coordinateCommands, featureCommands, 9, 9), line);
        }
    }

    @Test
    public void malformedSettingsFile_fallsBackToDefaultWithoutOverwriting() throws IOException {
        Path file = session.getProfileDirectory().resolve(ProfileSettings.FILE_NAME);
        Files.createDirectories(file.getParent());
        Files.writeString(file, "garbage\n");
        assertTrue(newSession(new Board()).isSafeClickEnabled());
        assertEquals("garbage\n", Files.readString(file));
    }
}
