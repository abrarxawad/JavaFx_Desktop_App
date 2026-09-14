package com.lifelink.controller;

import com.lifelink.database.DatabaseManager;
import com.lifelink.exception.AuthenticationException;
import com.lifelink.exception.DatabaseException;
import com.lifelink.model.User;
import com.lifelink.model.UserRole;
import com.lifelink.security.PasswordHasher;
import com.lifelink.security.SessionManager;
import com.lifelink.dao.UserDAO;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Controller for the Login screen (Login.fxml).
 *
 * <p><b>JavaFX MVC:</b> This controller sits between the FXML view and the
 * service/DAO layer. It handles user input events, delegates to the UserDAO
 * for authentication, and navigates to the appropriate dashboard.
 *
 * <p><b>Concurrency:</b> The login action runs on a background {@link Task}
 * (not the JavaFX Application Thread) because:
 * <ol>
 *   <li>JDBC operations can block for tens of milliseconds — running them on
 *       the UI thread would freeze the interface.</li>
 *   <li>BCrypt.verify() is intentionally slow (hashing algorithm) — it can
 *       take 100–500ms and must not block the UI thread.</li>
 * </ol>
 * {@link Platform#runLater(Runnable)} is used to update UI components from
 * the background thread safely.
 *
 * <p><b>Package:</b> com.lifelink.controller
 */
public class LoginController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    // ── FXML injected nodes ───────────────────────────────────────────────────
    @FXML private VBox         loginCard;
    @FXML private TextField    usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button       loginButton;
    @FXML private Button       registerButton;
    @FXML private Label        errorLabel;

    // ── Dependencies ──────────────────────────────────────────────────────────
    private final UserDAO        userDAO        = new UserDAO();
    private final SessionManager sessionManager = SessionManager.getInstance();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // ── Play a fade-in animation on the card ─────────────────────────────
        FadeTransition fade = new FadeTransition(Duration.millis(600), loginCard);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.play();

        // ── Allow Enter key to trigger login ─────────────────────────────────
        passwordField.setOnAction(this::handleLogin);
        usernameField.setOnAction(e -> passwordField.requestFocus());

        // ── Hide error when user starts typing ────────────────────────────────
        usernameField.textProperty().addListener((obs, o, n) -> hideError());
        passwordField.textProperty().addListener((obs, o, n) -> hideError());
    }

    // ── Login Action ──────────────────────────────────────────────────────────

    /**
     * Handles the Sign In button click.
     *
     * <p>Validates input, then runs authentication on a background thread to
     * prevent blocking the JavaFX Application Thread.
     */
    @FXML
    private void handleLogin(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        // ── Client-side validation ────────────────────────────────────────────
        if (username.isEmpty()) {
            showError("Please enter your username.");
            usernameField.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            showError("Please enter your password.");
            passwordField.requestFocus();
            return;
        }

        // ── Disable form while processing ─────────────────────────────────────
        setFormDisabled(true);

        // ── Background authentication task ────────────────────────────────────
        // Reason: JDBC + BCrypt must not run on the JavaFX UI thread
        Task<User> authTask = new Task<>() {
            @Override
            protected User call() throws Exception {
                // 1. Look up user by username
                Optional<User> optUser = userDAO.findByUsername(username);

                if (optUser.isEmpty()) {
                    throw new AuthenticationException("Invalid username or password.");
                }

                User user = optUser.get();

                // 2. Verify password (BCrypt — intentionally slow)
                if (!PasswordHasher.verify(password, user.getPasswordHash())) {
                    throw new AuthenticationException("Invalid username or password.");
                }

                // 3. Check account status
                if (!user.isActive()) {
                    throw new AuthenticationException(
                        "Your account is " + user.getStatus().toLowerCase() +
                        ". Please contact the administrator."
                    );
                }

                return user;
            }
        };

        // ── On success: navigate to correct dashboard ──────────────────────────
        authTask.setOnSucceeded(e -> {
            User user = authTask.getValue();
            sessionManager.login(user);
            logger.info("User '{}' logged in successfully (role={})",
                        user.getUsername(), user.getRole());
            navigateToDashboard(user.getRole());
        });

        // ── On failure: show error message ────────────────────────────────────
        authTask.setOnFailed(e -> {
            setFormDisabled(false);
            Throwable ex = authTask.getException();
            if (ex instanceof AuthenticationException) {
                showError(ex.getMessage());
            } else if (ex instanceof DatabaseException de) {
                showError(de.getUserMessage());
                logger.error("Database error during login: {}", ex.getMessage());
            } else {
                showError("An unexpected error occurred. Please try again.");
                logger.error("Unexpected login error", ex);
            }
        });

        // Run on a background thread (not the JavaFX Application Thread)
        Thread authThread = new Thread(authTask, "auth-thread");
        authThread.setDaemon(true); // don't prevent JVM shutdown
        authThread.start();
    }

    // ── Register Action ───────────────────────────────────────────────────────

    @FXML
    private void handleRegister(ActionEvent event) {
        try {
            navigateToScene("/fxml/Register.fxml");
        } catch (IOException e) {
            showError("Could not open registration form.");
            logger.error("Failed to load Register.fxml: {}", e.getMessage());
        }
    }

    // ── Dashboard Navigation ──────────────────────────────────────────────────

    /**
     * Routes the user to the correct dashboard based on their role.
     * Called on the background thread result — uses Platform.runLater for UI update.
     */
    private void navigateToDashboard(UserRole role) {
        Platform.runLater(() -> {
            String fxmlPath = switch (role) {
                case DONOR      -> "/fxml/DonorDashboard.fxml";
                case RECIPIENT  -> "/fxml/RecipientDashboard.fxml";
                case BLOOD_BANK -> "/fxml/BloodBankDashboard.fxml";
                case HOSPITAL   -> "/fxml/HospitalDashboard.fxml";
                case ADMIN      -> "/fxml/AdminDashboard.fxml";
            };

            try {
                navigateToScene(fxmlPath);
            } catch (IOException e) {
                logger.error("Failed to load dashboard '{}': {}", fxmlPath, e.getMessage());
                showError("Dashboard unavailable. Please contact support.");
                setFormDisabled(false);
            }
        });
    }

    // ── Scene Navigation Helper ───────────────────────────────────────────────

    private void navigateToScene(String fxmlPath) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        Parent root = loader.load();

        Stage stage = (Stage) loginButton.getScene().getWindow();
        Scene scene = new Scene(root, stage.getWidth(), stage.getHeight());
        scene.getStylesheets().add(
            getClass().getResource("/css/styles.css").toExternalForm()
        );

        stage.setScene(scene);
        stage.setMaximized(true);
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);

        // Shake animation to draw attention
        FadeTransition fade = new FadeTransition(Duration.millis(300), errorLabel);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.play();
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private void setFormDisabled(boolean disabled) {
        loginButton.setDisable(disabled);
        registerButton.setDisable(disabled);
        usernameField.setDisable(disabled);
        passwordField.setDisable(disabled);
        loginButton.setText(disabled ? "Signing in..." : "Sign In");
    }
}
