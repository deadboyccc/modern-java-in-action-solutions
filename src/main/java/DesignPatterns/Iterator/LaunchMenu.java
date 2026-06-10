package DesignPatterns.Iterator;


import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class LaunchMenu implements Menu {
    private final List<String> menu = new LinkedList<>();

    LaunchMenu() {
        menu.add("Launch1");
        menu.add("Launch2");
        menu.add("Launch3");
        menu.add("Launch4");


    }

    @Override
    public Iterator<String> createIterator() {
        return menu.iterator();
    }
}
