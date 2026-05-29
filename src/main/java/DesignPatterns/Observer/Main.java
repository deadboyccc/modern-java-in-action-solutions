package DesignPatterns.Observer;

import java.util.stream.IntStream;

public class Main {
    static void main() {
        BreakingNewsEmitter observable = new BreakingNewsEmitter();

        CanadianObserver observer1 = new CanadianObserver(observable);
        CanadianObserver observer2 = new CanadianObserver(observable);
        CanadianObserver observer3 = new CanadianObserver(observable);
        CanadianObserver observer4 = new CanadianObserver(observable);


        IntStream.rangeClosed(0, 3).forEach(i -> {
            System.out.println("Current Iteration : " + i);
            observable.manualTesting();
        });

    }
}
