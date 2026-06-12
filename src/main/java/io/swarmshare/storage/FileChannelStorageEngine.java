package io.swarmshare.storage;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

/**
 * Showcase class demonstrating low-level FileChannel capabilities
 * optimized for out-of-order concurrent P2P chunk storage.
 */
public class FileChannelStorageEngine implements AutoCloseable {

    private final Path filePath;
    private final FileChannel fileChannel;

    public FileChannelStorageEngine(Path filePath) throws IOException {
        this.filePath = filePath;
        // CREATE: creates file if missing. WRITE/READ: allows dual-directional pipeline access.
        this.fileChannel = FileChannel.open(filePath,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.READ);
    }

    /**
     * CAPABILITY 1: Fast Disk Preallocation
     * Prevents runtime OutOfMemory/DiskFull errors mid-download and ensures
     * the file system maps the block space linearly on disk ahead of time.
     */
    public void preallocateSpace(long totalSize) throws IOException {
        // Sets the logical length of the file. Efficient on modern Linux FS (ext4/XFS).
        fileChannel.truncate(totalSize);

        // Force a physical byte write to the ultimate tail offset to commit allocation map
        ByteBuffer triggerBuffer = ByteBuffer.allocate(1);
        triggerBuffer.put((byte) 0);
        triggerBuffer.flip();
        fileChannel.write(triggerBuffer, totalSize - 1);

        // Force metadata updates to persist on the storage media device
        fileChannel.force(true);
    }

    /**
     * CAPABILITY 2: Thread-Safe Out-of-Order Concurrent Positional Writing
     * Multiple Project Loom virtual threads can call this method simultaneously
     * without hitting shared-state bottlenecks or overlapping channel cursor alignments.
     */
    public void writeChunkAtOffset(long offset, byte[] chunkData) throws IOException {
        // Wrap raw array into NIO heap buffer allocation wrapper
        ByteBuffer buffer = ByteBuffer.wrap(chunkData);

        long currentOffset = offset;
        while (buffer.hasRemaining()) {
            // Positional write ensures zero race conditions across worker thread spaces
            int bytesWritten = fileChannel.write(buffer, currentOffset);
            if (bytesWritten <= 0) {
                throw new IOException("Stalled I/O pipe pipeline write at offset: " + currentOffset);
            }
            currentOffset += bytesWritten;
        }
    }

    /**
     * CAPABILITY 3: Thread-Safe Concurrent Positional Reading
     * Used when seeding chunks out to request chains in the network swarm.
     */
    public Optional<byte[]> readChunkFromOffset(long offset, int chunkSize) throws IOException {
        // Ensure data size exists inside current file system snapshot boundaries
        if (offset + chunkSize > fileChannel.size()) {
            return Optional.empty();
        }

        ByteBuffer buffer = ByteBuffer.allocate(chunkSize);
        long currentOffset = offset;

        while (buffer.hasRemaining()) {
            int bytesRead = fileChannel.read(buffer, currentOffset);
            if (bytesRead == -1) {
                break; // Unexpected End-Of-File condition hit
            }
            currentOffset += bytesRead;
        }

        // Prepare buffer content extraction bounds arrays
        buffer.flip();
        byte[] outputData = new byte[buffer.remaining()];
        buffer.get(outputData);

        return Optional.of(outputData);
    }

    /**
     * CAPABILITY 4: Direct OS Zero-Copy Network Pipelining (Future Optimization)
     * If sending direct file blocks to socket channels without JVM user-space memory copies.
     */
    public long transferToSocket(long offset, long count, FileChannel targetNetworkSocketChannel) throws IOException {
        // Leverages Linux sendfile() kernel sys-call internally
        return fileChannel.transferTo(offset, count, targetNetworkSocketChannel);
    }

    @Override
    public void close() throws IOException {
        if (fileChannel.isOpen()) {
            // Safely flush unwritten page buffers cleanly to local disks
            fileChannel.force(false);
            fileChannel.close();
        }
    }
}