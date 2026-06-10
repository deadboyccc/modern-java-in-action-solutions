package dev.dead.ch9;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;


public class ErrorFileParser {
    static void main() throws IOException {
        try (InputStream is = ErrorFileParser.class.getClassLoader().getResourceAsStream("errors.txt")) {
            assert is != null;
            var buffer = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            var errors = buffer.lines()
                    .filter(line -> !line.startsWith("ERROR"))
                    .limit(3)
                    .toList();
            System.out.println("Errors : " + errors);
        }
    }

}
