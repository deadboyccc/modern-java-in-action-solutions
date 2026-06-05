package DesignPatterns.Facade;

import java.util.ArrayList;
import java.util.List;

/**
 * Facade that provides a simplified interface to manage multiple components.
 */
public class Facade {
    private final List<Component> components;

    private Facade(List<Component> components) {
        this.components = new ArrayList<>(components);
    }

    public static Builder builder() {
        return new Builder();
    }

    public void startAll() {
        components.forEach(Component::start);
    }

    public void stopAll() {
        components.forEach(Component::stop);
        components.forEach(MaintenanceWorker::performMaintenance);
    }

    public void addComponent(Component component) {
        components.add(component);
    }

    /**
     * Builder for fluent facade construction.
     */
    public static class Builder {
        private final List<Component> components = new ArrayList<>();

        public Builder add(Component component) {
            components.add(component);
            return this;
        }

        public Facade build() {
            return new Facade(components);
        }
    }
}
