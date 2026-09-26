package com.lifelink.controller;

import com.lifelink.dao.BloodInventoryDAO;
import com.lifelink.dao.BloodRequestDAO;
import com.lifelink.model.BloodGroup;
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

import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/*
 Controller for the Blood Bank Dashboard.
 */
public class BloodBankDashboardController extends BaseDashboardController implements Initializable {

    // FXML nodes 
    @FXML private Label topbarTitle;
    @FXML private Label topbarUserName;

    // Panels
    @FXML private VBox overviewPanel;
    @FXML private VBox inventoryPanel;
    @FXML private VBox addStockPanel;
    @FXML private VBox requestsPanel;

    // Inventory summary labels
    @FXML private Label invAPos, invANeg, invBPos, invBNeg;
    @FXML private Label invABPos, invABNeg, invOPos, invONeg;
    @FXML private Label overviewStatus;

    // Add stock form
    @FXML private ComboBox<String> addGroup;
    @FXML private TextField        addQuantity;
    @FXML private DatePicker       addCollectionDate;
    @FXML private DatePicker       addExpiryDate;
    @FXML private Label            addStockStatus;

    // Inventory table
    @FXML private TableView<BloodInventory>               inventoryTable;
    @FXML private TableColumn<BloodInventory, Integer>    colInvId;
    @FXML private TableColumn<BloodInventory, String>     colInvGroup;
    @FXML private TableColumn<BloodInventory, Integer>    colInvQty;
    @FXML private TableColumn<BloodInventory, String>     colInvCollect;
    @FXML private TableColumn<BloodInventory, String>     colInvExpiry;
    @FXML private TableColumn<BloodInventory, String>     colInvStatus;

    // Requests table
    @FXML private TableView<BloodRequest>             reqTable;
    @FXML private TableColumn<BloodRequest, Integer>  colReqId;
    @FXML private TableColumn<BloodRequest, String>   colReqGroup;
    @FXML private TableColumn<BloodRequest, Integer>  colReqQty;
    @FXML private TableColumn<BloodRequest, String>   colReqPriority;
    @FXML private TableColumn<BloodRequest, String>   colReqStatus;
    @FXML private TableColumn<BloodRequest, String>   colReqBy;
    @FXML private TableColumn<BloodRequest, String>   colReqDate;

    //  Dependencies
    private final BloodInventoryDAO inventoryDAO = new BloodInventoryDAO();
    private final BloodRequestDAO   requestDAO   = new BloodRequestDAO();

    // The blood_bank_id associated with this user (default 1 for seed bank)
    private int bloodBankId = 1;

    // Initialize

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        User user = sessionManager.getCurrentUser();
        if (user != null) topbarUserName.setText(user.getUsername());

        addCollectionDate.setValue(LocalDate.now());
        addExpiryDate.setValue(LocalDate.now().plusDays(42));
        addGroup.getSelectionModel().select(0);

