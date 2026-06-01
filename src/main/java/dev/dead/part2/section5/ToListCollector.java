package dev.dead.part2.section5;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.IntStream;

public class ToListCollector<T> implements Collector<T, List<T>, List<T>> {

    public static void main(String[] args) {
        List<Integer> listOfNums = IntStream.rangeClosed(1, 100)
                .boxed()
                .collect(ToListCollector.personalToList());

        System.out.println(listOfNums);
    }

    public static <T> ToListCollector<T> personalToList() {
        return new ToListCollector<>();
    }

    @Override
    public Supplier<List<T>> supplier() {
        return ArrayList::new;
    }

    @Override
    public BiConsumer<List<T>, T> accumulator() {
        return List::add;
    }

    @Override
    public BinaryOperator<List<T>> combiner() {
        return (list1, list2) -> {
            list1.addAll(list2);
            return list1;
        };
    }

    @Override
    public Function<List<T>, List<T>> finisher() {
        return Function.identity();
    }

    @Override
    public Set<Characteristics> characteristics() {
        return Collections.unmodifiableSet(EnumSet.of(Characteristics.IDENTITY_FINISH));
    }
}
