# Scala → Modern Java 25 → Idiomatic Kotlin

A construct-by-construct mapping of everything in the "Comparing Java and Scala"
chapter, translated into modern Java 25 and idiomatic Kotlin equivalents.

---

## 1. Hello World

### Imperative style

**Scala**

```scala
object Beer {
  def main(args: Array[String]) {
    var n: Int = 2
    while (n <= 6) {
      println(s"Hello ${n} bottles of beer")
      n += 1
    }
  }
}
```

**Java 25**

```java
void main() {
    int n = 2;
    while (n <= 6) {
        System.out.println(STR."Hello \{n} bottles of beer");
        n++;
    }
}
```

Java 25 finalized **unnamed classes / instance main methods** (JEP 463 family), so
`main` no longer needs `public static`, a class wrapper, or `String[] args`.
`STR."..."` is the string template interpolator (finalized as a preview-successor feature; if templates aren't available
in your build, use
`"Hello %d bottles of beer".formatted(n)`).

**Kotlin**

```kotlin
fun main() {
    var n = 2
    while (n <= 6) {
        println("Hello $n bottles of beer")
        n++
    }
}
```

Kotlin has had top-level `fun main()` and `$variable` string templates since day one — this is the construct Scala's
`s"..."` and `object`-as-entry-point most directly maps to.

### Functional style

**Scala**

```scala
2 to 6 foreach { n => println(s"Hello ${n} bottles of beer") }
```

**Java 25**

```java
IntStream.rangeClosed(2, 6)
    .forEach(n -> System.out.println(STR."Hello \{n} bottles of beer"));
```

**Kotlin**

```kotlin
(2..6).forEach { n -> println("Hello $n bottles of beer") }
```

Kotlin's `IntRange` + trailing-lambda syntax reads almost identically to Scala's infix
`to`/`foreach`.

---

## 2. Collections

### Map

**Scala**

```scala
val authorsToAge = Map("Raoul" -> 23, "Mario" -> 40, "Alan" -> 53)
```

**Java 25**

```java
var authorsToAge = Map.of("Raoul", 23, "Mario", 40, "Alan", 53);
// or, for more than 10 entries:
var authorsToAge2 = Map.ofEntries(
    entry("Raoul", 23), entry("Mario", 40), entry("Alan", 53));
```

`Map.of`/`Map.ofEntries` (Java 9+) produce an **immutable** map, matching Scala's default. `var` gives the same "don't
annotate the type" feel as Scala's inference.

**Kotlin**

```kotlin
val authorsToAge = mapOf("Raoul" to 23, "Mario" to 40, "Alan" to 53)
```

Kotlin's infix `to` function building a `Pair` is the closest syntactic cousin of Scala's `->`. `mapOf` is read-only
(`Map`, not `MutableMap`), mirroring Scala's immutable-by-default collections.

### List / Set

**Scala**

```scala
val authors = List("Raoul", "Mario", "Alan")
val numbers = Set(1, 1, 2, 3, 5, 8)
```

**Java 25**

```java
var authors = List.of("Raoul", "Mario", "Alan");
var numbers = Set.of(1, 2, 3, 5, 8); // Set.of rejects duplicates at runtime
```

Java's `Set.of` actually **throws** on duplicate elements rather than silently deduping like Scala — worth calling out
as a real behavioral difference.

**Kotlin**

```kotlin
val authors = listOf("Raoul", "Mario", "Alan")
val numbers = setOf(1, 1, 2, 3, 5, 8) // silently deduplicated, like Scala
```

Kotlin's `setOf` matches Scala's dedup-silently behavior; Java's does not.

### Immutable vs. mutable

**Scala**

```scala
val numbers = Set(2, 5, 3)
val newNumbers = numbers + 8   // new Set; `numbers` untouched
```

**Java 25**

```java
Set<Integer> numbers = Set.of(2, 5, 3);
Set<Integer> newNumbers = Stream.concat(numbers.stream(), Stream.of(8))
    .collect(Collectors.toUnmodifiableSet());
```

Java has no single-method "persistent add"; you rebuild via streams. This is the sharpest verbosity gap in the whole
comparison — Java's immutable collections are copy-on-construct, not structurally-shared/persistent like Scala's or
Kotlin's persistent-collection libraries.

**Kotlin**

```kotlin
val numbers = setOf(2, 5, 3)
val newNumbers = numbers + 8   // new Set; `numbers` untouched
```

