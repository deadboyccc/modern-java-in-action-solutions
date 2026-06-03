package DesignPatterns.Factory;

import java.util.Collections;

public class LAVeggiePizza extends Pizza {
    LAPizzaIngredientFactory ingredientFactory;

    public LAVeggiePizza(LAPizzaIngredientFactory ingredientFactory) {
        this.ingredientFactory = ingredientFactory;
        super("LA Veggie", ingredientFactory.createDough(), ingredientFactory.createSauce()
                , Collections.singletonList("test1"));
    }

}
