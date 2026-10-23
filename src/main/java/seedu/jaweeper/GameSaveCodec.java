package seedu.jaweeper;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.CRC32;


public final class GameSaveCodec {

    static final String HEADER = "jaweeper-save-v1";
    static final int MAX_DIMENSION = 99;

    private static final String SIZE_KEY = "size";
    private static final String MINES_KEY = "mines";
    private static final String ELAPSED_KEY = "elapsed-ms";
    private static final String BOARD_MARKER = "board";
    private static final String CHECKSUM_KEY = "checksum";

    /** Number of lines before the grid: header, size, mines, elapsed time and board marker. */
    private static final int GRID_START = 5;

    private GameSaveCodec() {
    }

    /**
     * Serialises an unfinished board and the time spent on it.
     *
     * @param board The board to save; must be IN_PROGRESS.
     * @param elapsedMillis Time already spent on this game.
     * @return The complete file contents, one entry per line, ending with the checksum line.
     */
    public static List<String> encode(Board board, long elapsedMillis) {
        assert board != null : "Board must not be null";
        assert !board.isGameOver() : "Only unfinished games are saved";
        assert elapsedMillis >= 0 : "Elapsed time cannot be negative";

        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        lines.add(SIZE_KEY + " " + board.getRows() + " " + board.getCols());
        lines.add(MINES_KEY + " " + board.getTotalMines());
        lines.add(ELAPSED_KEY + " " + elapsedMillis);
        lines.add(BOARD_MARKER);
        lines.addAll(encodeGrid(board));
        lines.add(CHECKSUM_KEY + " " + checksum(lines));
        return lines;
    }

    /**
     * Returns only the grid rows of a board. Two boards with equal grid rows are in the same
     * position, which lets {@link SaveTracker} detect unsaved moves without hooking every command.
     */
    static List<String> encodeGrid(Board board) {
        List<String> rows = new ArrayList<>();
        for (int r = 0; r < board.getRows(); r++) {
            StringBuilder row = new StringBuilder();
            for (int c = 0; c < board.getCols(); c++) {
                row.append(toSymbol(board.grid[r][c]));
            }
            rows.add(row.toString());
        }
        return rows;
    }

    /**
     * Rebuilds a game from save-file lines after validating all of them.
     *
     * @param lines The file contents.
     * @return The restored board and elapsed time.
     * @throws SaveFileException If any line is missing, malformed, inconsistent or tampered with.
     */
    public static SavedGame decode(List<String> lines) throws SaveFileException {
        if (lines.isEmpty() || !HEADER.equals(lines.get(0))) {
            throw new SaveFileException("This is not a Jaweeper save file (line 1 should be '" + HEADER + "').");
        }
        verifyChecksum(lines);

        int[] size = readNumbers(lines, 1, SIZE_KEY, 2);
        int rows = size[0];
        int cols = size[1];
        requireRange(rows, 1, MAX_DIMENSION, "Row count", 2);
        requireRange(cols, 1, MAX_DIMENSION, "Column count", 2);

        int mines = readNumbers(lines, 2, MINES_KEY, 1)[0];
        requireRange(mines, 1, rows * cols - 1, "Mine count", 3);

        long elapsedMillis = readElapsed(lines);
        requireLine(lines, 4, BOARD_MARKER);

        int expectedLines = GRID_START + rows + 1;
        if (lines.size() != expectedLines) {
            throw new SaveFileException("Expected " + rows + " board rows for a " + rows + "x" + cols
                    + " board, but the file has " + (lines.size() - GRID_START - 1) + ".");
        }

        Cell[][] cells = readCells(lines, rows, cols);
        int mineCount = countMines(cells);
        if (mineCount != mines) {
            throw new SaveFileException("The board contains " + mineCount + " mines but line 3 says " + mines + ".");
        }

        Board board = Board.restore(rows, cols, mines, cells);
        verifyNumbers(lines, board);
        if (board.checkVictory()) {
            throw new SaveFileException("This saved game is already finished, so it cannot be continued.");
        }
        return new SavedGame(board, elapsedMillis);
    }

    private static char toSymbol(Cell cell) {
        if (cell.isFlagged) {
            return cell.isMine ? 'F' : 'f';
        }
        if (!cell.isRevealed) {
            return cell.isMine ? 'x' : '.';
        }
        assert !cell.isMine : "An unfinished game cannot contain a revealed mine";
        return (char) ('0' + cell.adjacentMines);
    }

    /**
     * Parses grid lines into cells. Digits only mark a cell as revealed here; their values
     * are compared with the recalculated adjacency counts in {@link #verifyNumbers}.
     */
    private static Cell[][] readCells(List<String> lines, int rows, int cols) throws SaveFileException {
        Cell[][] cells = new Cell[rows][cols];
        for (int r = 0; r < rows; r++) {
            int lineNumber = GRID_START + r + 1;
            String row = lines.get(GRID_START + r);
            if (row.length() != cols) {
                throw new SaveFileException("Line " + lineNumber + ": expected " + cols
                        + " cells but found " + row.length() + ".");
            }
            for (int c = 0; c < cols; c++) {
                cells[r][c] = fromSymbol(row.charAt(c), lineNumber, c + 1);
            }
        }
        return cells;
    }

