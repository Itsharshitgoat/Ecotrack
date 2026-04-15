# EcoTrack - Carbon Footprint Calculator

**EcoTrack** is a Java-based desktop application designed to function as a Carbon Footprint Calculator. The application uses Java AWT (Abstract Window Toolkit) for the frontend to maintain a "Modern Nature" visual theme with strict constraints (no Swing components) and a MySQL database backend.

## Visual Identity

The application is styled with a "Modern Nature" aesthetic, utilizing the following color palette:

- **Forest Green** (`#2D5A27`): Used for primary buttons and header panels (`new Color(45, 90, 39)`).
- **Sage** (`#A3B18A`): Used for main background panels (`new Color(163, 177, 138)`).
- **Off-White** (`#DAD7CD`): Used for text fields, labels, and dropdown menus (`new Color(218, 215, 205)`).
- **Carbon Grey** (`#344E41`): Used for main text colors (`new Color(52, 78, 65)`).

## Database Schema

The backend is supported by a MySQL database using the following 3 core tables:

1. **`users`**: Stores `user_id`, `username`, `password_hash`, and `total_score`.
2. **`emission_factors`**: Acts as a dynamic reference to map an `activity_type` (e.g., 'Shower') to its `co2_per_unit` factor.
3. **`activity_logs`**: Represents a daily log, securely storing `user_id`, `activity_date`, `activity_type`, `quantity`, and `calculated_co2`.

You can set up the database using the provided `schema.sql` file. Make sure to adjust the `DatabaseManager.java` connection properties (URL, USER, PASS) to match your local MySQL configuration.

## UI/UX Architecture

The layout relies exclusively on `java.awt` packages. We use `GridBagLayout`, `GridLayout`, and `BorderLayout` to keep the UI components stable across different screen sizes:

- **`Frame` (Main Window)**: Contains the main application.
  - **`HeaderPanel`**: A prominent top panel acting as the title.
  - **`InputPanel` (Left Column)**: Uses an AWT `Choice` component to select the activity, and a `Scrollbar` configured from 0 to 100 as the "Eco-Slider" to capture amount data, strictly eliminating non-numeric and negative inputs. It also features an AWT `Button` to log the activity.
  - **`MeterPanel` (Right Column)**: Features a large AWT `Label` to serve as a **Live Meter**, providing immediate visual feedback by changing background colors:
    - **Green**: <= 5.0 kg CO2
    - **Yellow**: > 5.0 kg and <= 15.0 kg CO2
    - **Red**: > 15.0 kg CO2

## Calculation Engine

When the application starts, it fetches the list of activities and their respective CO2 emission factors from the `emission_factors` table, caching it into a `Map<String, Double>`.

When the user records an activity, the engine pulls the value from the "Eco-Slider" (quantity) and performs the following calculation:
```
Total CO2 = Quantity * Factor
```
This calculation updates the daily total. The `calculated_co2` value is then saved to the `activity_logs` table via a secure `PreparedStatement`, ensuring safe and reliable "Silent CRUD" operations.

## How to Compile and Run

1. **Prerequisites**:
   - Ensure you have JDK 8+ installed.
   - You need a MySQL server running locally on port 3306.
   - You need the MySQL JDBC driver (e.g., `mysql-connector-java-8.0.x.jar`).

2. **Database Setup**:
   Execute the `schema.sql` script in your MySQL instance to create the database (`ecotrack`) and the necessary tables:
   ```bash
   mysql -u root -p < schema.sql
   ```

3. **Compile**:
   Compile the Java classes, ensuring the MySQL connector is in your classpath.
   ```bash
   javac -cp ".:mysql-connector-java-8.0.33.jar" DatabaseManager.java EcoTrackDashboard.java
   ```

4. **Run**:
   Run the `EcoTrackDashboard` class.
   ```bash
   java -cp ".:mysql-connector-java-8.0.33.jar" EcoTrackDashboard
   ```
   *Note: Modify the classpath separator `;` for Windows, `:` for Unix-based systems.*