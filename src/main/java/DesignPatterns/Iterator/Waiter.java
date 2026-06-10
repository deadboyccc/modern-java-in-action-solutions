package DesignPatterns.Iterator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Waiter {
    List<Menu> menus = new ArrayList<>();

    Waiter(List<Menu> menus) {
        this.menus = menus;
    }

    public void printAll() {
        var it = menus.iterator();
        while (it.hasNext()) {
            Iterator<String> innerMenuIterator = it.next().createIterator();
            printMenu(innerMenuIterator);

        }
    }

    public void printMenu(Iterator<String> menu) {
        while (menu.hasNext()) {
            System.out.println(menu.next());
        }
    }

}