Kotlin overloads `+`/`-` on its read-only collections, giving you Scala's exact one-liner. (Note: like Java's, Kotlin's
stdlib `+` copies rather than structurally sharing — for true persistent/structural-sharing collections in Kotlin you'd
reach for
`kotlinx.collections.immutable`.)

Mutable versions: Scala's `scala.collection.mutable` ↔ Java's `HashMap`/`HashSet`/
`ArrayList` ↔ Kotlin's `mutableMapOf`/`mutableSetOf`/`mutableListOf`.

### filter + map pipelines

**Scala**

```scala
val linesLongUpper = fileLines filter (_.length() > 10) map (_.toUpperCase())
```

**Java 25**

```java
var linesLongUpper = fileLines.stream()
    .filter(l -> l.length() > 10)
    .map(String::toUpperCase)
    .toList();
```

**Kotlin**

```kotlin
val linesLongUpper = fileLines.filter { it.length > 10 }.map { it.uppercase() }
```

Kotlin's `it` implicit single-lambda-parameter is the direct analogue of Scala's `_`
placeholder — both let you drop the parameter name entirely. Kotlin also operates directly on `List` (no `.stream()`/
`.toList()` bookending needed, since Kotlin collection operations are eagerly-evaluated extension functions, not a
separate lazy-pipeline type).

### Parallel execution

**Scala**: `fileLines.par filter (...) map (...)`

**Java 25**: `fileLines.parallelStream().filter(...).map(...).toList()`

**Kotlin**: no stdlib `.par`; drop to Java interop — `fileLines.parallelStream()...`
or use coroutines (`Dispatchers.Default` + `async`/`awaitAll`) for structured parallelism. This is a real gap: Kotlin's
collection extensions are sequential-only by design, unlike Scala's collections.

### Tuples

**Scala**

```scala
val raoul = ("Raoul", "+44 7700 700042")
val book = (2018, "Modern Java in Action", "Manning")
println(book._1)
```

**Java 25**: still no tuple literal or general-purpose tuple type. The idiomatic replacement is a `record`:

```java
record Pair<X, Y>(X x, Y y) {
}

var raoul = new Pair<>("Raoul", "+44 7700 700042");

record Book(int year, String title, String publisher) {
}

var book = new Book(2018, "Modern Java in Action", "Manning");
System.out.

println(book.year());
```

This is the single biggest philosophical divergence: Java 25 leans on **records**
(named, typed, self-documenting) instead of adding anonymous tuple syntax, even though it now has the pattern-matching
machinery (`record` deconstruction in
`switch`/`instanceof`) that would make tuples easy to add.

**Kotlin**

```kotlin
val raoul = "Raoul" to "+44 7700 700042"       // Pair<String, String>
val triple = Triple(2018, "Modern Java in Action", "Manning")
println(triple.first)
```

