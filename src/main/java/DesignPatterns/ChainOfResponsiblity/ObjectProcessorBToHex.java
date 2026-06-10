package DesignPatterns.ChainOfResponsiblity;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

public class ObjectProcessorBToHex extends ProcessingObject<String> {
    @Override
    protected String handleWork(String input) {
        var res = HexFormat.of().formatHex(input.getBytes(StandardCharsets.UTF_8));
        System.out.println("In Processor : " + res);
        return res;
    }
}
