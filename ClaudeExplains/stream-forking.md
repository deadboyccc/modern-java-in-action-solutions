# Forking a Stream: `StreamForker` explained

*Modern Java in Action, Appendix C: "Performing multiple operations in parallel on a stream"*

> **Scope.** Your excerpt starts at Listing C.2. Listing C.1 (the `StreamForker` skeleton: fields, `fork()`, `getResults()`, `Results`) is not in it, so I reconstructed it from how the later listings use it (§4.1). Everything here was compiled and run on **JDK 25**. Claims tagged **(measured)** come from those runs. The sandbox had **1 vCPU**, so timings show *overhead*, not parallel speed-up.

---

## 1. TL;DR

- **Problem:** a `Stream` can be traversed once, but you want several results (a joined string, a sum, a max, a grouping) from one pass.
- **Idea:** *push* every element of the one source stream into N queues. Turn each queue back into its own *pulled* `Stream` that runs one of your functions on its own thread.
- **Key trick:** a custom `Spliterator` whose `tryAdvance` blocks on a `BlockingQueue`. It adapts "someone pushes elements to me" into "a Stream pulls elements from me".
- **Verdict (the book says it too):** often *slower* than traversing several times when data is in memory. It pays off only for single-pass sources with heavy per-element work.
- **Modern Java:** first reach for `Collectors.teeing` or a composite collector (§9.1–9.2). If you truly need concurrent forks, use the rewrite in §9.3 (virtual threads, bounded queues, batching, typed keys, real error handling).

---

## 2. The problem

A stream is single-use:

```java
Stream<Dish> s = menu.stream();
int total = s.mapToInt(Dish::getCalories).sum();
s.map(Dish::getName).toList();
// IllegalStateException: stream has already been operated upon or closed   (measured)
```

Goal: one traversal, four results:

```java
Results r = new StreamForker<Dish>(menu.stream())
    .fork("shortMenu",     s -> s.map(Dish::getName).collect(joining(", ")))
    .fork("totalCalories", s -> s.mapToInt(Dish::getCalories).sum())
    .fork("mostCaloric",   s -> s.collect(reducing((a, b) -> a.getCalories() > b.getCalories() ? a : b)).get())
    .fork("byType",        s -> s.collect(groupingBy(Dish::getType)))
    .getResults();
int total = r.get("totalCalories");
```

Why not the obvious alternatives?

| Option | Why it falls short |
|---|---|
| Call `menu.stream()` four times | Needs a re-streamable, cheap source. `Files.lines(hugeFile)` re-reads the disk each time; a socket or cursor cannot be replayed. |
| `collect(toList())` first, then stream it N times | O(n) memory. Defeats streaming. |
| **`StreamForker`** | One pass over the source; each element is handed to every operation. |

---

## 3. Architecture at a glance

```
 caller thread                          one worker thread per fork (CompletableFuture)
 ─────────────                          ──────────────────────────────────────────────
 source.forEach(consumer)
   │ accept(t)       ┌─► queue1 ─► BlockingQueueSpliterator1 ─► Stream1 ─► f1 ─► Future1
   ├─ broadcast t ───┼─► queue2 ─► BlockingQueueSpliterator2 ─► Stream2 ─► f2 ─► Future2
   │                 └─► queue3 ─► BlockingQueueSpliterator3 ─► Stream3 ─► f3 ─► Future3
   └─ finish(): END_OF_STREAM into every queue (so every Stream terminates)

 results.get(key)  ──►  future.get()   (blocks until that fork is done)
```

Two directions of control meet in the middle:

- **Left of the queues = push.** The source stream's `forEach` *pushes* elements into the consumer.
- **Right of the queues = pull.** Each fork's terminal operation *pulls* elements through its Spliterator.

The queues and the Spliterator exist solely to convert push into pull.

| Piece | Role |
|---|---|
| `StreamForker<T>` | Fluent builder: stores `key → function`, then wires and runs everything. |
| `ForkingStreamConsumer<T>` | Push side. Broadcasts each element to all queues. Also doubles as the `Results` handle. |
| `BlockingQueue<T>` (one per fork) | Mailbox between the producer thread and one fork thread. |
| `BlockingQueueSpliterator<T>` | Pull side. Makes a queue look like a stream source. |
| `CompletableFuture.supplyAsync` | Runs a fork's function on its own thread and holds its result. |
| `END_OF_STREAM` | Poison pill: "no more elements". |

---

## 4. The pieces, one by one

### 4.1 `StreamForker`: the builder (Listing C.1, reconstructed)

