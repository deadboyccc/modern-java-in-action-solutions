package FP;

import org.jetbrains.annotations.NotNull;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Compares four ways of simplifying arithmetic expression trees in Java,
 * from the classic Visitor pattern to modern sealed types + switch patterns.
 *
 * <p>The goal in every approach is the same: apply the identity rules
 * <pre>
 *     x + 0  =>  x        0 + x  =>  x
 *     x * 1  =>  x        1 * x  =>  x
 * </pre>
 *
 * <p>Approaches, in the order they appear below:
 * <ol>
 *   <li><b>A. Visitor</b>              - the classic OO double-dispatch approach.</li>
 *   <li><b>B. instanceof + records</b> - record deconstruction inside {@code instanceof}.</li>
 *   <li><b>C. Lambda-based match</b>   - simulating a {@code match} expression with functions.</li>
 *   <li><b>D. Sealed + switch</b>      - native pattern-matching {@code switch} with guards.</li>
 * </ol>
 *
 * <p>Requires Java 21+ (record patterns and pattern-matching switch).
 */
public class PatternMatchingDemo {

    // =========================================================================
    // 1. DOMAIN MODEL
    //    Two parallel expression hierarchies, one per style:
    //      - "Classic" (Expr)       : open interface, supports the Visitor pattern.
    //      - "Modern"  (ModernExpr) : sealed interface, ideal for switch patterns.
    // =========================================================================

    // -------------------------------------------------------------------------
    // 1a. Classic hierarchy (used by approaches A, B and C)
    // -------------------------------------------------------------------------

    /**
     * Simplifies {@code x + 0} and {@code 0 + x} (addition only, no recursion).
     */
    public static Expr simplifyTraditional(Expr expr) {
        if (expr instanceof BinOp(String op, Expr left, Expr right)) {
            if ("+".equals(op)) {
                if (right instanceof NumberNode(int val) && val == 0) return left;  // x + 0
                if (left instanceof NumberNode(int val) && val == 0) return right;  // 0 + x
            }
        }
        return expr; // No rule matched: return unchanged.
    }

    /**
     * Generic "match" helper for {@link Expr}.
     *
     * @param e           the expression to inspect
     * @param binOpCase   invoked with (operator, left, right) if {@code e} is a {@link BinOp}
     * @param numCase     invoked with the value if {@code e} is a {@link NumberNode}
     * @param defaultCase invoked if {@code e} is any other {@link Expr} implementation
     * @param <R>         the result type produced by every case
     */
    public static <R> R patternMatchExpr(
            Expr e,
            TriFunction<String, Expr, Expr, R> binOpCase,
            Function<Integer, R> numCase,
            Supplier<R> defaultCase) {

        if (e instanceof BinOp(String op, Expr l, Expr r)) {
            return binOpCase.apply(op, l, r);
        } else if (e instanceof NumberNode(int v)) {
            return numCase.apply(v);
        } else {
            return defaultCase.get();
        }
    }

    /**
     * Simplifies using {@link #patternMatchExpr}. Handles both operands for
     * {@code +} (identity 0) and {@code *} (identity 1); no recursion.
     */
    public static Expr simplifyFunctional(Expr e) {
        // Case: binary operation, apply identity rules or rebuild unchanged.
        TriFunction<String, Expr, Expr, Expr> binOpCase = (opname, left, right) -> {
            if ("+".equals(opname)) {
                if (left instanceof NumberNode(int v) && v == 0) return right;  // 0 + x
                if (right instanceof NumberNode(int v) && v == 0) return left;  // x + 0
            }
            if ("*".equals(opname)) {
                if (left instanceof NumberNode(int v) && v == 1) return right;  // 1 * x
                if (right instanceof NumberNode(int v) && v == 1) return left;  // x * 1
            }
            return new BinOp(opname, left, right);
        };

        // Case: number, already simple; rebuild as-is.
        Function<Integer, Expr> numCase = NumberNode::new;

        // Case: unknown Expr subtype. Unreachable today (only BinOp and NumberNode
        // exist) but required by the helper's signature; falls back to 0.
        Supplier<Expr> defaultCase = () -> new NumberNode(0);

        return patternMatchExpr(e, binOpCase, numCase, defaultCase);
    }

    // -------------------------------------------------------------------------
    // 1b. Modern hierarchy (used by approach D)
    //     Sealed: the compiler knows every permitted subtype, so switch
    //     expressions over it can be checked for exhaustiveness.
    //     Plain records: no accept() method needed, since no Visitor is used.
    // -------------------------------------------------------------------------

