package DesignPatterns.Mediator;

/**
 * Mediator: colleagues communicate through this contract, not directly.
 * Centralizes control of how colleagues interact, reducing coupling between them.
 */
public interface Mediator {
    void register(User user);

    void sendMessage(String message, User sender);
}