```java
public class StreamForker<T> {
    private final Stream<T> stream;
    private final Map<Object, Function<Stream<T>, ?>> forks = new HashMap<>();

    public StreamForker(Stream<T> stream) { this.stream = stream; }

    public StreamForker<T> fork(Object key, Function<Stream<T>, ?> f) {
        forks.put(key, f);
        return this;                       // fluent
    }

    public Results getResults() {
        ForkingStreamConsumer<T> consumer = build();   // C.2: wire queues + futures
        try {
            stream.sequential().forEach(consumer);     // push every element to every queue
        } finally {
            consumer.finish();                         // always send END_OF_STREAM
        }
        return consumer;                               // hidden behind the Results interface
    }

    public interface Results { <R> R get(Object key); }
}
```

`fork` only *records* a function. Nothing is created until `getResults()` because the number of queues is unknown until then.

A fork is a `Function<Stream<T>, ?>`, not a `Collector`. So it can use **any** stream operation, including primitive streams (`mapToInt(...).sum()`) and `sorted()`/`limit()`.

### 4.2 `build()` and `getOperationResult`: the wiring (Listings C.2 and C.3)

For `fork("count", s -> s.count())` and `fork("sum", s -> s.mapToInt(..).sum())`:

```
start:           queues = []                       actions = {}
entry "count":   q1 = new LinkedBlockingQueue      queues = [q1]
                 sp1 = new BlockingQueueSpliterator(q1)
                 S1  = StreamSupport.stream(sp1, false)       // a Stream over an EMPTY queue
                 supplyAsync(() -> f_count.apply(S1))         // starts NOW on a pool thread,
                                                              // immediately blocks in q1.take()
                 actions = {"count" → Future1}
entry "sum":     same with q2 / S2 / Future2       queues = [q1, q2]
return           new ForkingStreamConsumer(queues, actions)
```

Two things to notice:

1. **The forks start before any data exists.** (measured) Each fork logs "starts its terminal op" on a `ForkJoinPool.commonPool-worker-N` thread *before* `main` emits element 1.
2. `build()` uses `reduce` with a *mutable* `HashMap` as identity, purely as a way to loop. It works because the stream is sequential. It is a misuse of `reduce` (a plain `for` loop or `collect` would be correct), and would break on a parallel stream (`ArrayList` is not thread-safe).

### 4.3 `ForkingStreamConsumer`: the push side (Listing C.4)

```java
public void accept(T t) { queues.forEach(q -> q.add(t)); }   // broadcast
void finish()           { accept((T) END_OF_STREAM); }       // broadcast the poison pill
public <R> R get(Object key) { return ((Future<R>) actions.get(key)).get(); } // block until done
```

Example with source `[x, y]` and two forks:

```
accept(x)  → q1=[x]        q2=[x]
accept(y)  → q1=[x,y]      q2=[x,y]
finish()   → q1=[x,y,END]  q2=[x,y,END]
```

It is a `Consumer` so it can be handed to `stream.forEach(...)`. It also implements `Results` so the same object carries the futures. Returning it as the narrow `Results` type hides `accept`/`finish` from callers.

### 4.4 `END_OF_STREAM`: the poison pill

A `Stream` ends when `tryAdvance` returns `false`. A queue cannot say "I'm closed", so the end must travel *in-band*, as a special element:

```java
static final Object END_OF_STREAM = new Object();
...
if (t != ForkingStreamConsumer.END_OF_STREAM)   // compared by IDENTITY (!=), not equals
```

A fresh `new Object()` compared by identity can never collide with a real element. `finish()` runs in a `finally`, so even if the source throws, every fork is told to stop and nothing waits forever.

### 4.5 `CompletableFuture.supplyAsync`: why threads are mandatory

Each fork's terminal operation is a **blocking pull loop**. If run on the caller's thread, the first fork would block on its empty queue while nobody pushes (the pusher *is* that same thread) → deadlock. So every fork needs its own thread, and `supplyAsync` gives you the thread and the result holder (`Future`) in one call. The default executor is `ForkJoinPool.commonPool()` (measured: `commonPool-worker-N`).

---

## 5. The Spliterator, fully

### 5.1 What a Spliterator is

A `Spliterator` is **an `Iterator` that can also split itself in two, and describes itself**. Every `Stream` is "a pipeline of operations over a Spliterator":

```java
list.stream()   ≈   StreamSupport.stream(list.spliterator(), /*parallel*/ false)
```

The terminal operation drives the Spliterator: it calls `tryAdvance` (short-circuiting ops such as `findFirst`) or `forEachRemaining` (which defaults to looping `tryAdvance`) until the Spliterator says it is exhausted.

