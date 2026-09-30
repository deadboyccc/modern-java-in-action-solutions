package DesignPatterns.Interpreter;

/**
 * Client: builds a grammar tree, then interprets it using the current context.
 */
public class InterpreterDemo {
    public static void main(String[] args) {
        Context sensors = new Context();
        sensors.assign("temperatureHigh", true);
        sensors.assign("windowOpen", false);
        sensors.assign("smokeDetected", false);

        // Rule: (temperatureHigh AND windowOpen) OR smokeDetected
        Expression rule = new OrExpression(
                new AndExpression(
                        new VariableExpression("temperatureHigh"),
                        new VariableExpression("windowOpen")),
                new VariableExpression("smokeDetected"));

        System.out.println("Should the alarm sound? " + rule.interpret(sensors));
    }
}
