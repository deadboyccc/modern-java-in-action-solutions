package DesignPatterns.Memento;

/**
 * Client: edits, saves checkpoints, and restores an earlier state with undo.
 */
public class MementoDemo {
    public static void main(String[] args) {
        Editor editor = new Editor();
        History history = new History();

        editor.write("Bridge ");
        history.save(editor);
        editor.write("and Memento");
        System.out.println("Current text: " + editor.text());

        history.undo(editor);
        System.out.println("After undo: " + editor.text() + " (cursor " + editor.cursor() + ")");
    }
}
