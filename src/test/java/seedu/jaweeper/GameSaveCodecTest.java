package seedu.jaweeper;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the save-file format: round trips and rejection of corrupted or hand-edited files.
 */
public class GameSaveCodecTest {

    /** A 3x4 board with mines at (1,4) and (3,2); some cells revealed, one safe cell and one mine flagged. */
    private static final List<String> GRID = List.of(
            "00.x",
            "11f.",
            "1F..");

    /**
     * Builds a complete, correctly checksummed save file around the given grid lines.
     */
    static List<String> fileWith(String size, String mines, List<String> grid) {
        List<String> lines = new ArrayList<>();
        lines.add(GameSaveCodec.HEADER);
        lines.add("size " + size);
        lines.add("mines " + mines);
        lines.add("elapsed-ms 52340");
        lines.add("board");
        lines.addAll(grid);
        lines.add("checksum " + GameSaveCodec.checksum(lines));
        return lines;
    }

    private static List<String> validFile() {
        return fileWith("3 4", "2", GRID);
    }

    private static void assertRejected(List<String> lines, String expectedMessagePart) {
        SaveFileException exception = assertThrows(SaveFileException.class, () -> GameSaveCodec.decode(lines));
        assertTrue(exception.getMessage().contains(expectedMessagePart),
                "Message was: " + exception.getMessage());
    }

    @Test
    public void decode_validFile_restoresBoardAndTime() throws SaveFileException {
        GameSaveCodec.SavedGame saved = GameSaveCodec.decode(validFile());
        Board board = saved.board();

        assertEquals(52340, saved.elapsedMillis());
        assertEquals(3, board.getRows());
        assertEquals(4, board.getCols());
        assertEquals(2, board.getTotalMines());
        assertEquals(GameState.IN_PROGRESS, board.getGameState());
        assertTrue(board.grid[0][3].isMine());
        assertTrue(board.grid[2][1].isMine() && board.grid[2][1].isFlagged());
        assertTrue(board.grid[1][2].isFlagged() && !board.grid[1][2].isMine());
        assertTrue(board.grid[0][0].isRevealed());
        assertFalse(board.isFirstMove(), "A board with revealed cells is past its first move");
    }

    @Test
    public void encode_thenDecode_givesSameFile() throws SaveFileException {
        List<String> original = validFile();
        GameSaveCodec.SavedGame saved = GameSaveCodec.decode(original);
        assertEquals(original, GameSaveCodec.encode(saved.board(), saved.elapsedMillis()));
    }

    @Test
    public void encode_freshRandomBoard_roundTrips() throws SaveFileException {
        Board board = new Board(16, 30, 99);
        board.grid[0][0].isFlagged = true;
        List<String> lines = GameSaveCodec.encode(board, 0);

        Board restored = GameSaveCodec.decode(lines).board();
        assertEquals(GameSaveCodec.encodeGrid(board), GameSaveCodec.encodeGrid(restored));
        assertTrue(restored.isFirstMove(), "Safe-first-click must still apply before any reveal");
    }

    @Test
    public void decode_editedCell_rejectedByChecksum() {
        List<String> lines = new ArrayList<>(validFile());
        lines.set(5, "00.."); // remove a mine without updating the checksum
        assertRejected(lines, "checksum");
    }

    @Test
    public void decode_missingOrWrongHeader_rejected() {
        assertRejected(List.of(), "not a Jaweeper save file");
        List<String> lines = new ArrayList<>(validFile());
        lines.set(0, "jaweeper-save-v9");
        assertRejected(lines, "not a Jaweeper save file");
    }

    @Test
    public void decode_truncatedFile_rejected() {
        List<String> lines = validFile().subList(0, 6);
        assertRejected(lines, "checksum");
    }

    @Test
    public void decode_inconsistentContents_rejectedEvenWithValidChecksum() {
        // Each file has a correct checksum, so only the content checks can catch the problem.
        assertRejected(fileWith("3 4", "3", GRID), "contains 2 mines");
        assertRejected(fileWith("3 5", "2", GRID), "expected 5 cells");
        assertRejected(fileWith("4 4", "2", GRID), "Expected 4 board rows");
        assertRejected(fileWith("3 4", "2", List.of("00.x", "11f?", "1F..")), "unknown symbol '?'");
        assertRejected(fileWith("3 4", "2", List.of("05.x", "11f.", "1F..")), "shows 5 but has 0");
        assertRejected(fileWith("0 4", "2", GRID), "Row count must be from 1");
        assertRejected(fileWith("3 4", "12", GRID), "Mine count must be from 1 to 11");
        assertRejected(fileWith("3 -4", "2", GRID), "not a valid number");
        assertRejected(fileWith("3", "2", GRID), "should be 'size'");
    }

    @Test
    public void decode_finishedGame_rejected() {
        List<String> allSafeRevealed = List.of("001x", "1121", "1x10");
        assertRejected(fileWith("3 4", "2", allSafeRevealed), "already finished");
    }
}
