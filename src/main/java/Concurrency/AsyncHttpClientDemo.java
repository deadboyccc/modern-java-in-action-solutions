package Concurrency;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

public class AsyncHttpClientDemo {

    // Reusable, thread-safe client with HTTP/2 and custom timeout
    private static final HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(10))
            .executor(Executors.newVirtualThreadPerTaskExecutor())
            .build();

    public static void main(String[] args) {
        System.out.println("--- 1. Simple Async GET Request ---");
        fetchSinglePost();

        System.out.println("\n--- 2. Chained Async Requests ---");
        fetchAndChainNextRequest();

        System.out.println("\n--- 3. Inlined Chained Async Requests ---");

        // Single-line chained pipel
        client.sendAsync(
                        HttpRequest.newBuilder(URI.create("https://www.google.com")).GET().build(),
                        HttpResponse.BodyHandlers.ofByteArray()
                )
                .thenApply(HttpResponse::body)
                .thenApply(bytes -> new String(bytes, StandardCharsets.UTF_8)) // Convert byte[] to String
                .thenAccept(html -> {
                    System.out.println("Fetched " + html.length() + " bytes of HTML from Google.");
                    System.out.println("Preview: " + html.substring(0, Math.min(html.length(), 20)) + "...");
                })
                .exceptionally(ex -> {
                    System.err.println("Request failed: " + ex.getMessage());
                    return null;
                }).join();
    }

    /**
     * Sends a single asynchronous GET request and prints the response body.
     */
    public static void fetchSinglePost() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://jsonplaceholder.typicode.com/posts/1"))
                .header("Accept", "application/json")
                .GET()
                .build();

        // sendAsync returns CompletableFuture<HttpResponse<String>>
        CompletableFuture<Void> future = client
                .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(body -> System.out.println("Response Body:\n" + body))
                .exceptionally(ex -> {
                    System.err.println("Request failed: " + ex.getMessage());
                    return null;
                });

        // Wait for completion (so the main thread doesn't exit prematurely)
        future.join();
    }

    /**
     * Fires a first request, processes the result, and chains a second async request using thenCompose.
     */
    public static void fetchAndChainNextRequest() {
        HttpRequest firstRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://jsonplaceholder.typicode.com/posts/1"))
                .GET()
                .build();

        CompletableFuture<Void> chainedFuture = client
                .sendAsync(firstRequest, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenCompose(firstResponseBody -> {
                    System.out.println("First request complete. Executing second request...");

                    // Example: using output from step 1 to build request 2 (e.g. user ID 1)
                    HttpRequest secondRequest = HttpRequest.newBuilder()
                            .uri(URI.create("https://jsonplaceholder.typicode.com/users/1"))
                            .GET()
                            .build();

                    return client.sendAsync(secondRequest, HttpResponse.BodyHandlers.ofString());
                })
                .thenApply(HttpResponse::body)
                .thenAccept(userResponseBody -> System.out.println("User Details:\n" + userResponseBody))
                .exceptionally(ex -> {
                    System.err.println("Chained request failed: " + ex.getMessage());
                    return null;
                });

        chainedFuture.join();
    }
}