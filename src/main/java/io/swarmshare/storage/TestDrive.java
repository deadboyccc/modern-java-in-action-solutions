package io.swarmshare.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class TestDrive {
    static void main() throws IOException {
        try (var fileChannelStorageEngine = new FileChannelStorageEngine(Path.of("test.txt"))) {
            // size is byte count
            fileChannelStorageEngine.preallocateSpace(1024);
            var str = "Hello World!";
            fileChannelStorageEngine.writeChunkAtOffset(100, str.getBytes(StandardCharsets.UTF_8));
            Optional<byte[]> bytes = fileChannelStorageEngine.readChunkFromOffset(100, str.getBytes().length);
            var res = new String(bytes.get(), StandardCharsets.UTF_8);
            System.out.println("read: " + res);
        }

        var file = Files.size(Path.of("test.txt"));
        System.out.println("Preallocated Space: " + file);
    }
}
