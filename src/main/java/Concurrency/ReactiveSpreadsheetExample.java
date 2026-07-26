package Concurrency;

import java.util.ArrayList;
import java.util.List;

public class ReactiveSpreadsheetExample {

    public static void main(String[] args) {

        System.out.println("=== Example 1: Simple Subscription ===");

        SimpleCell c1 = new SimpleCell("C1");
        SimpleCell c2 = new SimpleCell("C2");
        SimpleCell c3 = new SimpleCell("C3");

        // Publisher(C1) -> Subscriber(C3)
        // C3 simply copies whatever value C1 publishes.
        c1.subscribe(c3);

        c1.onNext(10);
        c2.onNext(20);

        System.out.println();
        System.out.println("=== Example 2: C3 = C1 + C2 ===");

        SimpleCell c1Example2 = new SimpleCell("C1");
        SimpleCell c2Example2 = new SimpleCell("C2");
        ArithmeticCell c3Example2 = new ArithmeticCell("C3");

        // Whenever C1 changes -> update the left side of C3
        c1Example2.subscribe(c3Example2::setLeft);

        // Whenever C2 changes -> update the right side of C3
        c2Example2.subscribe(c3Example2::setRight);

        c1Example2.onNext(10);
        c2Example2.onNext(20);
        c1Example2.onNext(15);

        System.out.println();
        System.out.println("=== Example 3: C5 = C3 + C4 ===");

        ArithmeticCell c5 = new ArithmeticCell("C5");
        ArithmeticCell c3Final = new ArithmeticCell("C3");

        SimpleCell c4 = new SimpleCell("C4");
        SimpleCell c1Final = new SimpleCell("C1");
        SimpleCell c2Final = new SimpleCell("C2");

        // C3 = C1 + C2
        c1Final.subscribe(c3Final::setLeft);
        c2Final.subscribe(c3Final::setRight);

        // C5 = C3 + C4
        c3Final.subscribe(c5::setLeft);
        c4.subscribe(c5::setRight);

        c1Final.onNext(10);
        c2Final.onNext(20);
        c1Final.onNext(15);
        c4.onNext(1);
        c4.onNext(3);
    }

    interface Publisher<T> {
        void subscribe(Subscriber<? super T> subscriber);
    }

    interface Subscriber<T> {
        void onNext(T value);
    }

    static class SimpleCell
            implements Publisher<Integer>, Subscriber<Integer> {

        private final String name;
        private final List<Subscriber<? super Integer>> subscribers =
                new ArrayList<>();
        private int value;

        public SimpleCell(String name) {
            this.name = name;
        }

        // Publisher API = allows other cells to subscribe
        @Override
        public void subscribe(Subscriber<? super Integer> subscriber) {
            subscribers.add(subscriber);
        }

        // Subscriber API = called whenever an upstream cell publishes a value
        @Override
        public void onNext(Integer newValue) {

            // Store the new value
            value = newValue;

            // Print it (could instead update a UI)
            System.out.println(name + ": " + value);

            // Notify downstream subscribers so they can react
            notifySubscribers();
        }

        private void notifySubscribers() {
            // Every subscriber receives the new value and may propagate it
            subscribers.forEach(subscriber -> subscriber.onNext(value));
        }
    }

    static class ArithmeticCell extends SimpleCell {

        private int left;
        private int right;

        public ArithmeticCell(String name) {
            super(name);
        }

        public void setLeft(int left) {
            this.left = left;

            // Instead of copying the value,
            // compute left + right and publish the result.
            onNext(this.left + this.right);
        }

        public void setRight(int right) {
            this.right = right;

            // Instead of copying the value,
            // compute left + right and publish the result.
            onNext(this.left + this.right);
        }
    }
}