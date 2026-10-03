package ApendixB;

import java.util.*;

/**
 * Appendix B.1 - Collections
 * <p>
 * Study guide for the main Java 8 Collections API additions described
 * in the appendix.
 */
public class B1_CollectionsStudyGuide {

    // ------------------------------------------------------------
    // MAP
    // ------------------------------------------------------------

    static void mapExamples() {
        System.out.println("\n=== MAP ===");

        Map<String, Integer> inventory = new HashMap<>();
        inventory.put("Aston Martin", 3);
        inventory.put("BMW", 2);

        // Java 8: return a default value when the key has NO mapping.
        int count = inventory.getOrDefault("Ferrari", 0);
        System.out.println("getOrDefault: " + count);

        // Important: if a key is mapped to null, the default is not returned.
        inventory.put("NullCar", null);
        System.out.println("mapped-to-null: "
                + inventory.getOrDefault("NullCar", 0));

        // forEach replaces many simple iteration loops.
        inventory.forEach((car, amount) ->
                System.out.println(car + " -> " + amount));

        // putIfAbsent only puts the value when there is no current mapping
        // for the key (a null mapping can therefore be replaced).
        inventory.putIfAbsent("Audi", 4);
        inventory.putIfAbsent("Audi", 99);
        System.out.println("putIfAbsent: " + inventory.get("Audi"));

        // compute always calculates a new value for the key.
        inventory.compute("Audi", (key, oldValue) ->
                oldValue == null ? 1 : oldValue + 1);
        System.out.println("compute: " + inventory.get("Audi"));

        // computeIfAbsent calculates a value only when the key has no value
        // (including a null value).
        Map<String, List<String>> carsByBrand = new HashMap<>();
        carsByBrand.computeIfAbsent("BMW", key -> new ArrayList<>()).add("M3");
        carsByBrand.computeIfAbsent("BMW", key -> new ArrayList<>()).add("M5");
        System.out.println("computeIfAbsent: " + carsByBrand);

        // computeIfPresent runs only when the key currently maps to a
        // non-null value.
        inventory.computeIfPresent("BMW", (key, oldValue) -> oldValue + 10);
        System.out.println("computeIfPresent: " + inventory.get("BMW"));

        // merge is convenient for "add if missing, combine if present".
        Map<String, Integer> wordCounts = new HashMap<>();
        wordCounts.merge("java", 1, Integer::sum);
        wordCounts.merge("java", 1, Integer::sum);
        wordCounts.merge("spring", 1, Integer::sum);
        System.out.println("merge: " + wordCounts);

        // remove(key, value) removes only when BOTH match.
        inventory.remove("Audi", 999); // does nothing
        inventory.remove("Audi", 5);   // removes the entry if value is 5
        System.out.println("remove(key, value): " + inventory);

        // replace only changes an existing mapping.
        inventory.replace("BMW", 100);
        System.out.println("replace: " + inventory.get("BMW"));

        // replace(key, oldValue, newValue) changes it only if oldValue matches.
        inventory.replace("BMW", 100, 200);

        // replaceAll applies a function to every map entry.
        inventory.replaceAll((car, amount) ->
                amount == null ? 0 : amount * 2);
        System.out.println("replaceAll: " + inventory);
    }

    static void mapCachingExample() {
        System.out.println("\n=== MAP CACHING ===");

        Map<String, String> cache = new HashMap<>();

        String first = getData(cache, "https://example.com");
        String second = getData(cache, "https://example.com");

        System.out.println(first);
        System.out.println(second);
    }

    // Same idea as the appendix: fetch only when the value is not cached.
    static String getData(Map<String, String> cache, String url) {
        return cache.computeIfAbsent(url, B1_CollectionsStudyGuide::fetchData);
    }

    static String fetchData(String url) {
        System.out.println("Fetching: " + url);
        return "data-for-" + url;
    }

    // ------------------------------------------------------------
    // ITERABLE / ITERATOR / COLLECTION
    // ------------------------------------------------------------

