package ApendixB;

import java.util.Arrays;
import java.util.List;

/**
 * Appendix B.7 - String
 * <p>
 * Java 8 adds the convenient static String.join method.
 */
public class B7_StringStudyGuide {

    static void joinExamples() {
        System.out.println("\n=== String.join ===");

        // Join several CharSequence values using a delimiter.
        String authors =
                String.join(", ", "Raoul", "Mario", "Alan");

        System.out.println(authors);

        // The delimiter is inserted BETWEEN values.
        String path =
                String.join("/", "home", "ahmed", "projects");

        System.out.println(path);

        // String.join also accepts an Iterable of CharSequence values.
        List<String> names =
                Arrays.asList("Java", "Kotlin", "Spring");

        String technologies =
                String.join(" | ", names);

        System.out.println(technologies);

        // Empty collections produce an empty string.
        String empty = String.join(", ", Arrays.asList());

        System.out.println("empty result: [" + empty + "]");
    }

    public static void main(String[] args) {
        joinExamples();
    }
}
