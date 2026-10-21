package seedu.jaweeper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * Provides UTF-8 text file operations without knowing any game-specific format.
 */
public final class Storage {

    private Storage() {
    }

    /**
     * Reads a text file. Missing or unreadable files are reported to the caller.
     */
    public static List<String> readLines(Path file) throws IOException {
        return Files.readAllLines(file, StandardCharsets.UTF_8);
    }

    /**
     * Writes a complete replacement through a temporary file in the same directory.
     * Creates parent directories when necessary. Uses an atomic move where supported;
     * the fallback replacement is not guaranteed to be atomic.
     */
    public static void writeLines(Path file, List<String> lines) throws IOException {
        Path target = file.toAbsolutePath().normalize();
        Files.createDirectories(target.getParent());
        Path temporary = Files.createTempFile(target.getParent(), ".jaweeper-", ".tmp");

        try {
            Files.write(temporary, lines, StandardCharsets.UTF_8);
            try {
                Files.move(temporary, target,
                        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
