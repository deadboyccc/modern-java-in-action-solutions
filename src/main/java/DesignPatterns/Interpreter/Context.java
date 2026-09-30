package DesignPatterns.Interpreter;

import java.util.HashMap;
import java.util.Map;

/**
 * Context: supplies the values used when interpreting variable expressions.
 */
public class Context {
    private final Map<String, Boolean> values = new HashMap<>();

    public void assign(String name, boolean value) {
        values.put(name, value);
    }

    boolean valueOf(String name) {
        Boolean value = values.get(name);
        if (value == null) {
            throw new IllegalArgumentException("No value assigned to: " + name);
        }
        return value;
    }
}
