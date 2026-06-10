package dev.dead.ch9;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoggerSupplier {
    static void main() {
        Logger logger = Logger.getLogger(LoggerSupplier.class.getName());
        logger.log(Level.INFO, "Hello World!");
        logger.log(Level.FINER, () -> "Problem: " + generateDiagnostic());
        

    }

    public static String generateDiagnostic() {
        return UUID.randomUUID().toString();
    }
}
