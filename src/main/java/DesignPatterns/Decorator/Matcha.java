package DesignPatterns.Decorator;

public class Matcha extends Beverage {
    public Matcha() {
        description = "Matcha";
    }

    @Override
    public double cost() {
        return 12.00;
    }
}
