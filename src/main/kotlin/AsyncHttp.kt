import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import kotlin.time.Duration.Companion.seconds

/*
 * Requires: kotlinx-coroutines-core AND kotlinx-coroutines-jdk8
 * (the jdk8 artifact provides CompletableFuture.await())
 *
 * build.gradle.kts:
 *   implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
 *   implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.9.0")
 */

// ---------------------------------------------------------------------------
// 1. Shared client
// ---------------------------------------------------------------------------
// A single HttpClient is thread-safe and meant to be reused across the whole
// app (it pools connections). Don't create a new one per request.
val client: HttpClient = HttpClient.newBuilder()
    .version(HttpClient.Version.HTTP_2)
    .connectTimeout(Duration.ofSeconds(10))
    .executor(Executors.newVirtualThreadPerTaskExecutor())
    .build()

fun request(url: String): HttpRequest =
    HttpRequest.newBuilder()
        .uri(URI.create(url))
        .timeout(Duration.ofSeconds(10))
        .GET()
        .build()

// ---------------------------------------------------------------------------
// 2. Blocking call (baseline — fine on a virtual thread, bad on a platform one)
// ---------------------------------------------------------------------------
fun fetchBlocking(url: String): String {
    val response = client.send(request(url), HttpResponse.BodyHandlers.ofString())
    return response.body()
}

// ---------------------------------------------------------------------------
// 3. Native async with CompletableFuture (no coroutines)
// ---------------------------------------------------------------------------
fun fetchAsyncFuture(url: String): CompletableFuture<String> =
    client.sendAsync(request(url), HttpResponse.BodyHandlers.ofString())
        .thenApply { it.body() }
        .exceptionally { e -> "error: ${e.message}" }

// ---------------------------------------------------------------------------
// 4. Bridging CompletableFuture into a suspend function
// ---------------------------------------------------------------------------
// This is the idiomatic bridge: sendAsync() returns a CompletableFuture,
// .await() (from kotlinx-coroutines-jdk8) suspends until it completes,
// propagating cancellation and exceptions naturally.
suspend fun fetch(url: String): String =
    client.sendAsync(request(url), HttpResponse.BodyHandlers.ofString())
        .await()
        .body()

// ---------------------------------------------------------------------------
// 5. Structured concurrency: run many requests concurrently, wait for all
// ---------------------------------------------------------------------------
suspend fun fetchAll(urls: List<String>): List<String> = coroutineScope {
    // async{} launches each call concurrently; awaitAll() suspends until
    // every one finishes. If one throws, the scope cancels the rest —
    // this is what "structured concurrency" buys you over raw futures.
    urls.map { url -> async { fetch(url) } }.awaitAll()
}

// ---------------------------------------------------------------------------
// 6. Limiting concurrency (don't open 500 sockets at once)
// ---------------------------------------------------------------------------
suspend fun fetchAllLimited(urls: List<String>, maxConcurrent: Int = 20): List<String> =
    coroutineScope {
        val semaphore = Semaphore(maxConcurrent)
        urls.map { url ->
            async {
                semaphore.withPermit { fetch(url) }
            }
        }.awaitAll()
    }

// ---------------------------------------------------------------------------
// 7. Per-request timeout independent of the HttpRequest timeout
// ---------------------------------------------------------------------------
suspend fun fetchWithTimeout(url: String, timeoutSeconds: Long = 5): String? =
    withTimeoutOrNull(timeoutSeconds.seconds) { fetch(url) }

// ---------------------------------------------------------------------------
// 8. Retry with exponential backoff
// ---------------------------------------------------------------------------
suspend fun fetchWithRetry(
    url: String,
    maxAttempts: Int = 3,
    initialDelayMs: Long = 200,
): String {
    var attempt = 0
    var delayMs = initialDelayMs
    while (true) {
        attempt++
        try {
            return fetch(url)
        } catch (e: Exception) {
            if (attempt >= maxAttempts) throw e
            delay(delayMs)
            delayMs *= 2 // exponential backoff
        }
    }
}

