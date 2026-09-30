package DesignPatterns.Interpreter;

import java.util.Objects;

/**
 * Terminal expression: a variable is one indivisible rule in the grammar.
 */
public class VariableExpression implements Expression {
    private final String name;

    public VariableExpression(String name) {
        this.name = Objects.requireNonNull(name, "name");
    }

    @Override
    public boolean interpret(Context context) {
        return context.valueOf(name);
    }
}
