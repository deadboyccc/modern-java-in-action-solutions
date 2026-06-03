package DesignPatterns.Factory;

public class NYPizzaStore extends PizzaStore {
    NYPizzaIngredientFactory ingredientFactory;

    public NYPizzaStore() {
        this.ingredientFactory = new NYPizzaIngredientFactory();


    }

    @Override
    protected Pizza createPizza(String item) {
        if ("Cheese".equalsIgnoreCase(item)) {
            return new NYCheesePizza(ingredientFactory);
        } else if ("Veggie".equalsIgnoreCase(item))
            return new NYVeggiePizza(ingredientFactory);
        {
        }

        throw new UnsupportedOperationException("Invalid Pizza type");
    }
}