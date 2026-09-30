package DesignPatterns.Prototype;

/**
 * Concrete prototype: copy returns a new circle with the same initial state.
 */
public class Circle implements Prototype<Circle> {
    private final String color;
    private double radius;

    public Circle(String color, double radius) {
        this.color = color;
        this.radius = radius;
    }

    @Override
    public Circle copy() {
        // These fields are values, so the new object does not share mutable state.
        // Because the color is a String (immutable) and radius is a primitive type (double),
        // so they are considered value types.
        // When we create a new Circle object, we are copying these values,
        // and the new object will have its own separate state.
        // This means that changes to the radius of the copied Circle
        // will not affect the original Circle, and vice versa.
        return new Circle(color, radius);
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    @Override
    public String toString() {
        return "Circle{color='" + color + "', radius=" + radius + "}";
    }
}
