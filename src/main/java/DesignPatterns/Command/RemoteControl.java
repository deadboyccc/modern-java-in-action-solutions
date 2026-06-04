package DesignPatterns.Command;

public class RemoteControl {
    private static final int slotCount = 8;
    Command[] onCommands = new Command[slotCount];
    Command[] offCommands = new Command[slotCount];

    RemoteControl() {
        for (int i = 0; i < slotCount; i++) {
            onCommands[i] = new NoCommand();
            offCommands[i] = new NoCommand();
        }
    }

    public void setCommand(int index, Command onCommand, Command offCommand) {
        onCommands[index] = onCommand;
        offCommands[index] = offCommand;
    }

    public void onButtonWasPushed(int slot) {
        onCommands[slot].execute();
    }

    public void offButtonWasPushed(int slot) {
        offCommands[slot].execute();
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("\n------ Remote Control -------\n");

        for (int i = 0; i < onCommands.length; i++) {
            String onClassName = (onCommands[i] != null) ? onCommands[i].getClass().getSimpleName() : "NoCommand";
            String offClassName = (offCommands[i] != null) ? offCommands[i].getClass().getSimpleName() : "NoCommand";

            stringBuilder.append(String.format("[slot %d] %-20s  %s\n", i, onClassName, offClassName));
        }

        return stringBuilder.toString();
    }
}
