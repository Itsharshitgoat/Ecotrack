import java.awt.*;
import java.awt.event.*;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

public class EcoTrackDashboard extends Frame {
    // "Modern Eames" Palette
    private final Color SURFACE = new Color(252, 249, 244);           // #fcf9f4 (Background)
    private final Color SURFACE_CONTAINER_LOW = new Color(246, 243, 238); // #f6f3ee (Cards - clear lift)
    private final Color SURFACE_CONTAINER_HIGH = new Color(229, 226, 221); // #e5e2dd (Focus Hearth)
    private final Color PRIMARY = new Color(48, 99, 97);              // #306361 (Teal)
    private final Color TERTIARY = new Color(142, 71, 50);            // #8e4732 (Terracotta)
    private final Color WARM_YELLOW = new Color(229, 182, 94);        // #e5b65e (Warning/Mid)
    private final Color ON_SURFACE = new Color(28, 28, 25);           // #1c1c19 (Text)

    private DatabaseManager dbManager;
    private double dailyCo2Total = 0.0;
    private final double DAILY_TARGET = 5.0; // 5 kg target
    private int currentUserId = 1;

    // Live UI State
    private Map<String, Double> emissionFactorsCache;

    // UI Components
    private Choice activityChoice;
    private Scrollbar ecoSlider;
    private Label sliderValueLabel;
    private Label projectionLabel;
    private Label liveMeterLabel;
    private FocusHearth hearth;
    private LeaderboardPanel leaderboardPanel;

    // Custom Component: Tonal Card
    class TonalCard extends Panel {
        private Color bgColor;
        public TonalCard(Color bgColor) {
            this.bgColor = bgColor;
        }
        @Override
        public void paint(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setColor(bgColor);
            g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
            super.paint(g);
        }
    }

    // Custom Component: Focus Hearth (Functional Daily Target)
    class FocusHearth extends Panel {
        private double currentTotal = 0.0;

        public void updateProgress(double total) {
            this.currentTotal = total;
            repaint();
        }

        @Override
        public void paint(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2d.setColor(SURFACE_CONTAINER_HIGH);
            g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);

            // Progress Bar Track
            int barWidth = getWidth() - 80;
            g2d.setColor(SURFACE_CONTAINER_LOW);
            g2d.fillRoundRect(40, 100, barWidth, 12, 12, 12);

            // Progress Bar Fill
            double ratio = Math.min(currentTotal / DAILY_TARGET, 1.0);
            int fillWidth = (int) (barWidth * ratio);

            Color fillColor = PRIMARY;
            if (currentTotal > 2.0 && currentTotal <= 5.0) fillColor = WARM_YELLOW;
            else if (currentTotal > 5.0) fillColor = TERTIARY;

            g2d.setColor(fillColor);
            g2d.fillRoundRect(40, 100, fillWidth, 12, 12, 12);

