package DesignPatterns.Facade;

public class ComponentTwo implements Component {
    @Override
    public void start() {
        System.out.println("Starting Component Two...");
    }

    @Override
    public void stop() {
        System.out.println("Stopping Component Two...");
    }

    @Override
    public String getName() {
        return "ComponentTwo";
    }
}
