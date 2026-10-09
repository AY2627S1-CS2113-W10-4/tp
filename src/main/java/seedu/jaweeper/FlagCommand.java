package seedu.jaweeper;

public class FlagCommand extends Command {
    private int row;
    private int col;

    public FlagCommand(int row, int col) {
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
        if (target.isRevealed) {
            System.out.println("Cell (" + (row + 1) + ", " + (col + 1) + ") is already revealed.");
        } else if (target.isFlagged) {
            System.out.println("Cell (" + (row + 1) + ", " + (col + 1) + ") is already flagged.");
        } else {
            target.isFlagged = true;
            System.out.println("Cell (" + (row + 1) + ", " + (col + 1) + ") flagged.");
        }
        board.printBoard();
        // Todo: Write a method in Board called board.checkVictory()
        // that loops through all cells. If all non-mine cells are revealed, print VICTORY!
    }
}

