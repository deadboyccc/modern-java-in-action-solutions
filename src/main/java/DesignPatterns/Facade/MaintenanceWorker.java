package DesignPatterns.Facade;

/**
 * Handles maintenance work for components marked with @CleanComponent.
 */
public class MaintenanceWorker {

    public static void performMaintenance(Component component) {
        if (component.getClass().isAnnotationPresent(CleanComponent.class)) {
            System.out.println("Running cleanup on: " + component.getName());
        } else {
            System.out.println("No cleanup needed for: " + component.getName());
        }
    }
}
