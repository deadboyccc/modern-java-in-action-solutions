package DesignPatterns.Iterator;

import java.util.Iterator;

public interface Menu {
    public Iterator<String> createIterator();
}