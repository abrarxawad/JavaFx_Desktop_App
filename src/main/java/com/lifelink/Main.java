package com.lifelink;

import com.lifelink.database.DatabaseManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;

/**
 * Entry point for the LifeLink JavaFX application.
 *
 * <p><b>JavaFX Lifecycle:</b>
 * <ol>
 *   <li>{@link #main(String[])} is called by the JVM — launches JavaFX.</li>
 *   <li>JavaFX calls {@link #start(Stage)} on the Application Thread.</li>
 *   <li>We load the Login FXML, apply the CSS, and show the window.</li>
 *   <li>{@link #stop()} is called when the window closes — we shut down
 *       the database connection pool and any thread pools here.</li>
 * </ol>
 *
 * <p><b>Package:</b> com.lifelink
 */
public class Main extends Application {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    /** Application window title. */
    private static final String APP_TITLE = "LifeLink — Blood Bank & Donor Navigation System";

    /** Minimum window dimensions. */
    private static final double MIN_WIDTH  = 1100;
    private static final double MIN_HEIGHT = 700;

    @Override
    public void start(Stage primaryStage) {
        try {
            logger.info("Starting LifeLink application...");

            // ── Load Login FXML ──────────────────────────────────────────────
            // FXMLLoader reads the FXML file and instantiates the controller.
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/Login.fxml")
            );
            Parent root = loader.load();

            // ── Create Scene with CSS ────────────────────────────────────────
            Scene scene = new Scene(root, MIN_WIDTH, MIN_HEIGHT);
            scene.getStylesheets().add(
                    Objects.requireNonNull(
                        getClass().getResource("/css/styles.css")
                    ).toExternalForm()
            );

            // ── Configure Stage ──────────────────────────────────────────────
            primaryStage.setTitle(APP_TITLE);
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(MIN_WIDTH);
            primaryStage.setMinHeight(MIN_HEIGHT);
            primaryStage.setMaximized(true);

            // ── Clean shutdown on window close ───────────────────────────────
            primaryStage.setOnCloseRequest(event -> {
                logger.info("Window close requested.");
                // stop() will be called automatically by JavaFX
            });

            primaryStage.show();
            logger.info("LifeLink application started successfully.");

        } catch (IOException e) {
            logger.error("Failed to load Login.fxml: {}", e.getMessage(), e);
            throw new RuntimeException("Application failed to start: " + e.getMessage(), e);
        }
    }

    @Override
    public void stop() {
        logger.info("Shutting down LifeLink...");
        // Shut down the database connection pool
        try {
            DatabaseManager.getInstance().shutdown();
        } catch (Exception e) {
            logger.warn("Error during DatabaseManager shutdown: {}", e.getMessage());
        }
        logger.info("LifeLink shut down complete.");
    }

    /**
     * JVM entry point.
     *
     * <p>JavaFX 17+ requires the main class to extend {@link Application}
     * and call {@link #launch(String...)} here.
     */
    public static void main(String[] args) {
        launch(args);
    }
}
