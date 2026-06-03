package DesignPatterns.Factory;

import java.util.Collections;

public class NYVeggiePizza extends Pizza {
    NYPizzaIngredientFactory ingredientFactory;

    public NYVeggiePizza(NYPizzaIngredientFactory ingredientFactory) {
        this.ingredientFactory = ingredientFactory;
        super("NY Veggie", ingredientFactory.createDough(), ingredientFactory.createSauce(), Collections.singletonList("test1"));
    }

}
