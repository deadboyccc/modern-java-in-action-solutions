package DesignPatterns.Visitor;

/**
 * Visitor: declares one operation for each concrete element type.
 * Adding a new element type means updating every visitor implementation.
 */
public interface CatalogVisitor {
    void visit(Book book);

    void visit(Magazine magazine);
}
