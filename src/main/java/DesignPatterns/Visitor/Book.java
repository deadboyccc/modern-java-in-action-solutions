package DesignPatterns.Visitor;

/**
 * Concrete element: double dispatch selects the visitor's book operation.
 */
public record Book(String title, double price, int pages) implements CatalogItem {
    @Override
    public void accept(CatalogVisitor visitor) {
        visitor.visit(this);
    }
}
