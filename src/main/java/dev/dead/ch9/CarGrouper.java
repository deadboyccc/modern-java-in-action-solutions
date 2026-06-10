package dev.dead.ch9;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;

import static java.util.stream.Collectors.groupingBy;

public class CarGrouper {

    // --- Main Execution ---
    public static void main(String[] args) {
        // 1. Create a sample dataset of cars
        List<Car> cars = Arrays.asList(
                new Car(Brand.TOYOTA, Color.RED, "Camry"),
                new Car(Brand.TOYOTA, Color.WHITE, "RAV4"),
                new Car(Brand.BMW, Color.BLACK, "M3"),
                new Car(Brand.BMW, Color.BLACK, "X5"),
                new Car(Brand.BMW, Color.BLUE, "3 Series"),
                new Car(Brand.FORD, Color.RED, "Mustang"),
                new Car(Brand.TESLA, Color.WHITE, "Model 3")
        );

        // 2. Define the nested collector exactly as specified
        Collector<? super Car, ?, Map<Brand, Map<Color, List<Car>>>> carGroupingCollector =
                groupingBy(Car::getBrand, groupingBy(Car::getColor));

        // 3. Apply the collector to the stream
        Map<Brand, Map<Color, List<Car>>> groupedCars = cars.stream()
                .collect(carGroupingCollector);

        // 4. Print the formatted nested Map structure
        System.out.println("--- Grouped Inventory ---");
        groupedCars.forEach((brand, colorMap) -> {
            System.out.println("\nBrand: " + brand);
            colorMap.forEach((color, carList) -> {
                System.out.println("  " + color + " -> " + carList);
            });
        });
    }

    // --- Enums & Model ---
    public enum Brand {TOYOTA, FORD, BMW, TESLA}

    public enum Color {RED, BLUE, BLACK, WHITE}

    public static class Car {
        private final Brand brand;
        private final Color color;
        private final String modelName;

        public Car(Brand brand, Color color, String modelName) {
            this.brand = brand;
            this.color = color;
            this.modelName = modelName;
        }

        public Brand getBrand() {
            return brand;
        }

        public Color getColor() {
            return color;
        }

        @Override
        public String toString() {
            return modelName + " (" + color + ")";
        }
    }
}