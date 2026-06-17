# JMH Practical Guide — Measure Java Performance Without Getting Fooled

> **Goal:** Write benchmarks that tell the truth. Understand *why* naïve benchmarks silently lie, and know the handful
> of annotations and patterns you'll use in every real benchmark.

---

## Table of Contents

1. [The Core Problem — Why Java Benchmarks Lie](#1-the-core-problem--why-java-benchmarks-lie)
2. [How JMH Solves It](#2-how-jmh-solves-it)
3. [The 6 Annotations You Actually Need](#3-the-6-annotations-you-actually-need)
4. [Your First Correct Benchmark](#4-your-first-correct-benchmark)
5. [The 3 Silent Killers — and Their Fixes](#5-the-3-silent-killers--and-their-fixes)
6. [State — Managing Your Benchmark's Data](#6-state--managing-your-benchmarks-data)
7. [Benchmark Modes — What Are You Actually Measuring?](#7-benchmark-modes--what-are-you-actually-measuring)
8. [Reading JMH Output](#8-reading-jmh-output)
9. [Parallel Streams — When to Use Them](#9-parallel-streams--when-to-use-them)
10. [Common Mistakes Checklist](#10-common-mistakes-checklist)
11. [Project Setup](#11-project-setup)

---

## 1. The Core Problem — Why Java Benchmarks Lie

When you write a Java loop to time something yourself, you're not measuring what you think you are. The JVM actively
rewrites your code as it runs, in ways that are invisible to you but completely legal because they don't change the
program's *observable* output.

### The JIT Compiler: Your Benchmarking Enemy

JIT stands for *Just-In-Time* compiler. The JVM doesn't interpret your bytecode forever — it watches which methods run
frequently and compiles those to native machine code at runtime, applying aggressive optimizations. This is why Java can
be fast. It's also why naïve benchmarks are useless.

Here are the three optimizations that destroy benchmarks most often:

---

### Optimization 1: Dead Code Elimination (DCE)

**What it is:** If the JVM can prove that a computation's result is never *used* anywhere observable (printed, returned,
stored in a field another thread could see), it simply deletes the computation entirely. Zero nanoseconds. No work done.

**Concrete example:**

```java
// You think you're measuring a 10-million-iteration sum loop.
// The JVM sees a local variable 'sum' that is computed and then thrown away.
// It proves: removing this loop changes nothing the outside world can observe.
// So it removes the entire loop. Your benchmark reports ~0 ns. Total lie.

public void broken() {
    long sum = 0;
    for (int i = 0; i < 10_000_000; i++) {
        sum += i;   // sum is never returned, stored, or printed
    }
    // sum is discarded here → JVM deletes the loop
}
```

**The fix:** Return the value. Returning it forces the JVM to keep the computation, because the caller now depends on
the result.

```java
public long correct() {
    long sum = 0;
    for (int i = 0; i < 10_000_000; i++) {
        sum += i;
    }
    return sum;   // caller depends on this → loop cannot be deleted
}
```

---

### Optimization 2: Constant Folding

**What it is:** If a computation depends only on values the JVM knows at compile time (constants, `static final`
fields), it computes the answer once and replaces your loop with a single hardcoded number.

**Concrete example:**

```java
// COUNT is a compile-time constant. The JVM knows:
//   sum of 0..9,999,999 = 49,999,995,000,000
// It replaces your loop with a single assignment. Benchmark: ~0 ns. Another lie.

public static final int COUNT = 10_000_000;

public long broken() {
    long sum = 0;
    for (int i = 0; i < COUNT; i++) {
        sum += i;
    }
    return sum;   // JVM already knows this is 49,999,995,000,000 → replaces loop
}
```

**The fix:** Put your data in a `@State` object (explained in §6). The JVM cannot assume that an object's field holds
the same value across method calls, so it must actually run the loop.

```java
// In a @State class:
public int count = 10_000_000;   // NOT final — the JVM can't fold this

// In your benchmark:
public long correct(MyState state) {
    long sum = 0;
    for (int i = 0; i < state.count; i++) {
        sum += i;
    }
    return sum;   // real work, real timing
}
```

---

### Optimization 3: Warmup — You're Not Always Measuring the Same Code

**What it is:** When Java code first runs, it's *interpreted* — executed slowly, instruction by instruction. After a
method is called about 10,000 times, the JIT compiles it to native machine code (5–50× faster). This process is called
*warmup*.

**Why this matters for benchmarks:** If you start timing too early, you measure the slow interpreter, not the fast
compiled code. Your numbers are meaningless for predicting production performance.

```
First call               → Interpreter (slow)
~100 invocations         → Lightly compiled (C1 tier)
~10,000 invocations      → Fully optimized native code (C2 tier) ← THIS is production behavior
```

**The fix:** Always have a warmup phase before measuring. JMH handles this automatically.

---

## 2. How JMH Solves It

JMH (Java Microbenchmark Harness) is the official OpenJDK tool for benchmarking. It doesn't just time your method — it
generates a complete benchmark runner around your annotated methods, with:

- A **warmup phase** that lets the JIT fully optimize your code before any timing starts
- **Forked processes** — each benchmark runs in a fresh JVM to prevent one benchmark from contaminating another's
  optimizer state
- **Blackhole** objects to consume results and defeat dead code elimination
- Statistically sound output with confidence intervals

You annotate your methods. JMH generates the infrastructure. You never write `System.nanoTime()` yourself.

---

## 3. The 6 Annotations You Actually Need

These cover 90% of real-world JMH usage.

```java
@BenchmarkMode(Mode.AverageTime)          // What to measure (§7)
@OutputTimeUnit(TimeUnit.MICROSECONDS)    // Unit for results
@State(Scope.Benchmark)                   // Marks this class as holding benchmark data (§6)
@Warmup(iterations = 3, time = 1)         // 3 warmup rounds, 1 second each
@Measurement(iterations = 5, time = 1)   // 5 measurement rounds, 1 second each
@Fork(2)                                  // Run in 2 separate JVM processes
public class MyBenchmark {

    public int dataSize = 1_000_000;      // NOT final — prevents constant folding

    @Benchmark                            // This method gets measured
    public long myMethod() {
        long sum = 0;
        for (int i = 0; i < dataSize; i++) sum += i;
        return sum;                       // return defeats dead code elimination
    }
}
```

### Annotation Quick Reference

| Annotation        | What It Does                                                              | Sensible Default             |
|-------------------|---------------------------------------------------------------------------|------------------------------|
| `@Benchmark`      | Marks the method to measure                                               | Required on every method     |
| `@BenchmarkMode`  | What metric to collect (time, throughput, etc.)                           | `Mode.AverageTime`           |
| `@OutputTimeUnit` | Unit for the reported numbers                                             | `TimeUnit.MICROSECONDS`      |
| `@Warmup`         | How many warmup rounds before timing starts                               | `iterations=3, time=1`       |
| `@Measurement`    | How many rounds to actually measure                                       | `iterations=5, time=1`       |
| `@Fork`           | How many separate JVM processes to run (each gets a clean optimizer state) | `2` for dev, `3` for publishing |
| `@State`          | Marks a class as holding data for benchmarks (explained in §6)            | `Scope.Benchmark`            |

### `@Fork` — Why Separate Processes Matter

When two benchmarks run in the same JVM, they interfere. The JIT's optimizer "remembers" which types it has seen
at each call site. If benchmark A always called `ArrayList.get()`, the JIT optimized assuming only `ArrayList`. When
benchmark B runs with a `LinkedList`, the optimizer's assumptions are wrong and it de-optimizes — making B look slow
for reasons unrelated to what you're testing.

Running each benchmark in a **fresh process** gives it a clean optimizer with no memory of other benchmarks.

```java
@Fork(2)  // Runs the full benchmark in 2 separate JVM processes, averages results
          // More forks = more trustworthy results, but longer runtime
```

---

## 4. Your First Correct Benchmark

Here is a complete, correct benchmark comparing sequential vs. parallel sum over a list. Every decision is explained.

```java
package com.example.benchmarks;

import org.openjdk.jmh.annotations.*;
import java.util.*;
import java.util.stream.*;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(value = 2, jvmArgs = {"-Xms512m", "-Xmx512m"})
//              ↑ Lock heap size so GC behavior is consistent across runs
public class SumBenchmark {

    // @Param runs the whole benchmark once per value listed here
    @Param({"1000", "100000", "1000000"})
    public int dataSize;

    private List<Integer> data;

    // @Setup runs before timing starts — use it to prepare data
    @Setup(Level.Trial)
    public void setup() {
        data = IntStream.range(0, dataSize)
                        .boxed()
                        .collect(Collectors.toList());
    }

    @Benchmark
    public long sequential() {
        return data.stream()
                   .mapToLong(Integer::longValue)
                   .sum();   // return value defeats dead code elimination
    }

    @Benchmark
    public long parallel() {
        return data.parallelStream()
                   .mapToLong(Integer::longValue)
                   .sum();
    }
}
```

**What each piece does:**

- `@Param` — JMH runs every `@Benchmark` method with each param value. You get 6 rows of output (2 methods × 3 sizes).
  This reveals the *crossover point* where parallel becomes faster than sequential.
- `@Setup(Level.Trial)` — Builds the list once per JVM fork, before any warmup or measurement. `Level.Trial` means "the
  entire run" — don't rebuild data between every single measurement iteration.
- `-Xms512m -Xmx512m` — Sets both the minimum and maximum heap to the same value. This prevents the JVM from
  requesting more memory mid-run, which would cause unpredictable pauses that corrupt your timing.
- Returning `long` from each `@Benchmark` — defeats dead code elimination on both.

---

## 5. The 3 Silent Killers — and Their Fixes

Quick reference for the problems from §1, with the exact patterns to use.

### Killer 1: Dead Code Elimination

**Symptom:** Benchmark reports suspiciously close to 0 ns/op.

```java
// BROKEN: result unused → loop deleted
@Benchmark
public void broken() {
    Math.sqrt(99999.0);   // result thrown away
}

// FIXED: return the result
@Benchmark
public double fixed() {
    return Math.sqrt(99999.0);
}
```

**When you have multiple values to consume** (not just one return value), use `Blackhole`. JMH injects it
automatically — just add it as a parameter:

```java
// Blackhole is JMH's "result sink" — consuming a value into it prevents
// the JVM from deleting the computation, without actually doing real I/O.
@Benchmark
public void multipleValues(Blackhole bh) {
    bh.consume(Math.sqrt(100.0));
    bh.consume(Math.sqrt(200.0));
    bh.consume(Math.sqrt(300.0));
}
```

Use `Blackhole` when: you have multiple results, or you're benchmarking `void` methods where there's no value to return.

---

### Killer 2: Constant Folding

**Symptom:** Sequential benchmark is suspiciously faster than you'd expect. Results don't scale with data size.

```java
// BROKEN: COUNT is a compile-time constant → loop becomes a single value
public static final int COUNT = 10_000_000;

@Benchmark
public long broken() {
    long sum = 0;
    for (int i = 0; i < COUNT; i++) sum += i;
    return sum;   // JVM sees: this is always 49,999,995,000,000 → replaces loop
}

// FIXED: non-final field in a @State object
@State(Scope.Benchmark)
public class MyBenchmark {
    public int count = 10_000_000;   // NOT static, NOT final

    @Benchmark
    public long fixed() {
        long sum = 0;
        for (int i = 0; i < count; i++) sum += i;
        return sum;   // real work — JVM must actually run the loop
    }
}
```

**Rule:** Any constant that determines loop bounds or computation input must be a non-final instance field.

---

### Killer 3: Measuring Unwarmed Code

**Symptom:** First few runs are much slower than later runs. Results vary wildly between `@Fork` runs.

```java
// BROKEN: no warmup → you're measuring the interpreter, not compiled code
@Warmup(iterations = 0)
@Measurement(iterations = 1)
```

```java
// CORRECT: give the JIT time to compile and stabilize
@Warmup(iterations = 3, time = 1)    // 3 seconds total of warmup
@Measurement(iterations = 5, time = 1)  // 5 seconds of real measurement
```

**How much warmup do you need?** For simple methods: 3 iterations × 1s is usually enough. For complex methods with many
call paths (e.g., benchmarking a service class): use 5 iterations × 2s.

---

## 6. State — Managing Your Benchmark's Data

`@State` marks a class (or the benchmark class itself) as holding data that JMH manages. This is how you:
- Prepare data before benchmarking starts (without that preparation counting toward timing)
- Prevent constant folding (non-final fields)
- Control whether data is shared between threads or private to each thread

### The Two Scopes You'll Actually Use

#### `Scope.Benchmark` — One instance, shared by all threads

```java
@State(Scope.Benchmark)
public class SharedState {
    public List<Integer> data;          // all benchmark threads read this

    @Setup(Level.Trial)
    public void setup() {
        data = IntStream.range(0, 1_000_000)
                        .boxed()
                        .collect(Collectors.toList());
    }
}
```

Use this when: your data is **read-only** and you want to avoid setting it up multiple times. Most benchmarks use this.

#### `Scope.Thread` — One instance per thread

```java
@State(Scope.Thread)
public class ThreadLocalState {
    public Random rng;

    @Setup(Level.Iteration)
    public void setup() {
        rng = new Random(42);   // each thread gets its own RNG, reset each iteration
    }
}
```

Use this when: each thread needs its own mutable state (counters, random generators, accumulators). If threads shared a
`Random`, they'd contend on it — you'd be measuring lock contention, not your actual algorithm.

### `@Setup` Levels — When Does Your Prep Code Run?

| Level              | When it runs                                           | Use it for                          |
|--------------------|--------------------------------------------------------|-------------------------------------|
| `Level.Trial`      | Once per entire benchmark run (before warmup starts)   | Building large datasets, opening connections |
| `Level.Iteration`  | Before every warmup and measurement iteration          | Resetting counters, clearing caches |
| `Level.Invocation` | Before **every single** `@Benchmark` call              | Almost never — the overhead distorts results |

```java
// Example: reset a counter before every measurement iteration
// so each iteration starts from the same state

@State(Scope.Thread)
public class CounterState {
    public int counter;

    @Setup(Level.Iteration)    // resets before each 1-second measurement window
    public void reset() {
        counter = 0;
    }
}
```

### `@Param` — Run the Same Benchmark Across Multiple Inputs

```java
@State(Scope.Benchmark)
public class ParamExample {

    @Param({"100", "10000", "1000000"})
    public int dataSize;            // JMH runs the full suite 3 times, once per value

    public List<Integer> data;

    @Setup(Level.Trial)
    public void setup() {
        data = IntStream.range(0, dataSize).boxed().collect(Collectors.toList());
    }
}
```

This is how you find the crossover point where parallel beats sequential — instead of guessing, you measure at multiple
scales and let the data show you.

---

## 7. Benchmark Modes — What Are You Actually Measuring?

| Mode                  | Reports                              | Use When                                         |
|-----------------------|--------------------------------------|--------------------------------------------------|
| `Mode.AverageTime`    | Average time per operation           | "Which of these two is faster?" — most common    |
| `Mode.Throughput`     | Operations per second                | "How many requests/sec can this handle?"         |
| `Mode.SampleTime`     | Full distribution: p50, p90, p99...  | "What are my worst-case latencies?"              |
| `Mode.SingleShotTime` | One cold run, no warmup              | "How slow is this on first call?" (startup cost) |

**For 80% of benchmarks, use `Mode.AverageTime`.**

```java
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
```

**Use `Mode.SampleTime` when tail latency matters** — for anything user-facing, API endpoints, DB queries:

```java
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Benchmark
public String cacheLookup() {
    return cache.get(key);
}
```

JMH will report: `p50 = 12µs`, `p90 = 18µs`, `p99 = 340µs`, `p99.9 = 2100µs`.
A high p99 with a low p50 reveals lock contention or GC pauses that averages hide.

---

## 8. Reading JMH Output

```
Benchmark                          (dataSize)  Mode  Cnt   Score    Error  Units
SumBenchmark.sequential                  1000  avgt    5   0.012 ±  0.001  ms/op
SumBenchmark.sequential                100000  avgt    5   0.856 ±  0.012  ms/op
SumBenchmark.sequential               1000000  avgt    5   8.432 ±  0.089  ms/op
SumBenchmark.parallel                    1000  avgt    5   0.198 ±  0.008  ms/op
SumBenchmark.parallel                  100000  avgt    5   0.412 ±  0.031  ms/op
SumBenchmark.parallel                 1000000  avgt    5   1.987 ±  0.144  ms/op
```

### Column Meanings

| Column       | Meaning                                                                      |
|--------------|------------------------------------------------------------------------------|
| `(dataSize)` | The `@Param` value for this row                                               |
| `Mode`       | Benchmark mode (`avgt` = AverageTime)                                        |
| `Cnt`        | How many measurement samples were collected                                  |
| `Score`      | The main result (time per op for `avgt`)                                     |
| `Error`      | ±2σ confidence interval — how much the results varied across runs            |
| `Units`      | `ms/op` = milliseconds per operation                                         |

### How to Interpret the Error Column

The `Error` is the ± margin. If Score is `8.432 ± 0.089`, the true value is somewhere in `[8.343, 8.521]`. That's tight
and trustworthy — about 1% relative error.

```
Score: 8.432 ± 0.089 ms/op    ← 1% error — trustworthy ✓
Score: 8.432 ± 4.201 ms/op    ← 50% error — meaningless ✗
```

**If error is more than ~5% of score:**
- Add more warmup iterations (the JIT may not have fully stabilized)
- Increase the fork count (`@Fork(3)`)
- Make sure no other heavy processes are running on your machine

### Reading the Parallel Story in the Example Output

```
dataSize=1000:   sequential=0.012ms,  parallel=0.198ms   → parallel is 16× SLOWER
dataSize=100000: sequential=0.856ms,  parallel=0.412ms   → parallel is 2× faster
dataSize=1000000:sequential=8.432ms,  parallel=1.987ms   → parallel is 4× faster
```

This is the crossover point in action. The overhead of splitting work across threads costs ~0.19ms regardless of data
size. That overhead dominates at 1,000 elements, but becomes negligible at 1,000,000.

---

## 9. Parallel Streams — When to Use Them

### The Core Mental Model

Parallel streams split your data into chunks, process them on multiple CPU cores simultaneously, then combine the
results. The split + combine steps have a fixed cost. Parallelism only helps when the actual computation cost exceeds
that fixed overhead.

```
Sequential: [process all N elements on 1 thread]
Parallel:   [split] → [process N/4 on thread 1] → [combine]
                     [process N/4 on thread 2]
                     [process N/4 on thread 3]
                     [process N/4 on thread 4]
```

Parallel wins when: `(work / 4 threads) + overhead < total work`
Parallel loses when: `overhead > the time saved by splitting`

### Quick Decision Rules

```
data size < 10,000?              → Use sequential. Overhead exceeds benefit.
Simple operation (sum, count)?   → Parallel needs ~100,000+ elements to win.
Complex per-element work?        → Parallel wins at smaller sizes (work dominates).
Data source is LinkedList?       → Never use parallel (splitting is O(n), kills all benefit).
Using .collect(toList())?        → Be careful — the merge step is single-threaded (see below).
Unsure?                          → Use @Param to benchmark both at your actual data sizes.
```

### The `.collect()` Trap

```java
// This looks parallel, but the final step — merging all sub-lists into one — is single-threaded.
// For large lists, the merge can cost more than you saved in filtering.
return data.parallelStream()
           .filter(n -> n % 2 == 0)
           .collect(Collectors.toList());   // ← serial merge bottleneck

// Better alternatives if you don't need a List:
long count = data.parallelStream().filter(n -> n % 2 == 0).count();         // no merge
int[] evens = data.parallelStream().filter(n -> n % 2 == 0)
                  .mapToInt(Integer::intValue).toArray();                    // array merge (fast)
```

### The Comparison Pitfall

When comparing sequential vs. parallel, use **identical data sources and types**. Otherwise you're measuring something
other than parallelism.

```java
// WRONG: sequential uses primitive IntStream, parallel uses boxed List<Integer>
// You're measuring autoboxing overhead, not parallelism overhead
@Benchmark
public long sequential() { return IntStream.range(0, COUNT).sum(); }

@Benchmark
public long parallel() { return data.parallelStream().mapToLong(Integer::longValue).sum(); }

// CORRECT: same data source, same types — only parallelism differs
@Benchmark
public long sequential() { return data.stream().mapToLong(Integer::longValue).sum(); }

@Benchmark
public long parallel() { return data.parallelStream().mapToLong(Integer::longValue).sum(); }
```

---

## 10. Common Mistakes Checklist

Before publishing or acting on any benchmark result, verify:

**Setup issues:**

- [ ] `@Fork` is at least `2` (not `0` — never run without forking)
- [ ] `@Warmup` has at least 3 iterations (don't set to `0`)
- [ ] Heap is locked: `-Xms` equals `-Xmx` in `jvmArgs`
- [ ] Benchmark is run standalone, not inside a JUnit test (running inside a test poisons the JIT profile)

**Dead code / constant folding:**

- [ ] Every `@Benchmark` method either returns a value or uses `Blackhole.consume()`
- [ ] Loop bounds and input sizes are non-final instance fields, not `static final` constants
- [ ] Data is prepared in `@Setup`, not as a `static final` initialized field

**Comparing correctly:**

- [ ] Sequential and parallel benchmarks use the same data type and source
- [ ] `@Param` is used to test at your actual production data sizes, not arbitrary ones
- [ ] Mutable state is reset with `@Setup(Level.Iteration)` if it changes across iterations

**Results:**

- [ ] Error (±) is less than 5% of Score
- [ ] If using parallel streams with `.collect()`, verified the merge bottleneck isn't dominating

---

## 11. Project Setup

### Gradle (Kotlin DSL) — Recommended

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

Benchmark files go in `src/jmh/java/`. Run with: `./gradlew jmh`

### Maven

```xml
<dependencies>
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
</dependencies>

<build>
  <plugins>
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-shade-plugin</artifactId>
      <executions>
        <execution>
          <goals><goal>shade</goal></goals>
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
  </plugins>
</build>
```

Build and run:

```bash
mvn clean package
java -jar target/benchmarks.jar                      # run all
java -jar target/benchmarks.jar SumBenchmark         # run one class
java -jar target/benchmarks.jar -f 1 -wi 1 -i 1      # quick smoke test
```

---

## Appendix: Minimal Correct Template

Copy this for every new benchmark. Fill in your logic. Every line has a reason.

```java
package com.example.benchmarks;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)           // measure average time per call
@OutputTimeUnit(TimeUnit.MICROSECONDS)     // report in microseconds
@State(Scope.Benchmark)                    // this class holds benchmark data
@Warmup(iterations = 3, time = 1)          // 3 seconds of warmup before timing
@Measurement(iterations = 5, time = 1)    // 5 seconds of actual measurement
@Fork(value = 2, jvmArgs = {"-Xms512m", "-Xmx512m"})  // 2 fresh JVMs, fixed heap
public class TemplateBenchmark {

    // Non-final field prevents constant folding
    public int inputSize = 100_000;

    // Prepare expensive data here — this doesn't count toward timing
    @Setup(Level.Trial)
    public void setup() {
        // e.g., build your list, open a connection, etc.
    }

    @Benchmark
    public long benchmarkA() {
        long result = 0;
        // your code here
        return result;   // always return or consume with Blackhole
    }

    @Benchmark
    public long benchmarkB() {
        long result = 0;
        // competing implementation here
        return result;
    }
}
```

---

*This guide covers the 20% of JMH that handles 80% of real benchmarking work. For advanced topics (profilers, async
profiling, hardware counters), see the official JMH samples at openjdk.org/projects/code-tools/jmh.*
