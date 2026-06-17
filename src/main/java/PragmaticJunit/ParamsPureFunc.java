package PragmaticJunit;

public class ParamsPureFunc {
    public String getCapitalized(String str) {
        // Prevent IndexOutOfBounds or NPE states
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}