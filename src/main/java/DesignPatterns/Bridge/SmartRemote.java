package DesignPatterns.Bridge;

/**
 * Refined abstraction: this remote adds controls without changing TV classes.
 */
public class SmartRemote extends RemoteControl {
    public SmartRemote(Television television) {
        super(television);
    }

    /**
     * Smart-remote feature: increase the bridged TV's volume.
     */
    public void volumeUp() {
        television().volumeUp();
    }

    /**
     * Smart-remote feature: decrease the bridged TV's volume.
     */
    public void volumeDown() {
        television().volumeDown();
    }

    /**
     * Smart-remote feature: toggle the bridged TV's mute state.
     */
    public void mute() {
        television().mute();
    }
}