            super.paint(g);
        }
    }

    // Custom Component: Structured Leaderboard Panel
    class LeaderboardPanel extends TonalCard {
        private List<DatabaseManager.LeaderData> leaders = new ArrayList<>();

        public LeaderboardPanel(Color bgColor) {
            super(bgColor);
        }

        public void updateLeaders(List<DatabaseManager.LeaderData> newLeaders) {
            this.leaders = newLeaders;
            repaint();
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2d.setFont(new Font("SansSerif", Font.PLAIN, 16));
            int yOffset = 70;

            for (int i = 0; i < leaders.size(); i++) {
                DatabaseManager.LeaderData ld = leaders.get(i);
                // Clean data row without borders
                g2d.setColor(ON_SURFACE);
                g2d.drawString((i + 1) + ".", 30, yOffset);
                g2d.drawString(ld.name, 60, yOffset);

                g2d.setColor(PRIMARY);
                g2d.drawString(String.format("%.2f kg", ld.score), 280, yOffset);

                yOffset += 35; // Generous vertical spacing
            }
        }
    }

    class PrimaryButton extends Component {
        private String label;
        private boolean isHovered = false;
        private ActionListener listener;

        public PrimaryButton(String label) {
            this.label = label;
            enableEvents(AWTEvent.MOUSE_EVENT_MASK);
        }

        public void setActionListener(ActionListener l) {
            this.listener = l;
        }

        @Override
        public void paint(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            GradientPaint gp = new GradientPaint(0, 0, PRIMARY, getWidth(), getHeight(), isHovered ? new Color(74, 124, 122) : PRIMARY);
            g2d.setPaint(gp);
            g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);

            g2d.setColor(SURFACE);
            g2d.setFont(new Font("SansSerif", Font.BOLD, 16));
            FontMetrics fm = g2d.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(label)) / 2;
            int y = ((getHeight() - fm.getHeight()) / 2) + fm.getAscent();
            g2d.drawString(label, x, y);
        }

        @Override
        protected void processMouseEvent(MouseEvent e) {
            if (e.getID() == MouseEvent.MOUSE_ENTERED) {
                isHovered = true;
                repaint();
            } else if (e.getID() == MouseEvent.MOUSE_EXITED) {
                isHovered = false;
                repaint();
            } else if (e.getID() == MouseEvent.MOUSE_CLICKED && listener != null) {
                listener.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, label));
            }
            super.processMouseEvent(e);
        }
    }

    public EcoTrackDashboard() {
        dbManager = new DatabaseManager();
        emissionFactorsCache = dbManager.getAllEmissionFactors();

        setTitle("EcoTrack - Modern Eames");
        setSize(1024, 768);
        setBackground(SURFACE); // Base tier
        setLayout(null); // Fixed absolute layout

        // 1. Focus Hearth (Hero Section)
        hearth = new FocusHearth();
        hearth.setLayout(null);
        hearth.setBounds(100, 40, 824, 140);

        Label greeting = new Label("Carbon Focus");
        greeting.setFont(new Font("SansSerif", Font.BOLD, 36));
        greeting.setForeground(ON_SURFACE);
        greeting.setBounds(40, 20, 300, 50);
        hearth.add(greeting);

        Label targetLabel = new Label("Daily Target: " + DAILY_TARGET + " kg");
        targetLabel.setFont(new Font("SansSerif", Font.PLAIN, 18));
        targetLabel.setForeground(PRIMARY);
        targetLabel.setBounds(600, 30, 200, 30);
        hearth.add(targetLabel);

        add(hearth);

        // 2. Input Section (Left Action Panel)
        TonalCard inputCard = new TonalCard(SURFACE_CONTAINER_LOW);
        inputCard.setLayout(null);
        inputCard.setBounds(100, 220, 400, 480); // Taller to match right column group

        Label taskLabel = new Label("Log Activity");
        taskLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        taskLabel.setForeground(ON_SURFACE);
        taskLabel.setBackground(SURFACE_CONTAINER_LOW);
        taskLabel.setBounds(30, 40, 200, 30);
        inputCard.add(taskLabel);

        activityChoice = new Choice();
        activityChoice.setBounds(30, 100, 340, 40);
        activityChoice.setFont(new Font("SansSerif", Font.PLAIN, 16));

        for (String key : emissionFactorsCache.keySet()) {
            activityChoice.add(key);
        }

        activityChoice.addItemListener(e -> updateLiveProjection());
        inputCard.add(activityChoice);

        Label amountLabel = new Label("Quantity:");
        amountLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        amountLabel.setForeground(ON_SURFACE);
        amountLabel.setBackground(SURFACE_CONTAINER_LOW);
        amountLabel.setBounds(30, 170, 100, 20);
        inputCard.add(amountLabel);

        // Eco-Slider
        ecoSlider = new Scrollbar(Scrollbar.HORIZONTAL, 0, 1, 0, 101);
        ecoSlider.setBounds(30, 200, 280, 20);
        ecoSlider.addAdjustmentListener(e -> updateLiveProjection());
        inputCard.add(ecoSlider);

        sliderValueLabel = new Label("0");
        sliderValueLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        sliderValueLabel.setForeground(ON_SURFACE);
        sliderValueLabel.setBackground(SURFACE_CONTAINER_LOW);
        sliderValueLabel.setBounds(330, 195, 40, 30);
        inputCard.add(sliderValueLabel);

        projectionLabel = new Label("+ 0.00 kg projected");
        projectionLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        projectionLabel.setForeground(TERTIARY);
        projectionLabel.setBackground(SURFACE_CONTAINER_LOW);
        projectionLabel.setBounds(30, 230, 200, 20);
        inputCard.add(projectionLabel);

        PrimaryButton logBtn = new PrimaryButton("Record Impact");
        logBtn.setBounds(30, 390, 340, 50);
        logBtn.setActionListener(this::handleLogActivity);
        inputCard.add(logBtn);

        add(inputCard);

        // 3. Live Gauge (Right Feedback Panel Top)
        TonalCard gaugeCard = new TonalCard(SURFACE_CONTAINER_LOW);
        gaugeCard.setLayout(null);
        gaugeCard.setBounds(524, 220, 400, 200);

        Label gaugeTitle = new Label("Today's Output");
        gaugeTitle.setFont(new Font("SansSerif", Font.BOLD, 22));
        gaugeTitle.setForeground(ON_SURFACE);
        gaugeTitle.setBackground(SURFACE_CONTAINER_LOW);
        gaugeTitle.setBounds(40, 30, 200, 30);
        gaugeCard.add(gaugeTitle);

        liveMeterLabel = new Label("0.00 kg", Label.LEFT);
        liveMeterLabel.setFont(new Font("SansSerif", Font.BOLD, 64));
        liveMeterLabel.setForeground(PRIMARY);
        liveMeterLabel.setBackground(SURFACE_CONTAINER_LOW);
        liveMeterLabel.setBounds(40, 80, 340, 90);
        gaugeCard.add(liveMeterLabel);

        add(gaugeCard);

        // 4. Monthly Leaderboard (Right Feedback Panel Bottom)
        leaderboardPanel = new LeaderboardPanel(SURFACE_CONTAINER_LOW);
        leaderboardPanel.setLayout(null);
        leaderboardPanel.setBounds(524, 440, 400, 260);

        Label leaderTitle = new Label("Monthly Leaders");
        leaderTitle.setFont(new Font("SansSerif", Font.BOLD, 18));
        leaderTitle.setForeground(ON_SURFACE);
        leaderTitle.setBackground(SURFACE_CONTAINER_LOW);
        leaderTitle.setBounds(30, 20, 200, 30);
        leaderboardPanel.add(leaderTitle);

        add(leaderboardPanel);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                System.exit(0);
            }
        });

        loadDailyTotal();
        loadLeaderboard();
    }

    private void updateLiveProjection() {
        int qty = ecoSlider.getValue();
        sliderValueLabel.setText(String.valueOf(qty));

        String activity = activityChoice.getSelectedItem();
        double factor = emissionFactorsCache.getOrDefault(activity, 0.0);
        double projected = qty * factor; // Emission factors are already in kg based on schema

        projectionLabel.setText(String.format("+ %.2f kg projected", projected));
    }

    private void handleLogActivity(ActionEvent e) {
        String selectedActivity = activityChoice.getSelectedItem();
        int quantity = ecoSlider.getValue();

        if (quantity <= 0) return;

        // DB operations in background
        new Thread(() -> {
            double factor = dbManager.getFactorForActivity(selectedActivity);
            double calculatedCo2 = quantity * factor; // Emission factors are already in kg

            dbManager.logActivity(currentUserId, selectedActivity, quantity, calculatedCo2);

            EventQueue.invokeLater(() -> {
                updateOutputMetric(dailyCo2Total + calculatedCo2);
                ecoSlider.setValue(0);
                updateLiveProjection();
            });

            loadLeaderboard();
        }).start();
    }

    private void loadLeaderboard() {
        new Thread(() -> {
            List<DatabaseManager.LeaderData> leaders = dbManager.getLowestMonthlyEmissions();
            EventQueue.invokeLater(() -> leaderboardPanel.updateLeaders(leaders));
        }).start();
    }

    private void loadDailyTotal() {
        new Thread(() -> {
            double total = dbManager.getDailyTotalForUser(currentUserId);
            EventQueue.invokeLater(() -> updateOutputMetric(total));
        }).start();
    }

    private void updateOutputMetric(double newTotal) {
        dailyCo2Total = newTotal;
        liveMeterLabel.setText(String.format("%.2f kg", dailyCo2Total));

        if (dailyCo2Total <= 2.0) {
            liveMeterLabel.setForeground(PRIMARY);
        } else if (dailyCo2Total <= 5.0) {
            liveMeterLabel.setForeground(WARM_YELLOW);
        } else {
            liveMeterLabel.setForeground(TERTIARY);
        }

        hearth.updateProgress(dailyCo2Total);
    }

    public static void main(String[] args) {
        EcoTrackDashboard app = new EcoTrackDashboard();
        app.setVisible(true);
    }
}
