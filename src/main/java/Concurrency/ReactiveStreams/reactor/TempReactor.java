package Concurrency.ReactiveStreams.reactor;

import Concurrency.ReactiveStreams.TempInfo;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Arrays;

public class TempReactor {

    /**
     * Emits a temperature report for a given town once per second, up to 5 times.
     */
    public static Flux<TempInfo> getTemperature(String town) {
        return Flux.interval(Duration.ofSeconds(1))
                .take(5) // Emits 5 items then sends onComplete automatically
                .map(_ -> TempInfo.fetch(town));
    }

    /**
     * Maps Fahrenheit temperatures to Celsius using .map()
     */
    public static Flux<TempInfo> getCelsiusTemperature(String town) {
        return getTemperature(town)
                .map(temp -> new TempInfo(
                        temp.getTown(),
                        (temp.getTemp() - 32) * 5 / 9
                ));
    }

    /**
     * Quiz 17.2: Filters only negative temperatures in Celsius
     */
    public static Flux<TempInfo> getNegativeTemperature(String town) {
        return getCelsiusTemperature(town)
                .filter(temp -> temp.getTemp() < 0);
    }

    /**
     * Listing 17.16: Merges streams for multiple towns into a single combined Flux
     */
    public static Flux<TempInfo> getCelsiusTemperatures(String... towns) {
        return Flux.merge(
                Arrays.stream(towns)
                        .map(TempReactor::getCelsiusTemperature)
                        .toList()
        );
    }
}