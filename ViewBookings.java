import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class ViewBookings extends JFrame {

    int userId;
    JTable table;

    public ViewBookings(int userId) {

        this.userId = userId;

        setTitle("Your Bookings");
        setSize(600, 400);
        setLocationRelativeTo(null);

        try {
            Connection con = DBConnection.getConnection();

            String query = "SELECT booking_id, source, destination, journey_date, seats FROM bookings WHERE user_id=?";
            PreparedStatement pst = con.prepareStatement(query);
            pst.setInt(1, userId);

            ResultSet rs = pst.executeQuery();

            String[] columns = {"Booking ID", "Source", "Destination", "Date", "Seats"};

            // Count rows
            rs.last();
            int rowCount = rs.getRow();
            rs.beforeFirst();

            String[][] data = new String[rowCount][5];

            int i = 0;
            while (rs.next()) {
                data[i][0] = rs.getString("booking_id");
                data[i][1] = rs.getString("source");
                data[i][2] = rs.getString("destination");
                data[i][3] = rs.getString("journey_date");
                data[i][4] = rs.getString("seats");
                i++;
            }

            table = new JTable(data, columns);
            JScrollPane scrollPane = new JScrollPane(table);
            add(scrollPane);

            con.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        setVisible(true);
    }
}