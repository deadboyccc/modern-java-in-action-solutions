package ApendixB;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

/**
 * Appendix B.5 - Files
 * <p>
 * Important Java 8 stream-producing methods:
 * - Files.lines
 * - Files.list
 * - Files.walk
 * - Files.find
 */
public class B5_FilesStudyGuide {

    static final Path DEMO_DIR =
            Paths.get("appendix-b-demo");

    static void createDemoFiles() throws IOException {
        Files.createDirectories(DEMO_DIR.resolve("subdir"));

        Files.write(
                DEMO_DIR.resolve("a.txt"),
                java.util.Arrays.asList("Java", "Spring", "Streams"));

        Files.write(
                DEMO_DIR.resolve("b.txt"),
                java.util.Arrays.asList("one", "two"));

        Files.write(
                DEMO_DIR.resolve("subdir").resolve("c.txt"),
                java.util.Arrays.asList("nested"));
    }

    // ------------------------------------------------------------
    // Files.lines
    // ------------------------------------------------------------

    static void linesExample() throws IOException {
        System.out.println("\n=== Files.lines ===");

        // Files.lines reads lines lazily as a Stream<String>.
        // The try-with-resources closes the stream and file.
        try (Stream<String> lines =
                     Files.lines(DEMO_DIR.resolve("a.txt"))) {

            lines.forEach(System.out::println);
        }
    }

    // ------------------------------------------------------------
    // Files.list
    // ------------------------------------------------------------

    static void listExample() throws IOException {
        System.out.println("\n=== Files.list ===");

        // Lists the DIRECT entries in a directory.
        // It does not recursively visit subdirectories.
        try (Stream<Path> paths = Files.list(DEMO_DIR)) {
            paths.forEach(path ->
                    System.out.println(path));
        }
    }

    // ------------------------------------------------------------
    // Files.walk
    // ------------------------------------------------------------

    static void walkExample() throws IOException {
        System.out.println("\n=== Files.walk ===");

        // Recursively traverses the directory tree.
        // The appendix notes that traversal is depth-first.
        try (Stream<Path> paths = Files.walk(DEMO_DIR)) {
            paths.forEach(path ->
                    System.out.println(path));
        }

        // Depth can be limited.
        try (Stream<Path> paths = Files.walk(DEMO_DIR, 1)) {
            System.out.println("Depth 1 only:");
            paths.forEach(System.out::println);
        }
    }

    // ------------------------------------------------------------
    // Files.find
    // ------------------------------------------------------------

    static void findExample() throws IOException {
        System.out.println("\n=== Files.find ===");

        // Recursively traverse and keep paths that satisfy the predicate.
        try (Stream<Path> paths = Files.find(
                DEMO_DIR,
                Integer.MAX_VALUE,
                (path, attributes) ->
                        attributes.isRegularFile()
                                && path.toString().endsWith(".txt"))) {

            paths.forEach(path ->
                    System.out.println("Found: " + path));
        }
    }

    static void cleanup() throws IOException {
        // Delete deepest entries first.
        try (Stream<Path> paths = Files.walk(DEMO_DIR)) {
            paths.sorted((a, b) -> b.getNameCount() - a.getNameCount())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        } catch (RuntimeException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException) {
                throw (IOException) cause;
            }
            throw e;
        }
    }

    public static void main(String[] args) throws Exception {
        createDemoFiles();

        try {
            linesExample();
            listExample();
            walkExample();
            findExample();
        } finally {
            cleanup();
        }
    }
}
