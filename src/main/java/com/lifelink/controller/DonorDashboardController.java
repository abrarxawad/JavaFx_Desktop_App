package com.lifelink.controller;

import com.lifelink.dao.BloodRequestDAO;
import com.lifelink.dao.DonationDAO;
import com.lifelink.dao.DonorDAO;
import com.lifelink.model.BloodRequest;
import com.lifelink.model.Donor;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Controller for the Donor Dashboard.
 *
 * <p>Manages 4 panels (Overview, Profile, History, Requests) shown in a StackPane.
 * Only one panel is visible at a time, toggled by sidebar buttons.
 *
 * <p><b>Package:</b> com.lifelink.controller
 */
public class DonorDashboardController extends BaseDashboardController implements Initializable {

    // ── FXML nodes ────────────────────────────────────────────────────────────

    @FXML private Label topbarUserName;
    @FXML private Label topbarTitle;

    // Panels
    @FXML private VBox overviewPanel;
    @FXML private VBox profilePanel;
    @FXML private VBox historyPanel;
    @FXML private VBox requestsPanel;

    // Stat cards
    @FXML private Label statTotalDonations;
    @FXML private Label statEligibility;
    @FXML private Label statLastDonation;
    @FXML private Label statOpenRequests;
    @FXML private Label overviewStatus;

    // Profile summary labels
    @FXML private Label profileName;
    @FXML private Label profileBloodGroup;
    @FXML private Label profileCity;
    @FXML private Label profilePhone;

    // Profile edit fields
    @FXML private TextField editFirstName;
    @FXML private TextField editLastName;
    @FXML private TextField editPhone;
    @FXML private TextField editCity;
    @FXML private TextField editAddress;
    @FXML private CheckBox  editAvailability;
    @FXML private Label     profileStatus;

    // Donation history table
    @FXML private TableView<DonationDAO.DonationRecord> historyTable;
    @FXML private TableColumn<DonationDAO.DonationRecord, String> colDonDate;
    @FXML private TableColumn<DonationDAO.DonationRecord, String> colDonGroup;
    @FXML private TableColumn<DonationDAO.DonationRecord, Integer> colDonQty;
    @FXML private TableColumn<DonationDAO.DonationRecord, String> colDonBank;
    @FXML private TableColumn<DonationDAO.DonationRecord, String> colDonStatus;

    // Blood requests table
    @FXML private TableView<BloodRequest> requestsTable;
    @FXML private TableColumn<BloodRequest, Integer> colReqId;
    @FXML private TableColumn<BloodRequest, String>  colReqGroup;
    @FXML private TableColumn<BloodRequest, Integer> colReqQty;
    @FXML private TableColumn<BloodRequest, String>  colReqPriority;
    @FXML private TableColumn<BloodRequest, String>  colReqStatus;
    @FXML private TableColumn<BloodRequest, String>  colReqBy;
    @FXML private TableColumn<BloodRequest, String>  colReqDate;

    // ── Dependencies ──────────────────────────────────────────────────────────

    private static final Logger logger = LoggerFactory.getLogger(DonorDashboardController.class);

    private final DonorDAO         donorDAO     = new DonorDAO();
    private final DonationDAO      donationDAO  = new DonationDAO();
    private final BloodRequestDAO  requestDAO   = new BloodRequestDAO();

    private Donor currentDonor;

    // ── Initialise ────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        User user = sessionManager.getCurrentUser();
        if (user != null) {
            topbarUserName.setText(user.getUsername());
        }

