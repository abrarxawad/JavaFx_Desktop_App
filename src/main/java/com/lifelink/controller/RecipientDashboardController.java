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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for the Recipient Dashboard.
 *
 * <p>Allows recipients to submit blood requests, track their history,
 * and view a summary of stats on the overview panel.
 *
 * <p><b>Package:</b> com.lifelink.controller
 */
public class RecipientDashboardController extends BaseDashboardController implements Initializable {

    //  FXML nodes 
    @FXML private Label topbarUserName;
    @FXML private Label topbarTitle;

    // Panels
    @FXML private VBox overviewPanel;
    @FXML private VBox newRequestPanel;
    @FXML private VBox myRequestsPanel;

    // Stats
    @FXML private Label statTotal;
    @FXML private Label statPending;
    @FXML private Label statFulfilled;
    @FXML private Label statEmergency;
    @FXML private Label overviewStatus;

    // New request form
    @FXML private ComboBox<String> reqBloodGroup;
    @FXML private TextField reqQuantity;
    @FXML private TextField reqLocation;
    @FXML private TextField reqHospital;
    @FXML private ComboBox<String> reqPriority;
    @FXML private TextArea  reqNotes;
    @FXML private Label     requestFormStatus;

    // Requests table
    @FXML private TableView<BloodRequest> requestsTable;
    @FXML private TableColumn<BloodRequest, Integer> colId;
    @FXML private TableColumn<BloodRequest, String>  colGroup;
    @FXML private TableColumn<BloodRequest, Integer> colQty;
    @FXML private TableColumn<BloodRequest, String>  colPriority;
    @FXML private TableColumn<BloodRequest, String>  colStatus;
    @FXML private TableColumn<BloodRequest, String>  colNotes;
    @FXML private TableColumn<BloodRequest, String>  colDate;

    // ── Dependencies 
    private final BloodRequestDAO requestDAO = new BloodRequestDAO();

    // ── Initialise 
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        User user = sessionManager.getCurrentUser();
        if (user != null) topbarUserName.setText(user.getUsername());

        reqPriority.getSelectionModel().select("NORMAL");
        reqBloodGroup.getSelectionModel().select(0);