    static void iterableIteratorCollectionExamples() {
        System.out.println("\n=== ITERABLE / ITERATOR / COLLECTION ===");

        List<String> names = new ArrayList<>(
                Arrays.asList("Raoul", "Mario", "Alan"));

        // Iterable.forEach
        names.forEach(System.out::println);

        // Iterable.spliterator gives a Spliterator for traversal/splitting.
        Spliterator<String> spliterator = names.spliterator();
        System.out.println("Spliterator estimated size: "
                + spliterator.estimateSize());

        // Iterator.forEachRemaining processes all remaining elements.
        Iterator<String> iterator = names.iterator();
        if (iterator.hasNext()) {
            System.out.println("First: " + iterator.next());
        }
        iterator.forEachRemaining(name ->
                System.out.println("Remaining: " + name));

        // Collection.removeIf mutates the collection by removing matches.
        List<Integer> numbers = new ArrayList<>(
                Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));

        numbers.removeIf(n -> n % 2 != 0);

        System.out.println("removeIf: " + numbers);

        // stream() creates a Stream from the collection.
        long evenCount = numbers.stream()
                .filter(n -> n % 2 == 0)
                .count();

        // parallelStream() creates a potentially parallel Stream.
        long parallelCount = numbers.parallelStream()
                .filter(n -> n % 2 == 0)
                .count();

