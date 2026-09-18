# Functional Data Structures & Lazy Evaluation in Kotlin

A practical guide to immutable tree updates via path copying, and lazy recursive streams, implemented using an idiomatic
Kotlin `LazyList`.

## Table of Contents

1. [Core Concepts](#1-core-concepts)
2. [Tree Updates: Imperative vs. Persistent](#2-tree-updates-imperative-vs-persistent)
3. [Lazy Recursive Streams: Sieve of Eratosthenes](#3-lazy-recursive-streams-sieve-of-eratosthenes)
4. [Kotlin Constructs & Language Features](#4-kotlin-constructs--language-features)
5. [Complete Single-File Implementation](#5-complete-single-file-implementation)

---

## 1. Core Concepts

| Concept                        | Definition                                                                                                                                                                                                           |
|--------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Immutability**               | Immutable data structures cannot be modified after creation. State changes are handled by constructing new data structures that reflect the updated values while leaving original references untouched.              |
| **Persistent Data Structures** | A data structure that preserves previous versions of itself when updated. Because nodes are immutable, multiple versions can safely exist simultaneously across different parts of an application or across threads. |
| **Structural Sharing**         | A technique where a newly created version of a persistent data structure reuses (shares) unchanged memory subtrees from previous versions, reducing memory footprint and allocation time.                            |
| **Path Copying**               | The process of duplicating only the nodes along the pathway from the root to an updated leaf node, while pointing directly to existing, untouched subtrees for all off-path branches.                                |
| **Lazy Evaluation**            | Delaying the execution of an expression until its evaluation is explicitly required.                                                                                                                                 |
| **Memoization (Thunks)**       | Caching the result of a delayed computation (a *thunk*) so that it executes at most once. Subsequent references reuse the cached output immediately.                                                                 |

---

## 2. Tree Updates: Imperative vs. Persistent

When modifying a Binary Search Tree (BST), two distinct paradigms exist.

### 2.1 Imperative (In-Place Mutation)

```
          [Mary:22]
         /         \
   [Emily:20]     [Tian:29]  <-- mutate Tian's child pointer to point to Will:26
                   /
              [Raoul:23]
```

- **Mechanism:** Directly overwrites memory fields (`t.right = ...`).
- **Consequence:** All readers holding a reference to `t` see the modification immediately. Thread safety requires
  explicit locking/synchronization.

### 2.2 Persistent / Functional (`fupdate` via Path Copying)

```
  Original Tree (t)                  New Tree (fupdate output)

     [Mary:22]                           (Mary:22)  <-- new root
    /         \                         /          \
[Emily:20]   [Tian:29]             (shared)      (Tian:29) <-- new node
  /   \          \                 /    \          /      \
[A]   [G]      [Raoul:23]       [A]     [G]   (shared)  (Will:26) <-- new node
                                                   /
                                             [Raoul:23]
```

- **Mechanism:** Recreates only the nodes along the path to the updated/inserted element. All non-path nodes (like the
  subtree under `Emily:20`) are shared directly via memory references.
- **Efficiency:** Updating a tree of height $d$ requires creating only $d + 1$ new nodes. In a balanced BST
  containing $N$ elements, time and space complexity is $O (\log N)$.

### 2.3 Comparison

|                  | Imperative    | Persistent                           |
|------------------|---------------|--------------------------------------|
| Mutation         | In place      | New nodes only along the update path |
| Old version      | Lost          | Still valid and accessible           |
| Thread safety    | Needs locking | Safe by construction                 |
| Space per update | $O(1)$        | $O(\log N)$ for a balanced tree      |

---

## 3. Lazy Recursive Streams: Sieve of Eratosthenes

Traditional candidate testing iterates over all integers up to $\sqrt{N}$ for divisibility checks. By the **Fundamental
Theorem of Arithmetic**, however, every non-prime number is built from smaller prime factors — so testing divisibility
against non-primes is redundant.

### Algorithm Pipeline

1. **Initial stream:** generate an infinite sequence of integers starting at `2`: `[2, 3, 4, 5, 6, 7, 8, 9, 10, ...]`
2. **Head extraction:** the head element (`2`) is identified as prime.
3. **Lazy tail filtering:** filter out all multiples of `2` from the remaining tail, producing `[3, 5, 7, 9, 11, ...]`.
4. **Recursion:** recursively apply steps 1–3 to the filtered tail:
    - Next head: **`3`**
    - Filter tail for multiples of `3` → `[5, 7, 11, 13, 17, ...]`
    - Next head: **`5`**
    - ...and so on, indefinitely.

Because the stream is lazy, only as many terms are ever computed as are actually requested (e.g., via `.take(10)`).

---

## 4. Kotlin Constructs & Language Features

| Construct                             | Role in this implementation                                                                                                                                                                             |
|---------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `sealed interface`                    | Restricts a class hierarchy to a closed set of subtypes defined in the same package, enabling exhaustive `when` matching at compile time without an `else` branch.                                      |
| `object` (singleton)                  | Defines a single thread-safe instance. `LazyList.Nil` uses `object` typed with `Nothing` to represent the empty list universally.                                                                       |
| `Nothing` type                        | The bottom type in Kotlin's type system — a value that never exists. `LazyList<Nothing>` is a subtype of `LazyList<T>` for any `T`, thanks to generic covariance (`out T`).                             |
| `out T` (declaration-site covariance) | Allows a generic producer type `LazyList<Derived>` to be assigned to `LazyList<Base>`.                                                                                                                  |
| `by lazy` delegate                    | Delegates property initialization to a thread-safe lambda (`LazyThreadSafetyMode.SYNCHRONIZED` by default). The expression runs only on first `tail` access and caches the result for subsequent calls. |
| `infix fun`                           | Allows calling single-parameter functions without dot notation or parentheses, e.g. `head cons { tail }`.                                                                                               |

---

## 5. Complete Single-File Implementation

```kotlin
import java.util.NoSuchElementException

/**
 * Functional LazyList featuring structural immutability,
 * lazy consing, and thread-safe tail memoization.
 */
sealed interface LazyList<out T> {
    val head: T
    val tail: LazyList<T>
    val isEmpty: Boolean

    /** Represents an empty LazyList node. */
    object Nil : LazyList<Nothing> {
        override val head: Nothing
            get() = throw NoSuchElementException("Head on empty LazyList")
        override val tail: Nothing
            get() = throw NoSuchElementException("Tail on empty LazyList")
        override val isEmpty: Boolean = true
    }

    /** Represents a populated node storing a head value and a deferred tail computation. */
    class Cons<out T>(
        override val head: T,
        tailProducer: () -> LazyList<T>
    ) : LazyList<T> {
        // Caches tail evaluation after first access.
        override val tail: LazyList<T> by lazy(tailProducer)
        override val isEmpty: Boolean = false
    }
}

/**
 * Infix operator constructing a LazyList node.
 * Mirrors Scala's `#::` cons operator.
 */
infix fun <T> T.cons(tail: () -> LazyList<T>): LazyList<T> = LazyList.Cons(this, tail)

/**
 * Lazily filters elements based on a predicate without evaluating non-matching nodes upfront.
 */
fun <T> LazyList<T>.filter(predicate: (T) -> Boolean): LazyList<T> = when (this) {
    is LazyList.Nil -> LazyList.Nil
    is LazyList.Cons -> if (predicate(head)) head cons { tail.filter(predicate) } else tail.filter(predicate)
}

/**
 * Eagerly materializes the first [n] elements into a standard Kotlin List.
 */
fun <T> LazyList<T>.take(n: Int): List<T> = when {
    n <= 0 || isEmpty -> emptyList()
    else -> listOf(head) + tail.take(n - 1)
}

/**
 * Generates an infinite recursive stream of incremental integers starting at [n].
 */
fun numbers(n: Int): LazyList<Int> = n cons { numbers(n + 1) }

/**
 * Recursive prime sieve implementation.
 */
fun primes(numbers: LazyList<Int>): LazyList<Int> =
    numbers.head cons { primes(numbers.tail.filter { it % numbers.head != 0 }) }

fun main() {
    val first10Primes = primes(numbers(2)).take(10)
    println("First 10 primes: $first10Primes")
    // Output: First 10 primes: [2, 3, 5, 7, 11, 13, 17, 19, 23, 29]
}
```

---

## Summary

- **Persistence** comes from immutability plus **structural sharing**: only the nodes on the update path are copied;
  everything else is reused.
- **Laziness** comes from deferring computation into thunks and **memoizing** them with `by lazy`, so infinite
  structures like `LazyList` can be built and consumed incrementally.
- Together, these two ideas — persistent trees and lazy streams — are the backbone of most functional data structure
  design, and Kotlin's `sealed interface`, `object`, covariance, and delegated properties map onto them cleanly.