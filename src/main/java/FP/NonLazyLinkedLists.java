package FP;

interface MyList<T> {
    // Returns the first element of the list
    T head();

    // Returns the rest of the list
    MyList<T> tail();

    // Returns true if the list is empty, false otherwise
    default boolean isEmpty() {
        return true;
    }
}

public class NonLazyLinkedLists {
}

// A non-lazy linked list implementation
class MyLinkedList<T> implements MyList<T> {
    private final T head;
    private final MyList<T> tail;

    public MyLinkedList(T head, MyList<T> tail) {
        this.head = head;
        this.tail = tail;
    }

    @Override
    public T head() {
        return head;
    }

    @Override
    public MyList<T> tail() {
        return tail;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }
}

class Empty<T> implements MyList<T> {

    @Override
    public T head() {
        throw new UnsupportedOperationException();
    }

    @Override
    public MyList<T> tail() {
        throw new UnsupportedOperationException();
    }
}

class Test {
    MyList<Integer> l =
            new MyLinkedList<>(
                    5,
                    new MyLinkedList<>(
                            10,
                            new Empty<>()
                    )
            );
}