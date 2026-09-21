package com.lifelink.controller;

import com.lifelink.dao.BloodRequestDAO;
import com.lifelink.model.BloodGroup;
import com.lifelink.model.BloodRequest;
import com.lifelink.model.User;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for the Hospital Dashboard.
 *
 * <p>Hospitals can submit urgent/emergency blood requests and track their history.
 *
 * <p><b>Package:</b> com.lifelink.controller
 */
public class HospitalDashboardController extends BaseDashboardController implements Initializable {

    // ── FXML ─────────────────────────────────────────────────────────────────
    @FXML private Label topbarTitle;
    @FXML private Label topbarUserName;

    @FXML private VBox overviewPanel;
    @FXML private VBox emergencyPanel;
    @FXML private VBox historyPanel;

    // Stats
    @FXML private Label statTotal;
    @FXML private Label statPending;
    @FXML private Label statEmergency;
    @FXML private Label statFulfilled;
    @FXML private Label overviewStatus;

    // Emergency form
    @FXML private ComboBox<String> emerGroup;
    @FXML private TextField        emerQuantity;
    @FXML private ComboBox<String> emerPriority;
    @FXML private TextArea         emerNotes;
    @FXML private Label            emerStatus;

    // History table
    @FXML private TableView<BloodRequest>             histTable;
    @FXML private TableColumn<BloodRequest, Integer>  colHId;
    @FXML private TableColumn<BloodRequest, String>   colHGroup;
    @FXML private TableColumn<BloodRequest, Integer>  colHQty;
    @FXML private TableColumn<BloodRequest, String>   colHPriority;
    @FXML private TableColumn<BloodRequest, String>   colHStatus;
    @FXML private TableColumn<BloodRequest, String>   colHNotes;
    @FXML private TableColumn<BloodRequest, String>   colHDate;

    private final BloodRequestDAO requestDAO = new BloodRequestDAO();

    // ── Initialize ────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        User user = sessionManager.getCurrentUser();
        if (user != null) topbarUserName.setText(user.getUsername());

        emerGroup.getSelectionModel().select(0);
        emerPriority.getSelectionModel().select("EMERGENCY");

        setupTable();
        loadStats();
    }

    // ── Panel switching ───────────────────────────────────────────────────────

    @FXML private void showOverview(ActionEvent e)   { switchPanel("overview"); loadStats(); }
    @FXML private void showEmergency(ActionEvent e)  { switchPanel("emergency"); resetForm(); }
    @FXML private void showHistory(ActionEvent e)    { switchPanel("history"); loadHistory(); }

    private void switchPanel(String which) {
        overviewPanel.setVisible(false);
        emergencyPanel.setVisible(false);
        historyPanel.setVisible(false);
        switch (which) {
            case "overview"   -> { overviewPanel.setVisible(true);  topbarTitle.setText("Hospital Dashboard"); }
            case "emergency"  -> { emergencyPanel.setVisible(true); topbarTitle.setText("Submit Blood Request"); }
            case "history"    -> { historyPanel.setVisible(true);   topbarTitle.setText("Request History"); }
        }
    }

    // ── Data ──────────────────────────────────────────────────────────────────

    private void loadStats() {
        User user = sessionManager.getCurrentUser();
        if (user == null) return;
        Task<List<BloodRequest>> task = new Task<>() {
            @Override protected List<BloodRequest> call() {
                return requestDAO.findByRequesterId(user.getUserId());
            }
        };
        task.setOnSucceeded(e -> {
            List<BloodRequest> list = task.getValue();
            statTotal.setText(String.valueOf(list.size()));
            statPending.setText(String.valueOf(list.stream().filter(r -> "PENDING".equals(r.getStatus()) || "MATCHING".equals(r.getStatus())).count()));
            statEmergency.setText(String.valueOf(list.stream().filter(r -> "EMERGENCY".equals(r.getPriority())).count()));
            statFulfilled.setText(String.valueOf(list.stream().filter(r -> "FULFILLED".equals(r.getStatus())).count()));
        });
        task.setOnFailed(e -> overviewStatus.setText("Failed to load stats."));
        Thread t = new Thread(task, "h-stats"); t.setDaemon(true); t.start();
    }

    private void loadHistory() {
        User user = sessionManager.getCurrentUser();
        if (user == null) return;
        Task<List<BloodRequest>> task = new Task<>() {
            @Override protected List<BloodRequest> call() {
                return requestDAO.findByRequesterId(user.getUserId());
            }
        };
        task.setOnSucceeded(e -> {
            ObservableList<BloodRequest> data = FXCollections.observableArrayList(task.getValue());
            histTable.setItems(data);
        });
        task.setOnFailed(e -> logger.error("Failed to load history: {}", task.getException().getMessage()));
        Thread t = new Thread(task, "h-hist"); t.setDaemon(true); t.start();
    }

    // ── Submit ────────────────────────────────────────────────────────────────

    @FXML
    private void handleSubmitEmergency(ActionEvent event) {
        User user = sessionManager.getCurrentUser();
        if (user == null) return;

        String bgStr   = emerGroup.getValue();
        String qtyStr  = emerQuantity.getText().trim();
        String priority = emerPriority.getValue();

        if (bgStr == null || qtyStr.isEmpty() || priority == null) {
            emerStatus.setStyle("-fx-text-fill:#fc8181;");
            emerStatus.setText("Please fill all required fields.");
            return;
        }
        int qty;
        try { qty = Integer.parseInt(qtyStr); if (qty < 1) throw new NumberFormatException(); }
        catch (NumberFormatException ex) {
            emerStatus.setStyle("-fx-text-fill:#fc8181;");
            emerStatus.setText("Quantity must be a positive number.");
            return;
        }
        BloodGroup bg;
        try { bg = BloodGroup.valueOf(bgStr); }
        catch (IllegalArgumentException ex) {
            emerStatus.setStyle("-fx-text-fill:#fc8181;");
            emerStatus.setText("Invalid blood group.");
            return;
        }

        BloodRequest req = new BloodRequest.Builder()
                .requesterId(user.getUserId())
                .requesterName(user.getUsername())
                .bloodGroup(bg)
                .quantity(qty)
                .priority(priority)
                .notes(emerNotes.getText().trim())
                .build();

        Task<Integer> task = new Task<>() {
            @Override protected Integer call() { return requestDAO.create(req); }
        };
        task.setOnSucceeded(e -> {
            emerStatus.setStyle("-fx-text-fill:#68d391;");
            emerStatus.setText("✅ Request #" + task.getValue() + " submitted! Priority: " + priority);
            loadStats();
        });
        task.setOnFailed(e -> {
            emerStatus.setStyle("-fx-text-fill:#fc8181;");
            emerStatus.setText("❌ Failed: " + task.getException().getMessage());
        });
        Thread t = new Thread(task, "h-submit"); t.setDaemon(true); t.start();
    }

    // ── Table setup ───────────────────────────────────────────────────────────

    private void setupTable() {
        colHId.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getRequestId()).asObject());
        colHGroup.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getBloodGroup() != null ? c.getValue().getBloodGroup().getLabel() : "—"));
        colHQty.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getQuantity()).asObject());
        colHPriority.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPriority()));
        colHStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));
        colHNotes.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getNotes() != null ? c.getValue().getNotes() : ""));
        colHDate.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getRequestDate() != null ? c.getValue().getRequestDate().toLocalDate().toString() : "—"));
    }

    private void resetForm() {
        emerGroup.getSelectionModel().select(0);
        emerQuantity.setText("1");
        emerPriority.getSelectionModel().select("EMERGENCY");
        emerNotes.clear();
        emerStatus.setText("");
    }
}
