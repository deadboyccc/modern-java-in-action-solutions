package dev.dead.part2.section5;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.*;

public class Main {
    static void main() {

        Trader raoul = new Trader("Raoul", "Cambridge");
        Trader mario = new Trader("Mario", "Milan");
        Trader alan = new Trader("Alan", "Cambridge");
        Trader brian = new Trader("Brian", "Cambridge");
        List<Transaction> transactions = Arrays.asList(new Transaction(brian, 2011, 300), new Transaction(raoul, 2012, 1000), new Transaction(raoul, 2011, 400), new Transaction(mario, 2012, 710), new Transaction(mario, 2012, 700), new Transaction(alan, 2012, 950));

        // 1. Find all transactions in the year 2011 and sort them by value (small to high).
        var transactionsSortedValue = transactions.stream().filter(transaction -> transaction.getYear() == 2011).sorted(comparing(Transaction::getValue)).toList();

        // 2. What are all the unique cities where the traders work?
        var uniqueCities = transactions.stream().map(Transaction::getTrader).map(Trader::getCity).distinct().toList();

        // 3. Finds all traders from Cambridge and sort them by name
        var cambridgeTradersSortedByName = transactions.stream().map(Transaction::getTrader).filter(trader -> trader.getCity().equals("Cambridge")).distinct().sorted(comparing(Trader::getName)).toList();

        // 4. Returns a string of all traders’ names sorted alphabetically
        var allTradersNamesSortedAlphabetically = transactions.stream().map(transaction -> transaction.getTrader().getName()).distinct().sorted(comparing(String::toLowerCase)).reduce("", (curr, acc) -> acc + curr);
        // -- more efficient using joining()
        var joiningString = transactions.stream().map(transaction -> transaction.getTrader().getName()).distinct().sorted(comparing(String::toLowerCase)).collect(Collectors.joining(", ")); // internally uses StringBuilder

        // 5. Are any traders based in Milan?
        var anyMilan = transactions.stream().map(transaction -> transaction.getTrader().getName()).anyMatch(name -> name.equals("Milan"));

        // 6. Prints all transactions’ values from the traders living in Cambridge
        transactions.stream().filter(transaction -> transaction.getTrader().getCity().equals("Cambridge")).map(Transaction::getValue).forEach(IO::println);

        // 7. What’s the highest value of all the transactions?
        var highestValueTrans = transactions.stream().max(comparing(Transaction::getValue));

        var hVTransUsingReduce = transactions.stream().map(Transaction::getValue).reduce(Integer::max);
        //      .reduce(Int.Min , (a,b)-> a>b?a:b)

        //8. Finds the transaction with the smallest value
        var lowestValueTrans = transactions.stream().min(comparing(Transaction::getValue));

        // stream of words to unique characters using flatMap
        var uniqueCharas = Stream.of("Hello", "World")
                // each one is a string[] -> stream<string[]>
                .map(s -> s.split(""))
                // each String[] -> stream<String> -> Stream<Stream<String>>> -> flattened
                .flatMap(Arrays::stream).distinct().collect(Collectors.joining());
        String[] arrayOfWords = {"Goodbye", "World"}; // list<String> -> Stream<String>
        // turn the string array to stream of strings
        Stream<String> streamOfWords = Arrays.stream(arrayOfWords);

        List<Integer> numbers1 = Arrays.asList(1, 2, 3);
        List<Integer> numbers2 = Arrays.asList(3, 4);
        List<int[]> pairs = numbers1.stream().flatMap(i -> numbers2.stream().map(j -> new int[]{i, j})).toList();

        // Numeric Streams
        // sum all transactions using primitive .sum()
        var sum = transactions.stream().mapToInt(Transaction::getValue).sum();

        // converting from numeric to boxed
        IntStream intStream = transactions.stream().mapToInt(Transaction::getValue);
        Stream<Integer> boxedStream = intStream.boxed();

        // OptionalInt
        int max = transactions.stream().mapToInt(Transaction::getValue).max().orElse(0);
        // ranges ( closed = inclusive and easier to read)
        var count = IntStream.rangeClosed(1, 100).filter(num -> num % 2 == 0).count();
        System.out.println("Count: " + count);

        // generate Pythagorean triplets
        Stream<Triple<Integer, Integer, Double>> pythagoreanTriples = IntStream.rangeClosed(1, 100)
                .boxed()
                .flatMap(a -> IntStream.rangeClosed(a, 100)
                        .mapToObj(b -> new Triple<>(a, b, Math.sqrt(a * a + b * b)))
                        .filter(triple -> triple.third() % 1 == 0)
                );
        // building streams of nullables
        var values =
                Stream.of("config", "home", "user")
                        .flatMap(key -> Stream.ofNullable(System.getProperty(key)));


        // 1. Get the stream
        InputStream is = Main.class.getResourceAsStream("/hamlet.txt");

        // 2. The cleanest modern pipe to get a Stream<String>
        assert is != null;
        try (Stream<String> lines = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)).lines()) {
            long uniqueWords = lines
                    .flatMap(line -> Stream.of(line.split(" ")))
                    .distinct().count();
            System.out.println("UniqueWords: " + uniqueWords);
        }
        // generate fib pairs with iterate
        // 0, 1, 1, 2, 3,
        var firstPair = new Pair<>(0, 1);
        Stream.iterate(firstPair, prevPair -> new Pair<Integer, Integer>(prevPair.second(), prevPair.first() + prevPair.second()))
                .limit(10)
                .forEach(IO::println);
        // tinkering with generate
        Stream.generate(Math::random)
                .limit(5)
                .forEach(System.out::println);
        List<Integer> listOfTens = Stream.generate(() -> 10).limit(10).toList();
        System.out.println("_".repeat(10));
        Path path = Path.of(Main.class.getResource("/hamlet.txt").getPath());
        try (Stream<String> lines = Files.lines(path)) {
            lines.flatMap(line -> Stream.of(line.split(" ")))
                    .forEach(IO::print);

        } catch (IOException e) {
            e.printStackTrace();
        }
        // grouping transactions by trader
        var transactionsGroupedByCurrency = transactions.stream()
                .collect(groupingBy(Transaction::getTrader));

        // count transactions using collectors
        var countOfTransactions = transactions.stream()
                .collect(counting());

        // finding min and max transaction by collectors
        var maxTransByCollector = transactions.stream()
                .collect(maxBy(comparing(Transaction::getValue)));

        var minTransByCollector = transactions.stream()
                .collect(minBy(comparing(Transaction::getValue)));

        // summingInt, averageInt , summarizingInt
        var summaryInt = transactions.stream()
                .collect(summarizingInt(Transaction::getValue));

        // joiningBy traders' names
        var simpleJoining = transactions.stream()
                .map(transaction -> transaction.getTrader().getName())
                .collect(joining(","));
        var joiningByNames = transactions.stream()
                .collect(mapping(transaction -> transaction.getTrader().getName(), Collectors.joining(" ,")));

        // total value using collectors
        var totalValueCollectors = transactions.stream()
                .collect(reducing(0, Transaction::getValue, Integer::sum));

        // max by using reducing
        var maxValueTrans = transactions.stream()
                .collect(reducing((t1, t2) -> t1.getValue() > t2.getValue() ? t1 : t2));


        // nested collectors
        var nestedCollectors = transactions.stream()
                // it's implicit to list
                .collect(groupingBy(Transaction::getTrader,
                        // the land of parentheses
                        filtering(transaction -> transaction.getValue() > 200, toList())));

        // var nested collectors : groupingBy then mapping the values
        var nestedCollectorsMapping = transactions.stream()
                .collect(groupingBy(Transaction::getTrader,
                        mapping(Transaction::getYear
                                , toSet())));


        // var flatMapping

        var nestedFlatMapping = transactions.stream()
                // stream per trader -> each stream is mapped to a stream of (year,value)-> flattened, then collected to list as value to keys
                .collect(groupingBy(Transaction::getTrader,
                        flatMapping(t -> Stream.of(t.getYear(), t.getValue()), toList())));


    }

}
