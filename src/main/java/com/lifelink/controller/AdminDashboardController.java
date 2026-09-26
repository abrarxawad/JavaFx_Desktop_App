package com.lifelink.controller;

import com.lifelink.dao.BloodInventoryDAO;
import com.lifelink.dao.BloodRequestDAO;
import com.lifelink.dao.DonationDAO;
import com.lifelink.dao.UserDAO;
import com.lifelink.json.JsonDataService;
import com.lifelink.model.BloodInventory;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;


public class AdminDashboardController extends BaseDashboardController implements Initializable {

    // ── FXML Nodes ────────────────────────────────────────────────────────────

    @FXML private Label topbarTitle;
    @FXML private Label topbarUserName;

    // Panels
    @FXML private VBox overviewPanel;
    @FXML private VBox usersPanel;
    @FXML private VBox requestsPanel;
    @FXML private VBox inventoryPanel;
    @FXML private VBox facilitiesPanel;

    // Overview Stats
    @FXML private Label statTotalUsers;
    @FXML private Label statActiveRequests;
    @FXML private Label statTotalDonations;
    @FXML private Label statTotalFacilities;

    // Users Table
    @FXML private TableView<User>             usersTable;
    @FXML private TableColumn<User, Integer>  colUserId;
    @FXML private TableColumn<User, String>   colUsername;
    @FXML private TableColumn<User, String>   colEmail;
    @FXML private TableColumn<User, String>   colRole;
    @FXML private TableColumn<User, String>   colStatus;
    @FXML private TableColumn<User, String>   colCreated;

    // Requests Table
    @FXML private ComboBox<String>                    reqFilterStatus;
    @FXML private TableView<BloodRequest>             reqTable;
    @FXML private TableColumn<BloodRequest, Integer>  colReqId;
    @FXML private TableColumn<BloodRequest, String>   colReqGroup;
    @FXML private TableColumn<BloodRequest, Integer>  colReqQty;
    @FXML private TableColumn<BloodRequest, String>   colReqPriority;
    @FXML private TableColumn<BloodRequest, String>   colReqStatus;
    @FXML private TableColumn<BloodRequest, String>   colReqBy;
    @FXML private TableColumn<BloodRequest, String>   colReqDate;

    // Inventory Table
    @FXML private TableView<BloodInventory>            invTable;
    @FXML private TableColumn<BloodInventory, Integer> colInvId;
    @FXML private TableColumn<BloodInventory, String>  colInvGroup;
    @FXML private TableColumn<BloodInventory, Integer> colInvQty;
    @FXML private TableColumn<BloodInventory, String>  colInvBank;
    @FXML private TableColumn<BloodInventory, String>  colInvCollect;
    @FXML private TableColumn<BloodInventory, String>  colInvExpiry;
    @FXML private TableColumn<BloodInventory, String>  colInvStatus;

    //Dependencies

    private final UserDAO           userDAO      = new UserDAO();
    private final BloodRequestDAO   requestDAO   = new BloodRequestDAO();
    private final BloodInventoryDAO inventoryDAO = new BloodInventoryDAO();
    private final DonationDAO       donationDAO  = new DonationDAO();

    // Initialize

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        User user = sessionManager.getCurrentUser();
        if (user != null) {
            topbarUserName.setText(user.getUsername());
        }

        reqFilterStatus.getSelectionModel().select("ALL");

        setupUsersTable();
        setupRequestsTable();
        setupInventoryTable();

