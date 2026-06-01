package dev.dead.part2.section5;

public class TestSystemUtils {

    // Using standard public static void main(String[] args) for traditional execution,
    // or keep it instance-main if using JDK 21+ unnamed classes.
    public static void main(String[] args) throws InterruptedException {
        Runtime rt = Runtime.getRuntime();

        System.out.println("=== CPU Cores ===");
        System.out.println("Available Processors (Cores): " + rt.availableProcessors());
        System.out.println();

        // 1. Properly Registering a Shutdown Hook
        rt.addShutdownHook(new Thread(() -> {
            System.out.println("\n[Shutdown Hook] JVM is exiting. Cleaning up resources...");
            // Small sleep to simulate resource cleanup during teardown
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {
            }
            System.out.println("[Shutdown Hook] Cleanup complete.");
        }));

        System.out.println("=== Initial Heap Memory Memory ===");
        printMemoryMetrics(rt);

        // 2. Simulating Explicit Heap Allocation
        System.out.println("\nAllocating an array of 3,000,000 Long objects...");
        Long[] longList = new Long[3000000];

        // Note: Just creating the array allocates the array reference space.
        // To truly pressure the heap, we populate it to instantiate the Long objects.
        for (int i = 0; i < longList.length; i += 100) {
            longList[i] = Long.valueOf(i); // allocating wrapper objects intermittently
        }

        System.out.println("\n=== Heap Memory Post-Allocation ===");
        printMemoryMetrics(rt);

        // 3. Requesting Garbage Collection
        System.out.println("\nExplicitly invoking Runtime.getRuntime().gc()...");
        // Dereference to make objects eligible for GC
        longList = null;
        rt.gc();

        // Let the current thread yield briefly to allow GC threads to work
        Thread.sleep(1000);

        System.out.println("\n=== Heap Memory Post-GC ===");
        printMemoryMetrics(rt);

        System.out.println("\nExiting main execution thread.");
    }

    /**
     * Helper to show the distinct states of JVM heap sizing.
     */
    private static void printMemoryMetrics(Runtime rt) {
        long MB = 1024 * 1024;
        // The maximum amount of memory the JVM will attempt to use (-Xmx)
        long maxMem = rt.maxMemory();
        // The total memory currently reserved by the JVM from the OS (-Xms + expansion)
        long totalMem = rt.totalMemory();
        // The amount of free space within that reserved total memory block
        long freeMem = rt.freeMemory();
        // The memory actively holding instantiated objects
        long usedMem = totalMem - freeMem;

        System.out.printf("Max Memory (-Xmx):  %4d MB%n", maxMem / MB);
        System.out.printf("Total Allocated:    %4d MB%n", totalMem / MB);
        System.out.printf("Used Memory:        %4d MB%n", usedMem / MB);
        System.out.printf("Free Pool Space:    %4d MB%n", freeMem / MB);
    }
}
