package com.lifelink.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

/**
 * Placeholder controller for the registration screen.
 * Full implementation will be added in Phase 5 (Authentication).
 *
 * <p><b>Package:</b> com.lifelink.controller
 */
public class RegisterController {

    @FXML
    private Button backToLoginBtn;

    @FXML
    private void handleBack(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) backToLoginBtn.getScene().getWindow();
        Scene scene = new Scene(root, stage.getWidth(), stage.getHeight());
        scene.getStylesheets().add(
            Objects.requireNonNull(getClass().getResource("/css/styles.css")).toExternalForm()
        );
        stage.setScene(scene);
    }
}
