package FP.ch20;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Map.entry;

/**
 * Java 25 equivalents of every construct from the Scala comparison chapter.
 * Run with: java ScalaComparisonDemo.java
 */
public class ScalaComparisonDemo {

    // =========================================================================
    // 4. DOMAIN TYPES USED BELOW
    // =========================================================================

    // Takes a plain (nullable) Person rather than Optional<Person> as a parameter —
    // using Optional as a parameter type is a well-known Java anti-pattern
    // (Effective Java, Item 55); Optional.ofNullable wraps it internally instead.
    static String getCarInsuranceName(Person person, int minAge) {
        return Optional.ofNullable(person)
                .filter(p -> p.age() >= minAge)
                .flatMap(Person::car)
                .flatMap(Car::insurance)
                .map(Insurance::name)
                .orElse("Unknown");
    }

    static boolean isJavaMentioned(String tweet) {
        return tweet.contains("Java");
    }

    // JDK 25 finalized JEP 512 (Compact Source Files and Instance Main Methods):
    // an instance main() — no `public`, no `static`, no `String[] args` — is now
    // a fully supported program entry point, not a preview feature.
    void main() {

        System.out.println("=== 1. HELLO WORLD ===");
        // imperative style
        int n = 2;
        while (n <= 6) {
            System.out.println("Hello " + n + " bottles of beer");
            n++;
        }
        // functional style
        IntStream.rangeClosed(2, 6)
                .forEach(m -> System.out.println("Hello " + m + " bottles of beer"));

        System.out.println("\n=== 2. COLLECTIONS ===");
        var authorsToAge = Map.ofEntries(
                entry("Raoul", 23), entry("Mario", 40), entry("Alan", 53));
        System.out.println("authorsToAge = " + authorsToAge);

        var authors = List.of("Raoul", "Mario", "Alan");
        var numbers = Set.of(1, 2, 3, 5, 8); // Set.of throws on duplicates, unlike Scala
        System.out.println("authors = " + authors);
        System.out.println("numbers = " + numbers);

        // immutable "add" — no persistent-structure method, so rebuild via stream
        Set<Integer> smallSet = Set.of(2, 5, 3);
        Set<Integer> withEight = Stream.concat(smallSet.stream(), Stream.of(8))
                .collect(Collectors.toUnmodifiableSet());
        System.out.println("original set untouched: " + smallSet);
        System.out.println("new set with 8 added:   " + withEight);

        // filter + map pipeline
        var fileLines = List.of("short", "this is a long enough line", "also long enough line");
        var linesLongUpper = fileLines.stream()
                .filter(l -> l.length() > 10)
                .map(String::toUpperCase)
                .toList();
        System.out.println("linesLongUpper = " + linesLongUpper);

        System.out.println("\n=== 3. TUPLES (via records) ===");
        var raoul = new Pair<>("Raoul", "+44 7700 700042");
        System.out.println("raoul = " + raoul.x() + ", " + raoul.y());

        var book = new Book(2018, "Modern Java in Action", "Manning");
        System.out.println("book.year() = " + book.year());

        System.out.println("\n=== 4. OPTIONAL ===");
        var person = new Person("Raoul", 30, Optional.of(new Car(Optional.of(new Insurance("Acme")))));
        System.out.println("insurance name = " + getCarInsuranceName(person, 18));
        System.out.println("insurance name (too young) = " + getCarInsuranceName(person, 40));

        System.out.println("\n=== 5. FIRST-CLASS FUNCTIONS ===");
        var tweets = List.of(
                "I love the new features in Java",
                "How's it going?",
                "An SQL query walks into a bar");
        tweets.stream().filter(ScalaComparisonDemo::isJavaMentioned).forEach(System.out::println);

        System.out.println("\n=== 6. ANONYMOUS FUNCTIONS ===");
        Function<String, Boolean> isLongTweet = tweet -> tweet.length() > 60;
        var sampleTweets = List.of(
                "A very short tweet",
                "This tweet was intentionally written to run well past sixty characters in length");
        sampleTweets.forEach(t -> System.out.println(isLongTweet.apply(t) + " <- " + t));

        System.out.println("\n=== 7. CLOSURES (mutable capture workaround) ===");
        // Java lambdas can only capture effectively-final locals, so mutation
        // requires a heap-allocated holder — unlike Scala, which captures the variable itself.
        int[] count = {0};
        Runnable inc = () -> count[0]++;
        inc.run();
        inc.run();
        System.out.println("count = " + count[0]); // 2

        System.out.println("\n=== 8. CURRYING ===");
        Function<Integer, Function<Integer, Integer>> multiplyCurry = x -> y -> x * y;
        int r = multiplyCurry.apply(2).apply(10);
        System.out.println("multiplyCurry(2)(10) = " + r);
        Function<Integer, Integer> multiplyByTwo = multiplyCurry.apply(2);
        Stream.of(1, 3, 5, 7).map(multiplyByTwo).forEach(System.out::println);

        System.out.println("\n=== 9. BOILERPLATE-FREE CLASSES ===");
        var student = new Student("Raoul", 1);
        student.id = 1337; // mutable, matching Scala's `var` fields
        System.out.println("student.id = " + student.id);

        var immutableStudent = new ImmutableStudent("Raoul", 1);
        var updated = new ImmutableStudent(immutableStudent.name(), 1337); // "mutation" = new instance
        System.out.println("updated.id() = " + updated.id());

        System.out.println("\n=== 10. TRAITS vs INTERFACES ===");
        System.out.println("new Empty().isEmpty() = " + new Empty().isEmpty());
    }

    // Traits vs interfaces: default methods, but no mutable instance state.
    interface Sized {
        int size();

        default boolean isEmpty() {
            return size() == 0;
        }
    }

    record Person(String name, int age, Optional<Car> car) {
    }

    record Car(Optional<Insurance> insurance) {
    }

    record Insurance(String name) {
    }

    // Tuple replacement (Java has no tuple literal) — a generic Pair record
    record Pair<X, Y>(X x, Y y) {
    }

    record Book(int year, String title, String publisher) {
    }

    // =========================================================================
    // MAIN
    // =========================================================================

    // "Boilerplate-free class" — Scala's `class Student(var name, var id)` was
    // MUTABLE, so the honest Java mapping is a plain mutable class, not a record.
    static class Student {
        String name;
        int id;

        Student(String name, int id) {
            this.name = name;
            this.id = id;
        }
    }

    // Immutable version, where a record IS the right mapping.
    record ImmutableStudent(String name, int id) {
    }

    static class Empty implements Sized {
        public int size() {
            return 0;
        }
    }
}