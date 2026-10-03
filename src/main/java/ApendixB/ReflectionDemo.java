package ApendixB;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;

public class ReflectionDemo {

    public static void main(String[] args) throws NoSuchMethodException {
        // Get the Method reference (Method inherits from Executable)
        Method method = ReflectionDemo.class.getMethod("registerUser", String.class, int.class);

        System.out.println("Inspecting Executable: " + method.getName());

        // getParameters() is defined in Executable
        Parameter[] parameters = method.getParameters();

        for (Parameter param : parameters) {
            System.out.println("---");
            System.out.println("Name: " + param.getName()); // Requires -parameters flag for real names
            System.out.println("Type: " + param.getType().getSimpleName());

            // Checking modifiers
            if (Modifier.isFinal(param.getModifiers())) {
                System.out.println("Modifier: final");
            }
        }
    }

    // The method we want to inspect
    public void registerUser(final String username, int age) {
    }
}