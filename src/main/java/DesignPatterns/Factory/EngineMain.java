package DesignPatterns.Factory;

public class EngineMain {
    static void main() {
        PizzaStore newYorkPizzaStore = new NYPizzaStore();
        PizzaStore laPizzaStore = new LAPizzaStore();

        newYorkPizzaStore.orderPizza("Cheese");
        newYorkPizzaStore.orderPizza("Veggie");

        laPizzaStore.orderPizza("Cheese");
        laPizzaStore.orderPizza("Veggie");


        // abstract Factory -> Many factories impl
        // abstract Product -> Many product per factory

    }
}
