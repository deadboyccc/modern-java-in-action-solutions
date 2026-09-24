package FP.CleanLazyList;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Functional LazyList featuring structural immutability,
 * lazy consing, and thread-safe tail memoization.
 */
public sealed interface LazyList<T> {

    static <T> LazyList<T> empty() {
        return new Nil<>();
    }

    /**
     * Constructs a node from a head and a deferred tail (Scala's {@code #::}).
     */
    static <T> LazyList<T> cons(T head, Supplier<LazyList<T>> tail) {
        return new Cons<>(head, tail);
    }

    T head();

    LazyList<T> tail();

    default boolean isEmpty() {
        return this instanceof Nil<?>;
    }

    // ---- Factories ----

    /**
     * Lazily filters elements; skips non-matching nodes iteratively (stack-safe).
     */
    default LazyList<T> filter(Predicate<? super T> predicate) {
        LazyList<T> current = this;
        while (current instanceof Cons<T> cell) {
            if (predicate.test(cell.head())) {
                return cons(cell.head(), () -> cell.tail().filter(predicate));
            }
            current = cell.tail();
        }
        return empty();
    }

    /**
     * Eagerly materializes the first {@code n} elements, without forcing the tail past the last one.
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

    // ---- Operations ----

    /**
     * An empty LazyList node.
     */
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
     * A populated node holding a head value and a deferred, memoized tail.
     */
    final class Cons<T> implements LazyList<T> {
        private final T head;
        private Supplier<LazyList<T>> tailProducer;
        private volatile LazyList<T> tail;

        private Cons(T head, Supplier<LazyList<T>> tailProducer) {
            this.head = head;
            this.tailProducer = Objects.requireNonNull(tailProducer);
        }

        @Override
        public T head() {
            return head;
        }

        /**
         * Evaluates the tail once (double-checked locking) and caches it.
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