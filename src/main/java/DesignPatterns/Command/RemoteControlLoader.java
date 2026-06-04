package DesignPatterns.Command;

public class RemoteControlLoader {
    public static void main(String[] args) {
        RemoteControl remoteControl = new RemoteControl();

        // 1. Create the Vendor/Receiver Objects
        CeilingFan livingRoomCeilingFan = new CeilingFan("Living Room");
        CeilingFan kitchenCeilingFan = new CeilingFan("Kitchen");
        GarageDoor garageDoor = new GarageDoor("Garage");
        Light livingRoomLight = new Light("Living Room");
        Light kitchenLight = new Light("Kitchen");

        // 2. Create the Command Objects
        // Kitchen Ceiling Fan Commands
        CeilingFanOffCommand kitchenCeilingFanOffCommand = new CeilingFanOffCommand(kitchenCeilingFan);
        CeilingFanOnCommand kitchenCeilingFanOnCommand = new CeilingFanOnCommand(kitchenCeilingFan);

        // Living Room Ceiling Fan Commands
        CeilingFanOffCommand livingRoomCeilingFanOffCommand = new CeilingFanOffCommand(livingRoomCeilingFan);
        CeilingFanOnCommand livingRoomCeilingFanOnCommand = new CeilingFanOnCommand(livingRoomCeilingFan);

        // Light Commands
        LightOnCommand livingRoomLightOn = new LightOnCommand(livingRoomLight);
        LightOffCommand livingRoomLightOff = new LightOffCommand(livingRoomLight);
        LightOnCommand kitchenLightOn = new LightOnCommand(kitchenLight);
        LightOffCommand kitchenLightOff = new LightOffCommand(kitchenLight);

        // Garage Door Commands
        GarageDoorUpCommand garageDoorUp = new GarageDoorUpCommand(garageDoor);
        GarageDoorDownCommand garageDoorDown = new GarageDoorDownCommand(garageDoor);

        // 3. Load Commands into the Remote Control Slots
        remoteControl.setCommand(0, kitchenCeilingFanOnCommand, kitchenCeilingFanOffCommand);
        remoteControl.setCommand(1, livingRoomCeilingFanOnCommand, livingRoomCeilingFanOffCommand);
        remoteControl.setCommand(2, livingRoomLightOn, livingRoomLightOff);
        remoteControl.setCommand(3, kitchenLightOn, kitchenLightOff);
        remoteControl.setCommand(4, garageDoorUp, garageDoorDown);

        // 4. Print the Remote Setup (uses your fixed toString() method!)
        System.out.println(remoteControl);

        // 5. Test the slots
        System.out.println("--- Testing Slot 0 (Kitchen Ceiling Fan) ---");
        remoteControl.onButtonWasPushed(0);
        remoteControl.offButtonWasPushed(0);

        System.out.println("\n--- Testing Slot 2 (Living Room Light) ---");
        remoteControl.onButtonWasPushed(2);
        remoteControl.offButtonWasPushed(2);

        System.out.println("\n--- Testing Slot 4 (Garage Door) ---");
        remoteControl.onButtonWasPushed(4);
        remoteControl.offButtonWasPushed(4);
    }
}