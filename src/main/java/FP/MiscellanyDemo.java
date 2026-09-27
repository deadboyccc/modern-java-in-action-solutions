package FP;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Demonstrates functional programming subtleties discussed in Section 19.5 (Miscellany).
 */
public class MiscellanyDemo {

    // =========================================================================
    // 1. DOMAIN MODELS & HELPER CLASSES
    // =========================================================================

    public static <A, B, C> Function<A, C> compose(Function<B, C> g, Function<A, B> f) {
        return x -> g.apply(f.apply(x));
    }

    public static <A> Function<A, A> repeat(int n, Function<A, A> f) {
        if (n == 0) {
            return x -> x; // Explicit lambda x->x fixes generic inference issues from Function.identity()
        }
        return compose(f, repeat(n - 1, f));
    }

    // =========================================================================
    // 2. SECTION 19.5.1: CACHING OR MEMOIZATION
    // =========================================================================

    // Kept standard public void main(String[] args) so it runs natively without Preview feature flags
    public static void main(String[] args) {
        System.out.println("=== 1. MEMOIZATION / CACHING DEMO ===");
        MemoizationExample memo = new MemoizationExample();
        Range r1 = new Range(1, 10);

        Integer val1 = memo.computeNumberOfNodesIdiomatic(r1);
        Integer val2 = memo.computeNumberOfNodesIdiomatic(r1);
        System.out.println("Cached result equals computed result: " + (val1.equals(val2)));

        System.out.println("\n=== 2. REFERENTIAL TRANSPARENCY vs VALUE EQUALITY DEMO ===");
        Tree initialTree = new Tree("Root", 10, null, null);
        Tree t2 = Tree.fUpdate("Will", 26, initialTree);
        Tree t3 = Tree.fUpdate("Will", 26, initialTree);

        System.out.println("Reference equality (t2 == t3): " + (t2 == t3));
        System.out.println("Value equality (t2.equals(t3)): " + t2.equals(t3));

        System.out.println("\n=== 3. FUNCTIONAL COMBINATORS DEMO ===");
        Function<Integer, Integer> doubleValue = x -> 2 * x;
        Function<Integer, Integer> octupleValue = repeat(3, doubleValue);

        Integer result = octupleValue.apply(10);
        System.out.println("repeat(3, x -> 2*x).apply(10) = " + result);
    }

    // =========================================================================
    // 3. SECTION 19.5.3: COMBINATORS
    // =========================================================================

    public record Range(int start, int end) {
    }

    public record Tree(String key, int val, Tree left, Tree right) {

        public static Tree fUpdate(String k, int v, Tree t) {
            if (t == null) return new Tree(k, v, null, null);

            if (k.compareTo(t.key()) < 0) {
                return new Tree(t.key(), t.val(), fUpdate(k, v, t.left()), t.right());
            } else if (k.compareTo(t.key()) > 0) {
                return new Tree(t.key(), t.val(), t.left(), fUpdate(k, v, t.right()));
            } else {
                return new Tree(k, v, t.left(), t.right());
            }
        }
    }

    // =========================================================================
    // MAIN EXECUTION & DEMONSTRATION
    // =========================================================================

    public static class MemoizationExample {
        // Explicit type arguments to ensure compiler infers types properly
        private final Map<Range, Integer> numberOfNodesCache = new ConcurrentHashMap<>();

        public Integer computeNumberOfNodes(Range range) {
            return (range.end() - range.start()) * 42;
        }

        public Integer computeNumberOfNodesIdiomatic(Range range) {
            return numberOfNodesCache.computeIfAbsent(range, this::computeNumberOfNodes);
        }
    }
}