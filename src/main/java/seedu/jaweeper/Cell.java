package seedu.jaweeper;

public class Cell {
    protected boolean isMine = false;
    protected boolean isRevealed = false;
    protected boolean isFlagged = false;
    protected int adjacentMines = 0;
    
    /**
     * Returns the string representation of the cell based on its current state.
     * 
     * @return A string icon representing the cell.
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

    /**
     * Checks if the current cell contains a mine.
     * 
     * @return true if the cell is a mine, false otherwise.
     */
    public boolean isMine() {
        return isMine;
    }

    /**
     * Checks if the current cell has been revealed by the player.
     * 
     * @return true if the cell is revealed, false otherwise.
     */
    public boolean isRevealed() {
        return isRevealed;
    }

    /**
     * Checks if the current cell has been flagged by the player.
     * 
     * @return true if the cell is flagged, false otherwise.
     */
    public boolean isFlagged() {
        return isFlagged;
    }
}
