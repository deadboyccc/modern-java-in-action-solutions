package DesignPatterns.Proxy.VirutalProxy;

// gives text : "loading"
// start a long running task to fetch text online + threadSleep to mimic expensive resource

import java.util.Optional;
import java.util.UUID;

public class VirtualProxy {
    private final UIElement uiElement;
    private Optional<String> msg = Optional.empty();

    VirtualProxy(UIElement uiElement) {
        this.uiElement = uiElement;
        System.out.println("Creating a virtual proxy");
    }

    public void getLengthyMessage() {
        if (msg.isEmpty()) {
            uiElement.setMsg("Loading...");
        }
        new Thread(() -> {
            var expensiveMsg = UUID.randomUUID().toString();
            msg = Optional.of(expensiveMsg);
            uiElement.setMsg(expensiveMsg);

        }).start();


    }
}
