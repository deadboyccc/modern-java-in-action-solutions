package DesignPatterns.Decorator;

import java.lang.reflect.Constructor;
import java.util.List;

public class ReflectionBeverageFactory {

    // Private constructor prevents instantiation from other classes
    private ReflectionBeverageFactory() {
    }

    // Global access point to get the single instance of the factory
    public static ReflectionBeverageFactory getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * Dynamically builds a decorated beverage using reflection.
     * * @param recipe List of classes where the first element is the Base Beverage
     * and subsequent elements are Decorators.
     *
     * @return The fully constructed and wrapped Beverage object.
     */
    public Beverage createBeverage(List<Class<? extends Beverage>> recipe) {
        if (recipe == null || recipe.isEmpty()) {
            throw new IllegalArgumentException("Recipe list cannot be empty");
        }

        // Local variable maintains state per-request, keeping the singleton stateless
        Beverage currBev = null;

        for (Class<? extends Beverage> clazz : recipe) {
            try {
                if (currBev == null) {
                    // Base Component instantiation (e.g., Matcha) -> requires no-args constructor
                    Constructor<? extends Beverage> constructor = clazz.getConstructor();
                    currBev = constructor.newInstance();
                } else {
                    // Decorator instantiation (e.g., Cinnamon) -> requires Beverage constructor
                    Constructor<? extends Beverage> constructor = clazz.getConstructor(Beverage.class);
                    currBev = constructor.newInstance(currBev);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to dynamically build beverage at: " + clazz.getSimpleName(), e);
            }
        }

        return currBev;
    }

    // Static nested class - loaded only when getInstance() is called
    private static class Holder {
        private static final ReflectionBeverageFactory INSTANCE = new ReflectionBeverageFactory();
    }
}