| Method | Contract | Book's value | Better value |
|---|---|---|---|
| `boolean tryAdvance(Consumer)` | If an element remains, hand it to the consumer and return `true`; else return `false`. | blocks on queue | same |
| `Spliterator trySplit()` | Carve off a part for another thread, or return `null` = "can't split". | `null` | `null` |
| `long estimateSize()` | Rough remaining count; `Long.MAX_VALUE` if unknown/infinite. | `0` | `Long.MAX_VALUE` |
| `int characteristics()` | Bit flags: `ORDERED`, `SIZED`, `SUBSIZED`, `DISTINCT`, `SORTED`, `NONNULL`, `IMMUTABLE`, `CONCURRENT`. | `0` | `ORDERED` |

### 5.2 `tryAdvance`: the only method that matters here (Listing C.5)

```java
public boolean tryAdvance(Consumer<? super T> action) {
    T t;
    while (true) {
        try { t = q.take(); break; }          // ① wait until the producer has pushed something
        catch (InterruptedException e) { }    // ② (swallowed, see §8)
    }
    if (t != END_OF_STREAM) {                 // ③ real element
        action.accept(t);                     //    deliver it to the pipeline
        return true;                          //    "there may be more"
    }
    return false;                             // ④ pill seen: exhausted
}
```

Trace with the queue `[a, b, END]`:

| Call | `take()` returns | Action | Returns |
|---|---|---|---|
| 1 | `a` | `action.accept(a)` | `true` |
| 2 | `b` | `action.accept(b)` | `true` |
| 3 | `END` | none | `false` → the stream is finished |

If the queue is empty at any call, `take()` **parks the fork's thread** until the producer adds something. That is how the pull side waits for the push side.

### 5.3 Minimal standalone demo of the whole trick (measured: prints `[10, 20, 30]`)

```java
BlockingQueue<Object> queue = new LinkedBlockingQueue<>();
Object END = new Object();

// PULL side: a Spliterator whose elements come from the queue
Spliterator<Integer> source = new Spliterators.AbstractSpliterator<>(Long.MAX_VALUE, Spliterator.ORDERED) {
    @Override public boolean tryAdvance(Consumer<? super Integer> action) {
        try {
            Object o = queue.take();                 // blocks until something is pushed
            if (o == END) return false;
            action.accept((Integer) o);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
};

// PUSH side: another thread produces at its own pace
Thread.startVirtualThread(() -> { for (int i = 1; i <= 3; i++) queue.add(i); queue.add(END); });

System.out.println(StreamSupport.stream(source, false).map(i -> i * 10).toList());   // [10, 20, 30]
```

### 5.4 Why a Spliterator and not an `Iterator`?

- **The Stream API only accepts Spliterators** (`StreamSupport.stream(Spliterator, boolean)`). An `Iterator` has to be wrapped via `Spliterators.spliteratorUnknownSize` anyway.
- **`hasNext()` cannot block cleanly.** It must answer *without consuming*, so over a queue you would `take()`, stash the element in a field, and hand it out in `next()`. `tryAdvance` fuses "wait + fetch + deliver" into one call, with no look-ahead state.

### 5.5 "Late-binding": what it means and why it matters here

The Javadoc definition: a late-binding Spliterator binds to its data source at first traversal, first split, or first size query, not when it is created. The book uses the term more loosely, for the property that actually matters here: **the Stream is built before any data exists, and nothing is read until the terminal operation runs.**

(measured) Build the stream, fill the queue *afterwards*, then run the terminal op:

```
stream built, queue size = 0 (nothing consumed, nothing blocked)
queue filled AFTER building the stream, size = 3
map sees a
map sees b
terminal op -> [a, b]
```

That is exactly what `getOperationResult` relies on: it creates `S1` over an empty queue, long before the producer starts.

### 5.6 Why `trySplit()` returns `null`

Splitting exists so a *parallel* stream can hand halves to different threads. A live queue cannot be split: the elements don't exist yet, there is no random access, and the size is unknown. Returning `null` means "not splittable", so even `.parallel()` would run this source sequentially. That is also why the book passes `parallel = false`.

### 5.7 Two small inaccuracies in the book's Spliterator

- **`estimateSize()` returns `0`**, which says "empty". The contract says to return `Long.MAX_VALUE` when unknown. (Harmless here, since the stream is sequential and not `SIZED`. measured: `getExactSizeIfKnown() == -1`.)
- **`characteristics()` returns `0`**, so the fork's stream is *unordered*. (measured: `ORDERED declared? false`.) The FIFO queue does preserve the order the source delivered, so `ORDERED` is the truthful flag. In a sequential pipeline the difference is invisible, but it is semantically right.

---

## 6. End-to-end timeline

Source `[1, 2, 3]`, forks `sum` and `cnt` (this matches the measured log):

