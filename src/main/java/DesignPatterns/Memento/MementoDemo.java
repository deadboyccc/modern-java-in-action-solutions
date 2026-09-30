package DesignPatterns.Memento;

/**
 * Client: edits, saves checkpoints, and restores an earlier state with undo.
 */
public class MementoDemo {
    static void main(String[] args) {
        Editor editor = new Editor();
        History history = new History();


        // save the original blank state before making any changes
        history.save(editor);

        editor.write("Bridge ");
        history.save(editor);
        editor.write("and Memento");

        // should print "Bridge and Memento" and cursor position 18
        System.out.println("Current text: " + editor.text() + " (cursor " + editor.cursor() + ")");

        history.undo(editor);

        // should print "Bridge " and cursor position 7
        System.out.println("After first undo: " + editor.text() + " (cursor " + editor.cursor() + ")");

        history.undo(editor);
        System.out.println("After second undo: " + editor.text() + " (cursor " + editor.cursor() + ")");
    }
}
