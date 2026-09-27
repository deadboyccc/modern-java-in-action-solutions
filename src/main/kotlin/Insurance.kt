// Idiomatic Kotlin equivalents of every construct from the Scala comparison chapter.
// Run with: kotlinc ScalaComparisonDemo.kt -include-runtime -d demo.jar && java -jar demo.jar

// =========================================================================
// 4. DOMAIN TYPES USED BELOW
// =========================================================================

data class Insurance(val name: String)
data class Car(val insurance: Insurance?)
data class Person(val name: String, val age: Int, val car: Car?)

// "Boilerplate-free class" — Scala's `class Student(var name, var id)` was
// MUTABLE, and Kotlin's constructor-property syntax matches it line for line.
class Student(var name: String, var id: Int)

// Immutable version — equals/hashCode/toString/copy generated for free.
data class ImmutableStudent(val name: String, val id: Int)

// Traits vs interfaces: default methods + abstract properties, but no
// backing-field state (unlike a real Scala trait's `var size: Int = 0`).
interface Sized {
    val size: Int
    fun isEmpty() = size == 0
}

class Empty : Sized {
    override val size = 0
}

// =========================================================================
// MAIN
// =========================================================================

fun main() {

    println("=== 1. HELLO WORLD ===")
    // imperative style
    var n = 2
    while (n <= 6) {
        println("Hello $n bottles of beer")
        n++
    }
    // functional style
    (2..6).forEach { m -> println("Hello $m bottles of beer") }

    println("\n=== 2. COLLECTIONS ===")
    val authorsToAge = mapOf("Raoul" to 23, "Mario" to 40, "Alan" to 53)
    println("authorsToAge = $authorsToAge")

    val authors = listOf("Raoul", "Mario", "Alan")
    val numbers = setOf(1, 1, 2, 3, 5, 8) // silently deduplicated, like Scala
    println("authors = $authors")
    println("numbers = $numbers")

    // immutable "add" — Kotlin overloads `+` on read-only collections
    val smallSet = setOf(2, 5, 3)
    val withEight = smallSet + 8
    println("original set untouched: $smallSet")
    println("new set with 8 added:   $withEight")

    // filter + map pipeline — `it` is Kotlin's answer to Scala's `_` placeholder
    val fileLines = listOf("short", "this is a long enough line", "also long enough line")
    val linesLongUpper = fileLines.filter { it.length > 10 }.map { it.uppercase() }
    println("linesLongUpper = $linesLongUpper")

    println("\n=== 3. TUPLES (Pair/Triple, else data class) ===")
    val raoul = "Raoul" to "+44 7700 700042"
    println("raoul = ${raoul.first}, ${raoul.second}")

    val book = Triple(2018, "Modern Java in Action", "Manning")
    println("book.first = ${book.first}")

    println("\n=== 4. NULLABLE TYPES (Kotlin's answer to Option/Optional) ===")
    val person = Person("Raoul", 30, Car(Insurance("Acme")))
    println("insurance name = ${getCarInsuranceName(person, 18)}")
    println("insurance name (too young) = ${getCarInsuranceName(person, 40)}")

    println("\n=== 5. FIRST-CLASS FUNCTIONS ===")
    val tweets = listOf(
        "I love the new features in Java",
        "How's it going?",
        "An SQL query walks into a bar"
    )
    tweets.filter(::isJavaMentioned).forEach(::println)

    println("\n=== 6. ANONYMOUS FUNCTIONS ===")
    val isLongTweet: (String) -> Boolean = { tweet -> tweet.length > 60 }
    println("isLongTweet = ${isLongTweet("A very short tweet")}")

    println("\n=== 7. CLOSURES (mutable capture — just works, unlike Java) ===")
    var count = 0
    val inc = { count++ }
    inc()
    inc()
    println("count = $count") // 2

    println("\n=== 8. CURRYING ===")
    val multiplyCurry: (Int) -> (Int) -> Int = { x -> { y -> x * y } }
    val r = multiplyCurry(2)(10)
    println("multiplyCurry(2)(10) = $r")
    val multiplyByTwo = multiplyCurry(2)
    listOf(1, 3, 5, 7).map(multiplyByTwo).forEach(::println)

    println("\n=== 9. BOILERPLATE-FREE CLASSES ===")
    val student = Student("Raoul", 1)
    student.id = 1337 // mutable, matching Scala's `var` fields directly
    println("student.id = ${student.id}")

    val immutableStudent = ImmutableStudent("Raoul", 1)
    val updated = immutableStudent.copy(id = 1337) // real structural "update"
    println("updated.id = ${updated.id}")

    println("\n=== 10. TRAITS vs INTERFACES ===")
    println("Empty().isEmpty() = ${Empty().isEmpty()}")
}

fun getCarInsuranceName(person: Person?, minAge: Int): String =
    person?.takeIf { it.age >= minAge }
        ?.car?.insurance?.name
        ?: "Unknown"

fun isJavaMentioned(tweet: String) = tweet.contains("Java")