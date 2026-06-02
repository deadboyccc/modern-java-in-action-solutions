package DesignPatterns.Factory;

public class LAPizzaStore extends PizzaStore {
    @Override
    protected Pizza createPizza(String item) {
        if ("Veggie".equalsIgnoreCase(item)) {
            return new LAVeggiePizza();
        } else if ("Cheese".equalsIgnoreCase(item)) {
            return new LACheesePizza();
        }
        throw new UnsupportedOperationException("Invalid Pizza type");
    }
}
