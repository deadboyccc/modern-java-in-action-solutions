package DesignPatterns.Flyweight;

/**
 * Flyweight: immutable tree data shared by every tree of the same type.
 */
public final class TreeType {
    private final String name;
    private final String color;
    private final String texture;

    TreeType(String name, String color, String texture) {
        this.name = name;
        this.color = color;
        this.texture = texture;
    }

    /**
     * The tree supplies its unique position; this shared object supplies its appearance.
     */
    void draw(int x, int y) {
        System.out.println(name + " tree (" + color + ", " + texture + ") at " + x + ", " + y);
    }

    String name() {
        return name;
    }
}
