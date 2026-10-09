package seedu.jaweeper;

public class RevealCommand extends Command {
    private int row;
    private int col;

    public RevealCommand(int row, int col) {
        this.row = row;
        this.col = col;
    }

    @Override
    public void execute(Board board) {
        Cell target = board.grid[row][col];
        
        if (target.isFlagged) {
            System.out.println("Cell is flagged! Unflag it first.");
            return;
        }
        
        target.isRevealed = true;

        if (target.isMine) {
            System.out.println("💥 BOOM! You hit a mine at (" + (row+1) + ", " + (col+1) + ").");
            System.out.println("========================");
            System.out.println("       GAME OVER        ");
            System.out.println("========================");
            // Hint: figure out how to tell Jaweeper.java to end the game loop here
        } else {
            // Tell Matthew's flood-fill to run here!
            board.floodFill(row, col);
            board.printBoard();

            if (board.checkVictory()) {
                System.out.println("========================");
                System.out.println("        VICTORY!        ");
                System.out.println("========================");
            }
        }
    }
}
