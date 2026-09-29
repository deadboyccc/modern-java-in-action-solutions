package DesignPatterns.Bridge;

/**
 * Implementor: this contract lets every remote work with any TV model.
 */
public interface Television {
    /**
     * Turn this TV on.
     */
    void powerOn();

    /**
     * Turn this TV off.
     */
    void powerOff();

    /**
     * Let the remote decide whether to turn the TV on or off.
     */
    boolean isOn();

    /**
     * Select a specific channel.
     */
    void setChannel(int channel);

    /**
     * Move to the next channel.
     */
    void channelUp();

    /**
     * Move to the previous channel.
     */
    void channelDown();

    /**
     * Raise the TV's volume.
     */
    void volumeUp();

    /**
     * Lower the TV's volume.
     */
    void volumeDown();

    /**
     * Toggle the TV's mute state.
     */
    void mute();
}
