package DesignPatterns.Flyweight;

import java.util.ArrayList;
import java.util.List;

/**
 * Client: many lightweight tree contexts share a small set of tree types.
 */
public class Forest {
    private final TreeTypeFactory treeTypeFactory = new TreeTypeFactory();
    private final List<Tree> trees = new ArrayList<>();

    public void plantTree(int x, int y, String name, String color, String texture) {
        TreeType sharedType = treeTypeFactory.getTreeType(name, color, texture);
        trees.add(new Tree(x, y, sharedType));
    }

    public void draw() {
        trees.forEach(Tree::draw);
    }

    public int treeCount() {
        return trees.size();
    }

    public int sharedTypeCount() {
        return treeTypeFactory.sharedTypeCount();
    }
}
