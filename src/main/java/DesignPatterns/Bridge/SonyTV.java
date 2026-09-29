package DesignPatterns.Bridge;

/**
 * Concrete implementor: a Sony TV can be substituted without changing a remote.
 */
public class SonyTV extends AbstractTelevision {
    @Override
    protected String modelName() {
        return "Sony";
    }
}
