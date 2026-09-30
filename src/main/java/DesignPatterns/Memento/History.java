package DesignPatterns.Memento;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Caretaker: stores snapshots without inspecting or changing their contents.
 */
public class History {
    private final Deque<Editor.Memento> snapshots = new ArrayDeque<>();

    public void save(Editor editor) {
        snapshots.push(editor.save());
    }

    /**
     * Restore the previous saved state; report when there is no snapshot.
     */
    public boolean undo(Editor editor) {
        if (snapshots.isEmpty()) {
            return false;
        }
        editor.restore(snapshots.pop());
        return true;
    }
}
