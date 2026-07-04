package dev.dead.OptionalCh;

import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SystemPropertiesTest {

    @Test
    void assertAllArePositive() {
        Properties props = new Properties();
        props.setProperty("a", "5");
        props.setProperty("b", "true");
        props.setProperty("c", "-3");

        SystemProperties prop = new SystemProperties(props);

        assertEquals(5, prop.readDuration("a"));
        assertEquals(0, prop.readDuration("b"));
        assertEquals(0, prop.readDuration("c"));
        assertEquals(0, prop.readDuration("d"));
    }


}