        loadOverviewStats();
    }

    //Panel Switching

    @FXML private void showOverview(ActionEvent e)   { switchPanel("overview"); loadOverviewStats(); }
    @FXML private void showUsers(ActionEvent e)      { switchPanel("users"); loadUsers(); }
    @FXML private void showRequests(ActionEvent e)   { switchPanel("requests"); loadRequests(); }
    @FXML private void showInventory(ActionEvent e)  { switchPanel("inventory"); loadInventory(); }
    @FXML private void showFacilities(ActionEvent e) { switchPanel("facilities"); }

    private void switchPanel(String which) {
        overviewPanel.setVisible(false);
        usersPanel.setVisible(false);
        requestsPanel.setVisible(false);
        inventoryPanel.setVisible(false);
        facilitiesPanel.setVisible(false);

        switch (which) {
            case "overview"   -> { overviewPanel.setVisible(true);   topbarTitle.setText("System Overview"); }
            case "users"      -> { usersPanel.setVisible(true);      topbarTitle.setText("User Management"); }
            case "requests"   -> { requestsPanel.setVisible(true);   topbarTitle.setText("Blood Requests"); }
            case "inventory"  -> { inventoryPanel.setVisible(true);  topbarTitle.setText("Global Inventory"); }
            case "facilities" -> { facilitiesPanel.setVisible(true); topbarTitle.setText("Facilities"); }
        }
    }

    //  Data Loading: Overview

    @FXML
    public void loadOverviewStats() {
        Task<Void> task = new Task<>() {
            @Override protected Void call() {
                // Not ideal to do 4 separate DB calls sequentially, but fine for small scale UI
                int users = userDAO.countAll();
                int pendingReqs = requestDAO.countByStatus("PENDING") + requestDAO.countByStatus("MATCHING");
                int donations = donationDAO.countAll();

                javafx.application.Platform.runLater(() -> {
                    statTotalUsers.setText(String.valueOf(users));
                    statActiveRequests.setText(String.valueOf(pendingReqs));
                    statTotalDonations.setText(String.valueOf(donations));
                    statTotalFacilities.setText("2"); // Hardcoded for now
                });
                return null;
            }
        };
        Thread t = new Thread(task, "admin-stats"); t.setDaemon(true); t.start();
    }

    // Data Loading: Users 

    @FXML
    public void loadUsers() {
        Task<List<User>> task = new Task<>() {
            @Override protected List<User> call() {
                return userDAO.getAllUsers();
            }
        };
        task.setOnSucceeded(e -> {
            ObservableList<User> data = FXCollections.observableArrayList(task.getValue());
            usersTable.setItems(data);
        });
        Thread t = new Thread(task, "admin-users"); t.setDaemon(true); t.start();
    }

    private void setupUsersTable() {
        colUserId.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getUserId()).asObject());
        colUsername.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getUsername()));
        colEmail.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEmail()));
        colRole.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getRole().name()));
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));
        colCreated.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getCreatedAt() != null ? c.getValue().getCreatedAt().toLocalDate().toString() : "—"));
    }

    @FXML
    private void handleDeleteUser(ActionEvent e) {
        User selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        logger.info("Attempting to delete user {}", selected.getUsername());
    }

    @FXML
    private void handleToggleUserStatus(ActionEvent e) {
        User selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        String newStatus = "ACTIVE".equals(selected.getStatus()) ? "SUSPENDED" : "ACTIVE";
        Task<Void> task = new Task<>() {
            @Override protected Void call() {
                userDAO.updateStatus(selected.getUserId(), newStatus);
                return null;
            }
        };
        task.setOnSucceeded(ev -> loadUsers());
        Thread t = new Thread(task, "toggle-status"); t.setDaemon(true); t.start();
    }

    //  Data Loading: Requests

    @FXML
    public void loadRequests() {
        String filter = reqFilterStatus.getValue();
        Task<List<BloodRequest>> task = new Task<>() {
            @Override protected List<BloodRequest> call() {
                if ("ALL".equals(filter) || filter == null) {
                    return requestDAO.findAll();
                } else if ("PENDING".equals(filter) || "MATCHING".equals(filter) || "PARTIALLY_FULFILLED".equals(filter)) {
                
                    return requestDAO.findPending();
                } else {
                    return requestDAO.findAll(); 
                }
            }
        };
        task.setOnSucceeded(e -> {
            ObservableList<BloodRequest> data = FXCollections.observableArrayList(task.getValue());
            if (!"ALL".equals(filter) && filter != null) {
                data.removeIf(r -> !r.getStatus().equals(filter) && !("PENDING".equals(filter) && "MATCHING".equals(r.getStatus())));
            }
            reqTable.setItems(data);
        });
        Thread t = new Thread(task, "admin-req"); t.setDaemon(true); t.start();
    }

    private void setupRequestsTable() {
        colReqId.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getRequestId()).asObject());
        colReqGroup.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getBloodGroup() != null ? c.getValue().getBloodGroup().getLabel() : "—"));
        colReqQty.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getQuantity()).asObject());
        colReqPriority.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPriority()));
        colReqStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));
        colReqBy.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getRequesterName()));
        colReqDate.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getRequestDate() != null ? c.getValue().getRequestDate().toLocalDate().toString() : "—"));
    }

    @FXML
    private void handleUpdateRequestStatus(ActionEvent e) {
        BloodRequest selected = reqTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;  
        String newStatus = "FULFILLED";
        Task<Void> task = new Task<>() {
            @Override protected Void call() {
                requestDAO.updateStatus(selected.getRequestId(), newStatus);
                return null;
            }
        };
        task.setOnSucceeded(ev -> loadRequests());
        Thread t = new Thread(task, "upd-req"); t.setDaemon(true); t.start();
    }

    @FXML
    private void handleExportJson(ActionEvent e) {
        Path exportPath = Path.of("data", "exports", "donors.json");
        JsonDataService.exportDonorsToJson(exportPath);
        logger.info("Donor export requested: {}", exportPath.toAbsolutePath());
        new Alert(Alert.AlertType.INFORMATION,
                "Donor data exported to JSON at: " + exportPath.toAbsolutePath()).showAndWait();
    }

    @FXML
    private void handleImportJson(ActionEvent e) {
        Path importPath = Path.of("data", "imports", "sample-donors.json");
        try {
            JsonDataService.importDonorsFromJson(importPath);
            loadUsers();
            new Alert(Alert.AlertType.INFORMATION,
                    "Sample donor JSON imported into SQLite successfully.").showAndWait();
        } catch (Exception ex) {
            logger.error("JSON import failed via admin action: {}", ex.getMessage(), ex);
            new Alert(Alert.AlertType.ERROR,
                    "JSON import failed: " + ex.getMessage()).showAndWait();
        }
    }

    @FXML
    private void handleExportRequestsJson(ActionEvent e) {
        Path exportPath = Path.of("data", "exports", "blood-requests.json");
        JsonDataService.exportBloodRequestsToJson(exportPath);
        new Alert(Alert.AlertType.INFORMATION,
                "Blood request export completed: " + exportPath.toAbsolutePath()).showAndWait();
    }

    //Data Loading: Inventor

    @FXML
    public void loadInventory() {
        Task<List<BloodInventory>> task = new Task<>() {
            @Override protected List<BloodInventory> call() {
                return inventoryDAO.findAll();
            }
        };
        task.setOnSucceeded(e -> {
            ObservableList<BloodInventory> data = FXCollections.observableArrayList(task.getValue());
            invTable.setItems(data);
        });
        Thread t = new Thread(task, "admin-inv"); t.setDaemon(true); t.start();
    }

    private void setupInventoryTable() {
        colInvId.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getInventoryId()).asObject());
        colInvGroup.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getBloodGroup() != null ? c.getValue().getBloodGroup().getLabel() : "—"));
        colInvQty.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getQuantity()).asObject());
        colInvBank.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getBloodBankName() != null ? c.getValue().getBloodBankName() : "—"));
        colInvCollect.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getCollectionDate() != null ? c.getValue().getCollectionDate().toString() : "—"));
        colInvExpiry.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getExpiryDate() != null ? c.getValue().getExpiryDate().toString() : "—"));
        colInvStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));
    }
}
