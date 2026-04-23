import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class BookTicket extends JFrame {

    private final int userId;

    private JComboBox<String> sourceCombo;
    private JComboBox<String> destinationCombo;
    private JTextField dateField;
    private JTextField seatsField;
    private JButton searchBtn;
    private JButton bookBtn;
    private JTable trainTable;
    private DefaultTableModel trainModel;

    public BookTicket(int userId) {

        this.userId = userId;

        setTitle("Book Ticket");
        setSize(900, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel searchPanel = new JPanel(new GridLayout(5, 2, 10, 10));
        searchPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        searchPanel.add(new JLabel("Source:"));
        sourceCombo = new JComboBox<>();
        searchPanel.add(sourceCombo);

        searchPanel.add(new JLabel("Destination:"));
        destinationCombo = new JComboBox<>();
        searchPanel.add(destinationCombo);

        searchPanel.add(new JLabel("Journey Date (YYYY-MM-DD):"));
        dateField = new JTextField();
        searchPanel.add(dateField);

        searchPanel.add(new JLabel("Number of Seats:"));
        seatsField = new JTextField();
        seatsField.setText("1");
        searchPanel.add(seatsField);

        searchBtn = new JButton("Search Trains");
        bookBtn = new JButton("Confirm Booking");
        searchPanel.add(searchBtn);
        searchPanel.add(bookBtn);

        String[] columns = {
            "Train ID",
            "Train Number",
            "Train Name",
            "Source",
            "Destination",
            "Departure",
            "Arrival",
            "Available Seats"
        };
        trainModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        trainTable = new JTable(trainModel);
        trainTable.removeColumn(trainTable.getColumnModel().getColumn(0));

        add(searchPanel, BorderLayout.NORTH);
        add(new JScrollPane(trainTable), BorderLayout.CENTER);

        loadCities();

        searchBtn.addActionListener(e -> searchTrains());
        bookBtn.addActionListener(e -> confirmBooking());

        setVisible(true);
    }

    private void loadCities() {
        String sql = "SELECT city_name FROM cities ORDER BY city_name";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {

            sourceCombo.removeAllItems();
            destinationCombo.removeAllItems();

            while (rs.next()) {
                String city = rs.getString("city_name");
                sourceCombo.addItem(city);
                destinationCombo.addItem(city);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Could not load cities from database.");
        }
    }

    private void searchTrains() {
        String source = getSelectedCity(sourceCombo);
        String destination = getSelectedCity(destinationCombo);
        int seats = parseSeatsForSearch();

        if (source.isEmpty() || destination.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select source and destination.");
            return;
        }

        if (source.equalsIgnoreCase(destination)) {
            JOptionPane.showMessageDialog(this, "Source and destination cannot be the same.");
            return;
        }

        if (seats <= 0) {
            JOptionPane.showMessageDialog(this, "Please enter a valid seat count.");
            return;
        }

        trainModel.setRowCount(0);

        String sql =
            "SELECT t.id, t.train_number, t.train_name, sc.city_name AS source, dc.city_name AS destination, " +
            "t.departure_time, t.arrival_time, t.available_seats " +
            "FROM trains t " +
            "JOIN routes r ON t.route_id = r.id " +
            "JOIN cities sc ON r.source_city_id = sc.id " +
            "JOIN cities dc ON r.destination_city_id = dc.id " +
            "WHERE sc.city_name = ? AND dc.city_name = ? AND t.available_seats >= ? " +
            "ORDER BY t.departure_time";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setString(1, source);
            pst.setString(2, destination);
            pst.setInt(3, seats);

            try (ResultSet rs = pst.executeQuery()) {
                boolean found = false;
                while (rs.next()) {
                    found = true;
                    trainModel.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("train_number"),
                        rs.getString("train_name"),
                        rs.getString("source"),
                        rs.getString("destination"),
                        rs.getTime("departure_time"),
                        rs.getTime("arrival_time"),
                        rs.getInt("available_seats")
                    });
                }

                if (!found) {
                    JOptionPane.showMessageDialog(this, "No trains found for this route.");
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Train search failed.");
        }
    }

    private void confirmBooking() {
        int selectedRow = trainTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a train first.");
            return;
        }

        String journeyDate = dateField.getText().trim();
        if (journeyDate.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter the journey date.");
            return;
        }

        int seats = parseSeats();
        if (seats <= 0) {
            JOptionPane.showMessageDialog(this, "Please enter a valid seat count.");
            return;
        }

        int modelRow = trainTable.convertRowIndexToModel(selectedRow);
        int trainId = Integer.parseInt(trainModel.getValueAt(modelRow, 0).toString());
        String trainNumber = trainModel.getValueAt(modelRow, 1).toString();
        String trainName = trainModel.getValueAt(modelRow, 2).toString();
        String source = trainModel.getValueAt(modelRow, 3).toString();
        String destination = trainModel.getValueAt(modelRow, 4).toString();

        String seatCheckSql = "SELECT available_seats FROM trains WHERE id = ?";
        String bookingSql =
            "INSERT INTO bookings(user_id, train_id, train_number, train_name, source, destination, journey_date, seats) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        String updateSeatsSql = "UPDATE trains SET available_seats = available_seats - ? WHERE id = ?";

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);

            try (PreparedStatement seatCheckPst = con.prepareStatement(seatCheckSql)) {
                seatCheckPst.setInt(1, trainId);

                int availableSeats;
                try (ResultSet rs = seatCheckPst.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalStateException("Train not found.");
                    }
                    availableSeats = rs.getInt("available_seats");
                }

                if (availableSeats < seats) {
                    con.rollback();
                    JOptionPane.showMessageDialog(this, "Not enough seats available on this train.");
                    return;
                }
            }

            try (PreparedStatement bookingPst = con.prepareStatement(bookingSql);
                 PreparedStatement updateSeatsPst = con.prepareStatement(updateSeatsSql)) {

                bookingPst.setInt(1, userId);
                bookingPst.setInt(2, trainId);
                bookingPst.setString(3, trainNumber);
                bookingPst.setString(4, trainName);
                bookingPst.setString(5, source);
                bookingPst.setString(6, destination);
                bookingPst.setDate(7, Date.valueOf(journeyDate));
                bookingPst.setInt(8, seats);
                bookingPst.executeUpdate();

                updateSeatsPst.setInt(1, seats);
                updateSeatsPst.setInt(2, trainId);
                updateSeatsPst.executeUpdate();

                con.commit();
                JOptionPane.showMessageDialog(this, "Booking successful for " + trainName + ".");
                searchTrains();
            } catch (Exception ex) {
                con.rollback();
                throw ex;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, "Please enter the date in YYYY-MM-DD format.");
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Booking failed.");
        }
    }

    private int parseSeats() {
        try {
            return Integer.parseInt(seatsField.getText().trim());
        } catch (Exception ex) {
            return -1;
        }
    }

    private int parseSeatsForSearch() {
        String text = seatsField.getText().trim();
        if (text.isEmpty()) {
            seatsField.setText("1");
            return 1;
        }
        return parseSeats();
    }

    private String getSelectedCity(JComboBox<String> comboBox) {
        Object value = comboBox.getSelectedItem();
        return value == null ? "" : value.toString();
    }
}
