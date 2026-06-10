package DesignPatterns.Template;


public abstract class CaffeineBeverage {

    final void prepareRecipe() {
        startHook();
        boilWater();
        brew();
        pourInCup();
        addCondiments();
        endHook();
    }


    protected void endHook() {
    }

    protected void startHook() {


    }


    abstract void brew();

    abstract void addCondiments();

    void boilWater() {
        System.out.println("Boiling water");
    }

    void pourInCup() {
        System.out.println("Pouring into cup");
    }
}
