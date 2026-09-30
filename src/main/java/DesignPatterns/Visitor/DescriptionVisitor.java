package DesignPatterns.Visitor;

/**
 * A second visitor shows that another operation can be added independently.
 */
public class DescriptionVisitor implements CatalogVisitor {
    @Override
    public void visit(Book book) {
        System.out.println("Book: " + book.title() + ", " + book.pages() + " pages");
    }

    @Override
    public void visit(Magazine magazine) {
        System.out.println("Magazine: " + magazine.title() + ", issue " + magazine.issue());
    }
}
