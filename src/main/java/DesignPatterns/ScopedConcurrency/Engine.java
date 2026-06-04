package DesignPatterns.ScopedConcurrency;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Engine {

    public static void main(String[] args) {
        System.out.println("🚀 Initializing Production-Ready Virtual Thread Engine");
        System.out.println("Running on Main Thread: " + Thread.currentThread());

        // The try-with-resources implicitly calls executor.close(),
        // acting as a clean barrier that waits for all tasks to complete.
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {

            System.out.println("\nForking parallel operations on Virtual Threads...");

            // 1. Submit tasks to execute concurrently on individual virtual threads
            Future<String> dbFuture = executor.submit(Engine::fetchDatabaseRecord);
            Future<String> apiFuture = executor.submit(Engine::fetchExternalApiData);

            // 2. Await results using plain, readable blocking calls.
            // .get() will unmount the virtual thread without blocking the underlying OS carrier thread.
            String dbResult = dbFuture.get();
            String apiResult = apiFuture.get();

            System.out.println("\n✨ Both virtual thread tasks completed successfully:");
            System.out.println("-> DB Output: " + dbResult);
            System.out.println("-> API Output: " + apiResult);

        } catch (InterruptedException e) {
            System.err.println("❌ Engine execution was interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            // Unwraps any exception thrown inside our async methods (e.g., RuntimeException)
            System.err.println("❌ Pipeline execution encountered an error: " + e.getCause().getMessage());
        }

        System.out.println("\n🔒 Engine shutdown safely. All virtual threads terminated cleanly.");
    }

    // --- High-Performance Network/IO Callbacks ---

    private static String fetchDatabaseRecord() {
        try {
            Thread.sleep(Duration.ofMillis(150));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("DB fetch interrupted", e);
        }
        return "User Profile Data [" + Thread.currentThread() + "]";
    }

    private static String fetchExternalApiData() {
        try {
            Thread.sleep(Duration.ofMillis(250));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("API fetch interrupted", e);
        }
        return "Payment Status Verified [" + Thread.currentThread() + "]";
    }
}