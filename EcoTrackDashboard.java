import java.awt.*;
import java.awt.event.*;

public class EcoTrackDashboard extends Frame {
    // Night Owl Colors
    private final Color DEEP_BACKGROUND = new Color(1, 22, 39);
    private final Color GLASS_PANEL = new Color(11, 41, 66, 180);
    private final Color ELECTRIC_BLUE = new Color(130, 170, 255);
    private final Color SOFT_PURPLE = new Color(199, 146, 234);
    private final Color TEXT_WHITE = new Color(214, 222, 235);

    private DatabaseManager dbManager;
    private double dailyCo2Total = 0.0;
    private int currentUserId = 1; // Assuming a logged-in user

    // UI Components
    private Choice activityChoice;
    private Scrollbar ecoSlider;
    private Label sliderValueLabel;
    private Label liveMeterLabel;
    private List leaderboardList;
    private Label gaugeCanvasLabel; // Replacing Canvas with Label for simplicity, or we can use Canvas if needed

    // Custom Glass Panel Class
    class GlassPanel extends Panel {
        public void paint(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Semi-transparent background
            g2d.setColor(GLASS_PANEL);
            g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);

            // Thin glowing border
            g2d.setColor(new Color(130, 170, 255, 100));
            g2d.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 30, 30);

            super.paint(g); // Paint child components
        }
    }

    public EcoTrackDashboard() {
        dbManager = new DatabaseManager();

        setTitle("EcoTrack - Night Owl Edition");
        setSize(1024, 768);
        setBackground(DEEP_BACKGROUND);
        setLayout(null); // Absolute positioning

        // 1. Header Section
        Panel headerPanel = new Panel();
        headerPanel.setBackground(DEEP_BACKGROUND);
        headerPanel.setBounds(0, 0, 1024, 80);
        Label titleLabel = new Label("EcoTrack Dashboard");
        titleLabel.setForeground(ELECTRIC_BLUE);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 36));
        headerPanel.add(titleLabel);
        add(headerPanel);

        // 2. Input Panel (The "Glass" Section)
        GlassPanel inputPanel = new GlassPanel();
        inputPanel.setLayout(null);
        inputPanel.setBounds(50, 120, 400, 480);

        Label activityLbl = new Label("Select Activity:");
        activityLbl.setForeground(SOFT_PURPLE);
        activityLbl.setFont(new Font("SansSerif", Font.BOLD, 18));
        activityLbl.setBounds(30, 40, 340, 30);
        inputPanel.add(activityLbl);

        activityChoice = new Choice();
        activityChoice.setBounds(30, 80, 340, 30);
        // We will populate this asynchronously or on start, assuming fixed for now to show logic
        activityChoice.add("Driving (Car)");
        activityChoice.add("Public Transit");
        activityChoice.add("Shower");
        activityChoice.add("Meat Meal");

        activityChoice.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                String selected = activityChoice.getSelectedItem();
                System.out.println("Selected Activity: " + selected);
            }
        });
        inputPanel.add(activityChoice);

        Label amountLbl = new Label("Amount:");
        amountLbl.setForeground(SOFT_PURPLE);
        amountLbl.setFont(new Font("SansSerif", Font.BOLD, 18));
        amountLbl.setBounds(30, 150, 340, 30);
        inputPanel.add(amountLbl);

        ecoSlider = new Scrollbar(Scrollbar.HORIZONTAL, 0, 1, 0, 101);
        ecoSlider.setBounds(30, 190, 300, 30);

        sliderValueLabel = new Label("0");
        sliderValueLabel.setForeground(TEXT_WHITE);
        sliderValueLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        sliderValueLabel.setBounds(340, 190, 40, 30);

        ecoSlider.addAdjustmentListener(e -> sliderValueLabel.setText(String.valueOf(e.getValue())));

        inputPanel.add(ecoSlider);
        inputPanel.add(sliderValueLabel);

        Button logBtn = new Button("Log Activity");
        logBtn.setBackground(ELECTRIC_BLUE);
        logBtn.setForeground(DEEP_BACKGROUND);
        logBtn.setFont(new Font("SansSerif", Font.BOLD, 18));
        logBtn.setBounds(100, 300, 200, 50);
        logBtn.addActionListener(this::handleLogActivity);
        inputPanel.add(logBtn);

        add(inputPanel);

        // 3. Real-time Gauge Panel
        GlassPanel gaugePanel = new GlassPanel();
        gaugePanel.setLayout(null);
        gaugePanel.setBounds(550, 120, 400, 280);

        Label gaugeTitle = new Label("Today's Total CO2");
        gaugeTitle.setForeground(SOFT_PURPLE);
        gaugeTitle.setFont(new Font("SansSerif", Font.BOLD, 20));
        gaugeTitle.setBounds(30, 20, 340, 30);
        gaugePanel.add(gaugeTitle);

        liveMeterLabel = new Label("0.0000 kg", Label.CENTER);
        liveMeterLabel.setForeground(TEXT_WHITE);
        liveMeterLabel.setFont(new Font("SansSerif", Font.BOLD, 36));
        liveMeterLabel.setBounds(30, 100, 340, 80);
        gaugePanel.add(liveMeterLabel);

        add(gaugePanel);

        // 4. Leaderboard Table Panel
        GlassPanel leaderPanel = new GlassPanel();
        leaderPanel.setLayout(null);
        leaderPanel.setBounds(550, 450, 400, 250);

        Label leaderTitle = new Label("Top Greenest Users");
        leaderTitle.setForeground(SOFT_PURPLE);
        leaderTitle.setFont(new Font("SansSerif", Font.BOLD, 20));
        leaderTitle.setBounds(30, 20, 340, 30);
        leaderPanel.add(leaderTitle);

        leaderboardList = new List();
        leaderboardList.setBounds(30, 60, 340, 150);
        leaderboardList.setBackground(DEEP_BACKGROUND);
        leaderboardList.setForeground(TEXT_WHITE);
        leaderPanel.add(leaderboardList);

        add(leaderPanel);

        // Window Closing Event
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                System.exit(0);
            }
        });

        loadLeaderboard();
    }

    private void handleLogActivity(ActionEvent e) {
        String selectedActivity = activityChoice.getSelectedItem();
        int quantity = ecoSlider.getValue();

        if (quantity <= 0) return;

        // Thread safe database calculation
        new Thread(() -> {
            double factor = dbManager.getFactorForActivity(selectedActivity);

            // New math logic:
            double calculatedCo2 = quantity * (factor / 1000.0);

            dbManager.logActivity(currentUserId, selectedActivity, quantity, calculatedCo2);

            // Update Math
            dailyCo2Total += calculatedCo2;

            // UI updates must be safe
            EventQueue.invokeLater(() -> {
                liveMeterLabel.setText(String.format("%.4f kg", dailyCo2Total));
                ecoSlider.setValue(0);
                sliderValueLabel.setText("0");
            });
        }).start();
    }

    private void loadLeaderboard() {
        new Thread(() -> {
            java.util.List<String> leaders = dbManager.getTopUsers();
            EventQueue.invokeLater(() -> {
                leaderboardList.removeAll();
                for (String leader : leaders) {
                    leaderboardList.add(leader);
                }
            });
        }).start();
    }

    public static void main(String[] args) {
        EcoTrackDashboard app = new EcoTrackDashboard();
        app.setVisible(true);
    }
}
