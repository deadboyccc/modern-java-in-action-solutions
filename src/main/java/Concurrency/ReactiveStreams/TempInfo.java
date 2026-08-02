package Concurrency.ReactiveStreams;

import java.util.Random;

public class TempInfo {

    public static final Random random = new Random();

    private final String town;
    private final int temp;

    public TempInfo(String town, int temp) {
        this.town = town;
        this.temp = temp;
    }

    /**
     * Static factory method to fetch current temperature for a given town.
     * Simulates a temperature reading between 0 and 99 °F.
     * Randomly fails ~1 out of 10 times to simulate real-world fetch errors.
     */
    public static TempInfo fetch(String town) {
        if (random.nextInt(10) == 0) {
            throw new RuntimeException("Error!");
        }
        return new TempInfo(town, random.nextInt(100));
    }

    public String getTown() {
        return town;
    }

    public int getTemp() {
        return temp;
    }

    @Override
    public String toString() {
        return town + " : " + temp;
    }
}