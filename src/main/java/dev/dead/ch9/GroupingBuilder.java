package dev.dead.ch9;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collector;

import static java.util.stream.Collectors.groupingBy;

public class GroupingBuilder<T, D, K> {

    private final Collector<? super T, ?, Map<K, D>> collector;

    private GroupingBuilder(Collector<? super T, ?, Map<K, D>> collector) {
        this.collector = collector;
    }

    // Factory method to kick off the DSL builder fluidly
    public static <T, D, K> GroupingBuilder<T, List<T>, K> groupOn(Function<? super T, ? extends K> classifier) {
        return new GroupingBuilder<>(groupingBy(classifier));
    }

    // --- Example Usage Demonstration ---
    public static void main(String[] args) {
        // Dummy Car classes to demonstrate compilation and execution
        class Car {
            String getBrand() {
                return "Toyota";
            }

            String getColor() {
                return "Red";
            }
        }

        List<Car> cars = List.of(new Car());

        /*
         * Standard Java Approach (Inside-Out / Hard to read):
         * groupingBy(Car::getBrand, groupingBy(Car::getColor))
         *
         * DSL Approach (Fluent / Sequential / Left-to-Right):
         * "Group on color, then after that, group on brand."
         */
        Collector<? super Car, ?, Map<String, Map<String, List<Car>>>> carGroupingCollector =
                GroupingBuilder.groupOn(Car::getColor)
                        .after(Car::getBrand)
                        .get();

        Map<String, Map<String, List<Car>>> groupedCars = cars.stream().collect(carGroupingCollector);
        System.out.println("DSL Collector executed successfully: " + groupedCars);
    }

    public Collector<? super T, ?, Map<K, D>> get() {
        return collector;
    }

    // Wraps the current collector inside a new outer groupingBy collector
    public <J> GroupingBuilder<T, Map<K, D>, J> after(Function<? super T, ? extends J> classifier) {
        return new GroupingBuilder<>(groupingBy(classifier, collector));
    }
}