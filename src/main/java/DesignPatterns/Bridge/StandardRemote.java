package DesignPatterns.Bridge;

/**
 * Concrete abstraction: the standard remote offers basic controls.
 */
public class StandardRemote extends RemoteControl {
    public StandardRemote(Television television) {
        super(television);
    }
}
