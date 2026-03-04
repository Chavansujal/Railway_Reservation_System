import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class ViewBookings extends JFrame {

    int userId;
    JTable table;
    DefaultTableModel model;

    public ViewBookings(int userId) {

        this.userId = userId;

        setTitle("Your Bookings");
        setSize(600, 400);
        setLocationRelativeTo(null);

        String[] columns = {"Booking ID", "Source", "Destination", "Date", "Seats"};
        model = new DefaultTableModel(columns, 0);
        table = new JTable(model);

        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane);

        try {
            Connection con = DBConnection.getConnection();

            if (con == null) {
                JOptionPane.showMessageDialog(this, "Database not connected!");
                return;
            }

            String query = "SELECT id, source, destination, journey_date, seats FROM bookings WHERE user_id=?";
            PreparedStatement pst = con.prepareStatement(query);
            pst.setInt(1, userId);

            ResultSet rs = pst.executeQuery();

            boolean hasData = false;

            while (rs.next()) {
                hasData = true;

                model.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("source"),
                        rs.getString("destination"),
                        rs.getDate("journey_date"),
                        rs.getInt("seats")
                });
            }

            if (!hasData) {
                JOptionPane.showMessageDialog(this, "No bookings found!");
            }

            rs.close();
            pst.close();
            con.close();

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading bookings!");
        }

        setVisible(true);
    }
}