import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    // Use environment variables for DB connection, fallback to defaults
    private static final String URL = System.getenv("ECOTRACK_DB_URL") != null ? System.getenv("ECOTRACK_DB_URL") : "jdbc:mysql://localhost:3306/ecotrack";
    private static final String USER = System.getenv("ECOTRACK_DB_USER") != null ? System.getenv("ECOTRACK_DB_USER") : "root";
    private static final String PASS = System.getenv("ECOTRACK_DB_PASS") != null ? System.getenv("ECOTRACK_DB_PASS") : "password";

    public Connection connect() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    // Secure Login using PreparedStatement
    public boolean authenticateUser(String username, String password) {
        String query = "SELECT user_id FROM users WHERE username = ? AND password_hash = ?";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password); // In production, hash the password first
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Dynamic factor fetching on demand
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

    // Secure Insert for Activity Log
    public void logActivity(int userId, String activityType, double quantity, double calculatedCo2) {
        String query = "INSERT INTO activity_logs (user_id, activity_date, activity_type, quantity, calculated_co2) VALUES (?, CURDATE(), ?, ?, ?)";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, activityType);
            pstmt.setDouble(3, quantity);
            pstmt.setDouble(4, calculatedCo2);
            pstmt.executeUpdate();

            // Optionally update user's total score in the users table
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

    // Fetch top users for leaderboard
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
}
