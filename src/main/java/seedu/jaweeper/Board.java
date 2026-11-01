package seedu.jaweeper;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Board {
    // 1. Public fields first
    @Deprecated
    public static final int SIZE = 9;
    @Deprecated
    public static final int MINES = 10;

    private static final Logger logger = Logger.getLogger(Board.class.getName());

    public Cell[][] grid;

    // 2. Private fields next
    private int rows;
    private int cols;
    private int totalMines;
    private GameState gameState = GameState.IN_PROGRESS;
    private boolean isFirstMove = true;

    public Board() {
        this(Difficulty.EASY.rows, Difficulty.EASY.cols, Difficulty.EASY.mines);
    }

    public Board(int rows, int cols, int totalMines) {
        this.rows = rows;
        this.cols = cols;
        this.totalMines = totalMines;

        assert rows > 0 && cols > 0 : "Board dimensions must be positive";
        assert totalMines > 0 && totalMines < (rows * cols) : "Invalid number of mines";

        grid = new Cell[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                grid[i][j] = new Cell();
            }
        }
        placeMines();
        calculateAdjacencies();
        logger.info("New board initialized with " + totalMines + " mines.");
    }

    

    private Board(int rows, int cols, int totalMines, Cell[][] cells) {
        this.rows = rows;
        this.cols = cols;
        this.totalMines = totalMines;
        this.grid = cells;
        calculateAdjacencies();

        boolean hasRevealedCell = false;
        for (Cell[] row : cells) {
            for (Cell cell : row) {
                hasRevealedCell |= cell.isRevealed;
            }
        }
        // The safe-first-click rule still applies to a game saved before any reveal.
        this.isFirstMove = !hasRevealedCell;
        assert countMines() == totalMines : "Restored board must contain exactly " + totalMines + " mines";
    }

    
    static Board restore(int rows, int cols, int totalMines, Cell[][] cells) {
        assert cells != null && cells.length == rows : "Row count must match the saved grid";
        assert rows > 0 && cols > 0 && cells[0].length == cols : "Column count must match the saved grid";
        assert totalMines > 0 && totalMines < rows * cols : "Invalid number of mines";
        return new Board(rows, cols, totalMines, cells);
    }


    private void placeMines() {
        Random rand = new Random();
        int minesPlaced = 0;
        while (minesPlaced < totalMines) {
            int r = rand.nextInt(rows);
            int c = rand.nextInt(cols);
            if (!grid[r][c].isMine) {
                grid[r][c].isMine = true;
                minesPlaced++;
            }
        }
        assert countMines() == totalMines : "Board must contain exactly " + totalMines + " mines!";
    }

    public boolean isFirstMove() {
        return isFirstMove;
    }

    /**
     * Marks the first move as used without relocating any mines.
     * Called instead of {@link #ensureSafeFirstClick(int, int)} when the player has
     * turned the safeclick setting off, so the first reveal can hit a mine.
     */
    public void skipSafeFirstClick() {
        isFirstMove = false;
    }

    public void ensureSafeFirstClick(int row, int col) {
        assert row >= 0 && row < rows : "Row index out of bounds: " + row;
        assert col >= 0 && col < cols : "Column index out of bounds: " + col;

        if (!isFirstMove) {
            return;
        }
        isFirstMove = false;

        int minesBefore = countMines();

        // Clear the whole 3x3 block when there is room to move its mines elsewhere. A board too
        // dense for that (only possible from a hand-made save) falls back to clearing just the
        // clicked cell, instead of searching forever for free cells that do not exist.
        int radius = 1;
        if (freeCellsOutside(row, col, 1) < minesInside(row, col, 1)) {
            radius = 0;
        }

        List<int[]> freeCells = new ArrayList<>();
        int minesToMove = 0;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                boolean inside = isInArea(r, c, row, col, radius);
                if (inside && grid[r][c].isMine) {
                    grid[r][c].isMine = false;
                    minesToMove++;
                } else if (!inside && !grid[r][c].isMine) {
                    freeCells.add(new int[]{r, c});
                }
            }
        }
        assert freeCells.size() >= minesToMove : "There must be room for every relocated mine";

        Collections.shuffle(freeCells, new Random());
        for (int i = 0; i < minesToMove; i++) {
            int[] cell = freeCells.get(i);
            grid[cell[0]][cell[1]].isMine = true;
        }

        calculateAdjacencies();

        assert countMines() == minesBefore : "Moving mines must not change total mine count";
        assert !grid[row][col].isMine : "First click must be safe";
        assert radius == 0 || grid[row][col].adjacentMines == 0
                : "First click must open a blank area when the board has room";
    }

    private int minesInside(int clickRow, int clickCol, int radius) {
        int count = 0;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (isInArea(r, c, clickRow, clickCol, radius) && grid[r][c].isMine) {
                    count++;
                }
            }
        }
        return count;
    }

    private int freeCellsOutside(int clickRow, int clickCol, int radius) {
        int count = 0;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (!isInArea(r, c, clickRow, clickCol, radius) && !grid[r][c].isMine) {
                    count++;
                }
            }
        }
        return count;
    }

    private static boolean isInArea(int r, int c, int clickRow, int clickCol, int radius) {
        return Math.abs(r - clickRow) <= radius && Math.abs(c - clickCol) <= radius;
    }

    private int countMines() {
        int count = 0;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (grid[r][c].isMine) {
                    count++;
                }
            }
        }
        return count;
    }

    public void copyStateFrom(Board freshBoard) {
        assert freshBoard != null : "Board to copy from must not be null";
        this.rows = freshBoard.rows;
        this.cols = freshBoard.cols;
        this.totalMines = freshBoard.totalMines;
        this.gameState = freshBoard.gameState;
        this.isFirstMove = freshBoard.isFirstMove;

        // Perform a deep copy of the grid cells so test assertions match correctly
        this.grid = new Cell[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Cell source = freshBoard.grid[r][c];
                Cell copy = new Cell();
                copy.isMine = source.isMine;
                copy.isRevealed = source.isRevealed;
                copy.isFlagged = source.isFlagged;
                copy.adjacentMines = source.adjacentMines;
                this.grid[r][c] = copy;
            }
        }
    }

    // @@author ProNoob116
    /**
     * Restores a snapshot of this same game into the existing grid, cell by cell.
     * Unlike {@link #copyStateFrom(Board)}, the grid array keeps its identity, so features
     * that recognise the game by its grid (such as {@link SaveTracker}) still see the same game.
     * Falls back to a full copy if the snapshot has different dimensions.
     */
    public void restoreFrom(Board snapshot) {
        assert snapshot != null : "Snapshot to restore must not be null";
        if (snapshot.rows != rows || snapshot.cols != cols || grid.length != rows) {
            copyStateFrom(snapshot);
            return;
        }
        this.totalMines = snapshot.totalMines;
        this.gameState = snapshot.gameState;
        this.isFirstMove = snapshot.isFirstMove;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Cell source = snapshot.grid[r][c];
                Cell target = grid[r][c];
                target.isMine = source.isMine;
                target.isRevealed = source.isRevealed;
                target.isFlagged = source.isFlagged;
                target.adjacentMines = source.adjacentMines;
            }
        }
    }

    /**
     * Returns an independent copy of this board, including every cell.
     */
    public Board deepCopy() {
        return new Board(this);
    }

    /**
     * Builds an exact copy of {@code source} without generating a random board first.
     */
    private Board(Board source) {
        copyStateFrom(source);
    }
    // @@author

    private void calculateAdjacencies() {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (grid[i][j].isMine) {
                    continue;
                }

                int count = 0;
                for (int di = -1; di <= 1; di++) {
                    for (int dj = -1; dj <= 1; dj++) {
                        int ni = i + di;
                        int nj = j + dj;
                        if (ni >= 0 && ni < rows && nj >= 0 && nj < cols && grid[ni][nj].isMine) {
                            count++;
                        }
                    }
                }
                grid[i][j].adjacentMines = count;
                assert grid[i][j].adjacentMines >= 0 && grid[i][j].adjacentMines <= 8
                        : "Adjacent mines count out of range!";
            }
        }
    }

    /**
     * Reveals the cell and, if it has no adjacent mines, keeps opening its neighbours.
     * Uses an explicit stack rather than recursion, so large open areas (up to 99x99 boards
     * from save files) cannot overflow the call stack.
     */
    public void floodFill(int row, int col) {
        Deque<int[]> pending = new ArrayDeque<>();
        pending.push(new int[]{row, col});
        while (!pending.isEmpty()) {
            int[] position = pending.pop();
            int r = position[0];
            int c = position[1];
            if (r < 0 || r >= rows || c < 0 || c >= cols) {
                continue;
            }
            Cell currentCell = grid[r][c];
            if (currentCell.isRevealed || currentCell.isFlagged || currentCell.isMine) {
                continue;
            }

            currentCell.isRevealed = true;
            logger.finer("Revealed cell at (" + r + ", " + c + ")");

            if (currentCell.adjacentMines > 0) {
                continue;
            }
            for (int dr = -1; dr <= 1; dr++) {
                for (int dc = -1; dc <= 1; dc++) {
                    if (dr != 0 || dc != 0) {
                        pending.push(new int[]{r + dr, c + dc});
                    }
                }
            }
        }
    }

    public void printBoard() {
        // Flagging stops at the mine count, but a hand-edited save could hold extra flags.
        int remainingMines = Math.max(0, totalMines - countFlags());

        System.out.println("Remaining Mines: " + remainingMines);

        System.out.print("    ");
        for (int j = 0; j < cols; j++) {
            System.out.printf(" %2d ", (j + 1));
        }
        System.out.println();

        String divider = "    +" + "---+".repeat(cols);

        for (int i = 0; i < rows; i++) {
            System.out.println(divider);
            System.out.printf(" %2d |", (i + 1));
            for (int j = 0; j < cols; j++) {
                System.out.print(" " + iconFor(grid[i][j]) + " |");
            }
            System.out.println();
        }
        System.out.println(divider);
    }

    /**
     * Returns the symbol to draw for a cell. Once the game is lost, every mine the player
     * had not flagged is uncovered as '*', and a flag on a safe cell is marked 'X' so the
     * player can see which flags were wrong. The cells themselves are not modified, so
     * progress figures such as the completion percentage are unaffected.
     */
    private String iconFor(Cell cell) {
        if (gameState == GameState.LOST) {
            if (cell.isMine && !cell.isFlagged) {
                return "*";
            }
            if (cell.isFlagged && !cell.isMine) {
                return "X";
            }
        }
        return cell.getIcon();
    }

    public boolean checkVictory() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (!grid[r][c].isMine && !grid[r][c].isRevealed) {
                    return false;
                }
            }
        }
        return true;
    }

    public int countFlags() {
        int flagCount = 0;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (grid[i][j] != null && grid[i][j].isFlagged) {
                    flagCount++;
                }
            }
        }
        assert flagCount >= 0 : "Flag count cannot be negative";
        return flagCount;
    }

    public double calculateCompletionPercentage() {
        int revealedCount = 0;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (grid[i][j] != null && grid[i][j].isRevealed && !grid[i][j].isMine) {
                    revealedCount++;
                }
            }
        }
        int totalSafeCells = (rows * cols) - totalMines;
        if (totalSafeCells == 0) {
            return 100.0;
        }
        return ((double) revealedCount / totalSafeCells) * 100.0;
    }

    public void printGameStateSummary() {
        logger.log(Level.INFO, "Generating game state summary for user.");
        int revealedCount = 0;
        int flagCount = countFlags();

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                // A mine revealed on a loss is not a safe cell.
                if (grid[i][j] != null && grid[i][j].isRevealed && !grid[i][j].isMine) {
                    revealedCount++;
                }
            }
        }

        int totalSafeCells = (rows * cols) - totalMines;
        int remainingSafeCells = totalSafeCells - revealedCount;
        double completion = calculateCompletionPercentage();

        System.out.println("--- Game State Summary ---");
        System.out.println("Total Mines on Board : " + totalMines);
        System.out.println("Active Flags Placed  : " + flagCount);
        System.out.println("Safe Cells Revealed  : " + revealedCount + " / " + totalSafeCells);
        System.out.println("Safe Cells Remaining : " + remainingSafeCells);
        System.out.printf("Completion Progress  : %.2f%%\n", completion);
        System.out.println("--------------------------");
    }

    public GameState getGameState() {
        return gameState;
    }

    public void setGameState(GameState gameState) {
        this.gameState = gameState;
    }

    public boolean isGameOver() {
        return gameState == GameState.WON || gameState == GameState.LOST;
    }

    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }

    public int getTotalMines() {
        return totalMines;
    }
}
