-- Create and Use Database
CREATE DATABASE IF NOT EXISTS ecotrack;
USE ecotrack;

-- Users Table
CREATE TABLE IF NOT EXISTS users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    total_score DECIMAL(10,2) DEFAULT 0.00
);

-- Emission Factors Table (Fetched on application startup)
CREATE TABLE IF NOT EXISTS emission_factors (
    factor_id INT AUTO_INCREMENT PRIMARY KEY,
    activity_type VARCHAR(50) NOT NULL UNIQUE,
    co2_per_unit DECIMAL(10,4) NOT NULL,
    unit_name VARCHAR(20) NOT NULL
);

-- Daily Activity Logs Table
CREATE TABLE IF NOT EXISTS activity_logs (
    log_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    activity_date DATE NOT NULL,
    activity_type VARCHAR(50) NOT NULL,
    quantity DECIMAL(10,2) NOT NULL,
    calculated_co2 DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- Insert Initial Emission Factors
INSERT IGNORE INTO emission_factors (activity_type, co2_per_unit, unit_name) VALUES
('Driving (Car)', 0.2100, 'km'),
('Public Transit', 0.0400, 'km'),
('Shower', 0.1200, 'minutes'),
('Meat Meal', 2.5000, 'servings');
