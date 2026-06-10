package DesignPatterns.Iterator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BreakfastMenu implements Menu {
    private final List<String> menu = new ArrayList<>();

    BreakfastMenu() {

        menu.add("Breakfast1");
        menu.add("Breakfast2");
        menu.add("Breakfast3");
        menu.add("Breakfast4");
        menu.add("Breakfast5");
        menu.add("Breakfast6");

    }

    @Override
    public Iterator<String> createIterator() {
        return menu.iterator();
    }
}
