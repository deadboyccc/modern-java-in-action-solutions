package Spliterator;
import java.util.Spliterator;
import java.util.function.Consumer;

public class WordCounterSpliterator implements Spliterator<Character> {

    private final String string;
    private int currentChar = 0;

    public WordCounterSpliterator(String string) {
        this.string = string;
    }

    @Override
    public boolean tryAdvance(Consumer<? super Character> action) {
        // Consume the current character
        action.accept(string.charAt(currentChar++));
        // Return true if there are further characters to be consumed
        return currentChar < string.length();
    }

    @Override
    public Spliterator<Character> trySplit() {
        int currentSize = string.length() - currentChar;

        // Returns null to signal that the String is small enough to be processed sequentially
        if (currentSize < 10) {
            return null;
        }

        // Try to split the remaining string in half, then look for a whitespace to split cleanly
        for (int splitPos = currentSize / 2 + currentChar; splitPos < string.length(); splitPos++) {
            if (Character.isWhitespace(string.charAt(splitPos))) {
                // Create a new Spliterator from the current position up to the split position
                Spliterator<Character> spliterator =
                        new WordCounterSpliterator(string.substring(currentChar, splitPos));

                // Advance the current starting position to the split position
                currentChar = splitPos;
                return spliterator; // Found a space and created the new Spliterator, exit loop
            }
        }
        return null;
    }

    @Override
    public long estimateSize() {
        return string.length() - currentChar;
    }

    @Override
    public int characteristics() {
        return ORDERED | SIZED | SUBSIZED | NONNULL | IMMUTABLE;
    }
}
