import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DBConnection {

    private static final String DB_NAME = System.getenv().getOrDefault("RAILWAY_DB_NAME", "railway");
    private static final String SERVER_URL = System.getenv().getOrDefault("RAILWAY_DB_SERVER_URL", "jdbc:mysql://localhost:3306/");
    private static final String URL = System.getenv().getOrDefault("RAILWAY_DB_URL", "jdbc:mysql://localhost:3306/railway");
    private static final String USER = System.getenv().getOrDefault("RAILWAY_DB_USER", "root");
    private static final String PASSWORD = System.getenv().getOrDefault("RAILWAY_DB_PASSWORD", "12345");

    private static boolean initialized = false;

    public static Connection getConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            initializeDatabase();
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (Exception e) {
            System.out.println("Database connection failed!");
            e.printStackTrace();
            return null;
        }
    }

    public static synchronized void initializeDatabase() throws SQLException {
        if (initialized) {
            return;
        }

        createDatabaseIfMissing();

        try (Connection con = DriverManager.getConnection(URL, USER, PASSWORD)) {
            createTables(con);
            upgradeUsersTable(con);
            upgradeBookingsTable(con);
            seedCities(con);
            seedRoutes(con);
            seedTrains(con);
            initialized = true;
            System.out.println("Database initialized successfully.");
        }
    }

    private static void createTables(Connection con) throws SQLException {
        try (Statement stmt = con.createStatement()) {
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS users (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT," +
                    "name VARCHAR(100) NOT NULL," +
                    "email VARCHAR(120) NOT NULL UNIQUE," +
                    "password VARCHAR(100) NOT NULL" +
                ")"
            );

            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS cities (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT," +
                    "city_name VARCHAR(80) NOT NULL UNIQUE," +
                    "state_name VARCHAR(80) NOT NULL" +
                ")"
            );

            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS routes (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT," +
                    "source_city_id INT NOT NULL," +
                    "destination_city_id INT NOT NULL," +
                    "distance_km INT NOT NULL," +
                    "UNIQUE KEY uk_route (source_city_id, destination_city_id)," +
                    "CONSTRAINT fk_routes_source FOREIGN KEY (source_city_id) REFERENCES cities(id)," +
                    "CONSTRAINT fk_routes_destination FOREIGN KEY (destination_city_id) REFERENCES cities(id)" +
                ")"
            );

            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS trains (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT," +
                    "train_number VARCHAR(20) NOT NULL UNIQUE," +
                    "train_name VARCHAR(100) NOT NULL," +
                    "route_id INT NOT NULL," +
                    "departure_time TIME NOT NULL," +
                    "arrival_time TIME NOT NULL," +
                    "available_seats INT NOT NULL DEFAULT 200," +
                    "CONSTRAINT fk_trains_route FOREIGN KEY (route_id) REFERENCES routes(id)" +
                ")"
            );

            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS bookings (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT," +
                    "user_id INT NOT NULL," +
                    "source VARCHAR(80) NOT NULL," +
                    "destination VARCHAR(80) NOT NULL," +
                    "journey_date DATE NOT NULL," +
                    "seats INT NOT NULL," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "CONSTRAINT fk_bookings_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE" +
                ")"
            );

            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS feedback (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT," +
                    "user_id INT NOT NULL," +
                    "rating INT NOT NULL," +
                    "feedback_text TEXT NOT NULL," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "CONSTRAINT fk_feedback_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE" +
                ")"
            );
        }
    }

    private static void createDatabaseIfMissing() throws SQLException {
        try (Connection con = DriverManager.getConnection(SERVER_URL, USER, PASSWORD);
             Statement stmt = con.createStatement()) {
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + DB_NAME);
        }
    }

    private static void upgradeUsersTable(Connection con) throws SQLException {
        ensureColumnExists(con, "users", "password", "VARCHAR(100) NULL");

        if (columnExists(con, "users", "password_hash")) {
            try (Statement stmt = con.createStatement()) {
                stmt.executeUpdate(
                    "UPDATE users SET password = password_hash " +
                    "WHERE (password IS NULL OR password = '') AND password_hash IS NOT NULL"
                );
            }
        }
    }

    private static void upgradeBookingsTable(Connection con) throws SQLException {
        ensureColumnExists(con, "bookings", "train_id", "INT NULL");
        ensureColumnExists(con, "bookings", "train_number", "VARCHAR(20) NULL");
        ensureColumnExists(con, "bookings", "train_name", "VARCHAR(100) NULL");
    }

    private static void ensureColumnExists(Connection con, String tableName, String columnName, String definition)
        throws SQLException {
        if (columnExists(con, tableName, columnName)) {
            return;
        }

        try (Statement stmt = con.createStatement()) {
            stmt.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + definition);
        }
    }

    private static boolean columnExists(Connection con, String tableName, String columnName) throws SQLException {
        DatabaseMetaData metaData = con.getMetaData();
        try (ResultSet rs = metaData.getColumns(con.getCatalog(), null, tableName, columnName)) {
            return rs.next();
        }
    }

    private static void seedCities(Connection con) throws SQLException {
        String[][] cities = {
            {"Mumbai", "Maharashtra"},
            {"New Delhi", "Delhi"},
            {"Kolkata", "West Bengal"},
            {"Chennai", "Tamil Nadu"},
            {"Bengaluru", "Karnataka"},
            {"Hyderabad", "Telangana"},
            {"Pune", "Maharashtra"},
            {"Ahmedabad", "Gujarat"},
            {"Jaipur", "Rajasthan"},
            {"Lucknow", "Uttar Pradesh"},
            {"Patna", "Bihar"},
            {"Bhopal", "Madhya Pradesh"},
            {"Nagpur", "Maharashtra"},
            {"Surat", "Gujarat"},
            {"Goa", "Goa"},
            {"Kanpur", "Uttar Pradesh"},
            {"Chandigarh", "Chandigarh"},
            {"Indore", "Madhya Pradesh"},
            {"Varanasi", "Uttar Pradesh"},
            {"Bhubaneswar", "Odisha"}
        };

        String sql = "INSERT INTO cities(city_name, state_name) VALUES (?, ?)";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            for (String[] city : cities) {
                if (!cityExists(con, city[0])) {
                    pst.setString(1, city[0]);
                    pst.setString(2, city[1]);
                    pst.addBatch();
                }
            }
            pst.executeBatch();
        }
    }

    private static void seedRoutes(Connection con) throws SQLException {
        Object[][] routes = {
            {"Mumbai", "New Delhi", 1384},
            {"New Delhi", "Mumbai", 1384},
            {"New Delhi", "Kolkata", 1530},
            {"Kolkata", "New Delhi", 1530},
            {"Chennai", "Bengaluru", 350},
            {"Bengaluru", "Chennai", 350},
            {"Hyderabad", "Pune", 560},
            {"Pune", "Hyderabad", 560},
            {"Ahmedabad", "Jaipur", 660},
            {"Jaipur", "Ahmedabad", 660},
            {"Lucknow", "Patna", 535},
            {"Patna", "Lucknow", 535},
            {"Bhopal", "Nagpur", 352},
            {"Nagpur", "Bhopal", 352},
            {"Surat", "Goa", 705},
            {"Goa", "Surat", 705},
            {"Kanpur", "Chandigarh", 520},
            {"Chandigarh", "Kanpur", 520},
            {"Indore", "Varanasi", 925},
            {"Varanasi", "Indore", 925}
        };

        for (Object[] route : routes) {
            insertRouteIfMissing(con, (String) route[0], (String) route[1], (Integer) route[2]);
        }
    }

    private static void seedTrains(Connection con) throws SQLException {
        Object[][] trains = {
            {"12951", "Rajdhani Express", "Mumbai", "New Delhi", "16:30:00", "08:35:00", 240},
            {"12952", "Rajdhani Express Return", "New Delhi", "Mumbai", "17:00:00", "09:05:00", 240},
            {"12302", "Howrah Mail", "New Delhi", "Kolkata", "17:20:00", "10:15:00", 210},
            {"12301", "Howrah Mail Return", "Kolkata", "New Delhi", "16:10:00", "09:00:00", 210},
            {"12608", "Lalbagh Express", "Chennai", "Bengaluru", "06:10:00", "11:45:00", 180},
            {"12607", "Lalbagh Express Return", "Bengaluru", "Chennai", "14:15:00", "19:50:00", 180},
            {"12724", "Deccan Queen", "Hyderabad", "Pune", "07:15:00", "16:20:00", 160},
            {"12723", "Deccan Queen Return", "Pune", "Hyderabad", "06:40:00", "15:50:00", 160},
            {"12980", "Maru Sagar Express", "Ahmedabad", "Jaipur", "21:00:00", "08:10:00", 175},
            {"12979", "Maru Sagar Express Return", "Jaipur", "Ahmedabad", "20:10:00", "07:15:00", 175},
            {"13202", "Magadh Express", "Lucknow", "Patna", "20:25:00", "06:40:00", 190},
            {"13201", "Magadh Express Return", "Patna", "Lucknow", "21:10:00", "07:30:00", 190},
            {"12160", "Narmada Express", "Bhopal", "Nagpur", "14:30:00", "20:55:00", 150},
            {"12159", "Narmada Express Return", "Nagpur", "Bhopal", "07:25:00", "13:45:00", 150},
            {"22918", "Konkan Superfast", "Surat", "Goa", "18:05:00", "08:15:00", 170},
            {"22917", "Konkan Superfast Return", "Goa", "Surat", "19:00:00", "09:10:00", 170},
            {"12442", "Shatabdi Link", "Kanpur", "Chandigarh", "05:50:00", "13:30:00", 165},
            {"12441", "Shatabdi Link Return", "Chandigarh", "Kanpur", "15:10:00", "22:40:00", 165},
            {"19314", "Kashi Express", "Indore", "Varanasi", "19:40:00", "11:25:00", 185},
            {"19313", "Kashi Express Return", "Varanasi", "Indore", "18:20:00", "10:05:00", 185}
        };

        for (Object[] train : trains) {
            insertTrainIfMissing(
                con,
                (String) train[0],
                (String) train[1],
                (String) train[2],
                (String) train[3],
                (String) train[4],
                (String) train[5],
                (Integer) train[6]
            );
        }
    }

    private static boolean cityExists(Connection con, String cityName) throws SQLException {
        String sql = "SELECT id FROM cities WHERE city_name = ?";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, cityName);
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static int getCityId(Connection con, String cityName) throws SQLException {
        String sql = "SELECT id FROM cities WHERE city_name = ?";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, cityName);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }
        throw new SQLException("City not found: " + cityName);
    }

    private static void insertRouteIfMissing(Connection con, String source, String destination, int distanceKm)
        throws SQLException {
        String checkSql = "SELECT id FROM routes WHERE source_city_id = ? AND destination_city_id = ?";
        String insertSql = "INSERT INTO routes(source_city_id, destination_city_id, distance_km) VALUES (?, ?, ?)";
        int sourceCityId = getCityId(con, source);
        int destinationCityId = getCityId(con, destination);

        try (PreparedStatement check = con.prepareStatement(checkSql)) {
            check.setInt(1, sourceCityId);
            check.setInt(2, destinationCityId);
            try (ResultSet rs = check.executeQuery()) {
                if (rs.next()) {
                    return;
                }
            }
        }

        try (PreparedStatement insert = con.prepareStatement(insertSql)) {
            insert.setInt(1, sourceCityId);
            insert.setInt(2, destinationCityId);
            insert.setInt(3, distanceKm);
            insert.executeUpdate();
        }
    }

    private static int getRouteId(Connection con, String source, String destination) throws SQLException {
        String sql =
            "SELECT r.id FROM routes r " +
            "JOIN cities sc ON r.source_city_id = sc.id " +
            "JOIN cities dc ON r.destination_city_id = dc.id " +
            "WHERE sc.city_name = ? AND dc.city_name = ?";

        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, source);
            pst.setString(2, destination);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }
        throw new SQLException("Route not found: " + source + " to " + destination);
    }

    private static void insertTrainIfMissing(
        Connection con,
        String trainNumber,
        String trainName,
        String source,
        String destination,
        String departureTime,
        String arrivalTime,
        int seats
    ) throws SQLException {
        String checkSql = "SELECT id FROM trains WHERE train_number = ?";
        String insertSql =
            "INSERT INTO trains(train_number, train_name, route_id, departure_time, arrival_time, available_seats) " +
            "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement check = con.prepareStatement(checkSql)) {
            check.setString(1, trainNumber);
            try (ResultSet rs = check.executeQuery()) {
                if (rs.next()) {
                    return;
                }
            }
        }

        try (PreparedStatement insert = con.prepareStatement(insertSql)) {
            insert.setString(1, trainNumber);
            insert.setString(2, trainName);
            insert.setInt(3, getRouteId(con, source, destination));
            insert.setString(4, departureTime);
            insert.setString(5, arrivalTime);
            insert.setInt(6, seats);
            insert.executeUpdate();
        }
    }
}
