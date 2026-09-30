package DesignPatterns.ChainOfResponsiblity;

import java.util.Locale;

/**
 * Concrete handler: normalizes text before later handlers receive it.
 */
public class ObjectProcessorAToLowerCase extends ProcessingObject<String> {
    @Override
    protected String handleWork(String input) {
        String result = input.toLowerCase(Locale.ROOT);
        System.out.println("Lowercase handler: " + result);
        return result;
    }
}
