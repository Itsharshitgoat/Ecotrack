# EcoTrack - Night Owl Edition

**EcoTrack** is a Java-based desktop application designed to function as a Carbon Footprint Calculator. In this edition, we have completely overhauled the visual identity to feature a "Night Owl Glassmorphism" aesthetic, while strictly adhering to Java AWT (Abstract Window Toolkit) without utilizing Swing. The backend remains robust, powered by a secure MySQL database.

## Visual Identity: The Night Owl Palette

The application uses deep dark tones with vibrant accents to achieve a modern, high-end look:

- **Deep Background** (`#011627`): Main Frame Background
- **Glass Panel** (`#0B2942`): Used for `GlassPanel` overlays to simulate semi-transparent containers (`alpha = 180`).
- **Electric Blue** (`#82AAFF`): Active Buttons / Highlights
- **Soft Purple** (`#C792EA`): Data Labels / Accents
- **Text White** (`#D6DEEB`): Primary Readability

### Glassmorphism in AWT
Because AWT components are heavyweight and rendered by the OS, true transparency and opacity manipulation is difficult. To simulate the "Glass" effect, we override the `paint` method of a custom `Panel` (`GlassPanel`). We draw a semi-transparent rounded rectangle accompanied by a subtle glowing border.

## Database Schema

The backend is supported by a MySQL database using the following 3 core tables:

1. **`users`**: Stores `user_id`, `username`, `password_hash`, and `total_score`.
2. **`emission_factors`**: Acts as a dynamic reference to map an `activity_type` to its `co2_per_unit` factor.
3. **`activity_logs`**: Represents a daily log, securely storing `user_id`, `activity_date`, `activity_type`, `quantity`, and `calculated_co2`.

You can set up the database using the provided `schema.sql` file.

## UI/UX Architecture (Coordinate-Based Grid System)

The layout has been meticulously mapped onto a $1024 \times 768$ absolute layout (`setLayout(null)`) for precise element placement:

- **Header Section** `(0, 0) to (1024, 80)`: Darkest Navy background featuring the EcoTrack title in Electric Blue.
- **Input Panel (Glass Section)** `(50, 120) to (450, 600)`: Hosts the interactive elements: a dropdown for the activity, an Eco-Slider for quantity selection, and the action button. An `ItemListener` tracks dropdown changes.
- **Real-time Gauge Panel** `(550, 120) to (950, 400)`: Features large text indicating the real-time calculated CO2 output.
- **Leaderboard Table Panel** `(550, 450) to (950, 700)`: Retrieves and lists the top "greenest" users.

## Calculation Engine and Event Flow

The mathematical logic has been updated to support standard kg conversion:
```
Total CO2 (kg) = Quantity * (Emission Factor / 1000.0)
```

**Thread Safety & Integration Flow:**
To prevent AWT's main thread from freezing during database queries, all JDBC actions run asynchronously.
1. **Extract**: Get the value from the UI controls.
2. **Fetch**: Execute `SELECT co2_per_unit FROM emission_factors WHERE activity_type = ?` on a background thread.
3. **Compute**: Perform the calculation.
4. **Update**: Persist data to `activity_logs` and update the `users` total score, then securely dispatch a UI update back to the EventQueue to update the Gauge and Leaderboard.

## How to Compile and Run

1. **Prerequisites**:
   - Ensure you have JDK 8+ installed.
   - You need a MySQL server running locally on port 3306.
   - You need the MySQL JDBC driver (e.g., `mysql-connector-java-8.0.x.jar`).

2. **Database Setup**:
   Execute the `schema.sql` script in your MySQL instance to create the database (`ecotrack`) and the necessary tables.

3. **Compile**:
   Compile the Java classes:
   ```bash
   javac -cp ".:mysql-connector-java-8.0.33.jar" DatabaseManager.java EcoTrackDashboard.java
   ```

4. **Run**:
   Run the `EcoTrackDashboard` class:
   ```bash
   java -cp ".:mysql-connector-java-8.0.33.jar" EcoTrackDashboard
   ```
   *Note: Modify the classpath separator `;` for Windows, `:` for Unix-based systems.*