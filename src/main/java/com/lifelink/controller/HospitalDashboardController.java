package com.lifelink.controller;

import com.lifelink.api.DirectoryApiService;
import com.lifelink.api.DonorSearchApiService;
import com.lifelink.api.HospitalApiService;
import com.lifelink.api.HospitalDTO;
import com.lifelink.api.LocationApiService;
import com.lifelink.api.NominatimLocationDTO;
import com.lifelink.dao.BloodRequestDAO;
import com.lifelink.model.BloodGroup;
import com.lifelink.model.BloodRequest;
import com.lifelink.model.Donor;
import com.lifelink.model.User;
import com.lifelink.util.HaversineCalculator;
import javafx.beans.property.SimpleDoubleProperty;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/*
 Controller for the Hospital Dashboard.
 */
public class HospitalDashboardController extends BaseDashboardController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(HospitalDashboardController.class);

    // FXML
    @FXML private Label topbarTitle;
    @FXML private Label topbarUserName;

    @FXML private VBox overviewPanel;
    @FXML private VBox emergencyPanel;
    @FXML private VBox historyPanel;
    @FXML private VBox nearbyFacilitiesPanel;
    @FXML private VBox donorSearchPanel;

    // Search nearby hospitals
    @FXML private TextField searchLocationField;
    @FXML private Label nearbyFacilitiesStatus;
    @FXML private TableView<HospitalDTO> facilityTable;

    // Compatible donor search
    @FXML private ComboBox<String> donorSearchGroup;
    @FXML private TextField donorSearchLocationField;
    @FXML private TextField donorSearchRadiusField;
    @FXML private Label donorSearchStatus;
    @FXML private TableView<Donor> donorSearchTable;
    @FXML private TableColumn<Donor, String> colDonorName;
    @FXML private TableColumn<Donor, String> colDonorGroup;
    @FXML private TableColumn<Donor, String> colDonorCity;
    @FXML private TableColumn<Donor, String> colDonorPhone;
    @FXML private TableColumn<Donor, Double> colDonorDistance;
    @FXML private TableColumn<Donor, String> colDonorEligibility;
    @FXML private TableColumn<HospitalDTO, String> colFacilityName;
    @FXML private TableColumn<HospitalDTO, String> colFacilityAddress;
    @FXML private TableColumn<HospitalDTO, Double> colFacilityLatitude;
    @FXML private TableColumn<HospitalDTO, Double> colFacilityLongitude;
    @FXML private TableColumn<HospitalDTO, Double> colFacilityDistance;
    @FXML private TableColumn<HospitalDTO, String> colFacilityType;

    // Stats
    @FXML private Label statTotal;
    @FXML private Label statPending;
    @FXML private Label statEmergency;
    @FXML private Label statFulfilled;
    @FXML private Label overviewStatus;

    // Emergency form
    @FXML private ComboBox<String> emerGroup;
    @FXML private TextField        emerQuantity;
    @FXML private TextField        emerLocation;
    @FXML private TextField        emerHospital;
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

    @FXML private TableView<DirectoryApiService.DirectoryEntry> donorDirectoryTable;
    @FXML private TableColumn<DirectoryApiService.DirectoryEntry, String> colDirectoryName;
    @FXML private TableColumn<DirectoryApiService.DirectoryEntry, String> colDirectoryGroup;
    @FXML private TableColumn<DirectoryApiService.DirectoryEntry, String> colDirectoryCity;
    @FXML private TableColumn<DirectoryApiService.DirectoryEntry, String> colDirectoryPhone;

    private final BloodRequestDAO requestDAO = new BloodRequestDAO();
    private final ExecutorService apiExecutor = Executors.newFixedThreadPool(2);
    private final LocationApiService locationApiService = new LocationApiService();
    private final HospitalApiService hospitalApiService = new HospitalApiService();
    private final DonorSearchApiService donorSearchApiService = new DonorSearchApiService();
    private final DirectoryApiService directoryApiService = new DirectoryApiService();

    //  Initialize 

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        User user = sessionManager.getCurrentUser();
        if (user != null) topbarUserName.setText(user.getUsername());

        emerGroup.getSelectionModel().select(0);
        donorSearchGroup.getSelectionModel().select(0);
        donorSearchRadiusField.setText("25");
        emerPriority.getSelectionModel().select("EMERGENCY");

        histTable.setRowFactory(tv -> {
            TableRow<BloodRequest> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getClickCount() == 2) {
                    openRequestDecisionDialog(row.getItem());
                }
            });
            return row;
        });

        setupTable();
        setupFacilityTable();
        setupCompatibleDonorTable();
        setupDirectoryTable();
        donorDirectoryTable.setRowFactory(tv -> {
            TableRow<DirectoryApiService.DirectoryEntry> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getClickCount() == 2) {
                    openDirectoryContactDialog(row.getItem());
                }
            });
            return row;
        });
        loadStats();
        loadDonorDirectory();
    }

    //  Panel switching 

    @FXML private void showOverview(ActionEvent e)   { switchPanel("overview"); loadStats(); }
    @FXML private void showEmergency(ActionEvent e)  { switchPanel("emergency"); resetForm(); }
    @FXML private void showHistory(ActionEvent e)    { switchPanel("history"); loadHistory(); }
    @FXML private void showNearbyFacilities(ActionEvent e) { switchPanel("nearby"); }
    @FXML private void showCompatibleDonors(ActionEvent e) { switchPanel("donors"); }
    @FXML private void showAllDonors(ActionEvent e) { loadDonorDirectory(); }

    private void switchPanel(String which) {
        overviewPanel.setVisible(false);
        emergencyPanel.setVisible(false);
        historyPanel.setVisible(false);
        nearbyFacilitiesPanel.setVisible(false);
        donorSearchPanel.setVisible(false);
        switch (which) {
            case "overview"   -> { overviewPanel.setVisible(true);  topbarTitle.setText("Hospital Dashboard"); }
            case "emergency"  -> { emergencyPanel.setVisible(true); topbarTitle.setText("Submit Blood Request"); }
            case "history"    -> { historyPanel.setVisible(true);   topbarTitle.setText("Request History"); }
            case "nearby"     -> { nearbyFacilitiesPanel.setVisible(true); topbarTitle.setText("Nearby Hospitals"); }
            case "donors"     -> { donorSearchPanel.setVisible(true); topbarTitle.setText("Compatible Donors"); }
        }
    }

    //  Data 

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
            statPending.setText(String.valueOf(list.stream().filter(r -> "PENDING".equals(r.getStatus()) || "MATCHING".equals(r.getStatus()) || "AWAITING_CONFIRMATION".equals(r.getStatus())).count()));
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

    //  Submit 

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

        String location = emerLocation.getText() == null ? "" : emerLocation.getText().trim();
        String hospital = emerHospital.getText() == null ? "" : emerHospital.getText().trim();

        BloodRequest req = new BloodRequest.Builder()
                .requesterId(user.getUserId())
                .requesterName(user.getUsername())
                .requesterType("HOSPITAL")
                .requesterLocation(location)
                .requesterCity(location)
                .hospitalName(hospital)
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

    //  Table setup 

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

    private void setupFacilityTable() {
        colFacilityName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        colFacilityAddress.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAddress()));
        colFacilityLatitude.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().getLatitude()).asObject());
        colFacilityLongitude.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().getLongitude()).asObject());
        colFacilityDistance.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().getDistanceKm()).asObject());
        colFacilityType.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getType()));
        facilityTable.setItems(FXCollections.observableArrayList());
    }

    private void setupCompatibleDonorTable() {
        colDonorName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDisplayName()));
        colDonorGroup.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getBloodGroup() != null ? c.getValue().getBloodGroup().getLabel() : "—"));
        colDonorCity.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCity() != null ? c.getValue().getCity() : "—"));
        colDonorPhone.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPhone() != null ? c.getValue().getPhone() : "—"));
        colDonorEligibility.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEligibilityStatus() != null ? c.getValue().getEligibilityStatus() : "UNKNOWN"));
        colDonorDistance.setCellValueFactory(c -> {
            Donor donor = c.getValue();
            if (donor == null || donor.getLatitude() == null || donor.getLongitude() == null) {
                return new SimpleDoubleProperty(Double.NaN).asObject();
            }
            String locationText = donorSearchLocationField.getText() == null ? "" : donorSearchLocationField.getText().trim();
            if (locationText.isBlank()) {
                return new SimpleDoubleProperty(Double.NaN).asObject();
            }
            try {
                NominatimLocationDTO geocode = locationApiService.geocodeLocation(locationText);
                double distanceKm = HaversineCalculator.calculateDistanceKm(
                        (double) geocode.getLatitude(), (double) geocode.getLongitude(),
                        (double) donor.getLatitude(), (double) donor.getLongitude());
                return new SimpleDoubleProperty(distanceKm).asObject();
            } catch (Exception ignored) {
                return new SimpleDoubleProperty(Double.NaN).asObject();
            }
        });
        donorSearchTable.setItems(FXCollections.observableArrayList());
    }

    private void setupDirectoryTable() {
        colDirectoryName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        colDirectoryGroup.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getBloodGroup()));
        colDirectoryCity.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCity()));
        colDirectoryPhone.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPhone()));
        donorDirectoryTable.setItems(FXCollections.observableArrayList());
    }

    private void loadDonorDirectory() {
        Task<List<DirectoryApiService.DirectoryEntry>> task = new Task<>() {
            @Override
            protected List<DirectoryApiService.DirectoryEntry> call() throws Exception {
                return directoryApiService.fetchDonors();
            }
        };

        task.setOnSucceeded(e -> {
            List<DirectoryApiService.DirectoryEntry> records = task.getValue();
            donorDirectoryTable.setItems(FXCollections.observableArrayList(records));
            if (records == null || records.isEmpty()) {
                overviewStatus.setText("No donor records found in the REST API dataset.");
            } else {
                overviewStatus.setText("Loaded " + records.size() + " donors from the LifeLink API directory.");
            }
        });

        task.setOnFailed(e -> {
            logger.error("Failed to load donor directory", task.getException());
            overviewStatus.setText("Unable to load donor directory from the API right now.");
        });

        apiExecutor.submit(task);
    }

    private void openDirectoryContactDialog(DirectoryApiService.DirectoryEntry donor) {
        if (donor == null) return;

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Donor Contact Details");
        alert.setHeaderText(donor.getName());
        alert.setContentText("Blood Group: " + donor.getBloodGroup() + "\n"
                + "City: " + donor.getCity() + "\n"
                + "Phone: " + donor.getPhone());
        alert.showAndWait();
    }

    @FXML
    private void handleSearchCompatibleDonors(ActionEvent event) {
        String bloodGroup = donorSearchGroup.getValue();
        String location = donorSearchLocationField.getText() == null ? "" : donorSearchLocationField.getText().trim();
        String radiusText = donorSearchRadiusField.getText() == null ? "25" : donorSearchRadiusField.getText().trim();

        if (bloodGroup == null || bloodGroup.isBlank()) {
            donorSearchStatus.setStyle("-fx-text-fill: #fca5a5;");
            donorSearchStatus.setText("Select a blood group before searching.");
            return;
        }

        double radiusKm;
        try {
            radiusKm = Double.parseDouble(radiusText);
            if (!Double.isFinite(radiusKm) || radiusKm <= 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException ex) {
            donorSearchStatus.setStyle("-fx-text-fill: #fca5a5;");
            donorSearchStatus.setText("Radius must be a positive number in kilometres.");
            return;
        }

        donorSearchStatus.setStyle("-fx-text-fill: #cbd5e1;");
        donorSearchStatus.setText("Searching compatible donors...");

        Task<List<Donor>> task = new Task<>() {
            @Override
            protected List<Donor> call() throws Exception {
                return donorSearchApiService.searchCompatibleDonors(bloodGroup, location, radiusKm);
            }
        };

        task.setOnSucceeded(e -> {
            List<Donor> results = task.getValue();
            donorSearchTable.setItems(FXCollections.observableArrayList(results));
            if (results == null || results.isEmpty()) {
                donorSearchStatus.setStyle("-fx-text-fill: #fca5a5;");
                donorSearchStatus.setText("No compatible donors were found for this blood group and search area.");
            } else {
                donorSearchStatus.setStyle("-fx-text-fill: #86efac;");
                donorSearchStatus.setText("Found " + results.size() + " compatible donors.");
            }
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            donorSearchStatus.setStyle("-fx-text-fill: #fca5a5;");
            if (ex instanceof IllegalArgumentException) {
                donorSearchStatus.setText(ex.getMessage());
            } else {
                donorSearchStatus.setText("Donor search is unavailable right now. Please try again later.");
            }
            logger.error("Compatible donor search failed", ex);
        });

        apiExecutor.submit(task);
    }

    @FXML
    private void handleSearchNearbyHospitals(ActionEvent event) {
        String location = searchLocationField.getText() == null ? "" : searchLocationField.getText().trim();
        if (location.isBlank()) {
            nearbyFacilitiesStatus.setStyle("-fx-text-fill: #fca5a5;");
            nearbyFacilitiesStatus.setText("Please enter a location such as Khulna.");
            return;
        }

        nearbyFacilitiesStatus.setStyle("-fx-text-fill: #cbd5e1;");
        nearbyFacilitiesStatus.setText("Searching nearby hospitals...");

        Task<List<HospitalDTO>> task = new Task<>() {
            @Override
            protected List<HospitalDTO> call() throws Exception {
                NominatimLocationDTO geocode = locationApiService.geocodeLocation(location);
                return hospitalApiService.searchNearbyHospitals(geocode.getLatitude(), geocode.getLongitude(), 10);
            }
        };

        task.setOnSucceeded(e -> {
            List<HospitalDTO> results = task.getValue();
            facilityTable.setItems(FXCollections.observableArrayList(results));
            if (results == null || results.isEmpty()) {
                nearbyFacilitiesStatus.setStyle("-fx-text-fill: #fca5a5;");
                nearbyFacilitiesStatus.setText("No nearby healthcare facilities were found for that location.");
            } else {
                nearbyFacilitiesStatus.setStyle("-fx-text-fill: #86efac;");
                nearbyFacilitiesStatus.setText("Found " + results.size() + " nearby facilities.");
            }
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            nearbyFacilitiesStatus.setStyle("-fx-text-fill: #fca5a5;");
            if (ex instanceof IllegalArgumentException) {
                nearbyFacilitiesStatus.setText(ex.getMessage());
            } else if (ex instanceof java.net.ConnectException || ex instanceof java.net.http.HttpTimeoutException) {
                nearbyFacilitiesStatus.setText("Unable to reach the location service. Please check your internet connection.");
            } else {
                nearbyFacilitiesStatus.setText("The hospital API is unavailable right now. Please try again later.");
            }
            logger.error("Nearby hospital search failed", ex);
        });

        apiExecutor.submit(task);
    }

    private void resetForm() {
        emerGroup.getSelectionModel().select(0);
        emerQuantity.setText("1");
        emerLocation.clear();
        emerHospital.clear();
        emerPriority.getSelectionModel().select("EMERGENCY");
        emerNotes.clear();
        emerStatus.setText("");
    }

    private void openRequestDecisionDialog(BloodRequest request) {
        if (request == null) return;

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Request Action");
        alert.setHeaderText("Blood request #" + request.getRequestId());
        alert.setContentText("Status: " + request.getStatus() + "\n"
                + "Location: " + (request.getRequesterLocation() != null && !request.getRequesterLocation().isBlank() ? request.getRequesterLocation() : "Not provided") + "\n"
                + "Hospital: " + (request.getHospitalName() != null && !request.getHospitalName().isBlank() ? request.getHospitalName() : "Not provided") + "\n"
                + "Notes: " + (request.getNotes() != null ? request.getNotes() : "—"));

        if (BloodRequest.STATUS_AWAITING_CONFIRMATION.equalsIgnoreCase(request.getStatus())
                || BloodRequest.STATUS_MATCHING.equalsIgnoreCase(request.getStatus())) {
            ButtonType accept = new ButtonType("Accept Donation");
            ButtonType reject = new ButtonType("Reject");
            alert.getButtonTypes().setAll(accept, reject, ButtonType.CLOSE);
            alert.showAndWait().ifPresent(type -> {
                if (type == accept) {
                    requestDAO.updateStatus(request.getRequestId(), BloodRequest.STATUS_FULFILLED);
                    loadHistory();
                    loadStats();
                } else if (type == reject) {
                    requestDAO.updateStatus(request.getRequestId(), BloodRequest.STATUS_CANCELLED);
                    loadHistory();
                    loadStats();
                }
            });
        } else {
            alert.getButtonTypes().setAll(ButtonType.CLOSE);
            alert.show();
        }
    }
}
