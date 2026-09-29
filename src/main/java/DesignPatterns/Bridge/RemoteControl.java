package DesignPatterns.Bridge;

import java.util.Objects;

/**
 * Abstraction: remote features delegate device work through the TV bridge.
 */
public abstract class RemoteControl {
    private final Television television;

    /**
     * Constructor injection chooses the implementation independently.
     */
    protected RemoteControl(Television television) {
        this.television = Objects.requireNonNull(television);
    }

    /**
     * Toggle power without depending on a specific TV model.
     */
    public void power() {
        if (television.isOn()) {
            television.powerOff();
        } else {
            television.powerOn();
        }
    }

    /**
     * Delegate channel changes to the bridged TV implementation.
     */
    public void channelUp() {
        television.channelUp();
    }

    /**
     * Delegate channel changes to the bridged TV implementation.
     */
    public void channelDown() {
        television.channelDown();
    }

    /**
     * Refined remotes can add their own interface while reusing this bridge.
     */
    protected Television television() {
        return television;
    }
}
