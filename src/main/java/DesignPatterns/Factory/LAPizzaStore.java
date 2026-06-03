package DesignPatterns.Factory;

public class LAPizzaStore extends PizzaStore {
    LAPizzaIngredientFactory ingredientFactory;

    public LAPizzaStore() {
        ingredientFactory = new LAPizzaIngredientFactory();
    }

    @Override
    protected Pizza createPizza(String item) {
        if ("Veggie".equalsIgnoreCase(item)) {
            return new LAVeggiePizza(ingredientFactory);
        } else if ("Cheese".equalsIgnoreCase(item)) {
            return new LACheesePizza(ingredientFactory);
        }
        throw new UnsupportedOperationException("Invalid Pizza type");
    }
}
