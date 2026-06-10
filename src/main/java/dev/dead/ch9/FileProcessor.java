package dev.dead.ch9;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class FileProcessor {

    public static String processFile(BufferedReaderProcessor p) throws IOException {
        // Look for "hamlet.txt" at the root of src/main/resources
        try (InputStream is = FileProcessor.class.getClassLoader().getResourceAsStream("hamlet.txt")) {

            if (is == null) {
                throw new FileNotFoundException("Could not find hamlet.txt in resources folder");
            }

            // Convert the InputStream into a BufferedReader
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                return p.process(br);
            }
        }
    }

    // 2. Define the Strategy/Behavior Parameterization Method

    // 3. Execute by passing different lambdas
    public static void main(String[] args) throws IOException {
        // Read one line
        String oneLine = processFile(BufferedReader::readLine);
        System.out.println("One Line: " + oneLine);

        // Read two lines
        String twoLines = processFile((BufferedReader b) -> b.readLine() + b.readLine());
        System.out.println("Two Lines: " + twoLines);

        // More stuff
        String str = processFile(Reader::readAllAsString);
        System.out.println("Str: " + str);
    }

    // 1. Define the Functional Interface
    @FunctionalInterface
    public interface BufferedReaderProcessor {
        String process(BufferedReader b) throws IOException;
    }
}