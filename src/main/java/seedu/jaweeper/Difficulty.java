package seedu.jaweeper;

public enum Difficulty {
    EASY(9, 9, 10),
    MEDIUM(16, 16, 40),
    HARD(16, 30, 99);

    public final int rows;
    public final int cols;
    public final int mines;

    Difficulty(int rows, int cols, int mines) {
        this.rows = rows;
        this.cols = cols;
        this.mines = mines;
    }
}