package DesignPatterns.Factory;

import java.util.Collections;
import java.util.List;

public class LACheesePizza extends Pizza {

    public LACheesePizza() {
        super("LA Cheese", "LA dough", "LA Sauce", Collections.singletonList("test1"));
    }

    protected LACheesePizza(String name, String dough, String sauce, List<String> toppings) {
        super(name, dough, sauce, toppings);
    }
}
