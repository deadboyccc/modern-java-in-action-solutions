package lazylist

/**
 * Functional LazyList featuring structural immutability,
 * lazy consing, and thread-safe tail memoization.
 */
sealed interface LazyList<out T> {
    val head: T
    val tail: LazyList<T>
    val isEmpty: Boolean

    /** A Singleton representing an empty LazyList node. */
    object Nil : LazyList<Nothing> {
        override val head: Nothing
            get() = throw NoSuchElementException("Head on empty LazyList")
        override val tail: Nothing
            get() = throw NoSuchElementException("Tail on empty LazyList")
        override val isEmpty: Boolean = true
    }

    /** Represents a populated node storing a head value and a deferred tail computation. */
    // cons = construct from head and tail
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
