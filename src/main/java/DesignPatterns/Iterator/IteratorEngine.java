package DesignPatterns.Iterator;

import java.util.List;

public class IteratorEngine {
    static void main() {
        var waiter = new Waiter(List.of(new BreakfastMenu(), new LaunchMenu()));
        waiter.printAll();
    }
}
