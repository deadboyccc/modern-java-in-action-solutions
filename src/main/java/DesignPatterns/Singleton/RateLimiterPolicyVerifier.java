package DesignPatterns.Singleton;

import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RateLimiterPolicyVerifier {
    private static final Logger logger = Logger.getLogger(RateLimiterPolicyVerifier.class.getName());
    private static final ExecutorService executor = HttpClientThreadPool.INSTANCE.getExecutor();
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final Semaphore rateLimiter = new Semaphore(2);

    private static final AtomicInteger successCount = new AtomicInteger();
    private static final AtomicInteger failureCount = new AtomicInteger();

    public static void bombard(URL url, int requestCount) {
        logger.info("Starting bombardment: %d requests to %s".formatted(requestCount, url));

        List<Future<?>> tasks = new ArrayList<>();
        for (int i = 0; i < requestCount; i++) {
            Future<?> task = executor.submit(() -> sendRequest(url));
            tasks.add(task);
        }

        logger.info("Submitted %d tasks, awaiting completion".formatted(requestCount));
        awaitTasks(tasks);
        logger.info("All tasks completed");
    }

    private static void awaitTasks(List<Future<?>> tasks) {
        for (Future<?> task : tasks) {
            try {
                task.get();
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Task execution failed", e);
            }
        }
    }

    private static void sendRequest(URL url) {
        try {
            rateLimiter.acquire();
            executeRequest(url);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            failureCount.incrementAndGet();
            logger.log(Level.WARNING, "Request interrupted", e);
        } finally {
            rateLimiter.release();
        }
    }

    private static void executeRequest(URL url) {
        try {
            HttpRequest request = HttpRequest.newBuilder(url.toURI())
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (isSuccess(response.statusCode())) {
                successCount.incrementAndGet();
            } else {
                failureCount.incrementAndGet();
                logger.warning("Non-2xx response [%d] from %s".formatted(response.statusCode(), url));
            }
        } catch (Exception e) {
            failureCount.incrementAndGet();
            logger.log(Level.SEVERE, "Request execution failed", e);
        }
    }

    private static boolean isSuccess(int statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }

    public static void printStats() {
        int success = successCount.get();
        int failure = failureCount.get();
        int total = success + failure;
        double rate = total > 0 ? (success * 100.0 / total) : 0;

        logger.info("Stats - Success: %d, Failures: %d, Total: %d, Rate: %.1f%%"
                .formatted(success, failure, total, rate));
    }

    public static void reset() {
        successCount.set(0);
        failureCount.set(0);
    }
}