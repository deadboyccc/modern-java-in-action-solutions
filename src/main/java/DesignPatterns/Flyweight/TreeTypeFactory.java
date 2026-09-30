package DesignPatterns.Flyweight;

import java.util.HashMap;
import java.util.Map;

/**
 * Flyweight factory: reuse one shared tree type for each appearance combination.
 */
public class TreeTypeFactory {
    private final Map<TreeKey, TreeType> treeTypes = new HashMap<>();

    public TreeType getTreeType(String name, String color, String texture) {
        TreeKey key = new TreeKey(name, color, texture);
        return treeTypes.computeIfAbsent(key, k -> new TreeType(k.name(), k.color(), k.texture()));
    }

    public int sharedTypeCount() {
        return treeTypes.size();
    }

    /**
     * Value key ensures trees share data only when all intrinsic details match.
     */
    private record TreeKey(String name, String color, String texture) {
    }
}
