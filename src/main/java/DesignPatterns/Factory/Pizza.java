package DesignPatterns.Factory;

import java.util.ArrayList;
import java.util.List;

public abstract class Pizza {

    private final String name;
    private final String dough;
    private final String sauce;
    private final List<String> toppings = new ArrayList<>();

    protected Pizza(String name, String dough, String sauce, List<String> toppings) {
        this.name = name;
        this.dough = dough;
        this.sauce = sauce;
        this.toppings.addAll(toppings);
    }

    public String getName() {
        return name;
    }

    public void prepare() {
        System.out.println("Preparing " + name);
    }

    public void bake() {
        System.out.println("Baking " + name);
    }

    public void cut() {
        System.out.println("Cutting " + name);
    }

    public void box() {
        System.out.println("Boxing " + name);
    }

    @Override
    public String toString() {
        StringBuilder display = new StringBuilder();
        display.append("---- ").append(name).append(" ----\n")
                .append(dough).append("\n")
                .append(sauce).append("\n");

        for (String topping : toppings) {
            display.append(topping).append("\n");
        }
        return display.toString();
    }
}
