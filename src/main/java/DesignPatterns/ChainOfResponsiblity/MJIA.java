package DesignPatterns.ChainOfResponsiblity;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * Client: wires handlers together and sends one request through the chain.
 */
public class MJIA {
    public static void main(String[] args) {
        ProcessingObject<String> p1 = new ObjectProcessorAToLowerCase();
        ProcessingObject<String> p2 = new ObjectProcessorBToHex();
        p1.setSuccessor(p2);

        String result = p1.handle("HELLO");
        System.out.println("Chain result: " + result);

        // Function composition can express the same linear pipeline compactly.
        System.out.println("_".repeat(20));
        UnaryOperator<String> firstProcessor = s -> s.toLowerCase(Locale.ROOT);
        UnaryOperator<String> secondProcessor = s -> HexFormat.of().formatHex(s.getBytes(StandardCharsets.UTF_8));
        Function<String, String> pipeline = firstProcessor.andThen(secondProcessor);
        System.out.println(pipeline.apply("HELLO"));
    }
}
