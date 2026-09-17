// Generates an infinite stream/sequence of integers starting at n: n #:: numbers(n + 1)
fun numbers(n: Int): Sequence<Int> = sequence {
    yield(n)
    yieldAll(numbers(n + 1))
}

// Sieve of Eratosthenes: numbers.head #:: primes(numbers.tail filter ...)
fun primes(numbers: Sequence<Int>): Sequence<Int> = sequence {
    val head = numbers.first()
    yield(head)

    // Filter the tail lazily and recursively pass it back into primes
    val tailFiltered = numbers.drop(1).filter { n -> n % head != 0 }
    yieldAll(primes(tailFiltered))
}

fun main() {
    // Generate first 10 primes lazily
    val first10Primes = primes(numbers(2)).take(10).toList()
    println(first10Primes) // [2, 3, 5, 7, 11, 13, 17, 19, 23, 29]
}