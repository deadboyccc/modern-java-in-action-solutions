package FP;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Functional programming subtleties from Section 19.5 (Miscellany).
 *
 * <p>Three independent ideas are demonstrated, each in its own section below:
 * <ol>
 *   <li><b>Memoization</b>      (19.5.1) - caching results of a pure function.</li>
 *   <li><b>Referential transparency vs. value equality</b> (19.5.2) - using a
 *       persistent (immutable) tree: two "identical" updates produce two
 *       distinct objects that are nevertheless equal in value.</li>
 *   <li><b>Combinators</b>      (19.5.3) - small higher-order functions
 *       ({@code compose}, {@code repeat}) that build new functions from old ones.</li>
 * </ol>
 *
 * <p>File layout:
 * <pre>
 *   1. Entry point (main)
 *   2. Memoization       - Range, MemoizationExample
 *   3. Persistent tree   - Tree
 *   4. Combinators       - compose, repeat
 * </pre>
 *
 * <p><b>Running:</b> {@code static void main()} (no {@code String[] args}, not
 * necessarily {@code public}) is a finalized feature in Java 25 (JEP 512) and
 * needs no preview flag. On older JDKs, use
 * {@code public static void main(String[] args)} instead.
 */
public class MiscellanyDemo {

    // =========================================================================
    // 1. ENTRY POINT
    // =========================================================================

    static void main() {
        demoMemoization();
        demoReferentialTransparency();
        demoCombinators();
    }

    // -------------------------------------------------------------------------
    // Demo drivers: each one exercises exactly one section below.
    // -------------------------------------------------------------------------

    private static void demoMemoization() {
        System.out.println("=== 1. MEMOIZATION / CACHING DEMO ===");

        MemoizationExample memo = new MemoizationExample();
        Range range = new Range(1, 10);

        // First call: cache miss -> computes and stores the value.
        Integer first = memo.computeNumberOfNodesIdiomatic(range);
        // Second call: cache hit -> returns the stored value, no recomputation.
        Integer second = memo.computeNumberOfNodesIdiomatic(range);

        System.out.println("Cached result equals computed result: " + first.equals(second));
    }

    private static void demoReferentialTransparency() {
        System.out.println("\n=== 2. REFERENTIAL TRANSPARENCY vs VALUE EQUALITY DEMO ===");

        Tree initialTree = new Tree("Root", 10, null, null);

        // Same input, same function -> two separate references but structurally identical trees.
        // The original tree is never modified (persistent data structure).
        Tree t2 = Tree.fUpdate("Will", 26, initialTree);
        Tree t3 = Tree.fUpdate("Will", 26, initialTree);

        // '==' compares object identity (are they the same object in memory?) -> false
        System.out.println("Reference equality (t2 == t3): " + (t2 == t3));
        // '.equals' on a record compares every component recursively -> true
        System.out.println("Value equality (t2.equals(t3)): " + t2.equals(t3));
    }

    private static void demoCombinators() {
        System.out.println("\n=== 3. FUNCTIONAL COMBINATORS DEMO ===");

        // A simple function that doubles its input.
        Function<Integer, Integer> doubleValue = x -> 2 * x;

        // Apply doubleValue three times: 10 -> 20 -> 40 -> 80
        // repeat(3, doubleValue) returns a new function that applies doubleValue three times.
        Function<Integer, Integer> octupleValue = repeat(3, doubleValue);

        // Show the result of applying the composed function to 10.
        // Expected output: 80
        System.out.println("repeat(3, x -> 2*x).apply(10) = " + octupleValue.apply(10));
    }

    // =========================================================================
    // 2. MEMOIZATION (Section 19.5.1)
    // =========================================================================
    //
    // Idea: if a function is pure (same input -> same output, no side effects),
    // we can safely remember its results and skip recomputation.

