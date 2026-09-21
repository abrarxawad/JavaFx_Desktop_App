package com.lifelink;

/**
 * A workaround launcher class to avoid the "JavaFX runtime components are missing" error
 * when running the application directly from IntelliJ IDEA's run button.
 *
 * <p>By using a class that does not extend javafx.application.Application as the
 * entry point, the JVM loads the JavaFX dependencies from the classpath automatically.
 */
public class Launcher {
    public static void main(String[] args) {
        Main.main(args);
    }
}
