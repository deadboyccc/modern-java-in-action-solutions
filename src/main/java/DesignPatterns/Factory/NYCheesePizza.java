package DesignPatterns.Factory;

import java.util.Collections;
import java.util.List;

public class NYCheesePizza extends Pizza {
    public NYCheesePizza() {
        super("NY Cheese", "NY dough", "NY Sauce", Collections.singletonList("test1"));

    }

    protected NYCheesePizza(String name, String dough, String sauce, List<String> toppings) {
        super(name, dough, sauce, toppings);
    }
}