        setupInventoryTable();
        setupRequestsTable();
        reqTable.setOnMouseClicked(event -> {
            if (event.getClickCount() >= 2) {
                BloodRequest selected = reqTable.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    openRequestDecisionDialog(selected);
                }
            }
        });
        loadOverview();
    }

    //Panel switching 

    @FXML private void showOverview(ActionEvent e)   { switchPanel("overview");   loadOverview(); }
    @FXML private void showInventory(ActionEvent e)  { switchPanel("inventory");  loadInventoryTable(); }
    @FXML private void showAddStock(ActionEvent e)   { switchPanel("addstock"); }
    @FXML private void showRequests(ActionEvent e)   { switchPanel("requests");   loadRequests(); }

    private void switchPanel(String which) {
        overviewPanel.setVisible(false);
        inventoryPanel.setVisible(false);
        addStockPanel.setVisible(false);
        requestsPanel.setVisible(false);
        switch (which) {
            case "overview"  -> { overviewPanel.setVisible(true);   topbarTitle.setText("Inventory Overview"); }
            case "inventory" -> { inventoryPanel.setVisible(true);  topbarTitle.setText("Blood Inventory"); }
            case "addstock"  -> { addStockPanel.setVisible(true);   topbarTitle.setText("Add Inventory"); }
            case "requests"  -> { requestsPanel.setVisible(true);   topbarTitle.setText("Blood Requests"); }
        }
    }

    // Data loading 

    @FXML
    public void loadOverview() {
        Task<Map<String, Integer>> task = new Task<>() {
            @Override protected Map<String, Integer> call() {
                return inventoryDAO.getTotalAvailableByBank(bloodBankId);
            }
        };
        task.setOnSucceeded(e -> {
            Map<String, Integer> totals = task.getValue();
            invAPos.setText(String.valueOf(totals.getOrDefault("A_POSITIVE",  0)));
            invANeg.setText(String.valueOf(totals.getOrDefault("A_NEGATIVE",  0)));
            invBPos.setText(String.valueOf(totals.getOrDefault("B_POSITIVE",  0)));
            invBNeg.setText(String.valueOf(totals.getOrDefault("B_NEGATIVE",  0)));
            invABPos.setText(String.valueOf(totals.getOrDefault("AB_POSITIVE", 0)));
            invABNeg.setText(String.valueOf(totals.getOrDefault("AB_NEGATIVE", 0)));
            invOPos.setText(String.valueOf(totals.getOrDefault("O_POSITIVE",  0)));
            invONeg.setText(String.valueOf(totals.getOrDefault("O_NEGATIVE",  0)));
            overviewStatus.setText("Last refreshed: " + java.time.LocalTime.now().withNano(0));
        });
        task.setOnFailed(e -> overviewStatus.setText("Failed to load inventory data."));
        Thread t = new Thread(task, "load-overview"); t.setDaemon(true); t.start();
    }

    private void loadInventoryTable() {
        Task<List<BloodInventory>> task = new Task<>() {
            @Override protected List<BloodInventory> call() {
                return inventoryDAO.findByBank(bloodBankId);
            }
        };
        task.setOnSucceeded(e -> {
            ObservableList<BloodInventory> data = FXCollections.observableArrayList(task.getValue());
            inventoryTable.setItems(data);
        });
        task.setOnFailed(e -> logger.error("Failed to load inventory: {}", task.getException().getMessage()));
        Thread t = new Thread(task, "load-inv"); t.setDaemon(true); t.start();
    }

    private void loadRequests() {
        Task<List<BloodRequest>> task = new Task<>() {
            @Override protected List<BloodRequest> call() {
                return requestDAO.findPending();
            }
        };
        task.setOnSucceeded(e -> {
            ObservableList<BloodRequest> data = FXCollections.observableArrayList(task.getValue());
            reqTable.setItems(data);
        });
        task.setOnFailed(e -> logger.error("Failed to load requests: {}", task.getException().getMessage()));
        Thread t = new Thread(task, "load-req"); t.setDaemon(true); t.start();
    }

    // Add stock 

    @FXML
    private void handleAddStock(ActionEvent event) {
        String groupStr   = addGroup.getValue();
        String qtyStr     = addQuantity.getText().trim();
        LocalDate collect = addCollectionDate.getValue();
        LocalDate expiry  = addExpiryDate.getValue();

        if (groupStr == null || qtyStr.isEmpty() || collect == null || expiry == null) {
            addStockStatus.setStyle("-fx-text-fill: #fc8181;");
            addStockStatus.setText("All fields are required.");
            return;
        }
        int qty;
        try {
            qty = Integer.parseInt(qtyStr);
            if (qty < 1) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            addStockStatus.setStyle("-fx-text-fill: #fc8181;");
            addStockStatus.setText("Quantity must be a positive number.");
            return;
        }
        if (expiry.isBefore(collect)) {
            addStockStatus.setStyle("-fx-text-fill: #fc8181;");
            addStockStatus.setText("Expiry date must be after collection date.");
            return;
        }

        BloodInventory inv = new BloodInventory();
        inv.setBloodBankId(bloodBankId);
        try { inv.setBloodGroup(BloodGroup.valueOf(groupStr)); } catch (IllegalArgumentException ignored) {}
        inv.setQuantity(qty);
        inv.setCollectionDate(collect);
        inv.setExpiryDate(expiry);
        inv.setStatus(BloodInventory.STATUS_AVAILABLE);

        Task<Integer> task = new Task<>() {
            @Override protected Integer call() { return inventoryDAO.addInventory(inv); }
        };
        task.setOnSucceeded(e -> {
            addStockStatus.setStyle("-fx-text-fill: #68d391;");
            addStockStatus.setText("✅ Added " + qty + " units of " + groupStr + " successfully!");
            loadOverview();
        });
        task.setOnFailed(e -> {
            addStockStatus.setStyle("-fx-text-fill: #fc8181;");
            addStockStatus.setText("❌ Failed: " + task.getException().getMessage());
        });
        Thread t = new Thread(task, "add-stock"); t.setDaemon(true); t.start();
    }

    @FXML
    private void handleMarkExpired(ActionEvent event) {
        Task<Integer> task = new Task<>() {
            @Override protected Integer call() { return inventoryDAO.markExpired(); }
        };
        task.setOnSucceeded(e -> {
            int count = task.getValue();
            logger.info("Marked {} inventory entries as expired", count);
            loadInventoryTable();
            loadOverview();
        });
        task.setOnFailed(e -> logger.error("Failed to mark expired: {}", task.getException().getMessage()));
        Thread t = new Thread(task, "mark-expired"); t.setDaemon(true); t.start();
    }

    // Table setup 

    private void openRequestDecisionDialog(BloodRequest request) {
        if (request == null) return;

        String details = "Request ID: " + request.getRequestId() + "\n"
                + "Requester: " + (request.getRequesterName() != null ? request.getRequesterName() : "Unknown") + "\n"
                + "Type: " + (request.getRequesterType() != null ? request.getRequesterType() : "RECIPIENT") + "\n"
                + "Blood Group: " + (request.getBloodGroup() != null ? request.getBloodGroup().getLabel() : "—") + "\n"
                + "Quantity: " + request.getQuantity() + " units\n"
                + "Location: " + (request.getRequesterLocation() != null && !request.getRequesterLocation().isBlank() ? request.getRequesterLocation() : "Not provided") + "\n"
                + "Hospital: " + (request.getHospitalName() != null && !request.getHospitalName().isBlank() ? request.getHospitalName() : "Not provided") + "\n"
                + "Status: " + request.getStatus() + "\n"
                + "Notes: " + (request.getNotes() != null ? request.getNotes() : "—");

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Blood Bank Request Match");
        alert.setHeaderText("Blood request from " + (request.getRequesterName() != null ? request.getRequesterName() : "requester"));
        alert.setContentText(details);

        ButtonType accept = new ButtonType("Accept & Reserve Stock");
        ButtonType complete = new ButtonType("Mark Donation Completed");
        ButtonType dismiss = new ButtonType("Dismiss");

        if (BloodRequest.STATUS_AWAITING_CONFIRMATION.equalsIgnoreCase(request.getStatus())) {
            alert.getButtonTypes().setAll(complete, dismiss, ButtonType.CLOSE);
        } else if (BloodRequest.STATUS_PENDING.equalsIgnoreCase(request.getStatus())
                || BloodRequest.STATUS_MATCHING.equalsIgnoreCase(request.getStatus())) {
            alert.getButtonTypes().setAll(accept, dismiss, ButtonType.CLOSE);
        } else {
            alert.getButtonTypes().setAll(ButtonType.CLOSE);
        }

        alert.showAndWait().ifPresent(type -> {
            if (type == accept) {
                if (request.getBloodGroup() == null) {
                    new Alert(Alert.AlertType.ERROR, "This request does not have a valid blood group.").showAndWait();
                    return;
                }
                try {
                    inventoryDAO.consumeAvailableStock(bloodBankId, request.getBloodGroup(), request.getQuantity());
                    requestDAO.updateStatus(request.getRequestId(), BloodRequest.STATUS_MATCHING);
                    loadOverview();
                    loadRequests();
                    new Alert(Alert.AlertType.INFORMATION, "Blood bank matched the request and reserved stock.").showAndWait();
                } catch (Exception ex) {
                    new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait();
                }
            } else if (type == complete) {
                requestDAO.updateStatus(request.getRequestId(), BloodRequest.STATUS_FULFILLED);
                loadRequests();
                loadOverview();
                new Alert(Alert.AlertType.INFORMATION, "The request has been marked as completed for the blood bank and donor records.").showAndWait();
            } else if (type == dismiss) {
                requestDAO.updateStatus(request.getRequestId(), BloodRequest.STATUS_CANCELLED);
                loadRequests();
            }
        });
    }

    private void setupInventoryTable() {
        colInvId.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getInventoryId()).asObject());
        colInvGroup.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getBloodGroup() != null ? c.getValue().getBloodGroup().getLabel() : "—"));
        colInvQty.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getQuantity()).asObject());
        colInvCollect.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getCollectionDate() != null ? c.getValue().getCollectionDate().toString() : "—"));
        colInvExpiry.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getExpiryDate() != null ? c.getValue().getExpiryDate().toString() : "—"));
        colInvStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));
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
}
