package dev.dead.part2.section5;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.IntStream;

/**
 * An optimized custom collector that partitions numbers into primes (true)
 * and non-primes (false) using previously discovered prime numbers.
 */
public class PrimeNumbersCollector
        implements Collector<Integer,
        Map<Boolean, List<Integer>>,
        Map<Boolean, List<Integer>>> {

    public static Map<Boolean, List<Integer>>
    partitionPrimesWithCustomCollector(int n) {
        return IntStream.rangeClosed(2, n)
                .boxed()
                .collect(new PrimeNumbersCollector());
    }

    public static void main(String[] args) {
        long fastest = Long.MAX_VALUE;

        System.out.println("Warmup and execution tracking starting...");

        for (int i = 0; i < 10; i++) {
            long start = System.nanoTime();

            partitionPrimesWithCustomCollector(1_000_000);

            long duration =
                    (System.nanoTime() - start) / 1_000_000;

            fastest = Math.min(fastest, duration);
        }

        System.out.printf(
                "Fastest execution done in %d ms%n",
                fastest
        );
    }

    @Override
    public Supplier<Map<Boolean, List<Integer>>> supplier() {
        return () -> {
            Map<Boolean, List<Integer>> acc = new HashMap<>();
            acc.put(true, new ArrayList<>());
            acc.put(false, new ArrayList<>());
            return acc;
        };
    }

    @Override
    public BiConsumer<Map<Boolean, List<Integer>>, Integer> accumulator() {
        return (acc, candidate) -> {
            boolean isPrime =
                    IsPrimeOptimization.isPrime(acc.get(true), candidate);

            acc.get(isPrime).add(candidate);
        };
    }

    @Override
    public BinaryOperator<Map<Boolean, List<Integer>>> combiner() {
        return (left, right) -> {
            left.get(true).addAll(right.get(true));
            left.get(false).addAll(right.get(false));
            return left;
        };
    }

    @Override
    public Function<Map<Boolean, List<Integer>>,
            Map<Boolean, List<Integer>>> finisher() {
        return Function.identity();
    }

    @Override
    public Set<Characteristics> characteristics() {
        return EnumSet.of(Characteristics.IDENTITY_FINISH);
    }
}
