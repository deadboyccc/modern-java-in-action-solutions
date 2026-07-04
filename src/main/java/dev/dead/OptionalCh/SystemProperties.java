package dev.dead.OptionalCh;

import java.util.Optional;
import java.util.Properties;

public class SystemProperties {

    private Properties prop;

    SystemProperties(Properties props) {
        this.prop = props;
    }

    static void main(String[] args) {
        Properties props = new Properties();
        props.setProperty("a", "5");
        props.setProperty("b", "true");
        props.setProperty("c", "-3");
        SystemProperties prop = new SystemProperties(props);
        var a =
                prop.readDuration("a");
        System.out.println(a);
    }

    public static Optional<Integer> stringToInt(String s) {
        try {
            return Optional.of(Integer.parseInt(s));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public int readDuration(String name) {
        return Optional.ofNullable(prop.getProperty(name))
                .flatMap(SystemProperties::stringToInt)
                .filter(i -> i > 0)
                .orElse(0);
    }
}
