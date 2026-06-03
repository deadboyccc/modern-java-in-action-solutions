package DesignPatterns.Factory;

public class LAPizzaIngredientFactory implements PizzaIngredientFactory {
    @Override
    public Dough createDough() {
        return new LADough();
    }

    @Override
    public Sauce createSauce() {
        return new LASauce();

    }

    @Override
    public Cheese createCheese() {
        return new LACheese();
    }
}
