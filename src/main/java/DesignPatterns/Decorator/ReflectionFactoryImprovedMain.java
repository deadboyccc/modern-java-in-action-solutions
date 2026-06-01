package DesignPatterns.Decorator;

import java.util.List;

public class ReflectionFactoryImprovedMain {


    public static void main(String[] args) {
        // Grab our single factory instance
        ReflectionBeverageFactory factory = ReflectionBeverageFactory.getInstance();

        // Recipe 1: Matcha with Pepper and Cinnamon
        List<Class<? extends Beverage>> complexRecipe = List.of(
                Matcha.class,
                Pepper.class,
                Cinnamon.class
        );
        Beverage spicyMatcha = factory.createBeverage(complexRecipe);
        System.out.println(spicyMatcha.getDescription() + " : $" + spicyMatcha.cost());


        // Recipe 2: Just plain Green Tea
        List<Class<? extends Beverage>> simpleRecipe = List.of(GreenTea.class);
        Beverage plainTea = factory.createBeverage(simpleRecipe);
        System.out.println(plainTea.getDescription() + " : $" + plainTea.cost());


        // Verifying it is indeed a true Singleton
        ReflectionBeverageFactory anotherFactoryRef = ReflectionBeverageFactory.getInstance();
        System.out.println("\nIs it the exact same factory instance? " + (factory == anotherFactoryRef));
    }
}
