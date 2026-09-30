package DesignPatterns.Builder;

import java.util.Objects;

/**
 * Product: the builder gathers the parts before this immutable car is created.
 */
public class Car {
    private final Brand brand;
    private final Color color;
    private final String modelName;

    /**
     * Only the builder can call this constructor, so cars are built consistently.
     */
    private Car(Brand brand, Color color, String modelName) {
        this.brand = brand;
        this.color = color;
        this.modelName = modelName;
    }

    /** Builder entry point keeps the construction process readable at the call site. */
    public static CarBuilder builder() {
        return new CarBuilder();
    }

    public Brand getBrand() {
        return brand;
    }

    public Color getColor() {
        return color;
    }

    public String getModelName() {
        return modelName;
    }

    /**
     * Enums keep the example's choices simple and valid.
     */
    public enum Brand {TOYOTA, FORD, BMW, TESLA}

    public enum Color {RED, BLUE, BLACK, WHITE}

    @Override
    public String toString() {
        return modelName + " (" + brand + ", " + color + ")";
    }

    /** Builder: separates gathering the required parts from creating the product. */
    public static class CarBuilder {
        private Brand brand;
        private Color color;
        private String modelName;

        private CarBuilder() {
        }

        /** Each setter returns this builder so calls can be chained. */
        public CarBuilder setBrand(Brand brand) {
            this.brand = Objects.requireNonNull(brand, "brand");
            return this;
        }

        public CarBuilder setColor(Color color) {
            this.color = Objects.requireNonNull(color, "color");
            return this;
        }

        public CarBuilder setModelName(String modelName) {
            this.modelName = Objects.requireNonNull(modelName, "modelName");
            return this;
        }

        /** Validation happens once, at the boundary between building and using a car. */
        public Car build() {
            Objects.requireNonNull(brand, "brand is required");
            Objects.requireNonNull(color, "color is required");
            Objects.requireNonNull(modelName, "modelName is required");
            return new Car(brand, color, modelName);
        }
    }
}