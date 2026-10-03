package ApendixB;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * Appendix B.6 - Reflection
 * <p>
 * Java 8 reflection additions:
 * - java.lang.reflect.Parameter
 * - java.lang.reflect.Executable
 * <p>
 * Method and Constructor share common executable-member functionality
 * through Executable.
 */
public class B6_ReflectionStudyGuide {

    static void parameterExample() throws Exception {
        System.out.println("\n=== Parameter ===");

        Method method = UserService.class
                .getMethod("findUser", String.class, int.class);

        // Parameter represents one method/constructor parameter.
        Parameter[] parameters = method.getParameters();

        for (Parameter parameter : parameters) {
            System.out.println("name: " + parameter.getName());
            System.out.println("type: " + parameter.getType().getName());
            System.out.println("modifiers: " + parameter.getModifiers());
            System.out.println("isNamePresent: "
                    + parameter.isNamePresent());
            System.out.println();
        }
    }

    static void executableExample() throws Exception {
        System.out.println("\n=== Executable ===");

        // Method extends Executable.
        Method method = UserService.class
                .getMethod("findUser", String.class, int.class);

        // Constructor also extends Executable.
        Constructor<UserService> constructor =
                UserService.class.getConstructor(String.class);

        System.out.println("Method parameter count: "
                + method.getParameterCount());

        System.out.println("Constructor parameter count: "
                + constructor.getParameterCount());

        System.out.println("Method return type: "
                + method.getReturnType().getSimpleName());

        System.out.println("Constructor parameter types: ");

        for (Class<?> type : constructor.getParameterTypes()) {
            System.out.println("  " + type.getSimpleName());
        }
    }

    /*
     * Note:
     *
     * Whether real source parameter names are available through
     * Parameter.getName() depends on how the class was compiled.
     *
     * With javac, compile with:
     *
     *     javac -parameters B6_ReflectionStudyGuide.java
     *
     * Then isNamePresent() can report true and real names such as
     * "id" and "limit" can be reflected.
     */
    public static void main(String[] args) throws Exception {
        parameterExample();
        executableExample();
    }

    static class UserService {
        public UserService(String name) {
            System.out.println("created: " + name);
        }

        public String findUser(String id, int limit) {
            return "user-" + id + "-" + limit;
        }
    }
}
