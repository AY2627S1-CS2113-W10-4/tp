package seedu.jaweeper;

public class Cell {
    protected boolean isMine = false;
    protected boolean isRevealed = false;
    protected boolean isFlagged = false;
    protected int adjacentMines = 0;
    
    /**
     * Returns the string representation of the cell based on its current state.
     */
    public String getIcon() {
        if (isFlagged) {
            return "F";
        }
        if (!isRevealed) {
            return ".";
        }
        if (isMine) {
            return "*";
        }
        if (adjacentMines == 0) {
            return " ";
        }
        return String.valueOf(adjacentMines);
    }
}