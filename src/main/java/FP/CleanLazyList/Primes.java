package FP.CleanLazyList;

public class Primes {

    /**
     * Infinite stream of incrementing integers starting at {@code n}.
     */
    static LazyList<Integer> numbersFrom(int n) {
        return LazyList.cons(n, () -> numbersFrom(n + 1));
    }

    /**
     * Recursive prime sieve.
     */
    static LazyList<Integer> primes(LazyList<Integer> numbers) {
        int prime = numbers.head();
        return LazyList.cons(prime, () -> primes(numbers.tail().filter(n -> n % prime != 0)));
    }

    void main() {
        var first10Primes = primes(numbersFrom(2)).take(10);
        IO.println("First 10 primes: " + first10Primes);
        // First 10 primes: [2, 3, 5, 7, 11, 13, 17, 19, 23, 29]
    }
}