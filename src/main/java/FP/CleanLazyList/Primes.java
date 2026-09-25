package FP.CleanLazyList;

/**
 * Demonstrates the classic Sieve of Eratosthenes expressed lazily:
 * an infinite stream of numbers, progressively filtered by each
 * prime found, computing only as many terms as are ever requested.
 */
public class Primes {

    /**
     * An infinite lazy stream of consecutive integers starting at {@code n}.
     * Nothing beyond {@code n} is computed until the tail is forced.
     */
    static LazyList<Integer> numbersFrom(int n) {
        return LazyList.cons(n, () -> numbersFrom(n + 1));
    }

    /**
     * Sieve of Eratosthenes, lazily.
     * <p>
     * The head of {@code numbers} is always prime (everything divisible by an
     * earlier prime has already been filtered out). Each recursive step:
     * <ol>
     *   <li>emits the current head as the next prime, and</li>
     *   <li>defers filtering multiples of it out of the rest of the stream,
     *       so only as many numbers as are actually consumed get tested.</li>
     * </ol>
     */
    static LazyList<Integer> primes(LazyList<Integer> numbers) {
        int prime = numbers.head();
        return LazyList.cons(prime, () -> primes(numbers.tail().filter(n -> n % prime != 0)));
    }

    void main() {
        // Start the sieve at 2 (the first candidate) and force just the first 10 results.
        var first10Primes = primes(numbersFrom(2)).take(10);
        IO.println("First 10 primes: " + first10Primes);
        // First 10 primes: [2, 3, 5, 7, 11, 13, 17, 19, 23, 29]
    }
}
