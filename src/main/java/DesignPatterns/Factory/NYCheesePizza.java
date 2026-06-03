package DesignPatterns.Factory;

import java.util.Collections;

public class NYCheesePizza extends Pizza {
    NYPizzaIngredientFactory ingredientFactory;

    public NYCheesePizza(NYPizzaIngredientFactory ingredientFactory) {
        this.ingredientFactory = ingredientFactory;

        super("NY Cheese", ingredientFactory.createDough(), ingredientFactory.createSauce(), Collections.singletonList("test1"));

    }

}
