package DesignPatterns.Decorator;

public class Pepper extends CondimentDecorator {
    Beverage beverage;

    public Pepper(Beverage beverage) {
        this.beverage = beverage;
    }

    @Override
    public String getDescription() {
        return beverage.getDescription() + ", Pepper";
    }

    @Override
    public double cost() {
        return beverage.cost() + .20;
    }
}
