package seedu.jaweeper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * Tests listing and deleting saved games.
 */
public class SavesAndDeleteCommandTest extends SessionTestBase {

    @Test
    public void saves_emptyThenListedAlphabetically() {
        runScript("saves\nf 1 1; save zeta; save alpha\nsaves\nexit\nexit\n");
        String text = takeOutput();
        assertTrue(text.contains("No saved games yet."), text);
        assertTrue(text.contains("Saved games (2):"), text);
        assertTrue(text.indexOf("  alpha") < text.indexOf("  zeta"), text);
    }

    @Test
    public void delete_needsConfirmationThenRemovesSave() {
        runScript("f 1 1; save trip\ndelete trip\nsaves\ndelete trip\nsaves\nexit\nexit\n");
        String text = takeOutput();
        assertTrue(text.contains("Type 'delete trip' again to confirm."), text);
        assertTrue(text.contains("Saved games (1):"), text);
        assertTrue(text.contains("Deleted save 'trip'."), text);
        assertTrue(text.contains("No saved games yet."), text);
        assertFalse(Files.exists(session.getProfileDirectory().resolve("saves").resolve("trip.txt")));
    }

    @Test
    public void delete_afterConfirmationForAnotherName_needsFreshConfirmation() {
        runScript("f 1 1; save a; save b\ndelete a\ndelete b\nexit\nexit\n");
        Path saves = session.getProfileDirectory().resolve("saves");
        assertTrue(Files.exists(saves.resolve("a.txt")));
        assertTrue(Files.exists(saves.resolve("b.txt")));
    }

    @Test
    public void delete_missingSave_reportsAndDoesNotArmConfirmation() {
        runScript("delete nope\ndelete nope\nexit\n");
        String text = takeOutput();
        assertEquals(2, text.split("No save named 'nope'", -1).length - 1, text);
        assertFalse(text.contains("again to confirm"), text);
    }

    @Test
    public void delete_invalidName_rejected() {
        runScript("delete ../x\ndelete\ndelete a b\nexit\n");
        String text = takeOutput();
        assertTrue(text.contains("Save names use 1-30 letters"), text);
        assertTrue(text.contains("Usage: delete NAME"), text);
    }

    @Test
    public void delete_currentGamesSave_makesGameCountAsUnsaved() {
        runScript("f 1 1; save trip\ndelete trip\ndelete trip\nexit\nexit\n");
        String text = takeOutput();
        assertTrue(text.contains("now counts as unsaved"), text);
        assertTrue(text.contains("You have unsaved progress"), text);
        assertTrue(text.contains("Goodbye!"), text);
    }

    @Test
    public void delete_savedGameNotInPlay_keepsCurrentGameSaved() {
        runScript("f 1 1; save a\nsave b\ndelete a\ndelete a\nexit\n");
        String text = takeOutput();
        assertFalse(text.contains("now counts as unsaved"), text);
        assertTrue(text.contains("Goodbye!"), text);
    }
}
