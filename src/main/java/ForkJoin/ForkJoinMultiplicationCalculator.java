package ForkJoin;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.stream.IntStream;

public class ForkJoinMultiplicationCalculator extends RecursiveTask<Long> {

    private static final long THRESHOLD = 5;
    private final int[] numbers;
    private final int start;
    private final int end;
    public ForkJoinMultiplicationCalculator(int[] numbers) {
        this(numbers, 0, numbers.length);
    }

    private ForkJoinMultiplicationCalculator(int[] numbers, int start, int end) {
        this.numbers = numbers;
        this.start = start;
        this.end = end;
    }

    public static void main(String[] args) {
        int[] arr = IntStream.range(1, 20).toArray();

        var task = new ForkJoinMultiplicationCalculator(arr);

        try (var pool = ForkJoinPool.commonPool()) {
            var answer = pool.invoke(task);
            System.out.println("Answer = " + answer);
        }
    }

    @Override
    protected Long compute() {
        var length = end - start;
        if (length < THRESHOLD) {
            return computeSequentially();
        }

        // left task
        var leftTask = new ForkJoinMultiplicationCalculator(numbers, start, start + (length / 2));
        leftTask.fork();

        // right task
        var rightTask = new ForkJoinMultiplicationCalculator(numbers, start + (length / 2), end);

        return rightTask.compute() * leftTask.join();
    }

    // streamed sequential product
    private long computeSequentially() {
        return IntStream.range(start, end)
                .mapToLong(i -> numbers[i])
                .reduce(1L, (a, b) -> a * b);
    }
}