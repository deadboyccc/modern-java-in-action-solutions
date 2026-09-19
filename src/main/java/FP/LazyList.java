package FP;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

class LazyList<T> implements MyList<T> {
    final T head;
    final Supplier<MyList<T>> tail;

    public LazyList(T head, Supplier<MyList<T>> tail) {
        this.head = head;
        this.tail = tail;
    }

    public static void main(String[] args) {
        LazyList<Integer> naturals = LazyList.from(1);

        var first = naturals;
        var second = (LazyList<Integer>) first.tail();
        var third = (LazyList<Integer>) second.tail();

        System.out.println("First three naturals individually: "
                + first.head() + ", " + second.head() + ", " + third.head());

        System.out.println("First 5 naturals using take: " + naturals.take(5));
    }

    public static LazyList<Integer> from(int n) {
        return new LazyList<Integer>(n, () -> from(n + 1));
    }

    @Override
    public T head() {
        return head;
    }

    @Override
    public MyList<T> tail() {
        return tail.get();
    }

    public List<T> take(int n) {
        List<T> result = new ArrayList<>();
        MyList<T> current = this;
        while (n > 0 && !current.isEmpty()) {
            result.add(current.head());
            current = current.tail();
            n--;
        }
        return result;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }
}