        requestsTable.setRowFactory(tv -> {
            TableRow<BloodRequest> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getClickCount() == 2) {
                    openRequestDecisionDialog(row.getItem());
                }
            });
            return row;
        });

        setupTable();
        loadStats();
    }

    //  Panel switching 
    @FXML private void showOverview(ActionEvent e)    { switchPanel("overview"); loadStats(); }
    @FXML private void showNewRequest(ActionEvent e)  { switchPanel("new"); resetForm(); }
    @FXML private void showMyRequests(ActionEvent e)  { switchPanel("requests"); loadRequests(); }

    private void switchPanel(String which) {
        overviewPanel.setVisible(false);
        newRequestPanel.setVisible(false);
        myRequestsPanel.setVisible(false);
        switch (which) {
            case "overview"  -> { overviewPanel.setVisible(true);    topbarTitle.setText("Dashboard"); }
            case "new"       -> { newRequestPanel.setVisible(true);  topbarTitle.setText("New Blood Request"); }
            case "requests"  -> { myRequestsPanel.setVisible(true);  topbarTitle.setText("My Requests"); }
        }
    }

    //  Data loading 

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
            long pending    = list.stream().filter(r -> "PENDING".equals(r.getStatus()) || "MATCHING".equals(r.getStatus()) || "AWAITING_CONFIRMATION".equals(r.getStatus())).count();
            long fulfilled  = list.stream().filter(r -> "FULFILLED".equals(r.getStatus())).count();
            long emergency  = list.stream().filter(r -> "EMERGENCY".equals(r.getPriority())).count();
            statPending.setText(String.valueOf(pending));
            statFulfilled.setText(String.valueOf(fulfilled));
            statEmergency.setText(String.valueOf(emergency));
        });
        task.setOnFailed(e -> logger.error("Failed to load stats: {}", task.getException().getMessage()));
        Thread t = new Thread(task, "load-stats"); t.setDaemon(true); t.start();
    }

    private void loadRequests() {
        User user = sessionManager.getCurrentUser();
        if (user == null) return;
        Task<List<BloodRequest>> task = new Task<>() {
            @Override protected List<BloodRequest> call() {
                return requestDAO.findByRequesterId(user.getUserId());
            }
        };
        task.setOnSucceeded(e -> {
            ObservableList<BloodRequest> data = FXCollections.observableArrayList(task.getValue());
            requestsTable.setItems(data);
        });
        task.setOnFailed(e -> logger.error("Failed to load requests: {}", task.getException().getMessage()));
        Thread t = new Thread(task, "load-req"); t.setDaemon(true); t.start();
    }

    // ── Submit request 

    @FXML
    private void handleSubmitRequest(ActionEvent event) {
        User user = sessionManager.getCurrentUser();
        if (user == null) return;

        String bgStr = reqBloodGroup.getValue();
        String qtyStr = reqQuantity.getText().trim();
        String priority = reqPriority.getValue();

        // Validation
        if (bgStr == null || bgStr.isEmpty()) {
            showFormError("Please select a blood group.");
            return;
        }
        if (qtyStr.isEmpty()) {
            showFormError("Please enter the quantity needed.");
            return;
        }
        int qty;
        try {
            qty = Integer.parseInt(qtyStr);
            if (qty < 1) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            showFormError("Quantity must be a positive number.");
            return;
        }
        if (priority == null) {
            showFormError("Please select a priority level.");
            return;
        }

        BloodGroup bg;
        try {
            bg = BloodGroup.valueOf(bgStr);
        } catch (IllegalArgumentException ex) {
            showFormError("Invalid blood group selected.");
            return;
        }

        String location = reqLocation.getText() == null ? "" : reqLocation.getText().trim();
        String hospital = reqHospital.getText() == null ? "" : reqHospital.getText().trim();

        BloodRequest request = new BloodRequest.Builder()
                .requesterId(user.getUserId())
                .requesterName(user.getUsername())
                .requesterType("RECIPIENT")
                .requesterLocation(location)
                .requesterCity(location)
                .hospitalName(hospital)
                .bloodGroup(bg)
                .quantity(qty)
                .priority(priority)
                .notes(reqNotes.getText().trim())
                .build();

        Task<Integer> task = new Task<>() {
            @Override protected Integer call() {
                return requestDAO.create(request);
            }
        };
        task.setOnSucceeded(e -> {
            requestFormStatus.setStyle("-fx-text-fill: #68d391;");
            requestFormStatus.setText("✅ Request #" + task.getValue() + " submitted successfully!");
            resetForm();
            loadStats();
        });
        task.setOnFailed(e -> {
            requestFormStatus.setStyle("-fx-text-fill: #fc8181;");
            requestFormStatus.setText("❌ Failed to submit request: " + task.getException().getMessage());
            logger.error("Request submission failed: {}", task.getException().getMessage());
        });
        Thread t = new Thread(task, "submit-req"); t.setDaemon(true); t.start();
    }

    //  Table setup 

    private void setupTable() {
        colId.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getRequestId()).asObject());
        colGroup.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getBloodGroup() != null ? c.getValue().getBloodGroup().getLabel() : "—"));
        colQty.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getQuantity()).asObject());
        colPriority.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPriority()));
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));
        colNotes.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getNotes() != null ? c.getValue().getNotes() : ""));
        colDate.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getRequestDate() != null ? c.getValue().getRequestDate().toLocalDate().toString() : "—"));
    }

    //  Helpers 

    private void showFormError(String msg) {
        requestFormStatus.setStyle("-fx-text-fill: #fc8181;");
        requestFormStatus.setText(msg);
    }

    private void resetForm() {
        reqBloodGroup.getSelectionModel().select(0);
        reqQuantity.setText("1");
        reqLocation.clear();
        reqHospital.clear();
        reqPriority.getSelectionModel().select("NORMAL");
        reqNotes.clear();
        requestFormStatus.setText("");
    }

    private void openRequestDecisionDialog(BloodRequest request) {
        if (request == null) return;

        String details = "Request ID: " + request.getRequestId() + "\n"
                + "Blood Group: " + (request.getBloodGroup() != null ? request.getBloodGroup().getLabel() : "—") + "\n"
                + "Quantity: " + request.getQuantity() + " units\n"
                + "Location: " + (request.getRequesterLocation() != null && !request.getRequesterLocation().isBlank() ? request.getRequesterLocation() : "Not provided") + "\n"
                + "Hospital: " + (request.getHospitalName() != null && !request.getHospitalName().isBlank() ? request.getHospitalName() : "Not provided") + "\n"
                + "Status: " + request.getStatus() + "\n"
                + "Notes: " + (request.getNotes() != null ? request.getNotes() : "—");

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Request Details");
        alert.setHeaderText("Blood request from " + (request.getRequesterName() != null ? request.getRequesterName() : "requester"));
        alert.setContentText(details);

        if (BloodRequest.STATUS_AWAITING_CONFIRMATION.equalsIgnoreCase(request.getStatus())) {
            ButtonType accept = new ButtonType("Accept Completed Donation");
            ButtonType dismiss = new ButtonType("Dismiss");
            alert.getButtonTypes().setAll(accept, dismiss, ButtonType.CLOSE);
            alert.showAndWait().ifPresent(type -> {
                if (type == accept) {
                    requestDAO.updateStatus(request.getRequestId(), BloodRequest.STATUS_FULFILLED);
                    loadRequests();
                    loadStats();
                } else if (type == dismiss) {
                    requestDAO.updateStatus(request.getRequestId(), BloodRequest.STATUS_CANCELLED);
                    loadRequests();
                    loadStats();
                }
            });
        } else {
            ButtonType ok = new ButtonType("Close");
            alert.getButtonTypes().setAll(ok);
            alert.show();
        }
    }
}
