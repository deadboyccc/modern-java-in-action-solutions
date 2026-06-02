package DesignPatterns.Factory;

public class NYPizzaStore extends PizzaStore {
    @Override
    protected Pizza createPizza(String item) {
        if ("Cheese".equalsIgnoreCase(item)) {
            return new NYCheesePizza();
        } else if ("Veggie".equalsIgnoreCase(item))
            return new NYVeggiePizza();
        {
        }

        throw new UnsupportedOperationException("Invalid Pizza type");
    }
}