| Step | Thread | Event |
|---|---|---|
| 1 | `main` | `getResults()` → `build()` creates `q1`, `q2`, `S1`, `S2`. |
| 2 | pool-1, pool-2 | Both forks start their terminal ops and block in `take()` on empty queues. |
| 3 | `main` | `stream.forEach(consumer)`: source emits 1, then 2, then 3. Each is copied into `q1` and `q2`. Each `add` wakes a blocked fork. |
| 4 | `main` | `finally { finish() }` puts `END` in both queues. |
| 5 | `main` | `getResults()` **returns**. The forks may still be draining (measured: "getResults() returned" printed *before* "fork done"). |
| 6 | pool-1, pool-2 | Forks consume `1,2,3`, hit `END` → `tryAdvance` returns `false` → terminal ops finish → futures complete. |
| 7 | `main` | `results.get("sum")` blocks on `Future.get()` until step 6 is done. |

Precision on the book's claim "`getResults` returns immediately": it returns without waiting for the **forks**, but it does **block for the whole traversal of the source** (step 3, synchronous in the caller's thread).

---

## 7. Design choices: what was chosen, and why

| Decision | Why | Cost / trade-off |
|---|---|---|
| **A fork is `Function<Stream<T>, ?>`** | Maximum freedom: any terminal op, primitive streams, `sorted`, `limit`. | Weakly typed result (`?`), keys are `Object`. |
| **One queue per fork (broadcast)** | A single shared queue would give each element to only *one* consumer. Every fork needs *every* element. | Memory and work scale with the number of forks. |
| **Queue as the bridge** | A Stream is pull-based and single-consumer; the source is pushed. A blocking queue converts one into the other and decouples speeds. | A lock hand-off per element per fork. |
| **Custom `Spliterator`** | The only way to feed a Stream from a blocking source (see §5.4). | ~25 lines of low-level code. |
| **Thread per fork (`supplyAsync`)** | Each fork is a blocking pull loop; N blocking loops need N threads (§4.5). | Threads. In the common pool they occupy (or force the pool to add) workers. |
| **In-band poison pill** | A queue has no "closed" state. | Needs an unchecked `(T)` cast, and null elements cannot be queued. |
| **Unbounded `LinkedBlockingQueue` + `add`** | `add` never blocks or throws, so the producer code stays trivial: no `InterruptedException`, no back-pressure logic. | Unbounded memory (§8). |
| **Builder + deferred wiring** | Queue count is unknown until all forks are registered. | `getResults()` is one-shot. |
| **`Consumer` that is also `Results`** | One object holds both the queues and the futures; no extra class. | Mixed responsibilities, hidden behind the interface. |
| **`stream.sequential().forEach`** | One producer thread means no concurrent writers and a stable order for every fork. | The source is never parallelised. |
| **`finish()` in a `finally`** | Forks always get `END`, even if the source throws. | They finish on *partial* data and nobody can see it. |

**The overall picture.** Java's `Stream` is *pull-based, single-use, single-consumer*. The goal needs *broadcast* (1 source → N independent pipelines). So the design inserts an impedance-matching layer:

```
 push (forEach) → [N queues] → pull (Spliterator → Stream) → N blocking loops on N threads
```

Everything else follows from that choice: the Spliterator (to pull from a queue), the poison pill (to end a Stream from a queue), the threads (N blocking loops), the futures (to collect N results). The price is a handoff per element per fork, which is why the book ends with *"Just measure it!"* and why the modern version (§9.3) batches.

---

## 8. Weak spots of the book's version

All of these were reproduced on the book's code (JDK 25):

| Weakness | What actually happens | Evidence |
|---|---|---|
| **Unbounded queues** | A fork slower than the producer makes its queue hold the whole stream. | **(measured)** 30M elements, 2 slow forks, `-Xmx48m`: heap sat at `44M->44M`, **1,284 consecutive Full GCs**, killed at 80 s. The bounded version finished in the same heap (62 s). |
| **Early-terminating fork leaks** | A fork using `findFirst`/`limit`/`anyMatch` stops reading, but the producer keeps filling its queue. | **(measured)** 2M-element source: that fork's queue retained **2,000,000** items (1,999,999 elements + `END`). |
| **`null` elements** | `LinkedBlockingQueue.add(null)` throws. | **(measured)** `NullPointerException` for `Stream.of("a", null, "b")`. |
| **Failures are late and ugly** | A fork that throws is invisible until `get(key)`, then arrives as `RuntimeException(ExecutionException(...))`. Its queue is never drained. | **(measured)** `getResults()` returned normally; `get("boom")` threw. |
| **Source failure** | `finally` sends `END`, so forks finish on partial data. The exception propagates, but the forks' work is wasted. | **(measured)** |
| **Interrupts swallowed** | `catch (InterruptedException e) { }` clears the flag and retries. The fork can never be cancelled. | Code reading. |
| **Common pool** | Forks are *blocking* tasks in a shared pool. | **(measured)** With `parallelism=2` and 4 forks, all 4 still ran: the pool added spare threads. So not a deadlock, but you get one platform thread per fork, taken from a pool shared with parallel streams and other `CompletableFuture`s. |
| **Per-element hand-off** | Every element costs a lock-protected queue operation per fork. | **(measured)** see the benchmark in §9.5. |
| **Type safety** | `Object` keys; `<R> R get(Object)` is unchecked; a wrong key gives NPE or `ClassCastException`. | Code reading. |
| **Data mismatch in the book's output** | Prints `Total calories: 4300`. | **(measured)** With the standard Chapter-4 menu data the sum is **4200**. A data slip in the book; irrelevant to the mechanics. |

