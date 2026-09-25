package FP.CleanLazyList;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * A functional, singly-linked lazy list.
 * <p>
 * Key properties:
 * <ul>
 *   <li><b>Structural immutability</b> — nodes never change identity once created.</li>
 *   <li><b>Lazy consing</b> — the tail is a {@link Supplier}, so it isn't computed
 *       until something actually asks for it (mirrors Scala's {@code #::}).</li>
 *   <li><b>Thread-safe memoization</b> — each tail is computed at most once,
 *       even under concurrent access, via double-checked locking.</li>
 * </ul>
 */
public sealed interface LazyList<T> {

    // ---- Factories ----

    /**
     * Returns the canonical empty list.
     */
    static <T> LazyList<T> empty() {
        return new Nil<>();
    }

    /**
     * Builds a node from an eager head and a deferred tail.
     * The tail supplier is not invoked until {@link #tail()} is called.
     */
    static <T> LazyList<T> cons(T head, Supplier<LazyList<T>> tail) {
        return new Cons<>(head, tail);
    }

    // ---- Core API ----

    /** Returns the first element. Throws if the list is empty. */
    T head();

    /** Returns the remainder of the list, forcing lazy evaluation if needed. */
    LazyList<T> tail();

    /** True only for the {@link Nil} case. */
    default boolean isEmpty() {
        return this instanceof Nil<?>;
    }

    // ---- Operations ----

    /**
     * Lazily filters elements: only the returned head is guaranteed to match;
     * everything past it stays deferred.
     * <p>
     * Non-matching elements are skipped via a plain loop rather than recursion,
     * so filtering a long run of rejected elements is stack-safe.
     */
    default LazyList<T> filter(Predicate<? super T> predicate) {
        LazyList<T> current = this;
        while (current instanceof Cons<T> cell) {
            if (predicate.test(cell.head())) {
                // Found a match: emit it, deferring the rest of the filtering.
                return cons(cell.head(), () -> cell.tail().filter(predicate));
            }
            // No match: advance without recursing, keeping the call stack flat.
            current = cell.tail();
        }
        // No matches left: return the empty list.
        return empty();
    }

    /**
     * Eagerly collects up to the first {@code n} elements into a {@link List}.
     * Forces evaluation of exactly the nodes visited — never the one past the
     * {@code n}th element.
     */
    default List<T> take(int n) {
        if (n <= 0) {
            return List.of();
        }
        var result = new ArrayList<T>(n);
        LazyList<T> current = this;
        while (current instanceof Cons<T> cell) {
            result.add(cell.head());
            if (result.size() == n) {
                break;
            }
            current = cell.tail();
        }
        return List.copyOf(result);
    }

    // ---- Node implementations ----

    /** The empty node; a marker with no data. */
    record Nil<T>() implements LazyList<T> {
        @Override
        public T head() {
            throw new NoSuchElementException("Head on empty LazyList");
        }

        @Override
        public LazyList<T> tail() {
            throw new NoSuchElementException("Tail on empty LazyList");
        }
    }

    /**
     * A non-empty node: an eager head plus a tail that is computed lazily
     * and cached after the first evaluation.
     */
    final class Cons<T> implements LazyList<T> {
        private final T head;

        // Cleared once 'tail' is populated, so the closure (and whatever it
        // captured) becomes eligible for garbage collection.
        private Supplier<LazyList<T>> tailProducer;

        // volatile so a tail computed by one thread is visible to others
        // without needing to acquire the lock in the common (already-computed) case.
        private volatile LazyList<T> tail;

        private Cons(T head, Supplier<LazyList<T>> tailProducer) {
            this.head = head;
            this.tailProducer = Objects.requireNonNull(tailProducer);
        }

        @Override
        public T head() {
            return head;
        }

        // -- needs review --

        /**
         * Computes and caches the tail on first access.
         * Double-checked locking avoids synchronizing on every subsequent call
         * while still guaranteeing the supplier runs exactly once.
         */
        @Override
        public LazyList<T> tail() {
            var result = tail;
            if (result == null) {
                synchronized (this) {
                    result = tail;
                    if (result == null) {
                        result = Objects.requireNonNull(tailProducer.get());
                        tail = result;
                        tailProducer = null; // release captured state for GC
                    }
                }
            }
            return result;
        }
    }
}
