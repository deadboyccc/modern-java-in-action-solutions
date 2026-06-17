package DesignPatterns.Proxy.VirutalProxy;

public class VirtualProxyTestDrive {
    static void main() throws InterruptedException {
        UIElement uiElement = new UIElement();
        VirtualProxy virtualProxy = new VirtualProxy(uiElement);

        virtualProxy.getLengthyMessage();


    }
}
