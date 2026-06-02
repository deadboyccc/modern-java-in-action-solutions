package DesignPatterns.Factory;

import java.util.Collections;
import java.util.List;

public class NYVeggiePizza extends Pizza {
    public NYVeggiePizza() {
        super("NY Veggie", "NY dough", "NY Sauce", Collections.singletonList("test1"));
    }

    protected NYVeggiePizza(String name, String dough, String sauce, List<String> toppings) {
        super(name, dough, sauce, toppings);
    }
}
