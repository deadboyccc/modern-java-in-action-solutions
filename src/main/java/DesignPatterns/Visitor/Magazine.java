package DesignPatterns.Visitor;

/**
 * Another concrete element: it provides its type-specific data to the visitor.
 */
public record Magazine(String title, double price, int issue) implements CatalogItem {
    @Override
    public void accept(CatalogVisitor visitor) {
        visitor.visit(this);
    }
}
