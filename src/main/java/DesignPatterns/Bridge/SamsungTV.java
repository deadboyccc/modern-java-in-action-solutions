package DesignPatterns.Bridge;

/**
 * Concrete implementor: a Samsung TV provides the shared TV operations.
 */
public class SamsungTV extends AbstractTelevision {
    @Override
    protected String modelName() {
        return "Samsung";
    }
}