        setupHistoryTable();
        setupRequestsTable();
        requestsTable.setRowFactory(tv -> {
            TableRow<BloodRequest> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getClickCount() == 2) {
                    openRequestActionDialog(row.getItem());
                }
            });
            return row;
        });
        loadDonorData();
    }

    // ── Panel switching ───────────────────────────────────────────────────────

    @FXML private void showOverview(ActionEvent e)  { switchPanel("overview"); }
    @FXML private void showProfile(ActionEvent e)   { switchPanel("profile"); }
    @FXML private void showHistory(ActionEvent e)   { switchPanel("history"); loadHistory(); }
    @FXML private void showRequests(ActionEvent e)  { switchPanel("requests"); loadRequests(); }

    private void switchPanel(String which) {
        overviewPanel.setVisible(false);
        profilePanel.setVisible(false);
        historyPanel.setVisible(false);
        requestsPanel.setVisible(false);

        switch (which) {
            case "overview"  -> { overviewPanel.setVisible(true);  topbarTitle.setText("Overview"); }
            case "profile"   -> { profilePanel.setVisible(true);   topbarTitle.setText("My Profile"); populateProfileForm(); }
            case "history"   -> { historyPanel.setVisible(true);   topbarTitle.setText("Donation History"); }
            case "requests"  -> { requestsPanel.setVisible(true);  topbarTitle.setText("Blood Requests"); }
        }
    }

    // ── Data loading ──────────────────────────────────────────────────────────

    private void loadDonorData() {
        User user = sessionManager.getCurrentUser();
        if (user == null) return;

        Task<Optional<Donor>> task = new Task<>() {
            @Override protected Optional<Donor> call() {
                return donorDAO.findByUserId(user.getUserId());
            }
        };

        task.setOnSucceeded(e -> {
            Optional<Donor> opt = task.getValue();
            if (opt.isPresent()) {
                currentDonor = opt.get();
                updateStatCards();
                profileName.setText(currentDonor.getFirstName() + " " + currentDonor.getLastName());
                profileBloodGroup.setText(currentDonor.getBloodGroup() != null ? currentDonor.getBloodGroup().getLabel() : "—");
                profileCity.setText(currentDonor.getCity() != null ? currentDonor.getCity() : "—");
                profilePhone.setText(currentDonor.getPhone() != null ? currentDonor.getPhone() : "—");
            } else {
                overviewStatus.setText("⚠ Donor profile not found. Please contact admin.");
                profileName.setText(user.getUsername());
            }
        });

        task.setOnFailed(e -> {
            logger.error("Failed to load donor data: {}", task.getException().getMessage());
            overviewStatus.setText("Failed to load donor data. Check database connection.");
        });

        Thread t = new Thread(task, "load-donor"); t.setDaemon(true); t.start();
    }

    private void updateStatCards() {
        if (currentDonor == null) return;
        statTotalDonations.setText(String.valueOf(currentDonor.getTotalDonations()));
        statEligibility.setText(currentDonor.getEligibilityStatus() != null ? currentDonor.getEligibilityStatus() : "UNKNOWN");
        statLastDonation.setText(currentDonor.getLastDonationDate() != null
                ? currentDonor.getLastDonationDate().toString() : "Never");

        // Count open requests matching this donor's blood group
        if (currentDonor.getBloodGroup() != null) {
            try {
                int count = requestDAO.findByBloodGroup(currentDonor.getBloodGroup().name()).size();
                statOpenRequests.setText(String.valueOf(count));
            } catch (Exception ignored) {}
        }
    }

    private void loadHistory() {
        if (currentDonor == null) return;
        Task<List<DonationDAO.DonationRecord>> task = new Task<>() {
            @Override protected List<DonationDAO.DonationRecord> call() {
                return donationDAO.findByDonor(currentDonor.getDonorId());
            }
        };
        task.setOnSucceeded(e -> {
            ObservableList<DonationDAO.DonationRecord> data = FXCollections.observableArrayList(task.getValue());
            historyTable.setItems(data);
        });
        task.setOnFailed(e -> logger.error("Failed to load history: {}", task.getException().getMessage()));
        Thread t = new Thread(task, "load-history"); t.setDaemon(true); t.start();
    }

    private void loadRequests() {
        if (currentDonor == null || currentDonor.getBloodGroup() == null) return;
        Task<List<BloodRequest>> task = new Task<>() {
            @Override protected List<BloodRequest> call() {
                return requestDAO.findByBloodGroup(currentDonor.getBloodGroup().name());
            }
        };
        task.setOnSucceeded(e -> {
            ObservableList<BloodRequest> data = FXCollections.observableArrayList(task.getValue());
            requestsTable.setItems(data);
        });
        task.setOnFailed(e -> logger.error("Failed to load requests: {}", task.getException().getMessage()));
        Thread t = new Thread(task, "load-requests"); t.setDaemon(true); t.start();
    }

    // ── Table setup ───────────────────────────────────────────────────────────

    private void setupHistoryTable() {
        colDonDate.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().donationDate != null ? c.getValue().donationDate.toString() : "—"));
        colDonGroup.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().bloodGroup != null ? c.getValue().bloodGroup : "—"));
        colDonQty.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().quantityMl).asObject());
        colDonBank.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().bankName != null ? c.getValue().bankName : "—"));
        colDonStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().status));
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

    private void openRequestActionDialog(BloodRequest request) {
        if (request == null) return;

        String details = "Request ID: " + request.getRequestId() + "\n"
                + "Requested By: " + (request.getRequesterName() != null ? request.getRequesterName() : "Unknown") + "\n"
                + "Blood Group: " + (request.getBloodGroup() != null ? request.getBloodGroup().getLabel() : "—") + "\n"
                + "Quantity: " + request.getQuantity() + " units\n"
                + "Location: " + (request.getRequesterLocation() != null && !request.getRequesterLocation().isBlank() ? request.getRequesterLocation() : "Not provided") + "\n"
                + "Hospital: " + (request.getHospitalName() != null && !request.getHospitalName().isBlank() ? request.getHospitalName() : "Not provided") + "\n"
                + "Notes: " + (request.getNotes() != null ? request.getNotes() : "—");

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Incoming Blood Request");
        alert.setHeaderText("Request from " + (request.getRequesterName() != null ? request.getRequesterName() : "a donor match"));
        alert.setContentText(details);

        ButtonType accept = new ButtonType("Accept Request");
        ButtonType markDone = new ButtonType("Mark Donation Done");
        ButtonType dismiss = new ButtonType("Dismiss Request");

        if (BloodRequest.STATUS_PENDING.equalsIgnoreCase(request.getStatus())
                || BloodRequest.STATUS_MATCHING.equalsIgnoreCase(request.getStatus())) {
            alert.getButtonTypes().setAll(accept, markDone, dismiss, ButtonType.CLOSE);
        } else if (BloodRequest.STATUS_AWAITING_CONFIRMATION.equalsIgnoreCase(request.getStatus())) {
            alert.getButtonTypes().setAll(markDone, dismiss, ButtonType.CLOSE);
        } else {
            alert.getButtonTypes().setAll(ButtonType.CLOSE);
        }

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isEmpty()) return;

        if (result.get() == accept) {
            requestDAO.updateStatus(request.getRequestId(), BloodRequest.STATUS_MATCHING);
            showRequests(null);
        } else if (result.get() == markDone) {
            requestDAO.updateStatus(request.getRequestId(), BloodRequest.STATUS_AWAITING_CONFIRMATION);
            DonationDAO.DonationRecord rec = new DonationDAO.DonationRecord();
            rec.donorId = currentDonor.getDonorId();
            rec.bloodGroup = currentDonor.getBloodGroup() != null ? currentDonor.getBloodGroup().name() : "O_POSITIVE";
            rec.donationDate = LocalDate.now();
            rec.quantityMl = 450;
            rec.status = "PENDING_CONFIRMATION";
            rec.bloodBankId = 1;
            rec.requestId = request.getRequestId();
            donationDAO.recordDonation(rec);
            showRequests(null);
            loadHistory();
        } else if (result.get() == dismiss) {
            requestDAO.updateStatus(request.getRequestId(), BloodRequest.STATUS_CANCELLED);
            showRequests(null);
        }
    }

    // ── Profile editing ───────────────────────────────────────────────────────

    private void populateProfileForm() {
        if (currentDonor == null) return;
        editFirstName.setText(currentDonor.getFirstName() != null ? currentDonor.getFirstName() : "");
        editLastName.setText(currentDonor.getLastName() != null ? currentDonor.getLastName() : "");
        editPhone.setText(currentDonor.getPhone() != null ? currentDonor.getPhone() : "");
        editCity.setText(currentDonor.getCity() != null ? currentDonor.getCity() : "");
        editAddress.setText(currentDonor.getAddress() != null ? currentDonor.getAddress() : "");
        editAvailability.setSelected(currentDonor.isAvailable());
    }

    @FXML
    private void handleSaveProfile(ActionEvent event) {
        if (currentDonor == null) {
            profileStatus.setStyle("-fx-text-fill: #fc8181;");
            profileStatus.setText("No donor profile loaded.");
            return;
        }
        currentDonor.setFirstName(editFirstName.getText().trim());
        currentDonor.setLastName(editLastName.getText().trim());
        currentDonor.setPhone(editPhone.getText().trim());
        currentDonor.setCity(editCity.getText().trim());
        currentDonor.setAddress(editAddress.getText().trim());
        currentDonor.setAvailable(editAvailability.isSelected());

        Task<Void> task = new Task<>() {
            @Override protected Void call() {
                donorDAO.update(currentDonor);
                return null;
            }
        };
        task.setOnSucceeded(e -> {
            profileStatus.setStyle("-fx-text-fill: #68d391;");
            profileStatus.setText("✅ Profile updated successfully.");
            loadDonorData();
        });
        task.setOnFailed(e -> {
            profileStatus.setStyle("-fx-text-fill: #fc8181;");
            profileStatus.setText("❌ Failed to save: " + task.getException().getMessage());
        });
        Thread t = new Thread(task, "save-profile"); t.setDaemon(true); t.start();
    }

    @FXML
    private void handleRecordDonation(ActionEvent event) {
        if (currentDonor == null) return;
        // Simple dialog to record a donation
        Dialog<DonationDAO.DonationRecord> dialog = new Dialog<>();
        dialog.setTitle("Record Donation");
        dialog.setHeaderText("Record a new blood donation");

        ButtonType saveBtn = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20));
        grid.setStyle("-fx-background-color: #1e2235;");

        DatePicker datePicker = new DatePicker(LocalDate.now());
        TextField bankField = new TextField("City Central Blood Bank");
        Label dateLabel = new Label("Donation Date:"); dateLabel.setStyle("-fx-text-fill:#f0f4ff;");
        Label bankLabel = new Label("Blood Bank:"); bankLabel.setStyle("-fx-text-fill:#f0f4ff;");

        grid.add(dateLabel, 0, 0); grid.add(datePicker, 1, 0);
        grid.add(bankLabel, 0, 1); grid.add(bankField, 1, 1);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setStyle("-fx-background-color:#1e2235;");

        dialog.setResultConverter(bt -> {
            if (bt == saveBtn) {
                DonationDAO.DonationRecord rec = new DonationDAO.DonationRecord();
                rec.donorId = currentDonor.getDonorId();
                rec.bloodGroup = currentDonor.getBloodGroup() != null ? currentDonor.getBloodGroup().name() : "O_POSITIVE";
                rec.donationDate = datePicker.getValue();
                rec.quantityMl = 450;
                rec.status = "COMPLETED";
                rec.bloodBankId = 1; // default bank
                return rec;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(rec -> {
            Task<Void> task = new Task<>() {
                @Override protected Void call() {
                    donationDAO.recordDonation(rec);
                    currentDonor.setTotalDonations(currentDonor.getTotalDonations() + 1);
                    currentDonor.setLastDonationDate(rec.donationDate);
                    donorDAO.update(currentDonor);
                    return null;
                }
            };
            task.setOnSucceeded(e -> { loadHistory(); loadDonorData(); });
            task.setOnFailed(e -> logger.error("Failed to record donation: {}", task.getException().getMessage()));
            Thread t = new Thread(task, "rec-donation"); t.setDaemon(true); t.start();
        });
    }
}
