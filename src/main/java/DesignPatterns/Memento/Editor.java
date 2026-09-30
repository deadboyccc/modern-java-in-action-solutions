package DesignPatterns.Memento;

/**
 * Originator: owns editable state and creates/restores its snapshots.
 */
public class Editor {
    private String text = "";
    private int cursor;

    public void write(String text) {
        this.text += text;
        cursor = this.text.length();
    }

    public String text() {
        return text;
    }

    public int cursor() {
        return cursor;
    }

    /**
     * Save enough private state to restore the editor later.
     */
    public Memento save() {
        return new Memento(text, cursor);
    }

    /**
     * Only the originator interprets its snapshot's contents.
     */
    public void restore(Memento memento) {
        text = memento.text;
        cursor = memento.cursor;
    }

    /**
     * Memento: immutable snapshot with no public state accessors.
     */
    public static final class Memento {
        private final String text;
        private final int cursor;

        private Memento(String text, int cursor) {
            this.text = text;
            this.cursor = cursor;
        }
    }
}
