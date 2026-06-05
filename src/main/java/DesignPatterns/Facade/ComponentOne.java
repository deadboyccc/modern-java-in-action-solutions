package DesignPatterns.Facade;

@CleanComponent
public class ComponentOne implements Component {
    @Override
    public void start() {
        System.out.println("Starting Component One...");
    }

    @Override
    public void stop() {
        System.out.println("Stopping Component One...");
    }

    @Override
    public String getName() {
        return "ComponentOne";
    }
}
