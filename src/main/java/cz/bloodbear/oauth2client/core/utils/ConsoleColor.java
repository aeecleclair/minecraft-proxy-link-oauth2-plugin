package cz.bloodbear.oauth2client.core.utils;

import org.slf4j.Logger;

public class ConsoleColor {
    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String MAGENTA = "\u001B[35m";
    private static final String CYAN = "\u001B[36m";

    private final Logger logger;
    public ConsoleColor(Logger logger) {
        this.logger = logger;
    }

    public static String text(String text, String color) { return color + text + RESET; }

    public void error(String text) { logger.error(text(text, RED)); }
    public void warn(String text) { logger.warn(text(text, YELLOW)); }
    public void info(String text) { logger.info(text(text, GREEN)); }
    public void debug(String text) { logger.info(text(text, MAGENTA)); }
    public void trace(String text) { logger.info(text(text, CYAN)); }
}
