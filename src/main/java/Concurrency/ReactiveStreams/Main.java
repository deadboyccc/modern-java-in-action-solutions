package Concurrency.ReactiveStreams;

import java.util.concurrent.Flow.Publisher;

public class Main {

    public static void main(String[] args) {
        // Subscribe to the stream of Celsius temperatures for New York
        getCelsiusTemperatures("New York").subscribe(new TempSubscriber());
    }

    /**
     * Creates a Publisher that yields Celsius temperatures by chaining
     * the raw publisher to a TempProcessor instance.
     */
    public static Publisher<TempInfo> getCelsiusTemperatures(String town) {
        return subscriber -> {
            TempProcessor processor = new TempProcessor();
            processor.subscribe(subscriber);
            // Connect raw stream (Fahrenheit) to the processor (acting as subscriber)
            getTemperatures(town).subscribe(processor);
        };
    }

    /**
     * Raw publisher producing Fahrenheit temperatures.
     */
    private static Publisher<TempInfo> getTemperatures(String town) {
        return subscriber -> subscriber.onSubscribe(new TempSubscription(subscriber, town));
    }
}