import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    // Environment variables with fallbacks
    private static final String URL = System.getenv("ECOTRACK_DB_URL") != null ? System.getenv("ECOTRACK_DB_URL") : "jdbc:mysql://localhost:3306/ecotrack";
    private static final String USER = System.getenv("ECOTRACK_DB_USER") != null ? System.getenv("ECOTRACK_DB_USER") : "root";
    private static final String PASS = System.getenv("ECOTRACK_DB_PASS") != null ? System.getenv("ECOTRACK_DB_PASS") : "password";

    public Connection connect() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    public boolean authenticateUser(String username, String password) {
        String query = "SELECT user_id FROM users WHERE username = ? AND password_hash = ?";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public double getFactorForActivity(String activityType) {
        String query = "SELECT co2_per_unit FROM emission_factors WHERE activity_type = ?";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, activityType);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getDouble("co2_per_unit");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    public void logActivity(int userId, String activityType, double quantity, double calculatedCo2) {
        String query = "INSERT INTO activity_logs (user_id, activity_date, activity_type, quantity, calculated_co2) VALUES (?, CURDATE(), ?, ?, ?)";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, activityType);
            pstmt.setDouble(3, quantity);
            pstmt.setDouble(4, calculatedCo2);
            pstmt.executeUpdate();

            updateUserScore(userId, calculatedCo2);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateUserScore(int userId, double calculatedCo2) {
        String query = "UPDATE users SET total_score = total_score + ? WHERE user_id = ?";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setDouble(1, calculatedCo2);
            pstmt.setInt(2, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<String> getTopUsers() {
        List<String> leaders = new ArrayList<>();
        String query = "SELECT username, total_score FROM users ORDER BY total_score ASC LIMIT 5";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                leaders.add(rs.getString("username") + " - " + String.format("%.2f kg", rs.getDouble("total_score")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return leaders;
    }

    // Feature added to ensure "Live Meter" accuracy upon app restarts
    public double getDailyTotalForUser(int userId) {
        String query = "SELECT SUM(calculated_co2) as daily_total FROM activity_logs WHERE user_id = ? AND activity_date = CURDATE()";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getDouble("daily_total");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }
}