        System.out.println("stream count: " + evenCount);
        System.out.println("parallelStream count: " + parallelCount);
    }

    // ------------------------------------------------------------
    // LIST
    // ------------------------------------------------------------

    static void listExamples() {
        System.out.println("\n=== LIST ===");

        List<Integer> numbers = new ArrayList<>(
                Arrays.asList(1, 2, 3, 4, 5));

        // replaceAll mutates the List in place.
        numbers.replaceAll(x -> x * 2);
        System.out.println("replaceAll: " + numbers);

        // sort mutates the List in place.
        numbers.sort(Comparator.reverseOrder());
        System.out.println("sort: " + numbers);

        // Java 8 List.of is NOT available; List.of was added later.
        // The appendix's List "of" entry reflects the book's API table,
        // but on Java 8 use Arrays.asList(...) or Collections methods.
        List<Integer> fixedSize = Arrays.asList(10, 20, 30);
        System.out.println("Java 8 fixed-size list: " + fixedSize);
    }

    // ------------------------------------------------------------
    // COLLECTIONS CLASS
    // ------------------------------------------------------------

    static void collectionsClassExamples() {
        System.out.println("\n=== COLLECTIONS CLASS ===");

        NavigableSet<Integer> baseSet =
                new TreeSet<>(Arrays.asList(3, 1, 2));

        NavigableSet<Integer> unmodifiableSet =
                Collections.unmodifiableNavigableSet(baseSet);

        NavigableSet<Integer> synchronizedSet =
                Collections.synchronizedNavigableSet(baseSet);

        NavigableSet<Integer> checkedSet =
                Collections.checkedNavigableSet(baseSet, Integer.class);

        // Empty Navigable collections were also added to Collections.
        NavigableSet<Integer> emptySet =
                Collections.emptyNavigableSet();

        Queue<Integer> queue = new ArrayDeque<>();
        Queue<Integer> checkedQueue =
                Collections.checkedQueue(queue, Integer.class);

        NavigableMap<Integer, String> baseMap = new TreeMap<>();
        baseMap.put(1, "one");

        NavigableMap<Integer, String> unmodifiableMap =
                Collections.unmodifiableNavigableMap(baseMap);

        NavigableMap<Integer, String> synchronizedMap =
                Collections.synchronizedNavigableMap(baseMap);

        NavigableMap<Integer, String> checkedMap =
                Collections.checkedNavigableMap(baseMap, Integer.class, String.class);

        NavigableMap<Integer, String> emptyMap =
                Collections.emptyNavigableMap();

        System.out.println("unmodifiable set: " + unmodifiableSet);
        System.out.println("synchronized set: " + synchronizedSet);
        System.out.println("checked set: " + checkedSet);
        System.out.println("checked queue: " + checkedQueue);
        System.out.println("unmodifiable map: " + unmodifiableMap);
        System.out.println("synchronized map: " + synchronizedMap);
        System.out.println("checked map: " + checkedMap);
        System.out.println("empty navigable set: " + emptySet);
        System.out.println("empty navigable map: " + emptyMap);
    }

    // ------------------------------------------------------------
    // FACTORY METHODS MENTIONED IN THE APPENDIX TABLE
    // ------------------------------------------------------------

    static void factoryMethodNote() {
        System.out.println("\n=== COLLECTION FACTORY METHOD NOTE ===");

        /*
         * The appendix's table lists:
         *
         *     Map.of(...)
         *     Map.ofEntries(...)
         *     List.of(...)
         *     Set.of(...)
         *
         * Those factory methods are actually Java 9 APIs, not Java 8 APIs.
         * They are left here as study notes because they appear in the
         * supplied appendix table, but they are intentionally commented out
         * so this file remains Java 8-compilable.
         *
         * Java 9 examples:
         *
         *     Map<String, Integer> map =
         *             Map.of("A", 1, "B", 2);
         *
         *     Map<String, Integer> map2 =
         *             Map.ofEntries(
         *                     Map.entry("A", 1),
         *                     Map.entry("B", 2));
         *
         *     List<String> list = List.of("A", "B");
         *     Set<String> set = Set.of("A", "B");
         */
        System.out.println("Map/List/Set of(...) factories are shown in comments as later Java 9 APIs.");
    }

    // ------------------------------------------------------------
    // COMPARATOR
    // ------------------------------------------------------------

    static void comparatorExamples() {
        System.out.println("\n=== COMPARATOR ===");

        List<String> names = new ArrayList<>(
                Arrays.asList("Alan", "Raoul", "Mario", "Bob"));

        Comparator<String> byLength =
                Comparator.comparingInt(String::length);

        // reversed: reverse the current comparator.
        names.sort(byLength.reversed());
        System.out.println("reversed: " + names);

        // thenComparing: use another comparison when the first is equal.
        Comparator<String> byLengthThenName =
                Comparator.comparingInt(String::length)
                        .thenComparing(Comparator.naturalOrder());

        names.sort(byLengthThenName);
        System.out.println("thenComparing: " + names);

        // Primitive-specialized comparison avoids boxing the extracted value.
        Comparator<String> byLength2 =
                Comparator.comparingInt(String::length);

        Comparator<String> byDouble =
                Comparator.comparingDouble(String::length);

        Comparator<String> byLong =
                Comparator.comparingLong(String::length);

        System.out.println("primitive comparators created: "
                + byLength2 + ", " + byDouble + ", " + byLong);

        // Natural order for Comparable values.
        List<Integer> values = new ArrayList<>(Arrays.asList(3, 1, 2));
        values.sort(Comparator.naturalOrder());
        System.out.println("naturalOrder: " + values);

        // nullsFirst / nullsLast.
        List<String> withNulls = new ArrayList<>(
                Arrays.asList("Bob", null, "Alice"));

        withNulls.sort(Comparator.nullsFirst(Comparator.naturalOrder()));
        System.out.println("nullsFirst: " + withNulls);

        withNulls.sort(Comparator.nullsLast(Comparator.naturalOrder()));
        System.out.println("nullsLast: " + withNulls);

        // reverseOrder is equivalent to naturalOrder().reverse().
        values.sort(Comparator.reverseOrder());
        System.out.println("reverseOrder: " + values);
    }

    public static void main(String[] args) {
        mapExamples();
        mapCachingExample();
        iterableIteratorCollectionExamples();
        listExamples();
        collectionsClassExamples();
        factoryMethodNote();
        comparatorExamples();
    }
}
