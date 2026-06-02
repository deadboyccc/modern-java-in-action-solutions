package dev.dead.part2.section5.RaceConditionSim;

import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.LongStream;

public class LockBasedAccumulator {
    private static final ReentrantLock lock = new ReentrantLock();

    static void main() {
        for (int i = 0; i < 5; i++) {
            int param = 1_000_000;
            System.out.println("Sum execution " + (i + 1) + ": " + sideEffectSum(param));
        }
    }

    public static long sideEffectSum(long n) {
        Accumulator accumulator = new Accumulator();
        // Parallel stream hammers the shared accumulator state concurrently
        LongStream.rangeClosed(1, n).parallel().forEach(accumulator::add);
        return accumulator.total;
    }

    // Encapsulating state to fit accumulator::add method reference
    public static class Accumulator {
        public long total = 0;

        public void add(long value) {
            lock.lock();
            try {
                this.total += value;
            } finally {
                lock.unlock();
            }
        }
    }
}
