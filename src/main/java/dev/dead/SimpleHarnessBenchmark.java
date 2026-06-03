package dev.dead;

import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(jvmArgs = {"-Xms4G", "-Xmx4G"})
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
public class SimpleHarnessBenchmark {

    @Benchmark
    public long seqSum(BenchmarkState state) {
        long sum = 0L;
        for (int i = 0; i < state.count; i++) {
            sum += i;
        }
        return sum;
    }

    @Benchmark
    public long parallelStreamSum(BenchmarkState state) {
        return IntStream.range(0, state.count).parallel().sum();
    }

    // Using State prevents the JIT compiler from optimizing away the limits
    @State(Scope.Benchmark)
    public static class BenchmarkState {
        public int count = 10_000_000;
    }
}