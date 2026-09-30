package DesignPatterns.Visitor;

/**
 * Element: accepts an operation object [ visitor ] that can work with this item type.
 */
public interface CatalogItem {
    void accept(CatalogVisitor visitor);
}
