package ApendixB;

import java.util.Arrays;

/**
 * Appendix B.3 - Arrays
 * <p>
 * Java 8 additions:
 * - parallelSort
 * - setAll
 * - parallelSetAll
 * - parallelPrefix
 */
public class B3_ArraysStudyGuide {

    // ------------------------------------------------------------
    // parallelSort
    // ------------------------------------------------------------

    static void parallelSortExamples() {
        System.out.println("\n=== parallelSort ===");

        int[] numbers = {9, 2, 7, 1, 5, 3};

        // Sorts the array in parallel.
        // For primitives, natural numeric order is used.
        Arrays.parallelSort(numbers);

        System.out.println(Arrays.toString(numbers));

        // Object arrays can use a Comparator.
        String[] names = {"Mario", "Alan", "Raoul", "Bob"};

        Arrays.parallelSort(
                names,
                (a, b) -> Integer.compare(a.length(), b.length()));

        System.out.println(Arrays.toString(names));

        // Range overload: sort only part of an array.
        int[] values = {5, 4, 3, 2, 1, 0};

        Arrays.parallelSort(values, 1, 5);

        System.out.println("range sort: "
                + Arrays.toString(values));
    }

    // ------------------------------------------------------------
    // setAll / parallelSetAll
    // ------------------------------------------------------------

    static void setAllExamples() {
        System.out.println("\n=== setAll / parallelSetAll ===");

        int[] evenNumbers = new int[10];

        // The function receives the index.
        // Here value = index * 2.
        Arrays.setAll(evenNumbers, i -> i * 2);

        System.out.println("setAll: "
                + Arrays.toString(evenNumbers));

        int[] squares = new int[10];

        // parallelSetAll computes indexes in parallel.
        // The function should be side-effect free.
        Arrays.parallelSetAll(squares, i -> i * i);

        System.out.println("parallelSetAll: "
                + Arrays.toString(squares));

        // Object arrays work too.
        String[] labels = new String[5];
        Arrays.setAll(labels, i -> "item-" + i);

        System.out.println("Object array: "
                + Arrays.toString(labels));
    }

    // ------------------------------------------------------------
    // parallelPrefix
    // ------------------------------------------------------------

    static void parallelPrefixExamples() {
        System.out.println("\n=== parallelPrefix ===");

        int[] ones = new int[10];
        Arrays.fill(ones, 1);

        // Each position becomes the combination of all values
        // from the beginning through that position.
        Arrays.parallelPrefix(ones, (a, b) -> a + b);

        System.out.println(Arrays.toString(ones));
        // Result:
        // [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]

        int[] products = {1, 2, 3, 4, 5};
        Arrays.parallelPrefix(products, (a, b) -> a * b);

        System.out.println("product prefix: "
                + Arrays.toString(products));

        // Range overload.
        int[] values = {100, 1, 1, 1, 1};
        Arrays.parallelPrefix(values, 1, 5, Integer::sum);

        System.out.println("range prefix: "
                + Arrays.toString(values));
    }

    public static void main(String[] args) {
        parallelSortExamples();
        setAllExamples();
        parallelPrefixExamples();
    }
}
