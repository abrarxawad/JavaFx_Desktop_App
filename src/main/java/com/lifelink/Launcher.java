package com.lifelink;

/**
 * Compatibility launcher for IDEs that do not automatically detect the JavaFX
 * application entry point.
 *
 * <p>The real application entry point is {@link Main}, which extends
 * {@code javafx.application.Application}. This class simply delegates to it so
 * the project can still be launched from the IDE or from a command line if the
 * default run configuration points here.
 */
@Deprecated(forRemoval = false)
public class Launcher {
    public static void main(String[] args) {
        Main.main(args);
    }
}
