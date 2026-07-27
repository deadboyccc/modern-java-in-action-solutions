package Concurrency;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import java.util.stream.IntStream;

public class CompletableFutureExamples {

    // Dedicated executor for I/O-bound async tasks using daemon threads
    private static final ExecutorService executor = Executors.newFixedThreadPool(300, runnable -> {
        Thread thread = new Thread(runnable);
        thread.setDaemon(true); // Ensures JVM shuts down without hanging
        return thread;
    });

    // Generate 200 mock shops with unique names
    private static final List<Shop> shops = IntStream.range(0, 200)
            .mapToObj(i -> new Shop("Shop-" + UUID.randomUUID()))
            .toList();

    public static void main(String[] args) {
        System.out.println("=== Running CompletableFuture Benchmarks ===\n");

        runBenchmark("1. Parallel Stream", () -> findPricesParallel("iPhone 15"));
        runBenchmark("2. Async CompletableFuture", () -> findPricesAsync("iPhone 15"));
        runBenchmark("3. Async Composition (thenCompose)", () -> findDiscountedPricesAsync("iPhone 15"));
        runBenchmark("4. Async Combination (thenCombine)", () -> findPricesAndCombineAsync("iPhone 15"));

        // Shutdown executor after all tasks finish
        executor.shutdown();
    }

    // =========================================================================
    // 1. Parallel Streams Baseline
    // =========================================================================

    /**
     * Finds prices using parallel streams.
     * Note: Bound by default to ForkJoinPool.commonPool() (# of CPU cores).
     */
    public static List<String> findPricesParallel(String product) {
        return shops.parallelStream()
                .map(shop -> String.format(Locale.US, "%s price is %.2f", shop.getName(), shop.getPrice(product)))
                .toList();
    }

    // =========================================================================
    // 2. Basic Async Execution
    // =========================================================================

    /**
     * Executes queries asynchronously across a custom thread pool.
     */
    public static List<String> findPricesAsync(String product) {
        List<CompletableFuture<String>> priceFutures = shops.stream()
                .map(shop -> CompletableFuture.supplyAsync(
                        () -> String.format(Locale.US, "%s price is %.2f", shop.getName(), shop.getPrice(product)),
                        executor
                ))
                .toList();

        return priceFutures.stream()
                .map(CompletableFuture::join)
                .toList();
    }

    // =========================================================================
    // 3. Pipeline Chaining & Sequential Composition (thenCompose)
    // =========================================================================

    /**
     * Demonstrates dependent async operations.
     * Use 'thenCompose' when Task B depends on the output of Task A (Flattening nested Futures).
     */
    public static List<String> findDiscountedPricesAsync(String product) {
        List<CompletableFuture<String>> priceFutures = shops.stream()
                .map(shop -> CompletableFuture.supplyAsync(
                                () -> String.format(Locale.US, "%s price is %.2f", shop.getName(), shop.getPrice(product)),
                                executor
                        )
                        .thenApply(priceString -> priceString.toLowerCase(Locale.ROOT))
                        // thenCompose chains another async operation returning a CompletableFuture
                        .thenCompose(priceString -> CompletableFuture.supplyAsync(
                                () -> String.format(Locale.US, " Discounted: %.2f", shop.getDiscount(priceString)),
                                executor
                        )))
                .toList();

        return priceFutures.stream()
                .map(CompletableFuture::join)
                .toList();
    }

    // =========================================================================
    // 4. Independent Async Combination (thenCombine)
    // =========================================================================

    /**
     * Demonstrates running two independent async operations in parallel and combining results.
     * Use 'thenCombine' when Task A and Task B can run simultaneously without waiting on each other.
     */
    public static List<String> findPricesAndCombineAsync(String product) {
        List<CompletableFuture<String>> priceFutures = shops.stream()
                .map(shop -> {
                    // Task A: Fetch initial formatted price
                    CompletableFuture<String> formattedPriceFuture = CompletableFuture.supplyAsync(
                            () -> String.format(Locale.US, "%s price is ", shop.getName()),
                            executor
                    );

                    // Task B: Fetch raw price independently
                    CompletableFuture<Double> rawPriceFuture = CompletableFuture.supplyAsync(
                            () -> shop.getPrice(product),
                            executor
                    );

                    // Combine results when BOTH futures complete
                    return formattedPriceFuture.thenCombine(
                            rawPriceFuture,
                            (formatted, raw) -> formatted + String.format(Locale.US, "%.2f", raw)
                    );
                })
                .toList();

        return priceFutures.stream()
                .map(CompletableFuture::join)
                .toList();
    }

    // =========================================================================
    // Benchmark Helper
    // =========================================================================

    private static void runBenchmark(String label, Supplier<List<String>> priceTask) {
        Instant start = Instant.now();
        List<String> prices = priceTask.get();
        long durationMs = Duration.between(start, Instant.now()).toMillis();

        System.out.printf("%-35s | Fetched: %3d items | Time: %4d ms%n", label, prices.size(), durationMs);
    }
}