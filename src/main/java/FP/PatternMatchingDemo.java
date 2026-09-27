package FP;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Modern Java demonstration comparing historical and contemporary pattern matching techniques.
 */
public class PatternMatchingDemo {

    // =========================================================================
    // 1. DOMAIN DATA STRUCTURES
    // =========================================================================

    public static Expr simplifyTraditional(Expr expr) {
        // Uses standard Java 21+ Record Pattern matching inside instanceof
        if (expr instanceof BinOp(String op, Expr left, Expr right)) {
            if ("+".equals(op)) {
                if (right instanceof NumberNode(int val) && val == 0) return left;
                if (left instanceof NumberNode(int val) && val == 0) return right;
            }
        }
        return expr;
    }

    public static <S, T, U, R> R patternMatchExpr(
            Expr e,
            TriFunction<String, Expr, Expr, R> binOpCase,
            Function<Integer, R> numCase,
            Supplier<R> defaultCase) {

        // Uses Java 21 record patterns instead of messy casting
        if (e instanceof BinOp(String op, Expr l, Expr r)) {
            return binOpCase.apply(op, l, r);
        } else if (e instanceof NumberNode(int v)) {
            return numCase.apply(v);
        } else {
            return defaultCase.get();
        }
    }

    public static Expr simplifyFunctional(Expr e) {
        TriFunction<String, Expr, Expr, Expr> binOpCase = (opname, left, right) -> {
            if ("+".equals(opname)) {
                if (left instanceof NumberNode(int v) && v == 0) return right;
                if (right instanceof NumberNode(int v) && v == 0) return left;
            }
            if ("*".equals(opname)) {
                if (left instanceof NumberNode(int v) && v == 1) return right;
                if (right instanceof NumberNode(int v) && v == 1) return left;
            }
            return new BinOp(opname, left, right);
        };

        Function<Integer, Expr> numCase = val -> new NumberNode(val);
        Supplier<Expr> defaultCase = () -> new NumberNode(0);

        return patternMatchExpr(e, binOpCase, numCase, defaultCase);
    }

    // =========================================================================
    // 2. TRADITIONAL APPROACH: NAIVE INSTANCEOF
    // =========================================================================

    public static ModernExpr simplifyModern(ModernExpr expr) {
        // Native Java 21+ switch pattern matching with record guards (`when`)
        return switch (expr) {
            case ModernBinOp(String op, ModernExpr left, ModernNumber(int val)) when op.equals("+") && val == 0 -> left;
            case ModernBinOp(String op, ModernNumber(int val), ModernExpr right) when op.equals("+") && val == 0 ->
                    right;
            case ModernBinOp(String op, ModernExpr left, ModernNumber(int val)) when op.equals("*") && val == 1 -> left;
            case ModernBinOp(String op, ModernNumber(int val), ModernExpr right) when op.equals("*") && val == 1 ->
                    right;
            default -> expr;
        };
    }

    // =========================================================================
    // 3. VISITOR DESIGN PATTERN
    // =========================================================================

    // Kept standard public void main(String[] args) so it runs natively without Preview feature flags
    public static void main(String[] args) {
        Expr expr = new BinOp("+", new NumberNode(5), new NumberNode(0));
        System.out.println("Original Expression: " + expr);
        System.out.println("Traditional Simplify: " + simplifyTraditional(expr));
        System.out.println("Visitor Simplify: " + expr.accept(new DefaultSimplifyVisitor()));
        System.out.println("Lambda Match Simplify: " + simplifyFunctional(expr));

        ModernExpr modernExpr = new ModernBinOp("+", new ModernNumber(5), new ModernNumber(0));
        System.out.println("Modern Java Simplify: " + simplifyModern(modernExpr));
    }

    public interface Expr {
        Expr accept(SimplifyExprVisitor visitor);
    }

    // =========================================================================
    // 4. FUNCTIONAL SIMULATION VIA LAMBDAS
    // =========================================================================

    public interface SimplifyExprVisitor {
        Expr visit(BinOp b);

        Expr visit(NumberNode n);
    }

    @FunctionalInterface
    public interface TriFunction<S, T, U, R> {
        R apply(S s, T t, U u);
    }

    public sealed interface ModernExpr permits ModernNumber, ModernBinOp {
    }

    // =========================================================================
    // 5. MODERN JAVA NATIVE PATTERN MATCHING
    // =========================================================================

    public record NumberNode(int val) implements Expr {
        @Override
        public Expr accept(SimplifyExprVisitor visitor) {
            return visitor.visit(this);
        }

        @Override
        public String toString() {
            return String.valueOf(val);
        }
    }

    public record BinOp(String opname, Expr left, Expr right) implements Expr {
        @Override
        public Expr accept(SimplifyExprVisitor visitor) {
            return visitor.visit(this);
        }

        @Override
        public String toString() {
            return "(" + left + " " + opname + " " + right + ")";
        }
    }

    public static class DefaultSimplifyVisitor implements SimplifyExprVisitor {
        @Override
        public Expr visit(BinOp e) {
            if ("+".equals(e.opname()) && e.right() instanceof NumberNode(int val) && val == 0) return e.left();
            if ("*".equals(e.opname()) && e.right() instanceof NumberNode(int val) && val == 1) return e.left();
            return e;
        }

        @Override
        public Expr visit(NumberNode n) {
            return n;
        }
    }

    public record ModernNumber(int val) implements ModernExpr {
    }

    // =========================================================================
    // MAIN EXECUTION
    // =========================================================================

    public record ModernBinOp(String opname, ModernExpr left, ModernExpr right) implements ModernExpr {
    }
}