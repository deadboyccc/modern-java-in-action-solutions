package ForkJoin;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.stream.LongStream;

public class ForkJoinSumCalculator extends RecursiveTask<Long> {

    public static final Long THRESHOLD = 10_000L;
    private final long[] numbers;
    private final int start;
    private final int end;
    public ForkJoinSumCalculator(long[] numbers) {
        this(numbers, 0, numbers.length);
    }

    private ForkJoinSumCalculator(long[] numbers, int start, int end) {
        this.numbers = numbers;
        this.start = start;
        this.end = end;
    }

    public static void main(String[] args) {
        long[] arr = LongStream.range(0, 1_000_000).toArray();

        ForkJoinSumCalculator task = new ForkJoinSumCalculator(arr);

        try (var pool = ForkJoinPool.commonPool()) {
            var answer = pool.invoke(task);
            System.out.println("Sum = " + answer);
        }
    }

    @Override
    protected Long compute() {
        var length = end - start;
        if (length < THRESHOLD) {
            return computeSequentially();
        }

        // Split the work
        ForkJoinSumCalculator leftTask =
                new ForkJoinSumCalculator(numbers, start, start + (length / 2));
        leftTask.fork(); // Pushes leftTask to the pool queue asynchronously

        ForkJoinSumCalculator rightTask =
                new ForkJoinSumCalculator(numbers, start + (length / 2), end);

        // Compute right in current thread, then join left result
        return rightTask.compute() + leftTask.join();
    }

    private long computeSequentially() {
        long sum = 0;
        for (int i = start; i < end; i++) {
            sum += numbers[i];
        }
        return sum;
    }
}
