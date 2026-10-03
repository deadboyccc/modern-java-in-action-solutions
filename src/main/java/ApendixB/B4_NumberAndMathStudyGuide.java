package ApendixB;

import java.math.BigInteger;

/**
 * Appendix B.4 - Number and Math
 * <p>
 * Study guide for the Number-wrapper and Math additions described
 * in the appendix.
 */
public class B4_NumberAndMathStudyGuide {

    // ------------------------------------------------------------
    // sum / min / max
    // ------------------------------------------------------------

    static void sumMinMaxExamples() {
        System.out.println("\n=== sum / min / max ===");

        System.out.println("Integer.sum: " + Integer.sum(10, 20));
        System.out.println("Integer.min: " + Integer.min(10, 20));
        System.out.println("Integer.max: " + Integer.max(10, 20));

        System.out.println("Long.sum: " + Long.sum(10L, 20L));
        System.out.println("Double.max: " + Double.max(1.5, 2.5));
    }

    // ------------------------------------------------------------
    // unsigned Integer / Long methods
    // ------------------------------------------------------------

    static void unsignedExamples() {
        System.out.println("\n=== unsigned numbers ===");

        int unsignedText = Integer.parseUnsignedInt("4294967295");
        long unsignedLongText =
                Long.parseUnsignedLong("18446744073709551615");

        System.out.println("parseUnsignedInt: " + unsignedText);
        System.out.println("parseUnsignedLong: " + unsignedLongText);

        // Compare as unsigned rather than signed values.
        System.out.println(
                "compareUnsigned: "
                        + Integer.compareUnsigned(-1, 1));

        // Unsigned division and remainder.
        int quotient = Integer.divideUnsigned(-1, 2);
        int remainder = Integer.remainderUnsigned(-1, 2);

        System.out.println("divideUnsigned: " + quotient);
        System.out.println("remainderUnsigned: " + remainder);

        System.out.println(
                "toUnsignedString: "
                        + Integer.toUnsignedString(-1));

        System.out.println(
                "Long.toUnsignedString: "
                        + Long.toUnsignedString(-1L));

        // Convert byte/short using unsigned interpretation.
        byte b = (byte) 0xFF;
        short s = (short) 0xFFFF;

        System.out.println("Byte.toUnsignedInt: "
                + Byte.toUnsignedInt(b));

        System.out.println("Short.toUnsignedInt: "
                + Short.toUnsignedInt(s));

        System.out.println("Short.toUnsignedLong: "
                + Short.toUnsignedLong(s));

        System.out.println("Integer.toUnsignedLong: "
                + Integer.toUnsignedLong(-1));
    }

    // ------------------------------------------------------------
    // isFinite
    // ------------------------------------------------------------

    static void finiteExamples() {
        System.out.println("\n=== isFinite ===");

        System.out.println("Double.isFinite(10.0): "
                + Double.isFinite(10.0));

        System.out.println("Double.isFinite(Infinity): "
                + Double.isFinite(Double.POSITIVE_INFINITY));

        System.out.println("Float.isFinite(10.0f): "
                + Float.isFinite(10.0f));

        System.out.println("Float.isFinite(NaN): "
                + Float.isFinite(Float.NaN));
    }

    // ------------------------------------------------------------
    // Boolean logical operations
    // ------------------------------------------------------------

    static void booleanExamples() {
        System.out.println("\n=== Boolean logical operations ===");

        System.out.println("logicalAnd: "
                + Boolean.logicalAnd(true, false));

        System.out.println("logicalOr: "
                + Boolean.logicalOr(true, false));

        System.out.println("logicalXor: "
                + Boolean.logicalXor(true, false));
    }

    // ------------------------------------------------------------
    // BigInteger exact conversions
    // ------------------------------------------------------------

    static void bigIntegerExactExamples() {
        System.out.println("\n=== BigInteger exact conversions ===");

        BigInteger safe = BigInteger.valueOf(42);

        System.out.println("byteValueExact: "
                + safe.byteValueExact());

        System.out.println("shortValueExact: "
                + safe.shortValueExact());

        System.out.println("intValueExact: "
                + safe.intValueExact());

        System.out.println("longValueExact: "
                + safe.longValueExact());

        // This throws ArithmeticException because the value does not
        // fit in an int without information loss.
        try {
            BigInteger tooLarge = BigInteger.valueOf(Integer.MAX_VALUE)
                    .add(BigInteger.ONE);

            tooLarge.intValueExact();
        } catch (ArithmeticException e) {
            System.out.println("Exact conversion failed: "
                    + e.getMessage());
        }
    }

    // ------------------------------------------------------------
    // Math exact arithmetic
    // ------------------------------------------------------------

    static void mathExactExamples() {
        System.out.println("\n=== Math exact arithmetic ===");

        System.out.println("addExact: " + Math.addExact(10, 20));
        System.out.println("subtractExact: " + Math.subtractExact(20, 10));
        System.out.println("multiplyExact: " + Math.multiplyExact(5, 4));
        System.out.println("incrementExact: " + Math.incrementExact(10));
        System.out.println("decrementExact: " + Math.decrementExact(10));
        System.out.println("negateExact: " + Math.negateExact(10));

        // Exact methods throw instead of silently overflowing.
        try {
            Math.addExact(Integer.MAX_VALUE, 1);
        } catch (ArithmeticException e) {
            System.out.println("Overflow detected: "
                    + e.getMessage());
        }

        try {
            Math.multiplyExact(Integer.MAX_VALUE, 2);
        } catch (ArithmeticException e) {
            System.out.println("Overflow detected: "
                    + e.getMessage());
        }

        // Convert long to int only when the value fits.
        System.out.println(
                "toIntExact: "
                        + Math.toIntExact(123L));

        try {
            Math.toIntExact((long) Integer.MAX_VALUE + 1);
        } catch (ArithmeticException e) {
            System.out.println("Long does not fit in int: "
                    + e.getMessage());
        }

        // Other Java 8 Math additions.
        System.out.println("floorMod: " + Math.floorMod(-10, 3));
        System.out.println("floorDiv: " + Math.floorDiv(-10, 3));
        System.out.println("nextDown: " + Math.nextDown(10.0));
    }

    public static void main(String[] args) {
        sumMinMaxExamples();
        unsignedExamples();
        finiteExamples();
        booleanExamples();
        bigIntegerExactExamples();
        mathExactExamples();
    }
}
