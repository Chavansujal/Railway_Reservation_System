CREATE DATABASE IF NOT EXISTS railway;
USE railway;

CREATE TABLE IF NOT EXISTS users (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(120) NOT NULL UNIQUE,
    password VARCHAR(100) NULL,
    password_hash VARCHAR(64) NULL
);

CREATE TABLE IF NOT EXISTS cities (
    id INT PRIMARY KEY AUTO_INCREMENT,
    city_name VARCHAR(80) NOT NULL UNIQUE,
    state_name VARCHAR(80) NOT NULL
);

CREATE TABLE IF NOT EXISTS routes (
    id INT PRIMARY KEY AUTO_INCREMENT,
    source_city_id INT NOT NULL,
    destination_city_id INT NOT NULL,
    distance_km INT NOT NULL,
    UNIQUE KEY uk_route (source_city_id, destination_city_id),
    CONSTRAINT fk_routes_source FOREIGN KEY (source_city_id) REFERENCES cities(id),
    CONSTRAINT fk_routes_destination FOREIGN KEY (destination_city_id) REFERENCES cities(id)
);

CREATE TABLE IF NOT EXISTS trains (
    id INT PRIMARY KEY AUTO_INCREMENT,
    train_number VARCHAR(20) NOT NULL UNIQUE,
    train_name VARCHAR(100) NOT NULL,
    route_id INT NOT NULL,
    departure_time TIME NOT NULL,
    arrival_time TIME NOT NULL,
    available_seats INT NOT NULL DEFAULT 200,
    CONSTRAINT fk_trains_route FOREIGN KEY (route_id) REFERENCES routes(id)
);

