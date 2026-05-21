package dev.dead.part2.section5;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;

public class Main {
    static void main() {
        Trader raoul = new Trader("Raoul", "Cambridge");
        Trader mario = new Trader("Mario", "Milan");
        Trader alan = new Trader("Alan", "Cambridge");
        Trader brian = new Trader("Brian", "Cambridge");
        List<Transaction> transactions = Arrays.asList(
                new Transaction(brian, 2011, 300),
                new Transaction(raoul, 2012, 1000),
                new Transaction(raoul, 2011, 400),
                new Transaction(mario, 2012, 710),
                new Transaction(mario, 2012, 700),
                new Transaction(alan, 2012, 950)
        );

        // 1. Find all transactions in the year 2011 and sort them by value (small to high).
        var transactionsSortedValue = transactions
                .stream()
                .filter(transaction -> transaction.getYear() == 2011)
                .sorted(comparing(Transaction::getValue))
                .toList();

        // 2. What are all the unique cities where the traders work?
        var uniqueCities = transactions.stream()
                .map(Transaction::getTrader)
                .map(Trader::getCity)
                .distinct()
                .toList();

        // 3. Finds all traders from Cambridge and sort them by name
        var cambridgeTradersSortedByName = transactions.stream()
                .map(Transaction::getTrader)
                .filter(trader -> trader.getCity().equals("Cambridge"))
                .distinct()
                .sorted(comparing(Trader::getName))
                .toList();

        // 4. Returns a string of all traders’ names sorted alphabetically
        var allTradersNamesSortedAlphabetically = transactions.stream()
                .map(transaction -> transaction.getTrader().getName())
                .distinct()
                .sorted(comparing(String::toLowerCase))
                .reduce("", (curr, acc) -> acc + curr);
        // -- more efficient using joining()
        var joiningString = transactions.stream()
                .map(transaction -> transaction.getTrader().getName())
                .distinct()
                .sorted(comparing(String::toLowerCase))
                .collect(Collectors.joining(", ")); // internally uses StringBuilder

        // 5. Are any traders based in Milan?
        var anyMilan = transactions.stream()
                .map(transaction -> transaction.getTrader().getName())
                .anyMatch(name -> name.equals("Milan"));

        // 6. Prints all transactions’ values from the traders living in Cambridge
        transactions.stream()
                .filter(transaction -> transaction.getTrader().getCity().equals("Cambridge"))
                .map(Transaction::getValue)
                .forEach(IO::println);

        // 7. What’s the highest value of all the transactions?
        var highestValueTrans = transactions.stream()
                .max(comparing(Transaction::getValue));

        var hVTransUsingReduce = transactions.stream()
                .map(Transaction::getValue)
                .reduce(Integer::max);
        //      .reduce(Int.Min , (a,b)-> a>b?a:b)

        //8. Finds the transaction with the smallest value
        var lowestValueTrans = transactions.stream()
                .min(comparing(Transaction::getValue));

        // stream of words to unique characters using flatMap
        var uniqueCharas = Stream.of("Hello", "World")
                // each one is a string[] -> stream<string[]>
                .map(s -> s.split(""))
                // each String[] -> stream<String> -> Stream<Stream<String>>> -> flattened
                .flatMap(Arrays::stream)
                .distinct()
                .collect(Collectors.joining());
        String[] arrayOfWords = {"Goodbye", "World"}; // list<String> -> Stream<String>
        // turn the string array to stream of strings
        Stream<String> streamOfWords = Arrays.stream(arrayOfWords);

        List<Integer> numbers1 = Arrays.asList(1, 2, 3);
        List<Integer> numbers2 = Arrays.asList(3, 4);
        List<int[]> pairs =
                numbers1.stream()
                        .flatMap(i -> numbers2.stream()
                                .map(j -> new int[]{i, j})
                        )
                        .toList();

    }


}
