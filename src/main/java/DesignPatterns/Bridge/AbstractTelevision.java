package DesignPatterns.Bridge;

/**
 * Shared implementation: concrete TV models inherit common device behavior.
 */
public abstract class AbstractTelevision implements Television {
    private boolean on;
    private int channel = 1;
    private int volume = 10;
    private boolean muted;

    /**
     * Each model supplies its own label for the demonstration output.
     */
    protected abstract String modelName();

    @Override
    public void powerOn() {
        on = true;
        show("Power on");
    }

    @Override
    public void powerOff() {
        on = false;
        show("Power off");
    }

    @Override
    public boolean isOn() {
        return on;
    }

    @Override
    public void setChannel(int channel) {
        this.channel = channel;
        show("Channel " + channel);
    }

    @Override
    public void channelUp() {
        setChannel(channel + 1);
    }

    @Override
    public void channelDown() {
        setChannel(channel - 1);
    }

    @Override
    public void volumeUp() {
        volume++;
        muted = false;
        show("Volume " + volume);
    }

    @Override
    public void volumeDown() {
        volume--;
        show("Volume " + volume);
    }

    @Override
    public void mute() {
        muted = !muted;
        show(muted ? "Muted" : "Unmuted");
    }

    /**
     * One output point makes the different TV implementations easy to see.
     */
    private void show(String action) {
        System.out.println(modelName() + ": " + action);
    }
}