CREATE TABLE IF NOT EXISTS bookings (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    source VARCHAR(80) NOT NULL,
    destination VARCHAR(80) NOT NULL,
    journey_date DATE NOT NULL,
    seats INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bookings_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

ALTER TABLE bookings ADD COLUMN IF NOT EXISTS train_id INT NULL;
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS train_number VARCHAR(20) NULL;
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS train_name VARCHAR(100) NULL;

CREATE TABLE IF NOT EXISTS feedback (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    rating INT NOT NULL,
    feedback_text TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_feedback_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

INSERT IGNORE INTO cities (city_name, state_name) VALUES
    ('Mumbai', 'Maharashtra'),
    ('New Delhi', 'Delhi'),
    ('Kolkata', 'West Bengal'),
    ('Chennai', 'Tamil Nadu'),
    ('Bengaluru', 'Karnataka'),
    ('Hyderabad', 'Telangana'),
    ('Pune', 'Maharashtra'),
    ('Ahmedabad', 'Gujarat'),
    ('Jaipur', 'Rajasthan'),
    ('Lucknow', 'Uttar Pradesh'),
    ('Patna', 'Bihar'),
    ('Bhopal', 'Madhya Pradesh'),
    ('Nagpur', 'Maharashtra'),
    ('Surat', 'Gujarat'),
    ('Goa', 'Goa'),
    ('Kanpur', 'Uttar Pradesh'),
    ('Chandigarh', 'Chandigarh'),
    ('Indore', 'Madhya Pradesh'),
    ('Varanasi', 'Uttar Pradesh'),
    ('Bhubaneswar', 'Odisha');

INSERT IGNORE INTO routes (source_city_id, destination_city_id, distance_km)
SELECT s.id, d.id, x.distance_km
FROM (
    SELECT 'Mumbai' AS source_city, 'New Delhi' AS destination_city, 1384 AS distance_km
    UNION ALL SELECT 'New Delhi', 'Mumbai', 1384
    UNION ALL SELECT 'New Delhi', 'Kolkata', 1530
    UNION ALL SELECT 'Kolkata', 'New Delhi', 1530
    UNION ALL SELECT 'Chennai', 'Bengaluru', 350
    UNION ALL SELECT 'Bengaluru', 'Chennai', 350
    UNION ALL SELECT 'Hyderabad', 'Pune', 560
    UNION ALL SELECT 'Pune', 'Hyderabad', 560
    UNION ALL SELECT 'Ahmedabad', 'Jaipur', 660
    UNION ALL SELECT 'Jaipur', 'Ahmedabad', 660
    UNION ALL SELECT 'Lucknow', 'Patna', 535
    UNION ALL SELECT 'Patna', 'Lucknow', 535
    UNION ALL SELECT 'Bhopal', 'Nagpur', 352
    UNION ALL SELECT 'Nagpur', 'Bhopal', 352
    UNION ALL SELECT 'Surat', 'Goa', 705
    UNION ALL SELECT 'Goa', 'Surat', 705
    UNION ALL SELECT 'Kanpur', 'Chandigarh', 520
    UNION ALL SELECT 'Chandigarh', 'Kanpur', 520
    UNION ALL SELECT 'Indore', 'Varanasi', 925
    UNION ALL SELECT 'Varanasi', 'Indore', 925
) x
JOIN cities s ON s.city_name = x.source_city
JOIN cities d ON d.city_name = x.destination_city;

INSERT IGNORE INTO trains (train_number, train_name, route_id, departure_time, arrival_time, available_seats)
SELECT x.train_number, x.train_name, r.id, x.departure_time, x.arrival_time, x.available_seats
FROM (
    SELECT '12951' AS train_number, 'Rajdhani Express' AS train_name, 'Mumbai' AS source_city, 'New Delhi' AS destination_city, '16:30:00' AS departure_time, '08:35:00' AS arrival_time, 240 AS available_seats
    UNION ALL SELECT '12952', 'Rajdhani Express Return', 'New Delhi', 'Mumbai', '17:00:00', '09:05:00', 240
    UNION ALL SELECT '12302', 'Howrah Mail', 'New Delhi', 'Kolkata', '17:20:00', '10:15:00', 210
    UNION ALL SELECT '12301', 'Howrah Mail Return', 'Kolkata', 'New Delhi', '16:10:00', '09:00:00', 210
    UNION ALL SELECT '12608', 'Lalbagh Express', 'Chennai', 'Bengaluru', '06:10:00', '11:45:00', 180
    UNION ALL SELECT '12607', 'Lalbagh Express Return', 'Bengaluru', 'Chennai', '14:15:00', '19:50:00', 180
    UNION ALL SELECT '12724', 'Deccan Queen', 'Hyderabad', 'Pune', '07:15:00', '16:20:00', 160
    UNION ALL SELECT '12723', 'Deccan Queen Return', 'Pune', 'Hyderabad', '06:40:00', '15:50:00', 160
    UNION ALL SELECT '12980', 'Maru Sagar Express', 'Ahmedabad', 'Jaipur', '21:00:00', '08:10:00', 175
    UNION ALL SELECT '12979', 'Maru Sagar Express Return', 'Jaipur', 'Ahmedabad', '20:10:00', '07:15:00', 175
    UNION ALL SELECT '13202', 'Magadh Express', 'Lucknow', 'Patna', '20:25:00', '06:40:00', 190
    UNION ALL SELECT '13201', 'Magadh Express Return', 'Patna', 'Lucknow', '21:10:00', '07:30:00', 190
    UNION ALL SELECT '12160', 'Narmada Express', 'Bhopal', 'Nagpur', '14:30:00', '20:55:00', 150
    UNION ALL SELECT '12159', 'Narmada Express Return', 'Nagpur', 'Bhopal', '07:25:00', '13:45:00', 150
    UNION ALL SELECT '22918', 'Konkan Superfast', 'Surat', 'Goa', '18:05:00', '08:15:00', 170
    UNION ALL SELECT '22917', 'Konkan Superfast Return', 'Goa', 'Surat', '19:00:00', '09:10:00', 170
    UNION ALL SELECT '12442', 'Shatabdi Link', 'Kanpur', 'Chandigarh', '05:50:00', '13:30:00', 165
    UNION ALL SELECT '12441', 'Shatabdi Link Return', 'Chandigarh', 'Kanpur', '15:10:00', '22:40:00', 165
    UNION ALL SELECT '19314', 'Kashi Express', 'Indore', 'Varanasi', '19:40:00', '11:25:00', 185
    UNION ALL SELECT '19313', 'Kashi Express Return', 'Varanasi', 'Indore', '18:20:00', '10:05:00', 185
) x
JOIN routes r ON r.source_city_id = (SELECT id FROM cities WHERE city_name = x.source_city)
             AND r.destination_city_id = (SELECT id FROM cities WHERE city_name = x.destination_city);

UPDATE users
SET password = password_hash
WHERE (password IS NULL OR password = '') AND password_hash IS NOT NULL;

