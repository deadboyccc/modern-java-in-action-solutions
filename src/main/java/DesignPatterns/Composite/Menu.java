package DesignPatterns.Composite;

import java.util.ArrayList;
import java.util.List;

public class Menu extends MenuComponent {
    private final List<MenuComponent> components = new ArrayList<>();
    private final String description;
    private final String name;

    public Menu(String description, String name) {
        this.description = description;
        this.name = name;
    }

    @Override
    public void add(MenuComponent menuComponent) {
        components.add(menuComponent);
    }

    @Override
    public void remove(MenuComponent menuComponent) {
        components.remove(menuComponent);
    }

    @Override
    public void print() {
        System.out.println("_".repeat(20));
        System.out.println("Menu " + description + " " + name);
        components.forEach(MenuComponent::print);
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public MenuComponent getChild(int i) {
        return components.get(i);
    }

    @Override
    public String getName() {
        return name;
    }
}
