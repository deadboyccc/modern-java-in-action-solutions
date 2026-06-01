package DesignPatterns.Decorator;

public abstract class Beverage {
    String description = "Unknown Beverage";

    public String getDescription() {
        return description;
    }

    abstract public double cost();
}
