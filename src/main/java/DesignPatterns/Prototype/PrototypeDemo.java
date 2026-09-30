package DesignPatterns.Prototype;

/**
 * Client: start from configured prototypes instead of repeating setup.
 */
public class PrototypeDemo {
    public static void main(String[] args) {
        Circle circlePrototype = new Circle("blue", 10);
        Circle circleCopy = circlePrototype.copy();
        circleCopy.setRadius(3);

        Rectangle rectanglePrototype = new Rectangle("red", 8, 4);
        Rectangle rectangleCopy = rectanglePrototype.copy();

        System.out.println("Prototype: " + circlePrototype);
        System.out.println("Copy:      " + circleCopy);
        System.out.println("Rectangle copy: " + rectangleCopy);
    }
}
