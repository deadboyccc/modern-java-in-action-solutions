package DesignPatterns.Bridge;

/**
 * Client: both remote abstractions pair freely with both TV implementations.
 */
public class BridgePatternDemo {
    public static void main(String[] args) {
        // Same remote abstraction, different TV implementation.
        RemoteControl standardSamsungRemote = new StandardRemote(new SamsungTV());
        standardSamsungRemote.power();
        standardSamsungRemote.channelUp();

        // New remote abstraction, same TV implementation hierarchy.
        SmartRemote smartSonyRemote = new SmartRemote(new SonyTV());
        smartSonyRemote.power();
        smartSonyRemote.channelUp();
        smartSonyRemote.volumeUp();
        smartSonyRemote.mute();
    }
}