    private static Cell fromSymbol(char symbol, int lineNumber, int column) throws SaveFileException {
        Cell cell = new Cell();
        switch (symbol) {
        case '.':
            break;
        case 'x':
            cell.isMine = true;
            break;
        case 'f':
            cell.isFlagged = true;
            break;
        case 'F':
            cell.isMine = true;
            cell.isFlagged = true;
            break;
        default:
            if (symbol < '0' || symbol > '8') {
                throw new SaveFileException("Line " + lineNumber + ", cell " + column
                        + ": unknown symbol '" + symbol + "'.");
            }
            cell.isRevealed = true;
            break;
        }
        return cell;
    }

    private static void verifyNumbers(List<String> lines, Board board) throws SaveFileException {
        for (int r = 0; r < board.getRows(); r++) {
            String row = lines.get(GRID_START + r);
            for (int c = 0; c < board.getCols(); c++) {
                Cell cell = board.grid[r][c];
                if (cell.isRevealed && row.charAt(c) - '0' != cell.adjacentMines) {
                    throw new SaveFileException("Line " + (GRID_START + r + 1) + ", cell " + (c + 1)
                            + ": shows " + row.charAt(c) + " but has " + cell.adjacentMines + " adjacent mines.");
                }
            }
        }
    }

    private static int countMines(Cell[][] cells) {
        int count = 0;
        for (Cell[] row : cells) {
            for (Cell cell : row) {
                if (cell.isMine) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Reads a line of the form "key n1 n2 ..." with exactly {@code count} non-negative integers.
     */
    private static int[] readNumbers(List<String> lines, int index, String key, int count)
            throws SaveFileException {
        String[] parts = requireLine(lines, index, null).split(" ", -1);
        if (parts.length != count + 1 || !parts[0].equals(key)) {
            throw new SaveFileException("Line " + (index + 1) + " should be '" + key + "' followed by "
                    + count + " number(s).");
        }
        int[] values = new int[count];
        for (int i = 0; i < count; i++) {
            values[i] = parseNonNegative(parts[i + 1], index + 1);
        }
        return values;
    }

    private static long readElapsed(List<String> lines) throws SaveFileException {
        String[] parts = requireLine(lines, 3, null).split(" ", -1);
        if (parts.length != 2 || !parts[0].equals(ELAPSED_KEY) || !parts[1].matches("\\d{1,15}")) {
            throw new SaveFileException("Line 4 should be '" + ELAPSED_KEY + "' followed by a whole number.");
        }
        return Long.parseLong(parts[1]);
    }

    private static int parseNonNegative(String text, int lineNumber) throws SaveFileException {
        // At most 9 digits keeps the value well inside int range; leading signs are rejected.
        if (!text.matches("\\d{1,9}")) {
            throw new SaveFileException("Line " + lineNumber + ": '" + text + "' is not a valid number.");
        }
        return Integer.parseInt(text);
    }

    private static void requireRange(int value, int min, int max, String what, int lineNumber)
            throws SaveFileException {
        if (value < min || value > max) {
            throw new SaveFileException("Line " + lineNumber + ": " + what + " must be from "
                    + min + " to " + max + ", but is " + value + ".");
        }
    }

    /**
     * Returns the line at {@code index}, optionally requiring it to equal {@code expected}.
     */
    private static String requireLine(List<String> lines, int index, String expected) throws SaveFileException {
        if (index >= lines.size()) {
            throw new SaveFileException("The save file ends early (line " + (index + 1) + " is missing).");
        }
        String line = lines.get(index);
        if (expected != null && !expected.equals(line)) {
            throw new SaveFileException("Line " + (index + 1) + " should be '" + expected + "'.");
        }
        return line;
    }

    private static void verifyChecksum(List<String> lines) throws SaveFileException {
        String last = lines.get(lines.size() - 1);
        if (lines.size() < 2 || !last.startsWith(CHECKSUM_KEY + " ")) {
            throw new SaveFileException("The save file is incomplete: its last line should be the checksum.");
        }
        String expected = checksum(lines.subList(0, lines.size() - 1));
        String actual = last.substring(CHECKSUM_KEY.length() + 1);
        if (!expected.equals(actual)) {
            throw new SaveFileException("The save file was changed outside Jaweeper or is damaged "
                    + "(checksum mismatch).");
        }
    }

    /**
     * Computes a CRC32 over the given lines, each terminated by a newline, as 8 lowercase hex digits.
     * CRC32 detects accidental damage and casual edits; it is not cryptographic protection.
     */
    static String checksum(List<String> lines) {
        CRC32 crc = new CRC32();
        for (String line : lines) {
            crc.update((line + "\n").getBytes(StandardCharsets.UTF_8));
        }
        return String.format(Locale.ROOT, "%08x", crc.getValue());
    }

    /**
     * A board restored from a file together with the time already spent on it.
     *
     * @param board The restored in-progress board.
     * @param elapsedMillis Time spent before the game was saved.
     */
    public record SavedGame(Board board, long elapsedMillis) {
    }
}
