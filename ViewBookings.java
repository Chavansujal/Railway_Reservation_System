import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ViewBookings extends JFrame {

    private final int userId;
    private JTable table;
    private DefaultTableModel model;

    public ViewBookings(int userId) {

        this.userId = userId;

        setTitle("Your Bookings");
        setSize(850, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        String[] columns = {"Booking ID", "Train Number", "Train Name", "Source", "Destination", "Date", "Seats"};
        model = new DefaultTableModel(columns, 0);
        table = new JTable(model);

        add(new JScrollPane(table));
        loadBookings();

        setVisible(true);
    }

    private void loadBookings() {
        String query =
            "SELECT id, train_number, train_name, source, destination, journey_date, seats " +
            "FROM bookings WHERE user_id = ? ORDER BY journey_date ASC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement pst = con.prepareStatement(query)) {

            if (con == null) {
                JOptionPane.showMessageDialog(this, "Database not connected!");
                return;
            }

            pst.setInt(1, userId);

            try (ResultSet rs = pst.executeQuery()) {
                boolean hasData = false;

                while (rs.next()) {
                    hasData = true;
                    model.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("train_number"),
                        rs.getString("train_name"),
                        rs.getString("source"),
                        rs.getString("destination"),
                        rs.getDate("journey_date"),
                        rs.getInt("seats")
                    });
                }

                if (!hasData) {
                    JOptionPane.showMessageDialog(this, "No bookings found!");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading bookings!");
        }
    }
}
