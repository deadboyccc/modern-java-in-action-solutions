package PragmaticJunit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class ParamsPureFuncTest {

    private ParamsPureFunc functionalUnit;

    static Stream<Arguments> capitalizationProvider() {
        return Stream.of(
                arguments("hello", "Hello"),
                arguments("WORLD", "World"),
                arguments("jAvA", "Java"),
                arguments("a", "A"),
                arguments("", "")
        );
    }

    @BeforeEach
    void setUp() {
        this.functionalUnit = new ParamsPureFunc();
    }

    @ParameterizedTest(name = "Transformation {index}: \"{0}\" -> \"{1}\"")
    @MethodSource("capitalizationProvider")
    void testGetCapitalized(String input, String expected) {
        assertEquals(expected, functionalUnit.getCapitalized(input));
    }

    @Test
    void testGetCapitalized_NullInput() {
        assertNull(functionalUnit.getCapitalized(null));
    }
}