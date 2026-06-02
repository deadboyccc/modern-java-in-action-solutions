package DesignPatterns.Factory;

import java.util.Collections;
import java.util.List;

public class LAVeggiePizza extends Pizza {

    public LAVeggiePizza() {
        super("LA Veggie", "LA dough", "LA Sauce", Collections.singletonList("test1"));
    }

    protected LAVeggiePizza(String name, String dough, String sauce, List<String> toppings) {
        super(name, dough, sauce, toppings);
    }
}
