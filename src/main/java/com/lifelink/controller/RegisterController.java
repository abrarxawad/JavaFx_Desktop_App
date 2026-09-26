package com.lifelink.controller;

import com.lifelink.dao.UserDAO;
import com.lifelink.exception.DatabaseException;
import com.lifelink.model.BloodGroup;
import com.lifelink.model.UserRole;
import com.lifelink.security.PasswordHasher;
import com.lifelink.util.ThemeManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;

/**
 * Controller for the Registration screen.
 * Handles creating new user accounts.
 *
 * <p><b>Package:</b> com.lifelink.controller
 */
public class RegisterController {

    private static final Logger logger = LoggerFactory.getLogger(RegisterController.class);

    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> roleComboBox;
    @FXML private ComboBox<String> bloodGroupComboBox;
    @FXML private VBox bloodGroupSection;
    @FXML private Label statusMessage;
    @FXML private Button backToLoginBtn;
    @FXML private Button registerBtn;

    private final UserDAO userDAO = new UserDAO();

    @FXML
    public void initialize() {
        roleComboBox.getItems().setAll("DONOR", "RECIPIENT", "HOSPITAL", "BLOOD_BANK");
        bloodGroupComboBox.getItems().setAll(
            BloodGroup.A_POSITIVE.getLabel(),
            BloodGroup.A_NEGATIVE.getLabel(),
            BloodGroup.B_POSITIVE.getLabel(),
            BloodGroup.B_NEGATIVE.getLabel(),
            BloodGroup.AB_POSITIVE.getLabel(),
            BloodGroup.AB_NEGATIVE.getLabel(),
            BloodGroup.O_POSITIVE.getLabel(),
            BloodGroup.O_NEGATIVE.getLabel()
        );
        bloodGroupComboBox.getSelectionModel().select(BloodGroup.O_POSITIVE.getLabel());
        roleComboBox.getSelectionModel().select("DONOR");

        roleComboBox.valueProperty().addListener((obs, oldVal, newVal) -> updateBloodGroupVisibility());
        updateBloodGroupVisibility();
    }

    private void updateBloodGroupVisibility() {
        boolean donorRole = "DONOR".equals(roleComboBox.getValue());
        bloodGroupSection.setVisible(donorRole);
        bloodGroupSection.setManaged(donorRole);
    }

    @FXML
    private void handleRegister(ActionEvent event) {
        // Clear previous messages
        statusMessage.setText("");
        statusMessage.setStyle("-fx-text-fill: #e06c75;"); // Error color (red)

        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String roleStr = roleComboBox.getValue();
        String bloodGroupValue = bloodGroupComboBox.getValue();

        // 1. Validation
        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || roleStr == null) {
            statusMessage.setText("All fields are required.");
            return;
        }

        if ("DONOR".equals(roleStr) && (bloodGroupValue == null || bloodGroupValue.isBlank())) {
            statusMessage.setText("Please select a blood group for donor registration.");
            return;
        }

        if (password.length() < 6) {
            statusMessage.setText("Password must be at least 6 characters.");
            return;
        }

        try {
            UserRole role = UserRole.valueOf(roleStr);
            
            // 2. Hash Password
            String hash = PasswordHasher.hash(password);
            
            // 3. Save to Database
            int userId = userDAO.createUser(username, email, hash, role);
            
            // 4. Create empty role profile
            try (var ctx = com.lifelink.database.DatabaseManager.getInstance().getConnectionWrapper()) {
                java.sql.Connection conn = ctx.getConnection();
                java.sql.PreparedStatement ps = null;
                switch (role) {
                    case DONOR -> {
                        String donorBloodGroup = BloodGroup.fromLabel(bloodGroupValue).name();
                        ps = conn.prepareStatement("INSERT INTO donors (user_id, first_name, last_name, blood_group, date_of_birth, gender, phone) VALUES (?, 'Unknown', 'User', ?, '2000-01-01', 'Other', '')");
                        ps.setInt(1, userId);
                        ps.setString(2, donorBloodGroup);
                    }
                    case RECIPIENT -> {
                        ps = conn.prepareStatement("INSERT INTO recipients (user_id, first_name, last_name, blood_group, phone) VALUES (?, 'Unknown', 'User', 'O_POSITIVE', '')");
                        ps.setInt(1, userId);
                    }
                    case BLOOD_BANK -> {
                        ps = conn.prepareStatement("INSERT INTO blood_banks (user_id, name) VALUES (?, ?)");
                        ps.setInt(1, userId);
                        ps.setString(2, username + " Bank");
                    }
                    case HOSPITAL -> {
                        ps = conn.prepareStatement("INSERT INTO hospitals (user_id, name) VALUES (?, ?)");
                        ps.setInt(1, userId);
                        ps.setString(2, username + " Hospital");
                    }
                }
                if (ps != null) {
                    ps.executeUpdate();
                    ps.close();
                }
            }
            
            logger.info("Successfully registered user: {}", username);
            
            // Show success and redirect
            statusMessage.setStyle("-fx-text-fill: #98c379;"); // Success color (green)
            statusMessage.setText("Registration successful! Returning to login...");
            
            // Go back to login screen after a short delay (or immediately)
            handleBack(event);

        } catch (DatabaseException e) {
            logger.warn("Registration failed: {}", e.getMessage());
            statusMessage.setText(e.getUserMessage()); // Shows friendly message (e.g. duplicate username)
        } catch (Exception e) {
            logger.error("Unexpected error during registration.", e);
            statusMessage.setText("An unexpected error occurred. Please try again.");
        }
    }

    @FXML
    private void handleBack(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) backToLoginBtn.getScene().getWindow();
            Scene scene = new Scene(root, stage.getWidth(), stage.getHeight());
            ThemeManager.applyTheme(scene);
            stage.setScene(scene);
        } catch (IOException e) {
            logger.error("Failed to load Login screen: {}", e.getMessage());
        }
    }
}
