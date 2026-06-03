package DesignPatterns.Factory;

import java.util.Collections;

public class LACheesePizza extends Pizza {
    PizzaIngredientFactory ingredientFactory;

    public LACheesePizza(PizzaIngredientFactory ingredientFactory) {
        this.ingredientFactory = ingredientFactory;
        super("LA Cheese", ingredientFactory.createDough(),
                ingredientFactory.createSauce(), Collections.singletonList("test1"));
    }


}
