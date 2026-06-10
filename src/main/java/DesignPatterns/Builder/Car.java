package DesignPatterns.Builder;

public class Car {

    // 2. Immutable Fields (Encapsulation)
    private final Brand brand;
    private final Color color;
    private final String modelName;
    // 3. Private Constructor (Enforces using the Builder)
    private Car(Brand brand, Color color, String modelName) {
        this.brand = brand;
        this.color = color;
        this.modelName = modelName;
    }

    // 4. Static entry point to start building fluidly
    public static CarBuilder builder() {
        return new CarBuilder();
    }

    // 6. Getters and toString
    public Brand getBrand() {
        return brand;
    }

    public Color getColor() {
        return color;
    }

    public String getModelName() {
        return modelName;
    }

    @Override
    public String toString() {
        return modelName + " (" + brand + ", " + color + ")";
    }

    // 1. Nested Enums
    public enum Brand {TOYOTA, FORD, BMW, TESLA}

    public enum Color {RED, BLUE, BLACK, WHITE}

    // 5. The Fluent Builder Class
    public static class CarBuilder {
        private Brand brand;
        private Color color;
        private String modelName;

        // Package-private constructor so it's instantiated via Car.builder()
        CarBuilder() {
        }

        public CarBuilder setBrand(Brand brand) {
            this.brand = brand;
            return this;
        }

        public CarBuilder setColor(Color color) {
            this.color = color;
            return this;
        }

        public CarBuilder setModelName(String modelName) {
            this.modelName = modelName;
            return this;
        }

        public Car build() {
            return new Car(brand, color, modelName);
        }
    }
}