package DesignPatterns.Observer;

import java.util.Arrays;

public class CanadianObserver implements Observer {
    private final BreakingNewsEmitter observable;

    public CanadianObserver(BreakingNewsEmitter observable) {
        this.observable = observable;
        observable.addObserver(this);
    }

    @Override
    public void update() {
        // new news || pull
        System.out.println(Arrays.toString(observable.getNewsStream().split("-")));
    }
}