---

## 9. The idiomatic JDK 25 way

**Honest answer first:** the JDK has no built-in "fork a Stream into N concurrent pipelines". What it does have:

- `Collectors.teeing` (JDK 12): 2 collectors, **one pass, one thread**.
- Virtual threads (final in JDK 21) and a closeable `ExecutorService` (JDK 19): they remove the main reasons the book's design is heavy.

**First question: do you need threads at all?** Threads are only justified when per-fork work is heavy, I/O-bound, or must use arbitrary stream operations. Otherwise, run all the operations in one pass on one thread.

### 9.1 Pure JDK: nested `teeing` (single pass, single thread)

```java
record MenuSummary(String names, int totalCalories, Dish top, Map<Dish.Type, List<Dish>> byType) {}

MenuSummary s = menu.stream().collect(teeing(
    teeing(mapping(Dish::name, joining(", ")), summingInt(Dish::calories), Map::entry),
    teeing(maxBy(comparingInt(Dish::calories)), groupingBy(Dish::type), Map::entry),
    (a, b) -> new MenuSummary(a.getKey(), a.getValue(), b.getKey().orElseThrow(), b.getValue())));
```

(measured: identical results to the book's output.) Fine for 2 to 4 operations. Beyond that, or when the set of operations is dynamic, nesting gets ugly, so use 9.2.

### 9.2 N-ary keyed collector (the book's API shape, minus threads)

Same fluent `key → operation` shape as `StreamForker`, but each operation is a `Collector`, all run **in one pass in the calling thread**. It also works on **parallel streams** (via the collectors' combiners), and typed keys remove every cast.

```java
static final Key<String>                     NAMES    = Key.of("shortMenu");
static final Key<Integer>                    CALORIES = Key.of("totalCalories");
static final Key<Optional<Dish>>             TOP      = Key.of("mostCaloricDish");
static final Key<Map<Dish.Type, List<Dish>>> BY_TYPE  = Key.of("dishesByType");

Results r = menu.stream().collect(new MultiCollector<Dish>()
    .add(NAMES,    mapping(Dish::name, joining(", ")))
    .add(CALORIES, summingInt(Dish::calories))
    .add(TOP,      maxBy(comparingInt(Dish::calories)))
    .add(BY_TYPE,  groupingBy(Dish::type))
    .build());

String names = r.get(NAMES);        // no cast, no unchecked warning at the call site
```

Shared vocabulary (`Key`, `Results`):

```java
package forking;

/** Typed, identity-based key: the result type R travels with the key, so call sites need no casts. */
public final class Key<R> {
    private final String name;

    private Key(String name) { this.name = name; }

    public static <R> Key<R> of(String name) { return new Key<>(name); }

    @Override public String toString() { return name; }
}
```

```java
package forking;

import java.util.Collections;
import java.util.Map;
import java.util.NoSuchElementException;

/** Immutable bag of results, read back with the same typed {@link Key} used to register them. */
public final class Results {
    private final Map<Key<?>, Object> values;

    Results(Map<Key<?>, Object> values) { this.values = Collections.unmodifiableMap(values); }

    @SuppressWarnings("unchecked") // safe: builders only ever store an R under a Key<R>
    public <R> R get(Key<R> key) {
        if (!values.containsKey(key)) throw new NoSuchElementException("No result for key '" + key + "'");
        return (R) values.get(key);
    }
}
```

The collector:

```java
package forking;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 * Runs several Collectors over ONE traversal, in the calling thread: the N-ary, keyed
 * generalisation of {@link java.util.stream.Collectors#teeing}. Works on parallel streams too.
 */
public final class MultiCollector<T> {

    /** A registered collector with its container type erased (A is captured exactly once, in of()). */
    private record Part<T>(Supplier<Object> supplier, BiConsumer<Object, T> accumulator,
                           BinaryOperator<Object> combiner, Function<Object, Object> finisher) {
        @SuppressWarnings("unchecked") // safe: a container always comes from this same collector's supplier
        static <T, A> Part<T> of(Collector<? super T, A, ?> c) {
            return new Part<>(c.supplier()::get,
                              (a, t) -> c.accumulator().accept((A) a, t),
                              (x, y) -> c.combiner().apply((A) x, (A) y),
                              a -> c.finisher().apply((A) a));
        }
    }

    private final Map<Key<?>, Part<T>> parts = new LinkedHashMap<>();

    public <R> MultiCollector<T> add(Key<R> key, Collector<? super T, ?, R> collector) {
        if (parts.putIfAbsent(key, Part.of(collector)) != null)
            throw new IllegalArgumentException("Duplicate key '" + key + "'");
        return this;
    }

    public Collector<T, ?, Results> build() {
        var keys = List.copyOf(parts.keySet());
        var ps = List.copyOf(parts.values());
        int n = ps.size();
        return Collector.of(
            () -> {
                var containers = new Object[n];
                for (int i = 0; i < n; i++) containers[i] = ps.get(i).supplier().get();
                return containers;
            },
            (containers, t) -> {
                for (int i = 0; i < n; i++) ps.get(i).accumulator().accept(containers[i], t);
            },
            (left, right) -> {
                for (int i = 0; i < n; i++) left[i] = ps.get(i).combiner().apply(left[i], right[i]);
                return left;
            },
            containers -> {
                var out = new LinkedHashMap<Key<?>, Object>();
                for (int i = 0; i < n; i++) out.put(keys.get(i), ps.get(i).finisher().apply(containers[i]));
                return new Results(out);
            });
    }
}
```

How it works: each registered `Collector<T, A, R>` has a different container type `A`. `Part.of` captures `A` once and erases it into `Object`-based lambdas. The cast `(A) a` is safe because a container only ever meets the collector that created it. The composite container is an `Object[]` with one slot per collector.

### 9.3 When you really need concurrent forks: the modern `StreamForker`

Use this only when the source is single-pass and the per-fork work is heavy or I/O-bound, or you need arbitrary stream ops (`sorted`, `limit`, `flatMap`) that `Collector`s can't express.

What changed relative to the book, and why:

| Change | Replaces | Benefit |
|---|---|---|
| **Virtual thread per fork**, `try`-with-resources executor | `supplyAsync` on the common pool | Blocking is cheap. No shared-pool pollution. Structured: no thread outlives `run()`. |
| **Bounded `ArrayBlockingQueue` + `put`** | unbounded `LinkedBlockingQueue` + `add` | Back-pressure; memory stays bounded. |
| **Batching** (512 elements per hand-off, one shared read-only batch for all forks) | one queue op per element per fork | ~512× fewer lock operations; also lets `null` elements through. |
| **`AbstractSpliterator`** with `ORDERED`, `Long.MAX_VALUE` | hand-written 4-method Spliterator | Correct metadata; only `tryAdvance` is hand-written. |
| **Identity-compared `end` batch** (typed `List<T>`) | `Object` sentinel + unchecked `(T)` cast | No unchecked cast in the queue protocol. |
| **A fork always drains until `end`** | nothing | A fork that finishes early or throws can never block the producer. |
| **`failed` flag checked by the producer** | nothing | Fail-fast: a failing fork stops the traversal. |
| **Interrupts restore the flag and cancel** | swallowed | Cancellable. `shutdownNow()` on producer failure. |
| **Typed `Key<R>` / `Results`** | `Object` keys, `<R> R get(Object)` | Compile-time-checked results. |
| **`run()` blocks until all forks finish** | `getResults()` returning early | Structured lifecycle; errors surface from `run()` as `CompletionException` naming the failing key. |

```java
package forking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Runs several stream functions concurrently over ONE traversal of the source.
 * The caller's thread pushes elements, in batches, into one bounded queue per fork;
 * each fork pulls them back out as a Stream on its own virtual thread.
 */
public final class StreamForker<T> {

    private static final int BATCH_SIZE = 512;   // elements per queue hand-off
    private static final int QUEUE_BATCHES = 8;  // bounded: <= BATCH_SIZE * QUEUE_BATCHES elements in flight per fork

    private final Stream<T> source;
    private final Map<Key<?>, Function<Stream<T>, ?>> forks = new LinkedHashMap<>();

    private StreamForker(Stream<T> source) { this.source = Objects.requireNonNull(source); }

    public static <T> StreamForker<T> from(Stream<T> source) { return new StreamForker<>(source); }

    public <R> StreamForker<T> fork(Key<R> key, Function<Stream<T>, R> operation) {
        if (forks.putIfAbsent(key, operation) != null)
            throw new IllegalArgumentException("Duplicate key '" + key + "'");
        return this;
    }

    /** Traverses the source once; returns when every fork has finished. */
    public Results run() {
        final var end = new ArrayList<T>();            // poison pill: recognised by identity, never a real batch
        final var failed = new AtomicBoolean();        // a fork threw -> stop feeding early
        final var queues = new ArrayList<BlockingQueue<List<T>>>();
        final var futures = new LinkedHashMap<Key<?>, Future<?>>();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor(); source) {
            forks.forEach((key, operation) -> {
                var queue = new ArrayBlockingQueue<List<T>>(QUEUE_BATCHES);
                queues.add(queue);
                futures.put(key, executor.submit(() -> runFork(operation, queue, end, failed)));
            });
            try {
                feed(queues, end, failed);
            } catch (Throwable t) {
                executor.shutdownNow();                // 'end' may not have reached every fork -> cancel them
                throw t;
            }
            return join(futures);
        }
    }

    // ---- producer side: pull from the source, push to every queue --------------------------------------------

    private void feed(List<BlockingQueue<List<T>>> queues, List<T> end, AtomicBoolean failed) {
        var batcher = new Batcher<T>(queues);
        var elements = source.sequential().spliterator();
        while (!failed.get() && elements.tryAdvance(batcher)) { /* one element per call */ }
        batcher.flush();
        broadcast(queues, end);
    }

    private static final class Batcher<T> implements Consumer<T> {
        private final List<BlockingQueue<List<T>>> queues;
        private List<T> batch = new ArrayList<>(BATCH_SIZE);

        Batcher(List<BlockingQueue<List<T>>> queues) { this.queues = queues; }

        @Override public void accept(T t) {
            batch.add(t);
            if (batch.size() == BATCH_SIZE) flush();
        }

        void flush() {
            if (batch.isEmpty()) return;
            broadcast(queues, batch);                  // the same read-only batch is shared by all forks
            batch = new ArrayList<>(BATCH_SIZE);
        }
    }

    private static <T> void broadcast(List<BlockingQueue<List<T>>> queues, List<T> batch) {
        try {
            for (var queue : queues) queue.put(batch); // blocks while a fork is behind: back-pressure
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CancellationException("Interrupted while feeding the forks");
        }
    }

    // ---- consumer side: one virtual thread per fork -----------------------------------------------------------

    private static <T, R> R runFork(Function<Stream<T>, R> operation, BlockingQueue<List<T>> queue,
                                    List<T> end, AtomicBoolean failed) {
        var elements = new QueueSpliterator<>(queue, end);
        try {
            return operation.apply(StreamSupport.stream(elements, false));
        } catch (Throwable t) {
            failed.set(true);
            throw t;
        } finally {
            elements.drain();  // invariant: a fork never stops consuming before 'end', so the producer can't block on it
        }
    }

    private static Results join(Map<Key<?>, Future<?>> futures) {
        var values = new LinkedHashMap<Key<?>, Object>();
        CompletionException failure = null;
        for (var entry : futures.entrySet()) {
            try {
                values.put(entry.getKey(), entry.getValue().get());
            } catch (ExecutionException e) {
                var wrapped = new CompletionException("Fork '" + entry.getKey() + "' failed", e.getCause());
                if (failure == null) failure = wrapped; else failure.addSuppressed(wrapped);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new CancellationException("Interrupted while waiting for the forks");
            }
        }
        if (failure != null) throw failure;
        return new Results(values);
    }

    /** Push -> pull adapter: a Spliterator whose elements arrive, batch by batch, through a BlockingQueue. */
    private static final class QueueSpliterator<T> extends Spliterators.AbstractSpliterator<T> {
        private final BlockingQueue<List<T>> queue;
        private final List<T> end;
        private Iterator<T> current = Collections.emptyIterator();
        private boolean finished;

        QueueSpliterator(BlockingQueue<List<T>> queue, List<T> end) {
            super(Long.MAX_VALUE, ORDERED);            // size unknown; encounter order = arrival order
            this.queue = queue;
            this.end = end;
        }

        @Override public boolean tryAdvance(Consumer<? super T> action) {
            while (!current.hasNext()) {
                if (finished) return false;
                var batch = take();
                if (batch == end) { finished = true; return false; }
                current = batch.iterator();
            }
            action.accept(current.next());
            return true;
        }

        @Override public Spliterator<T> trySplit() { return null; } // a live queue cannot be split

        void drain() {
            try {
                while (!finished) if (take() == end) finished = true;
            } catch (CancellationException _) { /* interrupted: stop draining (flag already restored) */ }
        }

        private List<T> take() {
            try {
                return queue.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new CancellationException("Interrupted while waiting for elements");
            }
        }
    }
}
```

Usage (same shape as the book):

```java
Results r = StreamForker.from(menu.stream())
    .fork(NAMES,    s -> s.map(Dish::name).collect(joining(", ")))
    .fork(CALORIES, s -> s.mapToInt(Dish::calories).sum())
    .fork(TOP,      s -> s.max(comparingInt(Dish::calories)))
    .fork(BY_TYPE,  s -> s.collect(groupingBy(Dish::type)))
    .run();                                   // blocks until all four are done
```

**Contract to respect:** each `operation` must *consume* the stream (a terminal op) and not return it or hand it to another thread. Each fork's stream is single-threaded.

**Trade-off:** batching means a fork sees elements only when 512 have accumulated or the source ends. Perfect for finite sources; wrong for a slow, live source where latency matters (lower `BATCH_SIZE` there).

**Test results (measured, JDK 25):** 9/9 checks pass: 3M elements with 3 forks; batch boundaries (0, 1, 511, 512, 513, 1024, 4103 elements); `null` elements; early-terminating fork; failing fork aborting a 50M-element source in 2 ms; source exception; caller interrupt; duplicate key; unknown key. It compiles with no warnings under `-Xlint:all`.

### 9.4 Other options, briefly

- **Gatherers (JEP 485, final in JDK 24):** don't fit. A `Gatherer` is an *intermediate* operation (stream → stream). A "fold everything, emit at the end" gatherer is equivalent to a composite `Collector` (§9.2), so there is no gain.
- **`StructuredTaskScope` (JEP 505):** still a *preview* API in JDK 25. It is the natural future home for the fork/join/cancel lifecycle that §9.3 hand-rolls.
- **Reactive code (Reactor):** the same problem is solved by multicasting, e.g. `Flux.publish(Function)` (one upstream subscription, N downstream pipelines, back-pressure built in). Not run here.

### 9.5 Rough overhead numbers (measured)

5M boxed `Integer`s in an `ArrayList`, 3 cheap ops (sum, count, max), median of 7 runs after warm-up, `-Xmx2g`, **1 vCPU, not JMH**:

| Approach | Median |
|---|---|
| 3 separate traversals (data is in memory) | **21 ms** |
| `MultiCollector` (1 pass, 1 thread) | 61 ms |
| Modern `StreamForker` (batched, virtual threads) | 143 ms |
| Book `StreamForker` (queue op per element) | **1112 ms** |

How to read this:

- The book's per-element queueing is ~8× slower than the batched version and ~50× slower than just streaming three times. This is what "the overhead of blocking queues can outweigh the advantages" means in numbers.
- For **cheap ops on in-memory data**, plain repeated traversal wins. It uses specialised primitive loops (`mapToLong(..).sum()`), while the generic collectors box and dispatch through lambdas.
- Single-pass approaches win when **traversal is the expensive part** (file, network, DB cursor) or the source can't be replayed, none of which an in-memory list simulates. With real multi-core hardware and heavy per-fork work, the concurrent version can also gain parallel speed-up (not measurable on this 1-vCPU sandbox).

### 9.6 Which one to use

| Situation | Use |
|---|---|
| Source is cheap to re-stream (in-memory collection) | Just stream it several times. |
| Single pass needed, 2 ops expressible as Collectors | `Collectors.teeing` |
| Single pass needed, N ops expressible as Collectors | `MultiCollector` (§9.2) |
| Single pass needed, heavy / I/O-bound ops, or ops Collectors can't express (`sorted`, `limit`, …) | Modern `StreamForker` (§9.3) |
| Already in a reactive pipeline | Multicast (`publish`) |

---

## 10. What I verified

| Item | Result |
|---|---|
| Book listings C.2–C.5 + reconstructed C.1 compile and run on JDK 25 | ✔ (sum prints 4200, not the book's 4300) |
| Threading, late-binding, characteristics, single-use, `null`, fork/source exceptions, early-termination leak | ✔ reproduced (§4–§8) |
| Back-pressure: 48 MB heap, slow forks | Book: GC death spiral. Modern: completes. |
| `teeing`, `MultiCollector` (sequential + parallel), modern `StreamForker` produce identical results | ✔ |
| Modern `StreamForker` stress/failure suite | ✔ 9/9 |
| Standalone push→pull Spliterator demo | ✔ `[10, 20, 30]` |
| Not tested | Reactor `publish` snippet; behaviour on multi-core hardware. |
