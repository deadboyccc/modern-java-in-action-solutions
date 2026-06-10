package dev.dead;


import java.util.List;
import java.util.Optional;

public class FunctionalInterfacesDemo {
    static void main() {
        var list = List.of("hello", "world");
        printProcessed(list, l -> Optional.of(l.getFirst()));
        printProcessed(list, l -> Optional.of(l.getLast()));


    }

    public static void printProcessed(List<String> l, ListProcessor lp) {
        System.out.println(lp.process(l).orElse("Empty"));

    }


    // 1. Define the Functional Interface
    @FunctionalInterface
    public interface ListProcessor {
        Optional<String> process(List<String> l);
    }
}
