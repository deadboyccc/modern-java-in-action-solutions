package dev.dead.part2.section5.RaceConditionSim;

import java.util.stream.LongStream;

public class Accumulator {

    public long total = 0;

    static void main() {
        for (int i = 0; i < 5; i++) {
            int param = 1_000_000;
            System.out.println(
                    sideEffectSum(param));

        }

    }

    public static long sideEffectSum(long n) {
        Accumulator accumulator = new Accumulator();
        LongStream.rangeClosed(1, n).parallel().forEach(accumulator::add);
        return accumulator.total;
    }

    public void add(long value) {

        total += value;
    }
}
