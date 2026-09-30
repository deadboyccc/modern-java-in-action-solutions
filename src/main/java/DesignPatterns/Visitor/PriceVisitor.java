package DesignPatterns.Visitor;

/**
 * Concrete visitor: adds a new operation without putting it on catalog items.
 */
public class PriceVisitor implements CatalogVisitor {
    private double total;

    @Override
    public void visit(Book book) {
        total += book.price();
    }

    @Override
    public void visit(Magazine magazine) {
        total += magazine.price();
    }

    public double total() {
        return total;
    }
}
