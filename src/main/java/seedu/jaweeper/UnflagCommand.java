package seedu.jaweeper;

public class UnflagCommand extends Command {
    private final int row;
    private final int col;

    public UnflagCommand(int row, int col) {
        this.row = row;
        this.col = col;
    }

    @Override
    public void execute(Board board) {
        if (row < 0 || row >= board.grid.length
                || col < 0 || col >= board.grid[0].length) {
            System.out.println("Cell (" + (row + 1) + ", " + (col + 1) + ") is out of bounds.");
            return;
        }

        Cell target = board.grid[row][col];
        if (!target.isFlagged) {
            System.out.println("Cell (" + (row + 1) + ", " + (col + 1) + ") is not flagged.");
        } else {
            target.isFlagged = false;
            System.out.println("Cell (" + (row + 1) + ", " + (col + 1) + ") unflagged.");
        }
        board.printBoard();
    }
}
