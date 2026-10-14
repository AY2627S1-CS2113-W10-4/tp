package seedu.jaweeper;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests parser invariants and keeps user input validation separate from assertions.
 */
public class ParserAssertionsTest {

    @Test
    public void assertions_parserClass_enabled() {
        assertTrue(Parser.class.desiredAssertionStatus(),
                "Parser assertions must be enabled during tests");
    }

    @Test
    public void parse_factoryReturnsNull_throwsAssertionError() {
        Map<String, BiFunction<Integer, Integer, Command>> commands =
                Map.of("r", (row, column) -> null);

        AssertionError error = assertThrows(
                AssertionError.class, () -> Parser.parse("r 1 1", commands));

        assertEquals("Registered command factory must not return null", error.getMessage());
    }

    @Test
    public void parse_invalidCoordinate_throwsIllegalArgumentException() {
        Map<String, BiFunction<Integer, Integer, Command>> commands =
                Map.of("r", (row, column) -> new HelpCommand());

        assertThrows(IllegalArgumentException.class,
                () -> Parser.parse("r 0 1", commands));
    }
}
