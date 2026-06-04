package DesignPatterns.Singleton;

import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RateLimiterPolicyVerifier {
    private static final Logger logger = Logger.getLogger(RateLimiterPolicyVerifier.class.getName());

    private static final ExecutorService threadPool = HttpClientThreadPool.INSTANCE.getExecutor();
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final Semaphore rateLimiter = new Semaphore(2);

    private static final AtomicInteger completed = new AtomicInteger(0);
    private static final AtomicInteger failed = new AtomicInteger(0);

    public static void printStats() {
        logger.info("Stats - Success: " + completed.get() + ", Failures: " + failed.get());
    }

    public static void bombard(URL url, int count) {
        logger.info("Starting bombardment: " + count + " requests to " + url);

        for (int i = 0; i < count; i++) {
            threadPool.submit(() -> {
                try {
                    rateLimiter.acquire();
                    logger.fine("Permit acquired, sending request");

                    try {
                        HttpRequest request = HttpRequest.newBuilder(url.toURI())
                                .GET()
                                .build();

                        HttpResponse<String> response = httpClient.send(request,
                                HttpResponse.BodyHandlers.ofString());

                        if (response.statusCode() == 200) {
                            completed.incrementAndGet();
                            logger.fine("Success [200]");
                        } else {
                            failed.incrementAndGet();
                            logger.warning("Non-2xx response [" + response.statusCode() + "]");
                        }
                    } finally {
                        rateLimiter.release();
                        logger.fine("Permit released");
                    }
                } catch (Exception e) {
                    failed.incrementAndGet();
                    logger.log(Level.SEVERE, "Request failed: " + e.getMessage(), e);
                }
            });
        }

        logger.info("Bombardment submitted: " + count + " tasks queued");


    }
}