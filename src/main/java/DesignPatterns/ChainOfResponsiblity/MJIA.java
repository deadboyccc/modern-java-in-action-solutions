package DesignPatterns.ChainOfResponsiblity;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

public class MJIA {
    static void main() {
        ProcessingObject<String> p1 = new ObjectProcessorAToLowerCase();
        ProcessingObject<String> p2 = new ObjectProcessorBToHex();
        p1.setSuccessor(p2);


        System.out.println(HexFormat.of().formatHex("HELLO".getBytes(StandardCharsets.UTF_8)));
        var result = p1.handle("HELLO");
        System.out.println(result);


    }
}
