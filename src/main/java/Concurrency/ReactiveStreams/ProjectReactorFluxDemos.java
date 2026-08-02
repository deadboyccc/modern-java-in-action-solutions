package Concurrency.ReactiveStreams;

import reactor.core.publisher.Flux;

import java.time.Duration;

public class ProjectReactorFluxDemos {
    public static void main() throws InterruptedException {
        Flux<Long> interval = Flux.interval(Duration.ofSeconds(2));
        interval.subscribe(System.out::println);
        Thread.sleep(10000);
    }
}
