// cleaner but not recursive, more efficient than the previous version
fun primes(): Sequence<Int> = sequence {
    var candidate = 2
    val foundPrimes = mutableListOf<Int>()

    while (true) {
        // Test divisibility only against previously found primes
        if (foundPrimes.none { prime -> candidate % prime == 0 }) {
            foundPrimes.add(candidate)
            yield(candidate)
        }
        candidate++
    }
}

fun main() {
    println(primes().take(10).toList()) // [2, 3, 5, 7, 11, 13, 17, 19, 23, 29]
}