    /**
     * Function composition: {@code compose(g, f)} = {@code g(f(x))}.
     * Read it right-to-left: apply f first, then g.
     *
     * @param g outer function  (B -> C), applied second
     * @param f inner function  (A -> B), applied first
     * @return a function A -> C
     */
    public static <A, B, C> Function<A, C> compose(Function<B, C> g, Function<A, B> f) {
        return x -> g.apply(f.apply(x));
    }

    /**
     * Applies {@code f} to its input {@code n} times: {@code f(f(...f(x)))}.
     *
     * <p>Defined recursively:
     * <ul>
     *   <li>n = 0 -> identity function (do nothing)</li>
     *   <li>n > 0 -> f composed with (n-1) repetitions of f</li>
     * </ul>
     *
     * <p>Note: the explicit lambda {@code x -> x} is used instead of
     * {@code Function.identity()} because it avoids generic type-inference
     * problems at this call site.
     */
    public static <A> Function<A, A> repeat(int n, Function<A, A> f) {
        if (n == 0) {
            return x -> x;
        }
        return compose(f, repeat(n - 1, f));
    }

    // =========================================================================
    // 3. PERSISTENT TREE: REFERENTIAL TRANSPARENCY (Section 19.5.2)
    // =========================================================================
    //
    // A "functional update" never mutates the tree. Instead it builds a new
    // tree that copies the nodes along the changed path and SHARES every
    // untouched subtree with the original.
    //
    // Consequence: calling fUpdate twice with the same arguments gives two
    // different objects (t2 != t3 by reference) that are equal by value.
    // This is why functional code should compare with equals(), not ==.

    /**
     * Immutable key for the cache. Records give us equals/hashCode for free.
     */
    public record Range(int start, int end) {
    }

    // =========================================================================
    // 4. COMBINATORS (Section 19.5.3)
    // =========================================================================
    //
    // A combinator is a higher-order function that builds new functions out of
    // existing ones, without referring to any concrete data.

    public static class MemoizationExample {

        /**
         * The cache. ConcurrentHashMap makes it safe to share between threads,
         * and provides the atomic {@code computeIfAbsent} used below.
         */
        private final Map<Range, Integer> numberOfNodesCache = new ConcurrentHashMap<>();

        /**
         * The "expensive" pure function (stand-in computation for the demo).
         */
        public Integer computeNumberOfNodes(Range range) {
            return (range.end() - range.start()) * 42;
        }

        /**
         * Memoized version: compute only if the key is not already cached.
         *
         * <p>Replaces the verbose "get, check for null, compute, put" pattern
         * with one atomic call.
         *
         * <p>Caveat: the mapping function passed to {@code computeIfAbsent}
         * must NOT modify the map itself (e.g. no recursive memoization
         * through the same ConcurrentHashMap) or it may deadlock / throw.
         */
        public Integer computeNumberOfNodesIdiomatic(Range range) {
            return numberOfNodesCache.computeIfAbsent(range, this::computeNumberOfNodes);
        }
    }

    public record Tree(String key, int val, Tree left, Tree right) {

        /**
         * Returns a NEW tree with (k -> v) inserted or updated.
         * The input tree {@code t} is left untouched.
         *
         * @param k key to insert/update
         * @param v value to associate with the key
         * @param t tree to update (may be null = empty tree)
         */
        public static Tree fUpdate(String k, int v, Tree t) {
            // Base case: empty spot found -> create a fresh leaf node.
            if (t == null) {
                return new Tree(k, v, null, null);
            }

            int cmp = k.compareTo(t.key());

            if (cmp < 0) {
                // Key belongs on the left: rebuild this node, reuse right subtree as-is.
                return new Tree(t.key(), t.val(), fUpdate(k, v, t.left()), t.right());
            } else if (cmp > 0) {
                // Key belongs on the right: rebuild this node, reuse left subtree as-is.
                return new Tree(t.key(), t.val(), t.left(), fUpdate(k, v, t.right()));
            } else {
                // Key already exists: replace its value, keep both subtrees.
                return new Tree(k, v, t.left(), t.right());
            }
        }
    }
}