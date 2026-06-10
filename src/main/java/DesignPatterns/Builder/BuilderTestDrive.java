package DesignPatterns.Builder;

public class BuilderTestDrive {
    static void main() {
        var car1 = Car.builder()
                .setBrand(Car.Brand.TOYOTA)
                .setColor(Car.Color.BLACK)
                .setModelName("Toyota")
                .build();
        var car2 = Car.builder()
                .setBrand(Car.Brand.BMW)
                .setColor(Car.Color.BLACK)
                .setModelName("BMW");
    }
}
