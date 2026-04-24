import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class ApiServer {
    private static final int PORT = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
    private static final Path FRONTEND_ROOT = Path.of("web", "frontend").toAbsolutePath().normalize();

    private static final DbConfig DB_CONFIG = loadDbConfig();

    public static void main(String[] args) throws IOException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            initDatabase();
        } catch (Exception ex) {
            System.err.println("Database initialization failed: " + ex.getMessage());
            ex.printStackTrace();
            return;
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/api/register", new RegisterHandler());
        server.createContext("/api/login", new LoginHandler());
        server.createContext("/api/bookings", new BookingsHandler());
        server.createContext("/api/cities", new CitiesHandler());
        server.createContext("/api/trains", new TrainsHandler());
        server.createContext("/api/health", exchange -> {
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }
            sendJson(exchange, 200, "{\"status\":\"ok\"}");
        });
        server.createContext("/", new StaticFileHandler());

        server.setExecutor(null);
        server.start();
        System.out.println("Server running on http://localhost:" + PORT);
    }

    private static void initDatabase() throws SQLException {
        try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                "CREATE TABLE IF NOT EXISTS users (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT," +
                    "name VARCHAR(100) NOT NULL," +
                    "email VARCHAR(120) NOT NULL UNIQUE," +
                    "password_hash VARCHAR(64) NOT NULL" +
                ")"
            );

            statement.executeUpdate(
                "CREATE TABLE IF NOT EXISTS bookings (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT," +
                    "user_id INT NOT NULL," +
                    "source VARCHAR(80) NOT NULL," +
                    "destination VARCHAR(80) NOT NULL," +
                    "journey_date DATE NOT NULL," +
                    "seats INT NOT NULL," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE" +
                ")"
            );
        }
    }

    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_CONFIG.url, DB_CONFIG.user, DB_CONFIG.password);
    }

    private static DbConfig loadDbConfig() {
        String url = firstEnv("RAILWAY_DB_URL", "JDBC_DATABASE_URL");
        String user = firstEnv("RAILWAY_DB_USER", "DB_USER", "MYSQLUSER", "MYSQL_USER");
        String password = firstEnv("RAILWAY_DB_PASSWORD", "DB_PASSWORD", "MYSQLPASSWORD", "MYSQL_PASSWORD");

        if (url.isBlank()) {
            String mysqlUrl = firstEnv("MYSQL_URL", "DATABASE_URL");
            if (mysqlUrl.startsWith("jdbc:")) {
                url = mysqlUrl;
            } else if (mysqlUrl.startsWith("mysql://")) {
                DbConfig parsedConfig = parseMysqlUrl(mysqlUrl);
                url = parsedConfig.url;
                if (user.isBlank()) user = parsedConfig.user;
                if (password.isBlank()) password = parsedConfig.password;
            }
        }

        if (url.isBlank()) {
            String host = firstEnv("DB_HOST", "MYSQLHOST", "MYSQL_HOST");
            String port = firstEnv("DB_PORT", "MYSQLPORT", "MYSQL_PORT");
            String database = firstEnv("DB_NAME", "MYSQLDATABASE", "MYSQL_DATABASE");

            if (host.isBlank()) host = "localhost";
            if (port.isBlank()) port = "3306";
            if (database.isBlank()) database = "railway";

            url = "jdbc:mysql://" + host + ":" + port + "/" + database;
        }

        if (user.isBlank()) user = "root";
        if (password.isBlank()) password = "12345";

        return new DbConfig(url, user, password);
    }

    private static DbConfig parseMysqlUrl(String mysqlUrl) {
        try {
            URI uri = URI.create(mysqlUrl);
            String host = uri.getHost();
            int port = uri.getPort() > 0 ? uri.getPort() : 3306;
            String database = uri.getPath() == null ? "railway" : uri.getPath().replaceFirst("^/", "");
            String user = "";
            String password = "";

            String userInfo = uri.getUserInfo();
            if (userInfo != null) {
                String[] parts = userInfo.split(":", 2);
                user = decode(parts[0]);
                if (parts.length > 1) password = decode(parts[1]);
            }

            return new DbConfig("jdbc:mysql://" + host + ":" + port + "/" + database, user, password);
        } catch (Exception ex) {
            return new DbConfig("", "", "");
        }
    }

    private static String firstEnv(String... names) {
        for (String name : names) {
            String value = System.getenv(name);
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private static class DbConfig {
        private final String url;
        private final String user;
        private final String password;

        private DbConfig(String url, String user, String password) {
            this.url = url;
            this.user = user;
            this.password = password;
        }
    }

    private static class RegisterHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            Map<String, String> data = parseForm(exchange.getRequestBody());
            String name = safe(data.get("name"));
            String email = safe(data.get("email")).toLowerCase();
            String password = safe(data.get("password"));

            if (name.isBlank() || email.isBlank() || password.isBlank()) {
                sendJson(exchange, 400, "{\"error\":\"All fields are required\"}");
                return;
            }

            String insertSql = "INSERT INTO users(name, email, password_hash) VALUES (?, ?, ?)";
            try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(insertSql)) {
                statement.setString(1, name);
                statement.setString(2, email);
                statement.setString(3, hash(password));
                statement.executeUpdate();
                sendJson(exchange, 201, "{\"message\":\"Registration successful\"}");
            } catch (SQLException ex) {
                if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("duplicate")) {
                    sendJson(exchange, 409, "{\"error\":\"Email already exists\"}");
                } else {
                    sendJson(exchange, 500, "{\"error\":\"Registration failed\"}");
                }
            }
        }
    }

    private static class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            Map<String, String> data = parseForm(exchange.getRequestBody());
            String email = safe(data.get("email")).toLowerCase();
            String password = safe(data.get("password"));

            if (email.isBlank() || password.isBlank()) {
                sendJson(exchange, 400, "{\"error\":\"Email and password are required\"}");
                return;
            }

            String sql = "SELECT id, name, password_hash FROM users WHERE email = ?";
            try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, email);

                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        sendJson(exchange, 401, "{\"error\":\"Invalid credentials\"}");
                        return;
                    }

                    String expectedHash = resultSet.getString("password_hash");
                    if (!expectedHash.equals(hash(password))) {
                        sendJson(exchange, 401, "{\"error\":\"Invalid credentials\"}");
                        return;
                    }

                    int userId = resultSet.getInt("id");
                    String name = resultSet.getString("name");
                    String payload = "{\"message\":\"Login successful\",\"user\":{\"id\":" + userId + ",\"name\":\"" + escapeJson(name) + "\",\"email\":\"" + escapeJson(email) + "\"}}";
                    sendJson(exchange, 200, payload);
                }
            } catch (SQLException ex) {
                sendJson(exchange, 500, "{\"error\":\"Login failed\"}");
            }
        }
    }

    private static class BookingsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            if ("POST".equals(method)) {
                createBooking(exchange);
                return;
            }
            if ("GET".equals(method)) {
                listBookings(exchange);
                return;
            }
            if ("DELETE".equals(method)) {
                cancelBooking(exchange);
                return;
            }
            sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
        }

        private void createBooking(HttpExchange exchange) throws IOException {
            Map<String, String> data = parseForm(exchange.getRequestBody());

            int userId = parseInt(data.get("userId"));
            String source = safe(data.get("source"));
            String destination = safe(data.get("destination"));
            String dateText = safe(data.get("journeyDate"));
            int seats = parseInt(data.get("seats"));

            if (userId <= 0 || source.isBlank() || destination.isBlank() || dateText.isBlank() || seats <= 0) {
                sendJson(exchange, 400, "{\"error\":\"Invalid booking details\"}");
                return;
            }

            LocalDate journeyDate;
            try {
                journeyDate = LocalDate.parse(dateText);
            } catch (Exception ex) {
                sendJson(exchange, 400, "{\"error\":\"Invalid journey date format\"}");
                return;
            }

            String sql = "INSERT INTO bookings(user_id, source, destination, journey_date, seats) VALUES (?, ?, ?, ?, ?)";
            try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, userId);
                statement.setString(2, source);
                statement.setString(3, destination);
                statement.setDate(4, Date.valueOf(journeyDate));
                statement.setInt(5, seats);
                statement.executeUpdate();
                sendJson(exchange, 201, "{\"message\":\"Booking successful\"}");
            } catch (SQLException ex) {
                sendJson(exchange, 500, "{\"error\":\"Booking failed\"}");
            }
        }

        private void listBookings(HttpExchange exchange) throws IOException {
            Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
            int userId = parseInt(query.get("userId"));

            if (userId <= 0) {
                sendJson(exchange, 400, "{\"error\":\"userId is required\"}");
                return;
            }

            String sql = "SELECT id, source, destination, journey_date, seats FROM bookings WHERE user_id = ? ORDER BY journey_date ASC";
            try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, userId);

                try (ResultSet resultSet = statement.executeQuery()) {
                    StringBuilder json = new StringBuilder();
                    json.append("{\"bookings\":[");
                    boolean first = true;
                    while (resultSet.next()) {
                        if (!first) {
                            json.append(",");
                        }
                        first = false;

                        json.append("{")
                            .append("\"id\":").append(resultSet.getInt("id")).append(",")
                            .append("\"source\":\"").append(escapeJson(resultSet.getString("source"))).append("\",")
                            .append("\"destination\":\"").append(escapeJson(resultSet.getString("destination"))).append("\",")
                            .append("\"journeyDate\":\"").append(resultSet.getDate("journey_date")).append("\",")
                            .append("\"seats\":").append(resultSet.getInt("seats"))
                            .append("}");
                    }
                    json.append("]}");
                    sendJson(exchange, 200, json.toString());
                }
            } catch (SQLException ex) {
                sendJson(exchange, 500, "{\"error\":\"Could not load bookings\"}");
            }
        }
        private void cancelBooking(HttpExchange exchange) throws IOException {
            Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
            int userId = parseInt(query.get("userId"));
            int bookingId = parseInt(query.get("bookingId"));

            if (userId <= 0 || bookingId <= 0) {
                sendJson(exchange, 400, "{\"error\":\"userId and bookingId are required\"}");
                return;
            }

            String sql = "DELETE FROM bookings WHERE id = ? AND user_id = ?";
            try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, bookingId);
                statement.setInt(2, userId);
                int affectedRows = statement.executeUpdate();

                if (affectedRows == 0) {
                    sendJson(exchange, 404, "{\"error\":\"Booking not found\"}");
                    return;
                }

                sendJson(exchange, 200, "{\"message\":\"Booking cancelled successfully\"}");
            } catch (SQLException ex) {
                sendJson(exchange, 500, "{\"error\":\"Could not cancel booking\"}");
            }
        }
    }

    private static class CitiesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            String sql = "SELECT city_name, state_name FROM cities ORDER BY city_name ASC";
            try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql); ResultSet resultSet = statement.executeQuery()) {
                StringBuilder json = new StringBuilder();
                json.append("{\"cities\":[");
                boolean first = true;
                while (resultSet.next()) {
                    if (!first) {
                        json.append(",");
                    }
                    first = false;

                    json.append("{")
                        .append("\"name\":\"").append(escapeJson(resultSet.getString("city_name"))).append("\",")
                        .append("\"state\":\"").append(escapeJson(resultSet.getString("state_name"))).append("\"")
                        .append("}");
                }
                json.append("]}");
                sendJson(exchange, 200, json.toString());
            } catch (SQLException ex) {
                sendJson(exchange, 500, "{\"error\":\"Could not load cities\"}");
            }
        }
    }

    private static class TrainsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
            String from = safe(query.get("from"));
            String to = safe(query.get("to"));

            String sql =
                "SELECT t.id, t.train_number, t.train_name, " +
                "sc.city_name AS source_city, dc.city_name AS destination_city, " +
                "TIME_FORMAT(t.departure_time, '%H:%i') AS depart, " +
                "TIME_FORMAT(t.arrival_time, '%H:%i') AS arrive, " +
                "TIMESTAMPDIFF(MINUTE, " +
                "  CONCAT('2000-01-01 ', t.departure_time), " +
                "  DATE_ADD(CONCAT('2000-01-01 ', t.arrival_time), INTERVAL (t.arrival_time < t.departure_time) DAY)" +
                ") AS duration_minutes, " +
                "t.available_seats, r.distance_km " +
                "FROM trains t " +
                "JOIN routes r ON t.route_id = r.id " +
                "JOIN cities sc ON r.source_city_id = sc.id " +
                "JOIN cities dc ON r.destination_city_id = dc.id " +
                "WHERE (? = '' OR sc.city_name = ?) " +
                "AND (? = '' OR dc.city_name = ?) " +
                "ORDER BY t.departure_time ASC";

            try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, from);
                statement.setString(2, from);
                statement.setString(3, to);
                statement.setString(4, to);

                try (ResultSet resultSet = statement.executeQuery()) {
                    StringBuilder json = new StringBuilder();
                    json.append("{\"trains\":[");
                    boolean first = true;
                    while (resultSet.next()) {
                        if (!first) {
                            json.append(",");
                        }
                        first = false;

                        int distanceKm = resultSet.getInt("distance_km");
                        int estimatedFare = Math.max(250, distanceKm * 2);
                        int platform = (resultSet.getInt("id") % 7) + 1;

                        json.append("{")
                            .append("\"number\":\"").append(escapeJson(resultSet.getString("train_number"))).append("\",")
                            .append("\"name\":\"").append(escapeJson(resultSet.getString("train_name"))).append("\",")
                            .append("\"from\":\"").append(escapeJson(resultSet.getString("source_city"))).append("\",")
                            .append("\"to\":\"").append(escapeJson(resultSet.getString("destination_city"))).append("\",")
                            .append("\"depart\":\"").append(escapeJson(resultSet.getString("depart"))).append("\",")
                            .append("\"arrive\":\"").append(escapeJson(resultSet.getString("arrive"))).append("\",")
                            .append("\"duration\":").append(resultSet.getInt("duration_minutes")).append(",")
                            .append("\"seats\":").append(resultSet.getInt("available_seats")).append(",")
                            .append("\"fare\":").append(estimatedFare).append(",")
                            .append("\"days\":\"Daily\",")
                            .append("\"platform\":").append(platform)
                            .append("}");
                    }
                    json.append("]}");
                    sendJson(exchange, 200, json.toString());
                }
            } catch (SQLException ex) {
                sendJson(exchange, 500, "{\"error\":\"Could not load trains\"}");
            }
        }
    }

    private static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendText(exchange, 405, "Method not allowed", "text/plain");
                return;
            }

            String rawPath = exchange.getRequestURI().getPath();
            String requestPath = (rawPath == null || rawPath.equals("/")) ? "/index.html" : rawPath;
            Path resolvedPath = FRONTEND_ROOT.resolve(requestPath.substring(1)).normalize();

            if (!resolvedPath.startsWith(FRONTEND_ROOT) || !Files.exists(resolvedPath) || Files.isDirectory(resolvedPath)) {
                sendText(exchange, 404, "Not found", "text/plain");
                return;
            }

            byte[] content = Files.readAllBytes(resolvedPath);
            sendBytes(exchange, 200, content, contentType(resolvedPath.getFileName().toString()));
        }
    }

    private static Map<String, String> parseForm(InputStream body) throws IOException {
        String content = new String(body.readAllBytes(), StandardCharsets.UTF_8);
        return parseQuery(content);
    }

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.isBlank()) {
            return map;
        }

        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] parts = pair.split("=", 2);
            String key = decode(parts[0]);
            String value = parts.length > 1 ? decode(parts[1]) : "";
            map.put(key, value);
        }
        return map;
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        sendText(exchange, status, json, "application/json; charset=utf-8");
    }

    private static void sendText(HttpExchange exchange, int status, String text, String contentType) throws IOException {
        sendBytes(exchange, status, text.getBytes(StandardCharsets.UTF_8), contentType);
    }

    private static void sendBytes(HttpExchange exchange, int status, byte[] bytes, String contentType) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String contentType(String filename) {
        if (filename.endsWith(".html")) return "text/html; charset=utf-8";
        if (filename.endsWith(".css")) return "text/css; charset=utf-8";
        if (filename.endsWith(".js")) return "application/javascript; charset=utf-8";
        return "application/octet-stream";
    }

    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ex) {
            return -1;
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new RuntimeException("Could not hash password", ex);
        }
    }

    private static String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}