    /**
     * Simplifies {@code +0 / 0+} and {@code *1 / 1*} using a pattern-matching
     * switch (no recursion).
     */
    public static ModernExpr simplifyModern(ModernExpr expr) {
        return switch (expr) {
            // x + 0  =>  x
            case ModernBinOp(String op, ModernExpr left, ModernNumber(int val))
                    when op.equals("+") && val == 0 -> left;

            // 0 + x  =>  x
            case ModernBinOp(String op, ModernNumber(int val), ModernExpr right)
                    when op.equals("+") && val == 0 -> right;

            // x * 1  =>  x
            case ModernBinOp(String op, ModernExpr left, ModernNumber(int val))
                    when op.equals("*") && val == 1 -> left;

            // 1 * x  =>  x
            case ModernBinOp(String op, ModernNumber(int val), ModernExpr right)
                    when op.equals("*") && val == 1 -> right;

            // No rule matched (guards are not exhaustive, so a default is required).
            default -> expr;
        };
    }

    public static void main(String[] args) {
        Expr expr = new BinOp("+", new NumberNode(5), new NumberNode(0));
        System.out.println("Original Expression: " + expr);

        // A. Visitor
        System.out.println("Visitor Simplify: " + expr.accept(new DefaultSimplifyVisitor()));

        // B. instanceof + record patterns
        System.out.println("Traditional Simplify: " + simplifyTraditional(expr));

        // C. Lambda-based match
        System.out.println("Lambda Match Simplify: " + simplifyFunctional(expr));

        // D. Sealed types + switch patterns (uses the parallel "modern" hierarchy)
        ModernExpr modernExpr = new ModernBinOp("+", new ModernNumber(5), new ModernNumber(0));
        System.out.println("Modern Java Simplify: " + simplifyModern(modernExpr));
    }

    /**
     * Base type of the classic expression tree.
     * The {@code accept} method is the entry point for the Visitor pattern.
     */
    public interface Expr {
        Expr accept(SimplifyExprVisitor visitor);
    }

    // =========================================================================
    // 2. APPROACH A: VISITOR PATTERN
    //    Behavior lives in a visitor object; each node dispatches to the
    //    matching visit(...) overload via accept(...).
    // =========================================================================

    public sealed interface ModernExpr permits ModernNumber, ModernBinOp {
    }

    /**
     * Visitor contract: one overload per concrete node type.
     */
    public interface SimplifyExprVisitor {
        Expr visit(BinOp b);

        Expr visit(NumberNode n);
    }

    // =========================================================================
    // 3. APPROACH B: INSTANCEOF WITH RECORD PATTERNS
    //    No visitor needed: deconstruct the record directly inside instanceof.
    //    Still an if-chain, but far less casting boilerplate than pre-Java 21.
    // =========================================================================

    /**
     * A function of three arguments (the JDK only ships up to two: BiFunction).
     */
    @FunctionalInterface
    public interface TriFunction<S, T, U, R> {
        R apply(S s, T t, U u);
    }

    // =========================================================================
    // 4. APPROACH C: FUNCTIONAL SIMULATION VIA LAMBDAS
    //    Emulates a "match" expression: a generic helper inspects the node
    //    type and delegates to one function per case.
    // =========================================================================

    /**
     * A literal integer, e.g. {@code 5}.
     */
    public record NumberNode(int val) implements Expr {
        @Override
        public Expr accept(SimplifyExprVisitor visitor) {
            return visitor.visit(this);
        }

        @Override
        public @NotNull String toString() {
            return String.valueOf(val);
        }
    }

    /**
     * A binary operation, e.g. {@code (5 + 0)}.
     */
    public record BinOp(String opname, Expr left, Expr right) implements Expr {
        @Override
        public Expr accept(SimplifyExprVisitor visitor) {
            return visitor.visit(this);
        }

        @Override
        public @NotNull String toString() {
            return "(" + left + " " + opname + " " + right + ")";
        }
    }

    public record ModernNumber(int val) implements ModernExpr {
    }

    // =========================================================================
    // 5. APPROACH D: MODERN JAVA (SEALED TYPES + SWITCH PATTERNS)
    //    The most declarative style: one case per rule, with nested record
    //    patterns and `when` guards. Cases are checked top to bottom.
    // =========================================================================

    public record ModernBinOp(String opname, ModernExpr left, ModernExpr right) implements ModernExpr {
    }

    // =========================================================================
    // 6. DEMO ENTRY POINT
    //    Runs every approach on the same input: 5 + 0  (expected result: 5).
    //    Uses a standard main(String[]) so no preview flags are needed.
    // =========================================================================

    /**
     * Applies the identity rules to the <em>right</em> operand only:
     * {@code x + 0 => x} and {@code x * 1 => x}.
     * (Left-operand rules and recursion into sub-trees are intentionally not handled.)
     */
    public static class DefaultSimplifyVisitor implements SimplifyExprVisitor {
        @Override
        public Expr visit(BinOp e) {
            if ("+".equals(e.opname()) && e.right() instanceof NumberNode(int val) && val == 0) return e.left();
            if ("*".equals(e.opname()) && e.right() instanceof NumberNode(int val) && val == 1) return e.left();
            return e;
        }

        @Override
        public Expr visit(NumberNode n) {
            return n; // A bare number is already as simple as it gets.
        }
    }
}