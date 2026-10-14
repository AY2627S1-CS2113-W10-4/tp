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

        if(board.isGameOver()) {
            System.out.println("Game is already over. Start a new game to continue playing.");
            return;
        }
        if (row < 0 || row >= Board.SIZE || col < 0 || col >= Board.SIZE) {
            System.out.println("Coordinates are out of bounds.");
            return;
        }

        Cell target = board.grid[row][col];

        if (target.isFlagged) {
            System.out.println("Cell is flagged! Unflag it first.");
            return;
        }

        if (target.isMine) {
            target.isRevealed = true;
            board.setGameState(GameState.LOST);
            System.out.println("💥 BOOM! You hit a mine at (" + (row+1) + ", " + (col+1) + ").");
            System.out.println("========================");
            System.out.println("       GAME OVER        ");
            System.out.println("========================");
        } else {
            board.floodFill(row, col);
            board.printBoard();

            if (board.checkVictory()) {
                board.setGameState(GameState.WON);
                System.out.println("========================");
                System.out.println("        VICTORY!        ");
                System.out.println("========================");
            }
        }
    }
}
