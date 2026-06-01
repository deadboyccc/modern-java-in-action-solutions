package DesignPatterns.Decorator;

public class SimpleFactoryImprovedMain {

    public static void main(String[] args) {
        // 1. Ordering pure green Tea
        var greenTea = new GreenTea();
        System.out.println(greenTea.getDescription() + " : " + greenTea.cost());

        // 2. Cinnamon matcha
        var cinnamonMatcha = BeverageFactory.create(new Matcha())
                .withCinnamon()
                .build();
        System.out.println(cinnamonMatcha.getDescription() + " : " + cinnamonMatcha.cost());


        // 3. Pepper + Cinnamon + Matcha
        var decoratedBev = BeverageFactory.create(new Matcha())
                .withPepper()
                .withCinnamon()
                .build();
        System.out.println(decoratedBev.getDescription() + " : " + decoratedBev.cost());
    }
}
