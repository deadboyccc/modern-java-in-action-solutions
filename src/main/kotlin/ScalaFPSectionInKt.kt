fun main() {
    (1..6).forEach { println(" this is a number $it") }
    val namesToAge = mapOf("Alice" to 25, "Bob" to 30, "Charlie" to 35)

    // persistent data structures are immutable, so we can create a new map with an additional entry without modifying the original map
    // 1:1 scala
    val set = setOf(1, 2, 3, 4, 5)
    val newSet = set + 10

    println("old set: ${set.hashCode().toHexString()}")
    println("new set: ${newSet.hashCode().toHexString()}")
}

class Tree(
    val key: String,
    val value: Int,
    val left: Tree? = null,
    val right: Tree? = null
)

object TreeProcessor {
    fun lookup(k: String, defaultValue: Int, t: Tree?): Int {
        if (t == null) return defaultValue
        if (k == t.key) return t.value
        return lookup(
            k, defaultValue,
            if (k < t.key) t.left else t.right
        )
    }
    // other methods processing a Tree
}
