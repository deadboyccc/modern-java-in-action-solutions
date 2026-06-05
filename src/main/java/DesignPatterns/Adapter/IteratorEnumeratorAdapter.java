package DesignPatterns.Adapter;

import java.util.Enumeration;
import java.util.Iterator;

public class IteratorEnumeratorAdapter implements Iterator<Object> {
    private final Enumeration<?> enumeration;

    public IteratorEnumeratorAdapter(Enumeration<?> enumeration) {
        this.enumeration = enumeration;
    }

    @Override
    public boolean hasNext() {
        return enumeration.hasMoreElements();
    }

    @Override
    public Object next() {
        return enumeration.nextElement();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
