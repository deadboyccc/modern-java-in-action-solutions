package Spliterator;

import java.util.stream.IntStream;

public class IterativeApproachCountWords {
    static void main() {

        final String SENTENCE =
                " Nel mezzo del cammin di nostra vita " +
                        "mi ritrovai in una selva oscura" +
                        " ché la dritta via era smarrita ";

        System.out.println("Found " + countWordsIteratively(SENTENCE) + " words");
        // doing it in stream api
        var count = IntStream.range(0, SENTENCE.length()).mapToObj(SENTENCE::charAt);
        WordCounter wordCounter = count.reduce(
                new WordCounter(0, true), // Identity / Initial State
                WordCounter::accumulate,  // Accumulator
                WordCounter::combine      // Combiner (for parallel streams)
        );
        var countParallel = IntStream.range(0, SENTENCE.length()).mapToObj(SENTENCE::charAt).parallel()
                .reduce(new WordCounter(0, true), WordCounter::accumulate, WordCounter::combine);

        System.out.println("Sequential Word count: " + wordCounter.getCounter());
        System.out.println("Parallel Word count: " + countParallel.getCounter());


    }

    private static int countWordsIteratively(String s) {
        char[] charArray = s.toCharArray();
        var lastSpace = Boolean.valueOf(false);
        var count = 0;

        for (char c : charArray) {
            // if the character is whitespace = set the flat to true
            if (Character.isWhitespace(c)) {
                lastSpace = true;
            } else {
                // if the current charactor is not a whitespace check (is the last character is whitespace ) ?
                // then count++
                // then finally set flag to false
                if (lastSpace) count++;
                lastSpace = false;
            }

        }
        return count;

    }
}

class WordCounter {
    private final int counter;
    private final boolean lastSpace;

    public WordCounter(int counter, boolean lastSpace) {
        this.counter = counter;
        this.lastSpace = lastSpace;
    }

    /**
     * Accumulates characters one by one.
     * Increases the counter when the current character is NOT a space,
     * but the previous character WAS a space.
     */
    public WordCounter accumulate(Character c) {
        if (Character.isWhitespace(c)) {
            return lastSpace ? this : new WordCounter(counter, true);
        } else {
            return lastSpace ? new WordCounter(counter + 1, false) : this;
        }
    }

    /**
     * Combines two WordCounters by summing their counters.
     * Used during parallel stream reduction.
     */
    public WordCounter combine(WordCounter wordCounter) {
        return new WordCounter(this.counter + wordCounter.counter, wordCounter.lastSpace);
    }

    public int getCounter() {
        return counter;
    }
}