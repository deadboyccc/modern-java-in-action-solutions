package DesignPatterns.Decorator;

public class BeverageFactory {

    // Acts as the starting point for a custom beverage
    public static BeverageBuilder create(Beverage baseBeverage) {
        return new BeverageBuilder(baseBeverage);
    }

    // A static inner helper to chain the decorators fluently
    public static class BeverageBuilder {
        private Beverage beverage;

        public BeverageBuilder(Beverage beverage) {
            this.beverage = beverage;
        }

        // Methods to add specific condiments dynamically
        public BeverageBuilder withCinnamon() {
            this.beverage = new Cinnamon(this.beverage);
            return this;
        }

        public BeverageBuilder withPepper() {
            this.beverage = new Pepper(this.beverage);
            return this;
        }

        // Terminal method to get the final decorated product
        public Beverage build() {
            return this.beverage;
        }
    }
}
