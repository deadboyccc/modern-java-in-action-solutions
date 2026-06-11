# JMH & JVM Microbenchmarking — Complete Reference Guide

> **Goal:** Master JMH fast. Understand why naïve benchmarks lie, how the JIT compiler can silently invalidate your
> tests, and how to measure parallel stream performance correctly.

---

## Table of Contents

1. [Why Microbenchmarking on the JVM Is Hard](#1-why-microbenchmarking-on-the-jvm-is-hard)
2. [JMH Architecture — The Controlled Environment](#2-jmh-architecture--the-controlled-environment)
3. [Core Annotations Cheat Sheet](#3-core-annotations-cheat-sheet)
4. [The JIT Compiler vs. Your Benchmark](#4-the-jit-compiler-vs-your-benchmark)
5. [State Management — @State and Scope](#5-state-management--state-and-scope)
6. [Blackhole — Consuming Results Correctly](#6-blackhole--consuming-results-correctly)
7. [Stream Parallelism & the ForkJoinPool Engine](#7-stream-parallelism--the-forkjoinpool-engine)
8. [Memory & GC Topology Under Benchmarks](#8-memory--gc-topology-under-benchmarks)
9. [Benchmark Modes](#9-benchmark-modes)
10. [Common Pitfalls & How to Fix Them](#10-common-pitfalls--how-to-fix-them)
11. [Full Working Examples](#11-full-working-examples)
12. [Reading JMH Output](#12-reading-jmh-output)
13. [Quick Decision Guide](#13-quick-decision-guide)

---

## 1. Why Microbenchmarking on the JVM Is Hard

The JVM is a **heavily speculative, adaptive runtime**. What you write in Java and what actually executes at the CPU
level are often completely different things. Several forces work against naïve benchmarks:

| JVM Optimization                | What It Does                                                           | Benchmark Impact                                                 |
|---------------------------------|------------------------------------------------------------------------|------------------------------------------------------------------|
| **Dead Code Elimination (DCE)** | Removes computations whose results are never used                      | Loop runs in 0ns — nothing happened                              |
| **Constant Folding**            | Replaces constant expressions with their compile-time values           | Your "loop" becomes a single value assignment                    |
| **Loop Unrolling**              | Expands short loops inline to avoid branch overhead                    | Timing reflects unrolled code, not realistic behavior            |
| **Speculative Inlining**        | Inlines a method assuming only one concrete type ever arrives          | Benchmark runs fast; production code with polymorphism does not  |
| **Escape Analysis**             | Detects objects that never leave the current thread's stack            | Heap allocations eliminated entirely — allocation benchmarks lie |
| **OSR (On-Stack Replacement)**  | Replaces a running interpreted method mid-execution with compiled code | Warmup-phase timing is meaningless                               |

**The fundamental rule:** never write a benchmark loop without JMH. The JIT optimizer will eat it.

---

## 2. JMH Architecture — The Controlled Environment

JMH generates source code that wraps your annotated methods. It compiles and runs them as separate OS processes with
carefully staged phases.

### The Execution Pipeline

```
Source Code
    │
    ▼
JMH Annotation Processor  ──► generates benchmark runner classes
    │
    ▼
@Fork — new OS process (clean JIT profile, clean heap)
    │
    ├──► @Warmup iterations  (JIT compiles, OSR fires, code stabilizes)
    │         │
    │         └── Tier 1 (C1/Client) → Tier 2 (C2/Server) compilation
    │
    └──► @Measurement iterations  (timing collected here only)
```

### Why `@Fork` Is Non-Negotiable

```java
@Fork(value = 2, jvmArgs = {"-Xms4G", "-Xmx4G"})
```

**Profile pollution:** The JIT uses **MethodDataObjects (MDOs)** to track which concrete types arrive at a call site. A
monomorphic call site (one type) gets fast direct dispatch. A megamorphic call site (3+ types) loses inlining entirely.
Running two benchmarks in the same process contaminates each other's MDOs.

**Heap determinism:** Setting `-Xms == -Xmx` locks heap boundaries at startup. The OS never needs to map more memory
pages mid-run. GC thresholds stay identical across every fork. Without this, GC timing variance bleeds into your
measurements.

**Rule of thumb:** Always use at least `@Fork(2)`. Use `@Fork(3)` for publishable results.

### Warmup vs. Measurement — What Actually Happens

```java
@Warmup(iterations = 3, time = 1)       // 3 × 1-second warmup rounds
@Measurement(iterations = 5, time = 1)  // 5 × 1-second measurement rounds
```

JVM compilation tiers during warmup:

```
First call         → Interpreter (slowest, no optimization)
~100 invocations   → C1 Tier 1 (profiled, lightly optimized)
~10,000 invocations→ C1 Tier 2 (profiled + inlining hints)
~10,000+ + MDO     → C2 Tier 4 (heavily optimized, speculative inlining, loop transforms)
```

Measurement only begins once C2-compiled native code is **stable**. If you reduce warmup too aggressively, you're
measuring C1 code, not C2 code — a 5–50× difference in throughput.

---

## 3. Core Annotations Cheat Sheet

```java

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 2, jvmArgs = {"-Xms512m", "-Xmx512m"})
public class MyBenchmark {

    // fields here are @State

    @Setup(Level.Trial)    // runs once before entire trial
    public void setup() { ...}

    @TearDown(Level.Iteration)  // runs after each measurement iteration
    public void teardown() { ...}

    @Benchmark
    public long myMethod() { ...}
}
```

### Annotation Reference

| Annotation        | Key Parameters                   | Purpose                               |
|-------------------|----------------------------------|---------------------------------------|
| `@Benchmark`      | —                                | Marks the method to measure           |
| `@BenchmarkMode`  | `Mode.*`                         | What to measure (see §9)              |
| `@OutputTimeUnit` | `TimeUnit.*`                     | Unit for reported results             |
| `@Fork`           | `value`, `jvmArgs`, `warmups`    | Process isolation count and JVM flags |
| `@Warmup`         | `iterations`, `time`, `timeUnit` | Warmup phase config                   |
| `@Measurement`    | `iterations`, `time`, `timeUnit` | Measurement phase config              |
| `@State`          | `Scope.*`                        | Declares lifecycle of an object       |
| `@Setup`          | `Level.*`                        | Code to run before benchmarking       |
| `@TearDown`       | `Level.*`                        | Code to run after benchmarking        |
| `@Param`          | `{values}`                       | Parameterize over multiple inputs     |

### `@Setup` / `@TearDown` Levels

| Level              | Runs                                                                                           |
|--------------------|------------------------------------------------------------------------------------------------|
| `Level.Trial`      | Once per entire benchmark run (all forks × all iterations)                                     |
| `Level.Iteration`  | Once per measurement/warmup iteration                                                          |
| `Level.Invocation` | Every single benchmark method call — **use with extreme caution**, the overhead is significant |

---

## 4. The JIT Compiler vs. Your Benchmark

### Dead Code Elimination — The Silent Killer

```java
// WRONG — JIT will eliminate this loop entirely
@Benchmark
public void deadLoop() {
    long sum = 0;
    for (int i = 0; i < 10_000_000; i++) {
        sum += i;  // result never used outside this method
    }
    // sum is discarded → JIT removes the loop → ~0ns measurement
}
```

The C2 compiler performs **escape analysis**: `sum` never escapes the method stack frame, so the entire computation is
provably side-effect-free and is deleted.

**Fix:** Return the value, or consume it with a `Blackhole` (see §6).

```java
// CORRECT — return forces the JIT to keep the computation
@Benchmark
public long seqSum() {
    long sum = 0;
    for (int i = 0; i < 10_000_000; i++) {
        sum += i;
    }
    return sum;  // now the caller depends on the value
}
```

### Constant Folding

```java
// DANGEROUS — count is a compile-time constant
public static final int COUNT = 10_000_000;

@Benchmark
public long constantLoop() {
    long sum = 0;
    for (int i = 0; i < COUNT; i++) {
        sum += i;
    }
    return sum;
    // C2 applies: sum = COUNT * (COUNT - 1) / 2 = 49_999_995_000_000L
    // The entire loop is replaced with a single constant → ~0ns
}
```

The arithmetic series formula `n(n-1)/2` is well-known to the optimizer. Any loop over a constant range summing indices
will be folded.

**Fix:** Put constants in a `@State` object — the JIT cannot assume the value of an object field is constant across
invocations.

```java

@State(Scope.Benchmark)
public static class BenchmarkState {
    public int count = 10_000_000;  // NOT final — defeats constant folding
}

@Benchmark
public long safeSum(BenchmarkState state) {
    long sum = 0;
    for (int i = 0; i < state.count; i++) {
        sum += i;
    }
    return sum;  // real work, real timing
}
```

### Speculative Inlining & Monomorphic Call Sites

The JIT optimizes heavily when it has seen only one concrete type at a call site:

```java
// JIT inlines this aggressively — only one concrete List implementation seen
List<Integer> list = new ArrayList<>(data);
list.

stream().

mapToInt(Integer::intValue).

sum();
```

If your benchmark always uses `ArrayList`, the JIT inlines `ArrayList.spliterator()` directly. In production code using
mixed collection types, this optimization evaporates and you have a megamorphic dispatch overhead the benchmark never
measured.

**Rule:** Benchmark the shapes of code that match production. If production uses `List<>` polymorphically, your
benchmark should too.

---

## 5. State Management — `@State` and Scope

`@State` tells JMH how to manage the lifecycle and sharing of a benchmark's data. Choosing the wrong scope gives you
incorrect results.

### Scope Options

```java
@State(Scope.Benchmark)   // one instance shared across all threads
@State(Scope.Thread)      // each benchmark thread gets its own instance
@State(Scope.Group)       // shared within a @BenchmarkMode group
```

#### `Scope.Benchmark` — Shared State

```java

@State(Scope.Benchmark)
public class SharedState {
    public List<Integer> data;

    @Setup(Level.Trial)
    public void setup() {
        data = IntStream.range(0, 1_000_000)
                .boxed()
                .collect(Collectors.toList());
    }
}
```

Use when: data is read-only and shared across threads. Avoids redundant allocations.

**Warning:** If benchmark threads **write** to shared state, you introduce false sharing and lock contention into your
measurement.

#### `Scope.Thread` — Thread-Local State

```java

@State(Scope.Thread)
public class ThreadLocalState {
    public Random rng;

    @Setup(Level.Iteration)
    public void setup() {
        rng = new Random(42);  // each thread gets a private RNG
    }
}
```

Use when: the benchmark involves mutable state (accumulators, random generators, counters) that should not be shared.

### `@Param` — Sweep Over Inputs

```java

@State(Scope.Benchmark)
public class ParamState {
    @Param({"100", "1000", "10000", "1000000"})
    public int dataSize;

    public List<Integer> data;

    @Setup(Level.Trial)
    public void setup() {
        data = IntStream.range(0, dataSize).boxed().collect(Collectors.toList());
    }
}
```

JMH runs the full benchmark suite for **each** param value. Results show performance characteristics across the input
size range — critical for finding the parallel stream crossover point.

---

## 6. Blackhole — Consuming Results Correctly

A `Blackhole` is JMH's mechanism for consuming values without them being optimized away, and without the cost of a real
side effect.

```java

@Benchmark
public void multipleResults(Blackhole bh, BenchmarkState state) {
    long sum = 0;
    for (int i = 0; i < state.count; i++) {
        sum += i;
        bh.consume(sum);  // prevents DCE on intermediate values
    }
}
```

### When to Use `return` vs. `Blackhole`

| Situation                               | Use                                                                                |
|-----------------------------------------|------------------------------------------------------------------------------------|
| Single computed result                  | `return value` — JMH sinks the return value automatically                          |
| Multiple intermediate values            | `bh.consume(v1); bh.consume(v2);`                                                  |
| void computation with side-effect check | `bh.consume(result)`                                                               |
| Object allocation benchmarks            | `bh.consume(new MyObject())` — prevents escape analysis from eliminating the `new` |

```java
// Benchmarking object creation — must consume to prevent elimination
@Benchmark
public void objectAllocation(Blackhole bh) {
    bh.consume(new byte[1024]);  // without this, the allocation disappears
}
```

`Blackhole.consume()` compiles to a very low-overhead check that defeats DCE without introducing measurable bias. It is
**not** a real write — it's a JMH-specific memory fence designed for this purpose.

---

## 7. Stream Parallelism & the ForkJoinPool Engine

### How Parallel Streams Split Work — Spliterators

When you call `.parallelStream()`, the stream framework calls `spliterator().trySplit()` recursively until subtasks are
small enough to execute directly.

```
data.parallelStream()
         │
         ▼
   Spliterator.trySplit()  →  left half   right half
                                  │            │
                            trySplit()    trySplit()
                              │    │        │    │
                            [L1] [L2]     [R1] [R2]   ← leaf tasks submitted to ForkJoinPool
```

**Splitting cost varies dramatically by data source:**

| Source                  | Split Strategy                 | Cost                      | Balance                |
|-------------------------|--------------------------------|---------------------------|------------------------|
| `IntStream.range(0, n)` | `mid = (start + end) / 2`      | O(1) — pure arithmetic    | Perfect halves always  |
| `ArrayList<T>`          | Divide array index offsets     | O(1) — pointer arithmetic | Perfect halves always  |
| `LinkedList<T>`         | Must traverse to midpoint      | O(n) — pointer chasing    | Poor — avoid           |
| `HashSet<T>`            | Internal bucket splitting      | O(n/parallelism)          | Uneven — unpredictable |
| Unmodifiable wrappers   | Delegates to inner spliterator | Depends on wrapped type   | Depends                |

**Practical implication:** If you need fast parallel streams, prefer `ArrayList` or primitive range streams. Avoid
`LinkedList` and `Set` as parallel stream sources.

```java
// Fast: arithmetic split, no boxing
IntStream.range(0,10_000_000).

parallel().

sum()

// Slower: index split + pointer dereference for each Integer
List<Integer> list = new ArrayList<>();
list.

parallelStream().

mapToLong(Integer::longValue).

sum()

// Slow: O(n) split cost, kills parallelism advantage
LinkedList<Integer> linked = new LinkedList<>();
linked.

parallelStream().

sum()  // don't do this
```

### The ForkJoinPool Work-Stealing Architecture

```
                    ┌─────────────────────┐
                    │   Global Submit()   │
                    │   Queue (shared)    │
                    └──────────┬──────────┘
                               │
              ┌────────────────┼────────────────┐
              ▼                ▼                ▼
        ┌──────────┐     ┌──────────┐     ┌──────────┐
        │ Worker 0 │     │ Worker 1 │     │ Worker 2 │
        │ [Deque]  │     │ [Deque]  │     │ [Deque]  │
        │ HEAD◄──  │     │ HEAD◄──  │     │ HEAD◄──  │
        │  own work│     │  own work│     │  own work│
        │  ──►TAIL │     │  ──►TAIL │     │  ──►TAIL │
        └──────┬───┘     └──────────┘     └──────┬───┘
               │   steal from tail                │
               └─────────────────────────────────►│
                  (Worker 0 steals Worker 2's tail)
```

**Key mechanics:**

- Workers push/pop their own tasks from the **head** (LIFO — preserves cache locality for depth-first work)
- Workers **steal** from the **tail** of other queues (FIFO — takes the oldest/largest tasks, minimizes steal frequency)
- Stealing is rare under balanced workloads — most tasks execute thread-locally

### Submitting to a Custom ForkJoinPool

By default, `parallelStream()` uses `ForkJoinPool.commonPool()`. This means all parallel streams in your JVM share the
same pool. In a production server this causes interference between unrelated parallel operations.

```java
// WRONG: uses common pool — shared with everything else in the JVM
data.parallelStream().

mapToLong(Integer::longValue).

sum();

// CORRECT: dedicated pool, isolated execution
ForkJoinPool customPool = new ForkJoinPool(4);  // 4 worker threads

long result = customPool.submit(
        () -> data.parallelStream().mapToLong(Integer::longValue).sum()
).get();
```

**How the hijacking works:** The stream framework checks `Thread.currentThread()` before submitting fork tasks. If the
current thread is a `ForkJoinWorkerThread`, its child tasks are registered with **that thread's pool**, not the common
pool. Submitting to `customPool` makes the initiating thread a worker in that pool, so all downstream fork tasks land
there too.

```java
// Benchmarking both pool strategies
@State(Scope.Benchmark)
public class PoolBenchmark {
    private List<Integer> data;
    private ForkJoinPool customPool;

    @Setup(Level.Trial)
    public void setup() {
        data = IntStream.range(0, 1_000_000).boxed().collect(Collectors.toList());
        customPool = new ForkJoinPool(
                Runtime.getRuntime().availableProcessors()
        );
    }

    @TearDown(Level.Trial)
    public void teardown() {
        customPool.shutdown();
    }

    @Benchmark
    public long commonPoolSum() {
        return data.parallelStream().mapToLong(Integer::longValue).sum();
    }

    @Benchmark
    public long customPoolSum() throws Exception {
        return customPool.submit(
                () -> data.parallelStream().mapToLong(Integer::longValue).sum()
        ).get();
    }
}
```

### The Parallel Stream Crossover Point

Parallelism has a fixed overhead: task splitting, thread scheduling, and join synchronization. It only pays off when the
per-element work exceeds this overhead.

```
Total parallel time = Split overhead + (Work / N threads) + Join overhead
Total sequential time = Work

Parallel wins when: (Work / N threads) + overhead < Work
```

**Empirical thresholds for simple numeric reductions:**

| Data Size        | Recommendation                          |
|------------------|-----------------------------------------|
| < 1,000          | Sequential always wins                  |
| 1,000 – 10,000   | Depends on per-element cost             |
| 10,000 – 100,000 | Parallel usually wins for pure CPU work |
| > 100,000        | Parallel wins unless memory-bound       |

For non-trivial per-element work (e.g., I/O, complex transforms), the crossover drops much lower.

---

## 8. Memory & GC Topology Under Benchmarks

### Thread-Local Allocation Buffers (TLABs)

The JVM gives each thread a private chunk of heap called a **TLAB**. Object allocation inside a TLAB is just a pointer
bump — no locks, near zero cost.

```
Heap (Eden Space)
┌─────────────────────────────────────────────────┐
│  Thread-0 TLAB          │  Thread-1 TLAB         │
│  [start_ptr → end_ptr]  │  [start_ptr → end_ptr] │
│  Objects: A, B, C       │  Objects: X, Y, Z      │
└─────────────────────────────────────────────────┘
         ▲                          ▲
    Fast-path alloc            Fast-path alloc
    (pointer bump, no lock)    (pointer bump, no lock)
```

**TLAB exhaustion:** When a thread fills its TLAB faster than GC can reclaim space, the JVM falls back to **slow-path
allocation** — acquiring a global lock to request a new TLAB from the shared heap. Under high-concurrency allocation (
like parallel stream `collect()`), this becomes a bottleneck.

### Why `parallelStream().collect()` Underperforms

```java
// This looks parallel but has significant serial bottlenecks
return data.parallelStream()
           .

filter(n ->n %2==0)
        .

collect(Collectors.toList());
```

**What happens internally:**

```
Thread 0: filter → produce sub-list [0, 2, 4, ...]
Thread 1: filter → produce sub-list [100002, 100004, ...]
Thread 2: filter → produce sub-list [200000, 200002, ...]
Thread 3: filter → produce sub-list [300000, 300002, ...]
                                         │
                                         ▼
                              SINGLE-THREAD MERGE
                              [0,2,4,...,100002,100004,...] ← O(N) serial step
```

Problems:

1. **Allocation pressure:** Each thread produces an intermediate `ArrayList`. Rapid allocation exhausts TLABs →
   slow-path heap locks
2. **Single-threaded merge:** The final combine step is serial, O(N). On large datasets it often erases the parallel
   filtering benefit
3. **GC pressure:** All intermediate lists become garbage immediately after merge, triggering Young Gen evacuations

**Better alternatives for collection benchmarks:**

```java
// Option 1: count only (no collection, no merge)
long count = data.parallelStream().filter(n -> n % 2 == 0).count();

// Option 2: primitive array (avoids boxing overhead and reduces GC pressure)
int[] evens = data.parallelStream()
        .filter(n -> n % 2 == 0)
        .mapToInt(Integer::intValue)
        .toArray();

// Option 3: Collectors.toUnmodifiableList() (Java 10+, slightly optimized merge)
List<Integer> result = data.parallelStream()
        .filter(n -> n % 2 == 0)
        .collect(Collectors.toUnmodifiableList());
```

### G1GC Under Benchmark Constraints

```java
@Fork(jvmArgs = {"-Xms512m", "-Xmx512m", "-XX:+UseG1GC"})
```

G1 divides the heap into equal-sized regions (~1–32 MB each). Under memory pressure from allocation-heavy parallel
benchmarks:

```
G1 Heap (512 MB, regions ~8 MB each)
┌──┬──┬──┬──┬──┬──┬──┬──┐
│E │E │E │E │S │O │O │O │   E=Eden, S=Survivor, O=Old
└──┴──┴──┴──┴──┴──┴──┴──┘
         │
         ▼  (Eden fills up from parallel allocations)
    Young GC triggered → Stop-The-World pause
         │
         ▼  (surviving objects promoted to Old)
    Old regions fill → Mixed GC → longer STW pauses
```

**The STW pause problem:** JMH measures wall-clock time for the entire iteration. A GC pause that lands mid-iteration
adds directly to your reported latency. In an allocation-heavy parallel benchmark, GC pauses can contribute 20–50% of
measured time — making the "parallel" result look worse than it actually is for computation.

**GC tuning flags for benchmarks:**

```bash
# Larger young gen reduces GC frequency
-XX:NewRatio=1

# Print GC activity so you can correlate with outlier iterations
-Xlog:gc*:file=gc.log

# Disable GC entirely for allocation-free benchmarks (dangerous — OOM risk)
-XX:+UnlockExperimentalVMOptions -XX:+UseEpsilonGC
```

### False Sharing — The Silent Parallel Killer

False sharing occurs when two threads modify **different variables that happen to live on the same cache line** (64
bytes on x86).

```java
// BROKEN: sum0 and sum1 likely share a cache line
// Every thread 0 write invalidates thread 1's cache line and vice versa
public long sum0;
public long sum1;
```

```java
// FIXED: pad fields to occupy separate cache lines
@sun.misc.Contended  // JVM annotation — adds padding automatically (JDK 8+)
public long sum0;

@sun.misc.Contended
public long sum1;
```

To enable `@Contended` at runtime: `-XX:-RestrictContended`

In JMH benchmarks, `@State(Scope.Thread)` objects are automatically padded by the framework. But if you manually store
results in a shared array, you must pad yourself.

---

## 9. Benchmark Modes

```java
@BenchmarkMode(Mode.AverageTime)
// or combine multiple:
@BenchmarkMode({Mode.AverageTime, Mode.Throughput})
```

| Mode                  | Measures                                      | Use When                                      |
|-----------------------|-----------------------------------------------|-----------------------------------------------|
| `Mode.Throughput`     | Operations per second                         | Maximizing throughput — caches, lookup tables |
| `Mode.AverageTime`    | Average time per operation                    | General comparison of two implementations     |
| `Mode.SampleTime`     | Distribution of operation times (percentiles) | Latency-sensitive code — p99, p999 matter     |
| `Mode.SingleShotTime` | One cold execution                            | Startup cost, first-call latency              |
| `Mode.All`            | All of the above                              | When you want the full picture                |

**When to use `SampleTime`:**

```java

@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Benchmark
public void latencySensitiveOp(Blackhole bh) {
    bh.consume(myService.processRequest());
}
```

`SampleTime` builds a histogram. JMH reports p50, p90, p95, p99, p99.9, p99.99. Use this for anything where tail latency
matters (APIs, DB queries, lock contention detection).

---

## 10. Common Pitfalls & How to Fix Them

### Pitfall 1: Measuring Unwarmed Code

```java
// WRONG: only 1 iteration, no warmup → measuring C1 or interpreter
@Warmup(iterations = 0)
@Measurement(iterations = 1)
```

```java
// CORRECT: give C2 time to stabilize
@Warmup(iterations = 5, time = 2)
@Measurement(iterations = 10, time = 2)
```

### Pitfall 2: Benchmarking Inside a Unit Test

```java
// WRONG: running JMH inside JUnit shares the JVM — profile pollution guaranteed
@Test
public void performanceTest() {
    Options opts = new OptionsBuilder().include(MyBenchmark.class.getName()).build();
    new Runner(opts).run();
}
```

Run JMH benchmarks from a `main()` method or CI pipeline, never from within a test framework process.

### Pitfall 3: Returning `void` Without Blackhole

```java
// WRONG: JIT eliminates the entire computation
@Benchmark
public void broken() {
    Math.sqrt(12345.6789);  // result unused → deleted
}

// CORRECT
@Benchmark
public double fixed() {
    return Math.sqrt(12345.6789);
}
```

### Pitfall 4: Forgetting to Reset Mutable State

```java

@State(Scope.Thread)
public class MutableState {
    public int counter = 0;  // starts at 0

    // MISSING: @Setup to reset counter between iterations
    // After warmup, counter is millions → measurement behavior differs from warmup
}
```

```java

@Setup(Level.Iteration)
public void reset() {
    counter = 0;  // always start each measured iteration clean
}
```

### Pitfall 5: Incorrect Parallel vs. Sequential Comparison

```java
// WRONG: comparing IntStream (primitive) sequential vs List<Integer> (boxed) parallel
// You're measuring boxing overhead, not parallelism overhead
@Benchmark
public long sequential() {
    return IntStream.range(0, COUNT).sum();
}

@Benchmark
public long parallel() {
    return data.parallelStream().mapToLong(Integer::longValue).sum();
}

// CORRECT: same data source, same types, only parallelism differs
@Benchmark
public long sequential() {
    return data.stream().mapToLong(Integer::longValue).sum();
}

@Benchmark
public long parallel() {
    return data.parallelStream().mapToLong(Integer::longValue).sum();
}
```

### Pitfall 6: System Load During Measurement

JMH benchmarks are sensitive to background JVM activity. Run with:

- Laptop plugged in (power management doesn't throttle CPU)
- No browser, IDE, or IDE indexing running
- Ideally on a server with no other load

---

## 11. Full Working Examples

### Example 1: SimpleHarnessBenchmark

```java
package com.example.benchmarks;

import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(value = 2, jvmArgs = {"-Xms4G", "-Xmx4G"})
public class SimpleHarnessBenchmark {

    // NOT final — prevents constant folding
    public int count = 10_000_000;

    @Benchmark
    public long seqSum() {
        long sum = 0L;
        for (int i = 0; i < count; i++) {
            sum += i;
        }
        return sum;  // returned value defeats DCE
    }

    @Benchmark
    public double sqrtSum() {
        double sum = 0;
        for (int i = 0; i < count; i++) {
            sum += Math.sqrt(i);
        }
        return sum;
    }
}
```

### Example 2: StreamParallelBenchmark

```java
package com.example.benchmarks;

import org.openjdk.jmh.annotations.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(value = 2, jvmArgs = {"-Xms512m", "-Xmx512m", "-XX:+UseG1GC"})
public class StreamParallelBenchmark {

    @Param({"1000", "100000", "1000000"})
    public int dataSize;

    private List<Integer> data;
    private ForkJoinPool customPool;

    @Setup(Level.Trial)
    public void setup() {
        data = IntStream.range(0, dataSize).boxed().collect(Collectors.toList());
        customPool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
    }

    @TearDown(Level.Trial)
    public void teardown() {
        customPool.shutdown();
    }

    // --- Baseline ---

    @Benchmark
    public long sequentialSum() {
        return data.stream().mapToLong(Integer::longValue).sum();
    }

    @Benchmark
    public long parallelSum() {
        return data.parallelStream().mapToLong(Integer::longValue).sum();
    }

    // --- Custom Pool Isolation ---

    @Benchmark
    public long customPoolSum() throws Exception {
        return customPool.submit(
                () -> data.parallelStream().mapToLong(Integer::longValue).sum()
        ).get();
    }

    // --- Collection Bottleneck ---

    @Benchmark
    public List<Integer> parallelCollect() {
        return data.parallelStream()
                .filter(n -> n % 2 == 0)
                .collect(Collectors.toList());
    }

    // --- Primitive Range (best-case parallel) ---

    @Benchmark
    public long primitiveRangeParallel() {
        return LongStream.range(0, dataSize).parallel().sum();
    }
}
```

### Example 3: Latency Distribution Benchmark

```java

@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 5, time = 2)
@Measurement(iterations = 10, time = 2)
@Fork(3)
public class CacheLatencyBenchmark {

    private Map<String, String> cache;
    private String[] keys;
    private int idx = 0;

    @Setup(Level.Trial)
    public void setup() {
        cache = new HashMap<>(10_000);
        keys = new String[10_000];
        for (int i = 0; i < 10_000; i++) {
            keys[i] = "key-" + i;
            cache.put(keys[i], "value-" + i);
        }
    }

    @Benchmark
    public String cacheHit() {
        return cache.get(keys[idx++ % keys.length]);  // cyclic, mostly hits
    }
}
```

---

## 12. Reading JMH Output

```
Benchmark                           (dataSize)  Mode  Cnt    Score    Error  Units
StreamParallelBenchmark.sequentialSum     1000  avgt    5    0.012 ±  0.001  ms/op
StreamParallelBenchmark.sequentialSum   100000  avgt    5    0.856 ±  0.012  ms/op
StreamParallelBenchmark.sequentialSum  1000000  avgt    5    8.432 ±  0.089  ms/op
StreamParallelBenchmark.parallelSum       1000  avgt    5    0.198 ±  0.008  ms/op  ← slower: overhead > work
StreamParallelBenchmark.parallelSum     100000  avgt    5    0.412 ±  0.031  ms/op  ← 2× faster
StreamParallelBenchmark.parallelSum    1000000  avgt    5    1.987 ±  0.144  ms/op  ← 4× faster
```

**Reading the columns:**

| Column       | Meaning                                                                         |
|--------------|---------------------------------------------------------------------------------|
| `(dataSize)` | `@Param` value for this row                                                     |
| `Mode`       | Benchmark mode (`avgt` = AverageTime)                                           |
| `Cnt`        | Number of measurement samples                                                   |
| `Score`      | Primary metric (time per op for `avgt`, ops/sec for `thrpt`)                    |
| `Error`      | ±2σ confidence interval — if this is large relative to Score, results are noisy |
| `Units`      | Time unit × operation unit                                                      |

**Interpreting error bars:**

```
Score: 8.432 ± 0.089 ms/op   ← tight: 1% relative error, trustworthy
Score: 8.432 ± 4.201 ms/op   ← noisy: 50% relative error, meaningless
```

If error is > 5% of score: add warmup iterations, increase fork count, check for background JVM activity (JIT
recompilation, GC pauses).

### Detecting GC Interference

Run with `-prof gc` to add GC stats to the output:

```bash
java -jar benchmarks.jar -prof gc StreamParallelBenchmark
```

Output adds columns like:

```
·gc.alloc.rate           MB/sec   ← allocation rate
·gc.alloc.rate.norm      B/op     ← bytes allocated per operation
·gc.count                count    ← GC events during measurement
·gc.time                 ms       ← total GC pause time
```

If `gc.time` is significant relative to benchmark `Score`, GC is corrupting your results. Options: increase heap, use
`EpsilonGC` (no GC), or redesign the benchmark to reduce allocation.

---

## 13. Quick Decision Guide

### Should I use parallel streams?

```
Element count < 1,000?          → Sequential
Per-element work < 100ns?       → Sequential (unless > 100k elements)
Data source is LinkedList?      → Sequential (O(n) split cost)
Need to collect to a List?      → Benchmark carefully, merge is serial
Multiple parallel ops in JVM?   → Use custom ForkJoinPool, not common pool
Otherwise                       → Benchmark with @Param over size range
```

### Which benchmark mode?

```
"Which implementation is faster?"         → Mode.AverageTime
"How many requests/sec can this handle?"  → Mode.Throughput
"What are the p99/p999 latencies?"        → Mode.SampleTime
"What's the cold start cost?"             → Mode.SingleShotTime
```

### Is my benchmark trustworthy?

```
Error > 5% of Score?           → Add forks/iterations, check background load
Score suspiciously near 0?     → DCE — return the value or use Blackhole
Parallel faster than expected? → Constant folding? Check @State on data size
Results change between runs?   → GC interference — add -prof gc, check heap
Warmup looks like measurement? → OSR compiled early — increase warmup time
```

---

## Appendix: Maven / Gradle Setup

### Maven

```xml

<dependency>
    <groupId>org.openjdk.jmh</groupId>
    <artifactId>jmh-core</artifactId>
    <version>1.37</version>
</dependency>
<dependency>
<groupId>org.openjdk.jmh</groupId>
<artifactId>jmh-generator-annprocess</artifactId>
<version>1.37</version>
<scope>provided</scope>
</dependency>
```

```xml

<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-shade-plugin</artifactId>
    <executions>
        <execution>
            <goals>
                <goal>shade</goal>
            </goals>
            <configuration>
                <finalName>benchmarks</finalName>
                <transformers>
                    <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                        <mainClass>org.openjdk.jmh.Main</mainClass>
                    </transformer>
                </transformers>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### Gradle (Kotlin DSL)

```kotlin
plugins {
    id("me.champeau.jmh") version "0.7.2"
}

jmh {
    warmupIterations.set(3)
    iterations.set(5)
    fork.set(2)
}

dependencies {
    jmh("org.openjdk.jmh:jmh-core:1.37")
    jmhAnnotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:1.37")
}
```

### Running

```bash
# Build fat jar
mvn clean package -P benchmark

# Run all benchmarks
java -jar target/benchmarks.jar

# Run specific benchmark
java -jar target/benchmarks.jar StreamParallelBenchmark

# With profiler
java -jar target/benchmarks.jar -prof gc StreamParallelBenchmark

# Quick smoke-test (1 fork, 1 warmup, 1 measurement)
java -jar target/benchmarks.jar -f 1 -wi 1 -i 1
```

---

## Appendix: JVM Flags Reference

| Flag                                                | Effect                                                                      |
|-----------------------------------------------------|-----------------------------------------------------------------------------|
| `-Xms4G -Xmx4G`                                     | Lock heap at 4GB — prevents GC-threshold drift                              |
| `-XX:+UseG1GC`                                      | Enable G1 garbage collector                                                 |
| `-XX:+UseZGC`                                       | Enable ZGC (low-latency, good for allocation-heavy benchmarks)              |
| `-XX:+UseEpsilonGC`                                 | No-op GC — OOM on first GC trigger, use only for allocation-free benchmarks |
| `-XX:+PrintCompilation`                             | Log JIT compilation events to stdout                                        |
| `-XX:+UnlockDiagnosticVMOptions -XX:+PrintInlining` | Log inlining decisions                                                      |
| `-XX:-TieredCompilation`                            | Force C2 directly — removes C1 warmup tier, longer warmup needed            |
| `-XX:CompileThreshold=1000`                         | Lower invocation threshold for faster C2 entry (dev only)                   |
| `-Xlog:gc*:file=gc.log`                             | Write full GC log to file                                                   |

---

*Reference compiled from JMH documentation, JVM internals, and Modern Java in Action. Covers the 80% of JMH you'll use
in 95% of production benchmarking work.*