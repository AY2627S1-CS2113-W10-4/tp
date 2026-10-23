package seedu.jaweeper;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;


public class SaveStore {

    /** Letters, digits, '_' and '-' only, so a name can never contain a path separator or "..". */
    private static final String NAME_PATTERN = "[a-z0-9_-]{1,30}";
    private static final String EXTENSION = ".txt";

    /** File names Windows refuses to create regardless of extension. */
    private static final Set<String> RESERVED_NAMES = Set.of(
            "con", "prn", "aux", "nul",
            "com1", "com2", "com3", "com4", "com5", "com6", "com7", "com8", "com9",
            "lpt1", "lpt2", "lpt3", "lpt4", "lpt5", "lpt6", "lpt7", "lpt8", "lpt9");

    private final Path savesDirectory;

    public SaveStore(Path savesDirectory) {
        assert savesDirectory != null : "Saves directory must not be null";
        this.savesDirectory = savesDirectory;
    }


    public static SaveStore forSession(GameSession session) {
        return new SaveStore(session.getProfileDirectory().resolve("saves"));
    }

 
    public static String normalizeName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Save name must not be empty.");
        }
        String normalized = name.strip().toLowerCase(Locale.ROOT);
        if (!normalized.matches(NAME_PATTERN)) {
            throw new IllegalArgumentException(
                    "Save names use 1-30 letters, digits, '_' or '-' (no spaces).");
        }
        if (RESERVED_NAMES.contains(normalized)) {
            throw new IllegalArgumentException("'" + normalized + "' is reserved by the system; choose another name.");
        }
        return normalized;
    }

   
    public boolean exists(String name) {
        return Files.isRegularFile(pathFor(name));
    }

    /**
     * Writes a game, replacing any existing save with the same name.
     *
     * @throws IOException If the file cannot be written; the previous save, if any, is kept.
     */
    public void save(String name, Board board, long elapsedMillis) throws IOException {
        Storage.writeLines(pathFor(name), GameSaveCodec.encode(board, elapsedMillis));
    }

    /**
     * Reads and fully validates a saved game.
     *
     * @throws SaveFileException If the save does not exist, cannot be read or is invalid.
     */
    public GameSaveCodec.SavedGame load(String name) throws SaveFileException {
        String normalized = normalizeName(name);
        List<String> lines;
        try {
            lines = Storage.readLines(pathFor(normalized));
        } catch (NoSuchFileException exception) {
            throw new SaveFileException("No save named '" + normalized + "'.", exception);
        } catch (IOException exception) {
            // Includes malformed UTF-8 (MalformedInputException), e.g. binary data in the file.
            throw new SaveFileException("Could not read save '" + normalized + "': " + exception.getMessage(),
                    exception);
        }
        return GameSaveCodec.decode(lines);
    }

    /**
     * Lists the names of saves in this directory, sorted alphabetically.
     * Files that are not valid save names (temporary files, notes) are ignored.
     * Validity of each file's contents is only checked when it is loaded.
     *
     * @throws IOException If the directory exists but cannot be read.
     */
    public List<String> list() throws IOException {
        List<String> names = new ArrayList<>();
        if (!Files.isDirectory(savesDirectory)) {
            return names;
        }
        try (DirectoryStream<Path> files = Files.newDirectoryStream(savesDirectory, "*" + EXTENSION)) {
            for (Path file : files) {
                String fileName = file.getFileName().toString();
                String name = fileName.substring(0, fileName.length() - EXTENSION.length());
                if (name.matches(NAME_PATTERN) && Files.isRegularFile(file)) {
                    names.add(name);
                }
            }
        }
        Collections.sort(names);
        return names;
    }

    /**
     * Deletes a save.
     *
     * @return True if a save was deleted; false if none existed.
     * @throws IOException If the file exists but cannot be deleted.
     */
    public boolean delete(String name) throws IOException {
        return Files.deleteIfExists(pathFor(name));
    }

    private Path pathFor(String name) {
        Path file = savesDirectory.resolve(normalizeName(name) + EXTENSION);
        assert file.getParent().equals(savesDirectory) : "Save name must not escape the saves directory";
        return file;
    }
}