Kotlin ships `Pair`/`Triple` in the stdlib (2- and 3-element only, unlike Scala's 22-element `TupleN` family) with
`.first`/`.second`/`.third` accessors — closer to Scala's `_1`/`_2` than Java gets, but still capped, and Kotlin's own
docs recommend a
`data class` over `Triple`+ for anything domain-meaningful, echoing Java's record philosophy:

```kotlin
data class Book(val year: Int, val title: String, val publisher: String)
```

### Option / Optional

**Scala**

```scala
def getCarInsuranceName(person: Option[Person], minAge: Int) =
  person.filter(_.age >= minAge)
        .flatMap(_.car)
        .flatMap(_.insurance)
        .map(_.name)
        .getOrElse("Unknown")
```

**Java 25**

```java
String getCarInsuranceName(Optional<Person> person, int minAge) {
    return person.filter(p -> p.getAge() >= minAge)
            .flatMap(Person::getCar)
            .flatMap(Car::getInsurance)
            .map(Insurance::getName)
            .orElse("Unknown");
}
```

Nearly identical shape and method names — Java's `Optional` was explicitly modeled on Scala's `Option`.

**Kotlin**

```kotlin
fun getCarInsuranceName(person: Person?, minAge: Int): String =
    person?.takeIf { it.age >= minAge }
        ?.car?.insurance?.name
        ?: "Unknown"
```

Kotlin doesn't use an `Optional`-style wrapper at all — it bakes optionality into the **type system** via nullable types
(`Person?`) plus the safe-call (`?.`), Elvis (`?:`), and `takeIf` operators. This is a deeper divergence than Java's:
where Scala and Java both wrap absence in a container type, Kotlin makes absence a first-class property of every type.

---

## 3. Functions

### First-class functions / method references

**Scala**

```scala
def isJavaMentioned(tweet: String): Boolean = tweet.contains("Java")
tweets.filter(isJavaMentioned).foreach(println)
```

**Java 25**

```java
static boolean isJavaMentioned(String tweet) {
    return tweet.contains("Java");
}
tweets.

stream().

filter(Main::isJavaMentioned).

forEach(System.out::println);
```

**Kotlin**

```kotlin
fun isJavaMentioned(tweet: String) = tweet.contains("Java")
tweets.filter(::isJavaMentioned).forEach(::println)
```

Kotlin's `::functionName` callable-reference syntax is a closer visual match to Scala's "just pass the method name" than
Java's `Class::method` is, since Kotlin allows bare top-level functions the way Scala allows bare `def`s.

### Function types

**Scala**: `def filter[T](p: (T) => Boolean): List[T]` — `(T) => Boolean` is a first-class function-type literal.

**Java 25**: no function-type literal; you name a functional interface —
`Predicate<T>` for `T => Boolean`, `Function<T, R>` for `T => R`. Java requires the type to be declared/imported before
use.

**Kotlin**: `fun <T> filter(p: (T) -> Boolean): List<T>` — Kotlin **does** have function-type literals
(`(T) -> Boolean`), making this the one place Kotlin matches Scala's expressiveness exactly and Java structurally
cannot.

### Anonymous functions / closures

**Scala**

```scala
val isLongTweet: String => Boolean = (tweet: String) => tweet.length() > 60
```

**Java 25**

```java
Function<String, Boolean> isLongTweet = tweet -> tweet.length() > 60;
```

**Kotlin**

```kotlin
val isLongTweet: (String) -> Boolean = { tweet -> tweet.length > 60 }
```

**Mutable closure capture** — the real behavioral gap:

**Scala**

```scala
var count = 0
val inc = () => count += 1
inc(); inc()
println(count) // 2
```

**Java 25**: still a compile error — lambdas may only capture *effectively final*
locals:

```java
int count = 0;
Runnable inc = () -> count += 1; // ❌ compile error, unchanged since Java 8
```

To mutate captured state in Java you need a heap-allocated holder:

```java
int[] count = {0};
Runnable inc = () -> count[0]++;
```

**Kotlin**

```kotlin
var count = 0
val inc = { count++ }
inc(); inc()
println(count) // 2
```

Kotlin lambdas — like Scala's — capture **variables**, not just values, so mutation of an outer `var` from inside a
lambda just works. This is a genuine language-level difference from Java, not just a syntax difference: Kotlin closures
behave exactly like Scala's here, while Java's fundamentally do not.

### Currying

**Scala**

```scala
def multiplyCurry(x: Int)(y: Int) = x * y
val r = multiplyCurry(2)(10)          // 20
val multiplyByTwo: Int => Int = multiplyCurry(2)
```

**Java 25**: no curried-declaration syntax; you curry manually by returning a
`Function`:

```java
static Function<Integer, Integer> multiplyCurry(int x) {
    return y -> x * y;
}
int r = multiplyCurry(2).apply(10);
Function<Integer, Integer> multiplyByTwo = multiplyCurry(2);
```

**Kotlin**: no built-in multi-argument-list curry syntax either, but higher-order functions + extension functions make
hand-rolled currying just as terse as Java's, and idiomatic Kotlin tends to reach for named/default arguments or simple
nested lambdas instead of currying:

```kotlin
fun multiplyCurry(x: Int): (Int) -> Int = { y -> x * y }
val r = multiplyCurry(2)(10)          // 20 — direct-call syntax on the returned lambda
val multiplyByTwo: (Int) -> Int = multiplyCurry(2)
```

Kotlin's `(Int) -> Int` return type being directly callable with `(10)` (no `.apply`
needed, unlike Java's `Function`) is the closest either JVM language gets to Scala's native curried-call syntax.

---

## 4. Classes and Traits

### Boilerplate-free data classes

**Scala**

```scala
class Student(var name: String, var id: Int)
val s = new Student("Raoul", 1)
s.id = 1337
```

**Java 25** — the record-vs-data-class distinction matters here. Scala's example is **mutable** (`var` fields with
implicit setters). A Java `record` is the wrong tool for that (records are immutable); a plain class with public fields,
or a small mutable class, is the honest equivalent:

