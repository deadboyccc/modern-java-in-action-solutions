package DesignPatterns.ChainOfResponsiblity;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public class MJIA {
    static void main() {
        ProcessingObject<String> p1 = new ObjectProcessorAToLowerCase();
        ProcessingObject<String> p2 = new ObjectProcessorBToHex();
        p1.setSuccessor(p2);


        System.out.println(HexFormat.of().formatHex("HELLO".getBytes(StandardCharsets.UTF_8)));
        var result = p1.handle("HELLO");
        System.out.println(result);

        // fun composition
        System.out.println("_".repeat(20));
        UnaryOperator<String> firstProcessor = String::toLowerCase;
        UnaryOperator<String> secondProcessor = s -> HexFormat.of().formatHex(s.getBytes(StandardCharsets.UTF_8));

        Function<String, String> pipeline = firstProcessor.andThen(secondProcessor);
        System.out.println(pipeline.apply("HELLO"));

    }
}
