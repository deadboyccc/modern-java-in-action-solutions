package Concurrency.ReactiveStreams;

import java.util.concurrent.Flow.Processor;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;

public class TempProcessor implements Processor<TempInfo, TempInfo> {

    // processor has-a subscriber
    private Subscriber<? super TempInfo> subscriber;

    @Override
    public void subscribe(Subscriber<? super TempInfo> subscriber) {
        this.subscriber = subscriber;
    }

    @Override
    public void onNext(TempInfo temp) {
        // Convert Fahrenheit to Celsius and pass the new TempInfo downstream
        int celsius = (temp.getTemp() - 32) * 5 / 9;
        subscriber.onNext(new TempInfo(temp.getTown(), celsius));
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        // Forward the subscription signal directly downstream
        subscriber.onSubscribe(subscription);
    }

    @Override
    public void onError(Throwable throwable) {
        // Forward error signals downstream
        subscriber.onError(throwable);
    }

    @Override
    public void onComplete() {
        // Forward completion signals downstream
        subscriber.onComplete();
    }
}