```java
class Student {
    String name;
    int id;

    Student(String name, int id) {
        this.name = name;
        this.id = id;
    }
}

var s = new Student("Raoul", 1);
s.id =1337;
```

If the Scala class were declared with `val` instead (immutable), the idiomatic Java 25 mapping *would* be a `record`:

```java
record Student(String name, int id) {
}

var s = new Student("Raoul", 1);
System.out.

println(s.name());          // getter is the field name, not getName()
var updated = new Student(s.name(), 1337); // "mutation" = a new instance
```

**Kotlin** — Kotlin's `data class` covers both cases depending on `val`/`var`:

```kotlin
// mutable, matching the Scala example exactly:
class Student(var name: String, var id: Int)

val s = Student("Raoul", 1)
s.id = 1337

// or, immutable + equals/hashCode/toString/copy generated for you:
data class Student(val name: String, val id: Int)

val s2 = s.copy(id = 1337)
```

Kotlin's primary-constructor property syntax (`var name: String` right in the constructor) is the most direct,
line-for-line match to Scala's
`class Student(var name: String, var id: Int)` of anything in this guide — same implicit constructor+getter+setter
generation, same syntax shape.

### Traits vs. interfaces

**Scala**

```scala
trait Sized {
  var size: Int = 0
  def isEmpty() = size == 0
}
class Empty extends Sized
```

**Java 25**: interfaces support default methods (since Java 8) but still **cannot hold mutable instance state** —
Scala's `var size: Int = 0` inside a trait has no Java interface equivalent:

```java
interface Sized {
    int size();                        // must be abstract; no field storage

    default boolean isEmpty() {
        return size() == 0;
    }
}

class Empty implements Sized {
    public int size() {
        return 0;
    }
}
```

For default-method behavior *with* real inheritable state, Java pushes you to an **abstract class** instead — but then
you lose multiple inheritance, which is exactly the trade-off Scala traits are designed to avoid.

**Kotlin**: interfaces are closer to Scala traits — they can declare **abstract properties** that implementing classes
back with real state, and provide default method bodies, and are multiply-implementable:

```kotlin
interface Sized {
    val size: Int
    fun isEmpty() = size == 0
}
class Empty : Sized {
    override val size = 0
}
println(Empty().isEmpty()) // true
```

Kotlin still can't give an interface a *field with a backing value* the way a Scala trait's `var size: Int = 0` can
(Kotlin interface properties must be overridden, not initialized, in the interface itself) — so
multiple-inheritance-of-state remains a genuinely Scala-only feature among these three languages. Both Java and Kotlin
approximate it with multiple inheritance of *behavior* only.

---

## Summary table

| Concept                    | Scala                   | Java 25                              | Kotlin                                               |
|----------------------------|-------------------------|--------------------------------------|------------------------------------------------------|
| Entry point                | `object { def main }`   | unnamed class, instance `main()`     | top-level `fun main()`                               |
| String interpolation       | `s"...${x}..."`         | `STR."...\{x}..."`                   | `"...$x..."`                                         |
| Immutable collections      | default                 | `List.of` / `Map.of` (copy-based)    | default (`listOf`, `mapOf`)                          |
| Placeholder lambda param   | `_`                     | none (must name it)                  | `it`                                                 |
| Tuples                     | native, 22-arity        | none — use `record`                  | `Pair`/`Triple` (2–3 only), else `data class`        |
| Optional value             | `Option[T]`             | `Optional<T>`                        | nullable type `T?`                                   |
| Function type literal      | `(T) => R`              | none — named functional interface    | `(T) -> R`                                           |
| Mutable lambda capture     | yes                     | no (effectively-final only)          | yes                                                  |
| Currying syntax            | `def f(x)(y)`           | manual, returns `Function`           | manual, returns `(T) -> R`                           |
| No-boilerplate class       | `class C(var x: Int)`   | `record C(int x)` *(immutable only)* | `class C(var x: Int)` / `data class`                 |
| Traits / multi-inheritance | full (state + behavior) | interfaces: behavior only            | interfaces: behavior + abstract properties, no state |

**Bottom line:** Kotlin tracks Scala more closely on *syntax and closures*
(placeholder lambdas, mutable capture, function-type literals, constructor-property sugar), while Java 25 tracks Scala
more closely on *philosophy* in one place — preferring named `record`s over anonymous tuples, the same way it prefers
explicit sealed types over implicit structural typing.