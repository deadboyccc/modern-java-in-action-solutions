package ApendixB;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.*;

/**
 * Appendix B.2 - Concurrency
 * <p>
 * Focus:
 * - AtomicInteger / AtomicLong update methods
 * - Adders and Accumulators
 * - ConcurrentHashMap additions
 */
public class B2_ConcurrencyStudyGuide {

    // ------------------------------------------------------------
    // ATOMIC
    // ------------------------------------------------------------

    static void atomicExamples() {
        System.out.println("\n=== ATOMIC ===");

        AtomicInteger value = new AtomicInteger(10);

        // getAndUpdate:
        // update atomically, but RETURN the old value.
        int oldValue = value.getAndUpdate(x -> x + 5);
        System.out.println("getAndUpdate old: " + oldValue);
        System.out.println("current: " + value.get());

        // updateAndGet:
        // update atomically, and RETURN the new value.
        int newValue = value.updateAndGet(x -> x * 2);
        System.out.println("updateAndGet new: " + newValue);

        // getAndAccumulate:
        // combine current value with a supplied value,
        // return the old value.
        int oldBeforeAccumulate =
                value.getAndAccumulate(10, Integer::sum);

        System.out.println("getAndAccumulate old: "
                + oldBeforeAccumulate);
        System.out.println("current: " + value.get());

        // accumulateAndGet:
        // combine current value with a supplied value,
        // return the updated value.
        int updated =
                value.accumulateAndGet(10, Integer::sum);

        System.out.println("accumulateAndGet new: " + updated);

        // Example from the appendix:
        // atomically keep the minimum of current value and 10.
        AtomicInteger observed = new AtomicInteger(20);
        int min = observed.accumulateAndGet(10, Integer::min);

        System.out.println("minimum: " + min);
    }

    // ------------------------------------------------------------
    // ADDERS
    // ------------------------------------------------------------

    static void adderExamples() {
        System.out.println("\n=== ADDERS ===");

        LongAdder adder = new LongAdder();

        // LongAdder starts at 0.
        adder.add(10);
        adder.add(20);
        adder.increment();

        // Read the current accumulated sum.
        long sum = adder.sum();

        System.out.println("LongAdder sum: " + sum);

        DoubleAdder doubleAdder = new DoubleAdder();
        doubleAdder.add(1.5);
        doubleAdder.add(2.5);

        System.out.println("DoubleAdder sum: " + doubleAdder.sum());
    }

    // ------------------------------------------------------------
    // ACCUMULATORS
    // ------------------------------------------------------------

    static void accumulatorExamples() {
        System.out.println("\n=== ACCUMULATORS ===");

        // The accumulator receives:
        // current value + new value -> new accumulated value.
        LongAccumulator sum =
                new LongAccumulator(Long::sum, 0);

        sum.accumulate(10);
        sum.accumulate(20);
        sum.accumulate(30);

        System.out.println("LongAccumulator result: " + sum.get());

        // A different combining function can compute a maximum.
        LongAccumulator max =
                new LongAccumulator(Long::max, Long.MIN_VALUE);

        max.accumulate(4);
        max.accumulate(12);
        max.accumulate(7);

        System.out.println("maximum: " + max.get());

        DoubleAccumulator product =
                new DoubleAccumulator((a, b) -> a * b, 1.0);

        product.accumulate(2.0);
        product.accumulate(3.0);

        System.out.println("DoubleAccumulator product: "
                + product.get());
    }

    // ------------------------------------------------------------
    // CONCURRENTHASHMAP
    // ------------------------------------------------------------

    static void concurrentHashMapExamples() {
        System.out.println("\n=== CONCURRENTHASHMAP ===");

        ConcurrentHashMap<String, Integer> map =
                new ConcurrentHashMap<>();

        map.put("A", 10);
        map.put("B", 20);
        map.put("C", 30);

        // forEach works on map entries.
        map.forEach(1, (key, value) ->
                System.out.println(key + " -> " + value));

        // reduce combines values into one result.
        int total = map.reduceValues(
                1,
                Integer::sum);

        System.out.println("reduceValues total: " + total);

        // search stops when the search function returns a non-null result.
        String found = map.search(
                1,
                (key, value) -> value > 15 ? key : null);

        System.out.println("search found: " + found);

        // Key-specific, value-specific, and entry-specific forms.
        map.forEachKey(1, key ->
                System.out.println("key: " + key));

        int valueSum = map.reduceValuesToInt(
                1,
                Integer::intValue,
                0,
                Integer::sum);

        System.out.println("reduceValuesToInt: " + valueSum);

        int keyHashSum = map.reduceKeysToInt(
                1,
                String::hashCode,
                0,
                Integer::sum);

        System.out.println("reduceKeysToInt: " + keyHashSum);

        int entrySum = map.reduceEntriesToInt(
                1,
                entry -> entry.getValue(),
                0,
                Integer::sum);

        System.out.println("reduceEntriesToInt: " + entrySum);

        // mappingCount returns a long.
        long count = map.mappingCount();
        System.out.println("mappingCount: " + count);

        // keySet is a live view backed by the map.
        java.util.Set<String> keyView = map.keySet();
        map.put("D", 40);

        System.out.println("keySet view: " + keyView);

        // newKeySet creates a Set backed by a ConcurrentHashMap.
        java.util.Set<String> concurrentSet = ConcurrentHashMap.<String>newKeySet();
        concurrentSet.add("Java");
        concurrentSet.add("Spring");

        System.out.println("newKeySet: " + concurrentSet);
    }

    public static void main(String[] args) {
        atomicExamples();
        adderExamples();
        accumulatorExamples();
        concurrentHashMapExamples();
    }
}
