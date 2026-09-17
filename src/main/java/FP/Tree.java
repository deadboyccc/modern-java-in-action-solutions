package FP;

public class Tree {
    // Fields made public/accessible for simplicity
    public final String key;
    public final int val;
    public final Tree left;
    public final Tree right;

    public Tree(String k, int v, Tree l, Tree r) {
        this.key = k;
        this.val = v;
        this.left = l;
        this.right = r;
    }
}

class TreeProcessor {

    // --- Lookup ---
    public static int lookup(String k, int defaultval, Tree t) {
        if (t == null) return defaultval;
        if (k.equals(t.key)) return t.val;
        return lookup(k, defaultval, k.compareTo(t.key) < 0 ? t.left : t.right);
    }

    // --- Imperative / In-Place Update (Requires non-final fields in Tree) ---
    // Modifies existing tree nodes directly, mutating shared state.
    public static Tree update(String k, int newval, Tree t) {
        if (t == null) {
            t = new Tree(k, newval, null, null);
        } else if (k.equals(t.key)) {
            // Note: Requires 'val' in Tree to NOT be final
            // t.val = newval;
        } else if (k.compareTo(t.key) < 0) {
            // t.left = update(k, newval, t.left);
        } else {
            // t.right = update(k, newval, t.right);
        }
        return t;
    }

    // --- Functional / Persistent Update ---
    // Creates new nodes along the path to the root while sharing unchanged subtrees.
    public static Tree fupdate(String k, int newval, Tree t) {
        return (t == null) ?
                new Tree(k, newval, null, null) :
                k.equals(t.key) ?
                        new Tree(k, newval, t.left, t.right) :
                        k.compareTo(t.key) < 0 ?
                                new Tree(t.key, t.val, fupdate(k, newval, t.left), t.right) :
                                new Tree(t.key, t.val, t.left, fupdate(k, newval, t.right));
    }
}