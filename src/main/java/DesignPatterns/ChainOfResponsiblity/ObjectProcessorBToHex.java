package DesignPatterns.ChainOfResponsiblity;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/**
 * Concrete handler: encodes the text it receives as UTF-8 hexadecimal.
 */
public class ObjectProcessorBToHex extends ProcessingObject<String> {
    @Override
    protected String handleWork(String input) {
        String result = HexFormat.of().formatHex(input.getBytes(StandardCharsets.UTF_8));
        System.out.println("Hex handler: " + result);
        return result;
    }
}
