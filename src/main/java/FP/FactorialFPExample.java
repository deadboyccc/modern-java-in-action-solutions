package FP;

import java.util.stream.LongStream;

public class FactorialFPExample {
    static long factorialIterative(long n) {
        long r = 1;
        for (int i = 1; i <= n; i++) {
            r *= i;
        }
        return r;
    }

    static long factorialRecursive(long n) {
        return n == 1 ? 1 : n * factorialRecursive(n - 1);
    }

    static long factorialStreams(long n) {
        return LongStream.rangeClosed(1, n).reduce(1, (long a, long b) -> a * b);
    }

    static long factorialTailRecursive(long n) {
        return factorialHelper(1, n);
    }

    static long factorialHelper(long acc, long n) {
        return n == 1 ? acc : factorialHelper(acc * n, n - 1);
    }

    static void main() {
        long n = 5;
        // not precise but gives a rough idea of the time taken for each method
        var now = System.currentTimeMillis();

        System.out.println("Factorial of " + n + " (iterative): " + factorialIterative(n));
        System.out.println("Factorial of " + n + " (recursive): " + factorialRecursive(n));
        System.out.println("Factorial of " + n + " (streams): " + factorialStreams(n));
        System.out.println("Factorial of " + n + " (tail recursive): " + factorialTailRecursive(n));
        System.out.println("Time taken: " + (System.currentTimeMillis() - now) + " ms");
    }

}
