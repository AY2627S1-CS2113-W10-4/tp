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
        Cell target = board.grid[row][col];
        if (!target.isRevealed) {
            target.isFlagged = true;
            System.out.println("Cell (" + (row+1) + ", " + (col+1) + ") flagged.");
        }
        board.printBoard();

        // Todo: Write a method in Board called board.checkVictory()
        // that loops through all cells. If all non-mine cells are revealed, print VICTORY!
    }
}