// ---------------------------------------------------------------------------
// 9. Fire-and-continue error isolation: one failure shouldn't kill the batch
// ---------------------------------------------------------------------------
// supervisorScope, unlike coroutineScope, does NOT cancel sibling children
// when one fails — pair it with runCatching per-request to collect partial
// results instead of losing everything to the first error.
suspend fun fetchAllTolerant(urls: List<String>): List<Result<String>> = supervisorScope {
    urls.map { url ->
        async { runCatching { fetch(url) } }
    }.awaitAll()
}

// ---------------------------------------------------------------------------
// 10. Virtual threads as a coroutine dispatcher (JDK 21+)
// ---------------------------------------------------------------------------
// Useful if you have existing blocking code (e.g. a JDBC driver, or a library
// with no suspend API) that you want to run without blocking a limited
// platform-thread pool. Each coroutine gets parked on a cheap virtual thread.
val virtualThreadDispatcher = Executors.newVirtualThreadPerTaskExecutor()
    .asCoroutineDispatcher()

suspend fun fetchOnVirtualThread(url: String): String =
    withContext(virtualThreadDispatcher) {
        // fetchBlocking() is a blocking call; safe here because virtual
        // threads are cheap to park — just don't do this on Dispatchers.Default.
        fetchBlocking(url)
    }

// ---------------------------------------------------------------------------
// main — demonstrates each pattern
// ---------------------------------------------------------------------------
fun main() = runBlocking {
    val urls = listOf(
        "https://example.com",
        "https://httpbin.org/delay/1",
        "https://httpbin.org/status/500",
    )

    println("-- blocking --")
    println(fetchBlocking(urls[0]).take(80))

    println("-- CompletableFuture async --")
    println(fetchAsyncFuture(urls[0]).await().take(80))

    println("-- suspend fetch --")
    println(fetch(urls[0]).take(80))

    println("-- concurrent fetchAll --")
    fetchAll(urls.take(2)).forEach { println(it.take(40)) }

    println("-- limited concurrency --")
    fetchAllLimited(urls.take(2), maxConcurrent = 2).forEach { println(it.take(40)) }

    println("-- with timeout --")
    println(fetchWithTimeout(urls[1], timeoutSeconds = 2) ?: "timed out")

    println("-- with retry --")
    runCatching { fetchWithRetry(urls[2], maxAttempts = 2) }
        .onFailure { println("failed after retries: ${it.message}") }

    println("-- tolerant batch (partial failures ok) --")
    fetchAllTolerant(urls).forEachIndexed { i, result ->
        result.fold(
            onSuccess = { println("[$i] ok: ${it.take(30)}") },
            onFailure = { println("[$i] failed: ${it.message}") },
        )
    }

    println("-- virtual thread dispatcher --")
    println(fetchOnVirtualThread(urls[0]).take(80))

    client.close() // JDK 21+: releases pooled connections
}

/*
 * Best-practice summary:
 * 1. One shared HttpClient for the app's lifetime — never per-request.
 * 2. Prefer suspend fun + .await() over raw CompletableFuture chains once
 *    you're inside coroutine code — it composes with structured concurrency,
 *    cancellation, and timeouts.
 * 3. Use coroutineScope/awaitAll for "all must succeed", supervisorScope for
 *    "collect what I can, tolerate individual failures".
 * 4. Always bound concurrency (Semaphore) when fanning out to many URLs.
 * 5. Wrap flaky calls in retry logic with backoff; don't retry immediately.
 * 6. Use withTimeoutOrNull for soft timeouts distinct from the HTTP-level
 *    connect/request timeouts already set on the client/request.
 * 7. Virtual threads (Executors.newVirtualThreadPerTaskExecutor()) are for
 *    bridging blocking code cheaply — coroutines are still the better default
 *    for new async code; use virtual threads as an interop tool, not a
 *    replacement for structured concurrency.
 */