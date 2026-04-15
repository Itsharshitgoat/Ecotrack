import java.awt.*;
import java.awt.event.*;
import java.util.Map;

public class EcoTrackDashboard extends Frame {
    // Brand Colors
    private final Color FOREST_GREEN = new Color(45, 90, 39);
    private final Color SAGE = new Color(163, 177, 138);
    private final Color OFF_WHITE = new Color(218, 215, 205);
    private final Color CARBON_GREY = new Color(52, 78, 65);

    private DatabaseManager dbManager;
    private Map<String, Double> emissionFactors;
    private double dailyCo2Total = 0.0;
    private int currentUserId = 1; // Assuming a logged-in user

    // UI Components
    private Choice activityChoice;
    private Scrollbar ecoSlider;
    private Label sliderValueLabel;
    private Label liveMeterLabel;

    public EcoTrackDashboard() {
        dbManager = new DatabaseManager();
        emissionFactors = dbManager.loadEmissionFactors(); // Load factors on startup

        setTitle("EcoTrack - Silent CRUD Logger");
        setSize(800, 600);
        setBackground(SAGE);
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        // 1. Header Panel
        Panel headerPanel = new Panel(new FlowLayout());
        headerPanel.setBackground(FOREST_GREEN);
        Label titleLabel = new Label("EcoTrack Dashboard");
        titleLabel.setForeground(OFF_WHITE);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        headerPanel.add(titleLabel);

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL;
        add(headerPanel, gbc);

        // 2. Input Panel (Left)
        Panel inputPanel = new Panel(new GridLayout(5, 1, 10, 10));
        Label activityLbl = new Label("Select Activity:");
        activityLbl.setForeground(CARBON_GREY);
        activityChoice = new Choice();
        for (String activity : emissionFactors.keySet()) {
            activityChoice.add(activity);
        }

        Label amountLbl = new Label("Amount (Units):");
        amountLbl.setForeground(CARBON_GREY);

        // Eco-Slider (Horizontal, Initial 0, Min 0, Max 100) - Eliminates negative inputs
        ecoSlider = new Scrollbar(Scrollbar.HORIZONTAL, 0, 1, 0, 101);
        sliderValueLabel = new Label("0");
        sliderValueLabel.setForeground(CARBON_GREY);

        ecoSlider.addAdjustmentListener(e -> sliderValueLabel.setText(String.valueOf(e.getValue())));

        Button logBtn = new Button("Log Activity");
        logBtn.setBackground(FOREST_GREEN);
        logBtn.setForeground(OFF_WHITE);
        logBtn.addActionListener(this::handleLogActivity);

        inputPanel.add(activityLbl);
        inputPanel.add(activityChoice);
        inputPanel.add(amountLbl);
        inputPanel.add(ecoSlider);
        inputPanel.add(sliderValueLabel);
        inputPanel.add(logBtn);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1; gbc.insets = new Insets(20, 20, 20, 20);
        add(inputPanel, gbc);

        // 3. Live Meter Panel (Right)
        Panel meterPanel = new Panel(new BorderLayout());
        Label meterTitle = new Label("Today's CO2 Impact");
        meterTitle.setForeground(CARBON_GREY);
        liveMeterLabel = new Label("0.0 kg CO2", Label.CENTER);
        liveMeterLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        liveMeterLabel.setBackground(Color.GREEN); // Starts Green
        liveMeterLabel.setPreferredSize(new Dimension(200, 150));

        meterPanel.add(meterTitle, BorderLayout.NORTH);
        meterPanel.add(liveMeterLabel, BorderLayout.CENTER);

        gbc.gridx = 1; gbc.gridy = 1;
        add(meterPanel, gbc);

        // Window Closing Event
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                System.exit(0);
            }
        });
    }

    private void handleLogActivity(ActionEvent e) {
        String selectedActivity = activityChoice.getSelectedItem();
        int quantity = ecoSlider.getValue();

        if (quantity <= 0) return; // Validation: Discard 0 values

        double factor = emissionFactors.getOrDefault(selectedActivity, 0.0);
        double calculatedCo2 = quantity * factor;

        // Persist to Database
        dbManager.logActivity(currentUserId, selectedActivity, quantity, calculatedCo2);

        // Update Math
        dailyCo2Total += calculatedCo2;

        // Update Live Meter UI
        liveMeterLabel.setText(String.format("%.2f kg CO2", dailyCo2Total));
        if (dailyCo2Total <= 5.0) {
            liveMeterLabel.setBackground(Color.GREEN);
        } else if (dailyCo2Total <= 15.0) {
            liveMeterLabel.setBackground(Color.YELLOW);
        } else {
            liveMeterLabel.setBackground(Color.RED);
        }

        // Reset Slider for next entry
        ecoSlider.setValue(0);
        sliderValueLabel.setText("0");
    }

    public static void main(String[] args) {
        EcoTrackDashboard app = new EcoTrackDashboard();
        app.setVisible(true);
    }
}
