package DesignPatterns.Decorator;

public class GreenTea extends Beverage {
    public GreenTea() {
        description = "Green Tea";
    }

    @Override
    public double cost() {
        return 8.00;
    }
}
