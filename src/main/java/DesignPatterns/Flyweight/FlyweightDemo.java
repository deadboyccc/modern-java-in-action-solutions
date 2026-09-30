package DesignPatterns.Flyweight;

/**
 * Client demo: each tree has a position, while matching
 * appearance data [ TreeType ] is shared.
 */
public class FlyweightDemo {
    static void main(String[] args) {

        Forest forest = new Forest();
        forest.plantTree(10, 20, "Oak", "green", "rough");
        forest.plantTree(30, 40, "Oak", "green", "rough");
        forest.plantTree(70, 80, "Oak", "green", "rough");

        forest.plantTree(50, 60, "Pine", "dark green", "needle-like");
        forest.plantTree(90, 100, "Pine", "dark green", "needle-like");


        forest.draw();
        // should be 5 trees, but only 2 shared types (Oak and Pine)
        System.out.println(forest.treeCount() + " trees use " + forest.sharedTypeCount() + " shared types.");
    }
}
