package DesignPatterns.Singleton;

import java.net.URI;
import java.net.URL;

public class Main {
    public static void main(String[] args) throws Exception {
        URL url = URI.create("https://httpbin.org/delay/1").toURL();

        RateLimiterPolicyVerifier.bombard(url, 10);
        Thread.sleep(10_000);  // was 3000, now 5000
        RateLimiterPolicyVerifier.printStats();
    }
}
