package DesignPatterns.Mediator;

import java.util.ArrayList;
import java.util.List;

/**
 * Concrete mediator: centralizes message routing between chat participants.
 */
public class ChatRoom implements Mediator {
    private final List<User> users = new ArrayList<>();

    @Override
    public void register(User user) {
        users.add(user);
    }

    @Override
    public void sendMessage(String message, User sender) {
        for (User user : users) {
            if (user != sender) {
                user.receive(message, sender.name());
            }
        }
    }
}
