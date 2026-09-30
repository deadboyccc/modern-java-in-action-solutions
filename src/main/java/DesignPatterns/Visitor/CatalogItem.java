package DesignPatterns.Visitor;

/**
 * Element: accepts an operation object that can work with this item type.
 */
public interface CatalogItem {
    void accept(CatalogVisitor visitor);
}
