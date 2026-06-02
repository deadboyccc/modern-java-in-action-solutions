import java.util.Spliterator
import java.util.function.Consumer

fun main() {
    println("Hello World!")
}
class Spliterator : Spliterator<String> {

    override fun tryAdvance(action: Consumer<in String>?): Boolean {
        TODO("Not yet implemented")
    }

    override fun trySplit(): Spliterator<String?>? {
        TODO("Not yet implemented")
    }

    override fun estimateSize(): Long {
        TODO("Not yet implemented")
    }

    override fun characteristics(): Int {
        TODO("Not yet implemented")
    }

}