package lazylist

/**
 * Idiomatic, thread-safe LazyList for Kotlin using memoized thunks (`by lazy`).
 * Mirroring Scala's LazyList / Stream data structure.
 */
sealed interface LazyList<out T> {
    val head: T
    val tail: LazyList<T>
    val isEmpty: Boolean

    object Nil : LazyList<Nothing> {
        override val head: Nothing get() = throw NoSuchElementException("Head on empty LazyList")
        override val tail: Nothing get() = throw NoSuchElementException("Tail on empty LazyList")
        override val isEmpty: Boolean = true
    }

    class Cons<out T>(
        override val head: T,
        tailProducer: () -> LazyList<T>
    ) : LazyList<T> {
        // Memoizes the tail so it's evaluated at most once
        override val tail: LazyList<T> by lazy(tailProducer)
        override val isEmpty: Boolean = false
    }
}

// Infix operator to mirror Scala's #:: (cons)
infix fun <T> T.cons(tail: () -> LazyList<T>): LazyList<T> = LazyList.Cons(this, tail)

// Lazy filter extension function
fun <T> LazyList<T>.filter(predicate: (T) -> Boolean): LazyList<T> = when (this) {
    is LazyList.Nil -> LazyList.Nil
    is LazyList.Cons -> if (predicate(head)) head cons { tail.filter(predicate) } else tail.filter(predicate)
}

// Terminal operation to evaluate and collect N elements eagerly
fun <T> LazyList<T>.take(n: Int): List<T> = when {
    n <= 0 || isEmpty -> emptyList()
    else -> listOf(head) + tail.take(n - 1)
}

// Generates an infinite stream of numbers: n cons { numbers(n + 1) }
fun numbers(n: Int): LazyList<Int> = n cons { numbers(n + 1) }

// Recursive prime sieve using head and filtered tail
fun primes(numbers: LazyList<Int>): LazyList<Int> =
    numbers.head cons { primes(numbers.tail.filter { it % numbers.head != 0 }) }

fun main() {
    val first10Primes = primes(numbers(2)).take(10)
    println("First 10 primes: $first10Primes") // Output: [2, 3, 5, 7, 11, 13, 17, 19, 23, 29]
}