package DesignPatterns.Visitor;

import java.util.List;

/**
 * Client: apply independent operations to a fixed set of catalog element types.
 */
public class VisitorDemo {
    public static void main(String[] args) {
        List<CatalogItem> catalog = List.of(
                new Book("Patterns in Practice", 32.50, 280),
                new Magazine("Object Monthly", 8.00, 12));

        DescriptionVisitor descriptions = new DescriptionVisitor();
        PriceVisitor prices = new PriceVisitor();
        for (CatalogItem item : catalog) {
            item.accept(descriptions);
            item.accept(prices);
        }
        System.out.println("Catalog total: $" + prices.total());
    }
}
