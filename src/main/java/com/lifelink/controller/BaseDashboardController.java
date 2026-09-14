package com.lifelink.controller;

import com.lifelink.security.SessionManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.Button;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;

/**
 * Abstract base controller providing common actions shared by all dashboard controllers.
 *
 * <p><b>OOP — Inheritance:</b> All five dashboard controllers extend this class
 * to avoid duplicating the logout logic in each one.
 *
 * <p><b>Package:</b> com.lifelink.controller
 */
public abstract class BaseDashboardController {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final SessionManager sessionManager = SessionManager.getInstance();

    /**
     * Handles the Logout button action.
     * Clears the session and returns to the Login screen.
     */
    @FXML
    protected void handleLogout(ActionEvent event) {
        logger.info("User '{}' logging out.", sessionManager.getCurrentUser() != null
                ? sessionManager.getCurrentUser().getUsername() : "unknown");
        sessionManager.logout();

        try {
            Button source = (Button) event.getSource();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) source.getScene().getWindow();
            Scene scene = new Scene(root, stage.getWidth(), stage.getHeight());
            scene.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/css/styles.css")).toExternalForm()
            );
            stage.setScene(scene);
        } catch (IOException e) {
            logger.error("Failed to navigate to Login screen after logout: {}", e.getMessage());
        }
    }
}
