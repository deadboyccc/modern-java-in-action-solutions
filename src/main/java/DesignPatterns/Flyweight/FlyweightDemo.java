package DesignPatterns.Flyweight;

/**
 * Client demo: each tree has a position, while matching appearance data is shared.
 */
public class FlyweightDemo {
    public static void main(String[] args) {
        Forest forest = new Forest();
        forest.plantTree(10, 20, "Oak", "green", "rough");
        forest.plantTree(30, 40, "Oak", "green", "rough");
        forest.plantTree(50, 60, "Pine", "dark green", "needle-like");

        forest.draw();
        System.out.println(forest.treeCount() + " trees use " + forest.sharedTypeCount() + " shared types.");
    }
}
