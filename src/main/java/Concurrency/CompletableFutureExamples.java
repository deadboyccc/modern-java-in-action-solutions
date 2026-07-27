package Concurrency;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

public class CompletableFutureExamples {

    // Custom daemon thread pool dedicated to I/O-bound tasks
    private static final ExecutorService executor = Executors.newFixedThreadPool(300,
            r -> {
                Thread t = new Thread(r);
                t.setDaemon(true); // Allows the JVM to shut down cleanly
                return t;
            }
    );

    // Generate 300 test shops with unique names
    private static final List<Shop> shops = IntStream.range(0, 300)
            .mapToObj(i -> new Shop("Shop-" + UUID.randomUUID()))
            .toList();

    public static void main(String[] args) {
        // Measure execution time for CompletableFuture async search
        Instant startAsync = Instant.now();
        List<String> asyncPrices = findPricesFutureAsync("testDemo");
        System.out.println("Fetched " + asyncPrices.size() + " prices async.");
        System.out.println("Async Duration: " + Duration.between(startAsync, Instant.now()).toMillis() + " ms");

        System.out.println("-".repeat(30));

        // Measure execution time for Parallel Stream search
        Instant startParallel = Instant.now();
        List<String> parallelPrices = findPrices("testDemo");
        System.out.println("Fetched " + parallelPrices.size() + " prices parallel.");
        System.out.println("Parallel Duration: " + Duration.between(startParallel, Instant.now()).toMillis() + " ms");
    }

    /**
     * Finds prices using parallel streams.
     * Note: Bound to the common ForkJoinPool thread count (usually # of CPU cores).
     */
    public static List<String> findPrices(String product) {
        return shops.parallelStream()
                .map(shop -> String.format("%s price is %.2f", shop.getName(), shop.getPrice(product)))
                .toList();
    }

    /**
     * Finds prices asynchronously using CompletableFuture with a custom executor pool.
     */
    public static List<String> findPricesFutureAsync(String product) {
        List<CompletableFuture<String>> priceFutures = shops.stream()
                .map(shop -> CompletableFuture.supplyAsync(
                        () -> String.format("%s price is %.2f", shop.getName(), shop.getPrice(product)),
                        executor
                ))
                .toList();

        return priceFutures.stream()
                .map(CompletableFuture::join)
                .toList();
    }
}