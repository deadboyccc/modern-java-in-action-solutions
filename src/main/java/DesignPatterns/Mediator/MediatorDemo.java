package DesignPatterns.Mediator;

/**
 * Client: colleagues know their mediator, while the mediator knows the group.
 */
public class MediatorDemo {
    public static void main(String[] args) {
        Mediator chatRoom = new ChatRoom();
        User alice = new User("Alice", chatRoom);
        new User("Bob", chatRoom);
        new User("Charlie", chatRoom);

        alice.send("The meeting starts in five minutes.");
    }
}
