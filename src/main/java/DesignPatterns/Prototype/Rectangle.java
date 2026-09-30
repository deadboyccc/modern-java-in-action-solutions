package DesignPatterns.Prototype;

/**
 * A second concrete prototype shows that copying is not tied to one class.
 */
public class Rectangle implements Prototype<Rectangle> {
    private final String color;
    private final double width;
    private final double height;

    public Rectangle(String color, double width, double height) {
        this.color = color;
        this.width = width;
        this.height = height;
    }

    @Override
    public Rectangle copy() {
        return new Rectangle(color, width, height);
    }

    @Override
    public String toString() {
        return "Rectangle{color='" + color + "', width=" + width + ", height=" + height + "}";
    }
}
