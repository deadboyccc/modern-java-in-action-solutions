import java.util.*
import java.util.Spliterator.*
import java.util.function.Consumer

class WordCounterSpliterator(private val string: String) : Spliterator<Char> {

    private var currentChar = 0

    override fun tryAdvance(action: Consumer<in Char>?): Boolean {
        // Consume the current character
        action?.accept(string[currentChar++])

        // Return true if there are further characters to be consumed
        return currentChar < string.length
    }

    override fun trySplit(): Spliterator<Char>? {
        val currentSize = string.length - currentChar

        // Returns null to signal that the String is small enough to be processed sequentially
        if (currentSize < 10) return null

        // Try to split the remaining string in half, then look for a whitespace to split cleanly
        for (splitPos in currentSize / 2 + currentChar until string.length) {
            if (string[splitPos].isWhitespace()) {
                // Create a new Spliterator from the current position up to the split position
                val spliterator = WordCounterSpliterator(string.substring(currentChar, splitPos))

                // Advance the current starting position to the split position
                currentChar = splitPos
                return spliterator // Found a space and created the new Spliterator, exit loop
            }
        }
        return null
    }

    override fun estimateSize(): Long = (string.length - currentChar).toLong()

    override fun characteristics(): Int =
        ORDERED or SIZED or SUBSIZED or NONNULL or IMMUTABLE
}
