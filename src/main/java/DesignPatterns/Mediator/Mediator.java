package DesignPatterns.Mediator;

/**
 * Mediator: colleagues communicate through this contract, not directly.
 */
public interface Mediator {
    void register(User user);

    void sendMessage(String message, User sender);
}
