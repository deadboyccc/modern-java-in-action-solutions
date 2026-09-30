package DesignPatterns.Mediator;

import java.util.Objects;

/**
 * Colleague: a user sends through the mediator and never addresses peers directly.
 */
public class User {
    private final String name;
    private final Mediator mediator;

    public User(String name, Mediator mediator) {
        this.name = Objects.requireNonNull(name, "name");
        this.mediator = Objects.requireNonNull(mediator, "mediator");
        mediator.register(this);
    }

    public void send(String message) {
        mediator.sendMessage(message, this);
    }

    void receive(String message, String senderName) {
        System.out.println(name + " receives from " + senderName + ": " + message);
    }

    String name() {
        return name;
    }
}
