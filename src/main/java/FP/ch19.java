package FP;

import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;
import java.util.function.ToIntFunction;

public class ch19 {
    public static void main(String[] args) {
        Function<String, Integer> strToInt = Integer::parseInt;
        ToIntFunction<Integer> toInt = Integer::intValue;

        // Currying example
        DoubleUnaryOperator convertCtoF = curriedConverter(9.0 / 5, 32);
        double v = convertCtoF.applyAsDouble(0);
        System.out.println("0°C = " + v + "°F");

        TrainJourney j1 = new TrainJourney(100, null);
        TrainJourney j2 = new TrainJourney(200, null);

        // Pure/Functional append: leaves original j1 and j2 unchanged
        TrainJourney j3 = TrainJourney.append(j1, j2);

        System.out.println("j1 price: " + j1.price + ", onward: " + (j1.onward != null ? j1.onward.price : "null"));
        System.out.println("j2 price: " + j2.price + ", onward: " + (j2.onward != null ? j2.onward.price : "null"));
        System.out.println("j3 price: " + j3.price + ", onward: " + (j3.onward != null ? j3.onward.price : "null"));

        System.out.println("_".repeat(20));

        // Print entire journey chain safely
        TrainJourney curr = j3;
        while (curr != null) {
            String onwardStr = (curr.onward != null) ? String.valueOf(curr.onward.price) : "null";
            System.out.println("Journey leg price: " + curr.price + ", onward: " + onwardStr);
            curr = curr.onward;
        }
    }

    static DoubleUnaryOperator curriedConverter(double f, double b) {
        return (double x) -> x * f + b;
    }
}

class TrainJourney {
    public int price;
    public TrainJourney onward;

    public TrainJourney(int price, TrainJourney onward) {
        this.price = price;
        this.onward = onward;
    }

    // Pure / Functional implementation: creates new objects to preserve persistent data structure
    public static TrainJourney append(TrainJourney a, TrainJourney b) {
        return a == null ? b : new TrainJourney(a.price, append(a.onward, b));
    }

    // Impure / Destructive implementation: mutates the original structure
    public static TrainJourney link(TrainJourney a, TrainJourney b) {
        if (a == null) return b;
        TrainJourney t = a;
        while (t.onward != null) {
            t = t.onward;
        }
        t.onward = b;
        return a;
    }
}