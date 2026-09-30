package DesignPatterns.Builder;

/**
 * Client: each build sequence reads like a description of the resulting product.
 */
public class BuilderTestDrive {
    public static void main(String[] args) {
        Car toyota = Car.builder()
                .setBrand(Car.Brand.TOYOTA)
                .setColor(Car.Color.BLACK)
                .setModelName("Toyota")
                .build();

        Car bmw = Car.builder()
                .setBrand(Car.Brand.BMW)
                .setColor(Car.Color.BLUE)
                .setModelName("BMW")
                .build();

        System.out.println(toyota);
        System.out.println(bmw);
    }
}
