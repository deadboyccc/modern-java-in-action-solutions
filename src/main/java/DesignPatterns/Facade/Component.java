package DesignPatterns.Facade;

/**
 * Common interface for all components.
 */
public interface Component {
    void start();

    void stop();

    String getName();
}

