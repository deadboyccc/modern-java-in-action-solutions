
package dev.dead;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Benchmarks sequential vs parallel stream performance across three workload types:
 * <p>
 * 1. CPU-bound   — arithmetic-heavy reduction (no I/O, no contention)
 * 2. Memory-bound — collect-to-list (dominated by allocation + GC pressure)
 * 3. Mixed       — filter + map + collect (realistic pipeline shape)
 * <p>
 * Varying DATA_SIZE lets you observe the crossover point where parallelism pays off.
 * On typical hardware parallel streams break even somewhere around 10_000–100_000
 * elements for CPU-bound work; memory-bound work often never breaks even due to
 * false sharing and allocator contention.
 * <p>
 * Run:
 * ./gradlew shadowJar
 * java -jar build/benchmarks.jar StreamParallelBenchmark
 */
@BenchmarkMode(Mode.AverageTime)          // report mean latency per op
@OutputTimeUnit(TimeUnit.MICROSECONDS)    // µs is readable for this range
@State(Scope.Benchmark)                   // one state instance shared across all threads
@Warmup(iterations = 3, time = 1)        // 3 × 1s warm-up forks
@Measurement(iterations = 5, time = 1)   // 5 × 1s measurement forks
@Fork(value = 2, jvmArgs = {
        "-Xms512m", "-Xmx512m",           // fixed heap → reproducible GC behaviour
        "-XX:+UseG1GC"
})
public class StreamParallelBenchmark {

    // ── tuneable parameters ─────────────────────────────────────────────────

    @Param({"1000", "100000", "1000000"})
    public int dataSize;

    // ── shared state ────────────────────────────────────────────────────────

    private List<Integer> data;
    /**
     * By default parallelStream() uses the common ForkJoinPool, which is shared
     * with the rest of the JVM. If your application has other parallel work in
     * flight the common pool can become a bottleneck.
     * <p>
     * This variant submits to a dedicated pool sized to the available processors.
     * Compare against parallelSum to see whether pool isolation buys anything
     * in a benchmark context (it usually doesn't — the common pool is already
     * sized to processors — but it matters under production load).
     * <p>
     * Note: the ForkJoinPool is created once at setup to avoid including
     * construction cost in the measurement.
     */
    private ForkJoinPool customPool;

    // ── 1. CPU-bound: sum reduction ─────────────────────────────────────────

    /**
     * Lets you run the benchmark directly from an IDE without the shadow jar step.
     * Only include benchmarks that match the regex pattern — change it freely.
     * <p>
     * Run → dev.dead.StreamParallelBenchmark.main()
     */
    public static void main(String[] args) throws RunnerException {
        Options opts = new OptionsBuilder()
                .include(StreamParallelBenchmark.class.getSimpleName())
                .param("dataSize", "100000")   // single size for quick IDE runs
                .warmupIterations(2)
                .measurementIterations(3)
                .forks(1)
                .build();

        new Runner(opts).run();
    }

    /**
     * Setup runs once per (fork × param) combination, outside any timed section.
     * The list is unmodifiable so all benchmarks share the same backing array
     * without accidental mutation.
     */
    @Setup(Level.Trial)
    public void setup() {
        data = IntStream.range(0, dataSize)
                .boxed()
                .collect(Collectors.toUnmodifiableList());
    }

    // ── 2. Memory-bound: collect to list ────────────────────────────────────

    /**
     * Purely arithmetic reduction. Parallel streams shine here when elements >> cores
     * because each worker gets a disjoint subrange with no shared mutable state.
     * <p>
     * Pitfall to watch: autoboxing Integer→long adds ~1ns/element; for a fair
     * comparison both variants do the same boxing, so relative numbers are valid.
     */
    @Benchmark
    public long sequentialSum() {
        return data.stream()
                .mapToLong(Integer::longValue)
                .sum();
    }

    @Benchmark
    public long parallelSum() {
        return data.parallelStream()
                .mapToLong(Integer::longValue)
                .sum();
    }

    // ── 3. Mixed pipeline: filter + expensive map + collect ─────────────────

    /**
     * Collect creates a new ArrayList and populates it. Parallel collect uses a
     * Collector that splits into sub-lists then merges — O(n) extra allocation.
     * For memory-bound tasks parallel often loses or ties due to:
     * - Thread-local allocators competing for the same TLAB pool
     * - The merge step being single-threaded
     * <p>
     * Compare throughput at different heap sizes to see GC pressure effects.
     */
    @Benchmark
    public List<Integer> sequentialCollect() {
        return data.stream()
                .filter(n -> n % 2 == 0)
                .collect(Collectors.toList());
    }

    @Benchmark
    public List<Integer> parallelCollect() {
        return data.parallelStream()
                .filter(n -> n % 2 == 0)
                .collect(Collectors.toList());
    }

    // ── 4. Custom ForkJoinPool (bonus) ───────────────────────────────────────

    /**
     * Simulates a realistic pipeline where each element requires non-trivial CPU work.
     * Math.sqrt + Math.log approximate "do something meaningful per element" without
     * I/O. Parallel typically wins here once dataSize is large enough to amortise
     * ForkJoinPool overhead (~a few thousand tasks minimum).
     * <p>
     * The Blackhole pattern is intentional: we want the JIT to believe the result is
     * consumed so it cannot eliminate the computation. Returning the list achieves
     * the same effect.
     */
    @Benchmark
    public List<Double> sequentialMixedPipeline() {
        return data.stream()
                .filter(n -> n % 3 == 0)
                .map(n -> Math.sqrt(n) + Math.log1p(n))   // log1p avoids log(0)
                .collect(Collectors.toList());
    }

    @Benchmark
    public List<Double> parallelMixedPipeline() {
        return data.parallelStream()
                .filter(n -> n % 3 == 0)
                .map(n -> Math.sqrt(n) + Math.log1p(n))
                .collect(Collectors.toList());
    }

    @Setup(Level.Trial)
    public void setupPool() {
        customPool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
    }

    @TearDown(Level.Trial)
    public void teardownPool() {
        customPool.shutdown();
    }

    // ── main (optional programmatic runner) ──────────────────────────────────

    @Benchmark
    public long customPoolSum() throws Exception {
        return customPool.submit(() ->
                data.parallelStream()
                        .mapToLong(Integer::longValue)
                        .sum()
        ).get();
    }
}
