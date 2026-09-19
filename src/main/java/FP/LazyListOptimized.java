package FP;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * A lazy, persistent list. The tail is computed at most once and memoized
 * on first access, so repeated calls to {@link #tail()} return the same
 * node instead of recomputing (and re-allocating) it every time.
 */
class LazyListOptimized<T> implements MyList<T> {
    private final T head;
    private volatile Supplier<MyList<T>> tailSupplier;
    private volatile MyList<T> tailValue;

    public LazyListOptimized(T head, Supplier<MyList<T>> tailSupplier) {
        this.head = head;
        this.tailSupplier = tailSupplier;
    }

    public static LazyListOptimized<Integer> from(int n) {
        return new LazyListOptimized<>(n, () -> from(n + 1));
    }

    public static void main(String[] args) {
        LazyListOptimized<Integer> naturals = LazyListOptimized.from(1);

        LazyListOptimized<Integer> first = naturals;
        @SuppressWarnings("unchecked")
        LazyListOptimized<Integer> second = (LazyListOptimized<Integer>) first.tail();
        @SuppressWarnings("unchecked")
        LazyListOptimized<Integer> third = (LazyListOptimized<Integer>) second.tail();

        System.out.println("First three naturals individually: "
                + first.head() + ", " + second.head() + ", " + third.head());

        System.out.println("First 5 naturals using take: " + naturals.take(5));
    }

    @Override
    public T head() {
        return head;
    }

    @Override
    public MyList<T> tail() {
        // Double-checked locking: compute the tail once, cache it, and
        // drop the supplier reference so its captured closure can be GC'd.
        MyList<T> result = tailValue;
        if (result == null) {
            synchronized (this) {
                result = tailValue;
                if (result == null) {
                    result = tailSupplier.get();
                    tailValue = result;
                    tailSupplier = null;
                }
            }
        }
        return result;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    public List<T> take(int n) {
        List<T> result = new ArrayList<>(Math.max(n, 0));
        MyList<T> current = this;
        while (n > 0 && !current.isEmpty()) {
            result.add(current.head());
            current = current.tail();
            n--;
        }
        return result;
    }
}