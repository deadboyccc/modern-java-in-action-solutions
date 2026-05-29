package DesignPatterns.Observer;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class BreakingNewsEmitter implements Observable {
    private final Set<Observer> observers = new HashSet<>();
    private String newsStream = UUID.randomUUID().toString();


    @Override
    public void addObserver(Observer o) {
        observers.add(o);
    }

    @Override
    public void removeObserver(Observer o) {
        observers.remove(o);
    }

    public String getNewsStream() {
        return newsStream;
    }

    @Override
    public void notifyObservers() {
        observers.stream().parallel().forEach(Observer::update);
    }

    public void manualTesting() {
        newsStream = UUID.randomUUID().toString();
        this.notifyObservers();
    }


}
