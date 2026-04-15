# EcoTrack - "Modern Eames" Edition

**EcoTrack** is a Java-based desktop Carbon Footprint Calculator. This version abandons standard digital layouts in favor of the **"Modern Eames"** design philosophy—a system prioritizing functional warmth, intentional asymmetry, and deep editorial typography, entirely constructed within the constraints of Java AWT (no Swing).

## Visual Identity: The Modern Eames Palette

The application uses tonal depth and an organic mid-century modern aesthetic:

- **Surface (`#fcf9f4`)**: The main application background.
- **Surface Container Low (`#f6f3ee`)**: Used for task cards and input fields.
- **Surface Container Highest (`#e5e2dd`)**: The Focus Hearth container.
- **Primary / Teal (`#306361`)**: Core CTA elements and gradients.
- **Tertiary / Terracotta (`#8e4732`)**: Used for soft, organic accents.
- **On-Surface (`#1c1c19`)**: High-legibility, off-black text.

### The "No-Line" Rule & Custom AWT Overrides
We enforce a strict "No-Line" rule. Layout boundaries are achieved through subtle background shifts (Tonal Stacking) rather than 1px borders. To achieve rounded, borderless UI elements in AWT, we override the `paint` methods of `Panel` components to render anti-aliased `Graphics2D` rounded rectangles (`TonalCard` and `FocusHearth`).

## Database Architecture & Logic Fixes

The backend relies on MySQL (`users`, `emission_factors`, `activity_logs`).

**Living System UX & Thread-Safe Event Logic:**
We have overhauled the calculation and UI feedback loop to prevent AWT UI freezing and provide immediate, real-time feedback:
1. **Input**: Quantities are captured via an interactive `Scrollbar` (Eco-Slider).
2. **Live Feedback**: As the slider moves, an `AdjustmentListener` fetches a locally cached emission factor to immediately update a "projected CO2" label, allowing the user to see the impact before committing to the DB.
3. **Asynchronous Execution**: Upon clicking "Record Impact", a background `Thread` is spawned.
3. **Secure Retrieval**: `DatabaseManager` runs a `PreparedStatement` to securely fetch the specific emission factor.
4. **Calculations**: `Total CO2 = Quantity * (Factor / 1000.0)`.
5. **State Updates**: Data is logged to the DB, and `EventQueue.invokeLater()` is dispatched to safely update the GUI.

## The Layout (1024x768 Asymmetric Grid)

The UI uses `setLayout(null)` to enforce an intentional, magazine-style layout with generous left margins:
- **Focus Hearth (`100, 40`)**: A wide top banner creating a warm, editorial greeting with a functional daily target progress bar.
- **Input Tonal Card (`100, 220`)**: The primary interaction layer stacked cleanly on the left.
- **Live Gauge Card (`524, 220`)**: A prominent hero metric panel on the right highlighting today's total.
- **Leaderboard Panel (`524, 440`)**: Displays the Top 5 users with the lowest emissions for the current month.

## How to Compile and Run

1. **Prerequisites**:
   - JDK 8+ installed.
   - Local MySQL Server on port 3306.
   - MySQL JDBC driver (`mysql-connector-java-8.x.x.jar`).

2. **Database Setup**:
   Execute the included `schema.sql` script to create the `ecotrack` database and tables.

3. **Compile**:
   ```bash
   javac -cp ".:mysql-connector-java-8.0.33.jar" DatabaseManager.java EcoTrackDashboard.java
   ```

4. **Run**:
   ```bash
   java -cp ".:mysql-connector-java-8.0.33.jar" EcoTrackDashboard
   ```
   *(Windows: Use `;` instead of `:` for the classpath separator).*