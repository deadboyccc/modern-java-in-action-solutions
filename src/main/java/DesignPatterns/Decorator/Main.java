package DesignPatterns.Decorator;

public class Main {
    static void main() {
        // ordering pure green Tea
        var greenTea = new GreenTea();
        System.out.println(greenTea.getDescription() + " : " + greenTea.cost());

        // cinnamon matcha
        var cinnamonCondiment = new Cinnamon(new Matcha());
        System.out.println(cinnamonCondiment.getDescription() + " : " + cinnamonCondiment.cost());


        // Pepper +Cinnamon + matcha
        var decoratedBev = new Cinnamon(new Pepper(new Matcha()));
        System.out.println(decoratedBev.getDescription() + " : " + decoratedBev.cost());


    }
}
