package DesignPatterns.Template;

public class Matcha extends CaffeineBeverage {
    @Override
    void brew() {
        System.out.println("Brewing Matcha");

    }

    @Override
    void addCondiments() {
        System.out.println("Adding Matcha Condiments");

    }

    @Override
    protected void startHook() {
        System.out.println("Starting Matcha");
    }

    @Override
    protected void endHook() {
        System.out.println("Ending Matcha");
    }
}
