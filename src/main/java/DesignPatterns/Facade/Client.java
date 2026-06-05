package DesignPatterns.Facade;

/**
 * Demonstrates the Facade pattern with a clean, modern approach.
 */
public class Client {
    public static void main(String[] args) {
        // Build facade with fluent API
        var facade = Facade.builder()
                .add(new ComponentOne())
                .add(new ComponentTwo())
                .build();

        facade.startAll();
        facade.stopAll();
    }
}
