package DesignPatterns.Interpreter;

import java.util.Objects;

/**
 * Nonterminal expression: NOT reverses the meaning of a smaller expression.
 */
public class NotExpression implements Expression {
    private final Expression expression;

    public NotExpression(Expression expression) {
        this.expression = Objects.requireNonNull(expression, "expression");
    }

    @Override
    public boolean interpret(Context context) {
        return !expression.interpret(context);
    }
}
