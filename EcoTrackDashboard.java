import java.awt.*;
import java.awt.event.*;

public class EcoTrackDashboard extends Frame {
    // "Modern Eames" Palette
    private final Color SURFACE = new Color(252, 249, 244);           // #fcf9f4 (Base Tier)
    private final Color SURFACE_CONTAINER_LOW = new Color(246, 243, 238); // #f6f3ee (Cards)
    private final Color SURFACE_CONTAINER_HIGH = new Color(229, 226, 221); // #e5e2dd (Focus Hearth)
    private final Color PRIMARY = new Color(48, 99, 97);              // #306361 (Muted Teal)
    private final Color TERTIARY = new Color(142, 71, 50);            // #8e4732 (Terracotta)
    private final Color ON_SURFACE = new Color(28, 28, 25);           // #1c1c19 (Text, not pure black)

    private DatabaseManager dbManager;
    private double dailyCo2Total = 0.0;
    private int currentUserId = 1;

    // UI Components
    private Choice activityChoice;
    private TextField ecoInput; // Replaced Scrollbar with text input for cleaner "Input Field" styling
    private Label liveMeterLabel;
    private List leaderboardList;

    // Custom Component: Tonal Card (No Borders, Rounded, Background Shift)
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
            g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24); // Smooth rounded corners
            super.paint(g);
        }
    }

    // Custom Component: Focus Hearth (The greeting area with Terracotta accent)
    class FocusHearth extends Panel {
        @Override
        public void paint(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Base high tier background
            g2d.setColor(SURFACE_CONTAINER_HIGH);
            g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);

            // Soft terracotta glow/accent (bleeding edge feel)
            g2d.setColor(new Color(142, 71, 50, 40)); // Low opacity terracotta
            g2d.fillOval(getWidth() - 150, -50, 200, 200);

            super.paint(g);
        }
    }

    // Custom Component: Primary Button
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

            // Gradient fill from primary to a slightly lighter teal
            GradientPaint gp = new GradientPaint(0, 0, PRIMARY, getWidth(), getHeight(), new Color(74, 124, 122));
            g2d.setPaint(gp);
            g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24); // xl roundedness

            // Text
            g2d.setColor(SURFACE);
            g2d.setFont(new Font("SansSerif", Font.BOLD, 16));
            FontMetrics fm = g2d.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(label)) / 2;
            int y = ((getHeight() - fm.getHeight()) / 2) + fm.getAscent();
            g2d.drawString(label, x, y);
        }

        @Override
        protected void processMouseEvent(MouseEvent e) {
            if (e.getID() == MouseEvent.MOUSE_CLICKED && listener != null) {
                listener.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, label));
            }
            super.processMouseEvent(e);
        }
    }

    public EcoTrackDashboard() {
        dbManager = new DatabaseManager();

        setTitle("EcoTrack - The Modern Eames");
        setSize(1024, 768);
        setBackground(SURFACE);
        setLayout(null); // Absolute positioning for editorial layout

        // 1. The Focus Hearth (Top Area)
        // Intentional Asymmetry: Wide left margin (100), tight right margin
        FocusHearth hearth = new FocusHearth();
        hearth.setLayout(null);
        hearth.setBounds(100, 40, 850, 160);

        Label greeting = new Label("Good morning.");
        greeting.setFont(new Font("SansSerif", Font.BOLD, 42)); // display-lg proxy
        greeting.setForeground(ON_SURFACE);
        greeting.setBounds(40, 30, 400, 50);
        hearth.add(greeting);

        Label subGreeting = new Label("Here is your carbon focus for the day.");
        subGreeting.setFont(new Font("SansSerif", Font.PLAIN, 18));
        subGreeting.setForeground(PRIMARY);
        subGreeting.setBounds(42, 90, 400, 30);
        hearth.add(subGreeting);

        add(hearth);

        // 2. Input Section (Left side stacked paper)
        TonalCard inputCard = new TonalCard(SURFACE_CONTAINER_LOW);
        inputCard.setLayout(null);
        inputCard.setBounds(100, 240, 400, 360);

        Label taskLabel = new Label("Log Activity");
        taskLabel.setFont(new Font("SansSerif", Font.BOLD, 22)); // title-md proxy
        taskLabel.setForeground(ON_SURFACE);
        taskLabel.setBounds(30, 30, 200, 30);
        inputCard.add(taskLabel);

        // Dropdown
        activityChoice = new Choice();
        activityChoice.setBounds(30, 90, 340, 40);
        activityChoice.setFont(new Font("SansSerif", Font.PLAIN, 16));
        activityChoice.add("Driving (Car)");
        activityChoice.add("Public Transit");
        activityChoice.add("Shower");
        activityChoice.add("Meat Meal");

        activityChoice.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                // UI interaction on choice change
            }
        });
        inputCard.add(activityChoice);

        // Input Field (Replacing slider for editorial cleanliness)
        Label amountLabel = new Label("Quantity (Units):");
        amountLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        amountLabel.setForeground(ON_SURFACE);
        amountLabel.setBounds(30, 150, 200, 20);
        inputCard.add(amountLabel);

        ecoInput = new TextField("");
        ecoInput.setFont(new Font("SansSerif", Font.PLAIN, 16));
        ecoInput.setBounds(30, 180, 340, 30);
        ecoInput.setBackground(SURFACE_CONTAINER_HIGH); // Input background
        // Removing borders in AWT TextField is hard, so we rely on background contrast
        inputCard.add(ecoInput);

        PrimaryButton logBtn = new PrimaryButton("Record Impact");
        logBtn.setBounds(30, 260, 340, 50);
        logBtn.setActionListener(this::handleLogActivity);
        inputCard.add(logBtn);

        add(inputCard);

        // 3. Live Gauge (Right side, top)
        TonalCard gaugeCard = new TonalCard(SURFACE_CONTAINER_LOW);
        gaugeCard.setLayout(null);
        gaugeCard.setBounds(550, 240, 400, 160);

        Label gaugeTitle = new Label("Today's Output");
        gaugeTitle.setFont(new Font("SansSerif", Font.PLAIN, 14));
        gaugeTitle.setForeground(TERTIARY); // Soft terracotta accent
        gaugeTitle.setBounds(30, 20, 200, 20);
        gaugeCard.add(gaugeTitle);

        liveMeterLabel = new Label("0.00 kg", Label.LEFT);
        liveMeterLabel.setFont(new Font("SansSerif", Font.BOLD, 48)); // display-lg
        liveMeterLabel.setForeground(ON_SURFACE);
        liveMeterLabel.setBounds(30, 50, 340, 70);
        gaugeCard.add(liveMeterLabel);

        add(gaugeCard);

        // 4. Leaderboard (Right side, bottom)
        TonalCard leaderCard = new TonalCard(SURFACE_CONTAINER_LOW);
        leaderCard.setLayout(null);
        leaderCard.setBounds(550, 440, 400, 260);

        Label leaderTitle = new Label("Community Leaders");
        leaderTitle.setFont(new Font("SansSerif", Font.BOLD, 18));
        leaderTitle.setForeground(ON_SURFACE);
        leaderTitle.setBounds(30, 20, 200, 30);
        leaderCard.add(leaderTitle);

        leaderboardList = new List();
        leaderboardList.setBounds(30, 60, 340, 170);
        leaderboardList.setFont(new Font("SansSerif", Font.PLAIN, 14));
        leaderboardList.setBackground(SURFACE_CONTAINER_LOW);
        leaderboardList.setForeground(PRIMARY);
        leaderCard.add(leaderboardList);

        add(leaderCard);

        // Window Closing
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                System.exit(0);
            }
        });

        loadLeaderboard();
        loadDailyTotal(); // Fetch initial today's total if app restarts
    }

    private void handleLogActivity(ActionEvent e) {
        String selectedActivity = activityChoice.getSelectedItem();
        String inputStr = ecoInput.getText().trim();

        if (inputStr.isEmpty()) return;

        double quantity;
        try {
            quantity = Double.parseDouble(inputStr);
        } catch (NumberFormatException ex) {
            System.err.println("Invalid numeric input");
            return;
        }

        if (quantity <= 0) return;

        // DB operations on background thread
        new Thread(() -> {
            double factor = dbManager.getFactorForActivity(selectedActivity);

            // Formula: Input * (Emission Factor / 1000)
            double calculatedCo2 = quantity * (factor / 1000.0);

            dbManager.logActivity(currentUserId, selectedActivity, quantity, calculatedCo2);

            dailyCo2Total += calculatedCo2;

            EventQueue.invokeLater(() -> {
                liveMeterLabel.setText(String.format("%.2f kg", dailyCo2Total));
                ecoInput.setText("");
            });

            // Refresh leaderboard
            loadLeaderboard();
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

    private void loadDailyTotal() {
        new Thread(() -> {
            double total = dbManager.getDailyTotalForUser(currentUserId);
            dailyCo2Total = total;
            EventQueue.invokeLater(() -> {
                liveMeterLabel.setText(String.format("%.2f kg", dailyCo2Total));
            });
        }).start();
    }

    public static void main(String[] args) {
        EcoTrackDashboard app = new EcoTrackDashboard();
        app.setVisible(true);
    }
}
