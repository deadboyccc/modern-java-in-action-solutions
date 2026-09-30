package DesignPatterns.Interpreter;

/**
 * Abstract expression: every grammar rule can be evaluated against a context.
 */
public interface Expression {
    boolean interpret(Context context);
}
