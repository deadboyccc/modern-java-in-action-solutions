package DesignPatterns.Mediator;

/**
 * Client: colleagues know their mediator, while the mediator knows the group.
 */
public class MediatorDemo {
    static void main(String[] args) {
        // Create a chat room mediator and users
        Mediator chatRoom = new ChatRoom();
        User alice = new User("Alice", chatRoom);

        new User("Bob", chatRoom);
        new User("Charlie", chatRoom);
        new User("David", chatRoom);

        alice.send("The meeting starts in five minutes.");
    }
}
