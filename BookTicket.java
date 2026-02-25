import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class BookTicket extends JFrame {

    int userId;

    JTextField sourceField, destinationField, dateField, seatsField;
    JButton bookBtn;

    public BookTicket(int userId) {

        this.userId = userId;  // store logged-in user id

        setTitle("Book Ticket");
        setSize(400, 400);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(9,1,10,10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));

        panel.add(new JLabel("Source:"));
        sourceField = new JTextField();
        panel.add(sourceField);

        panel.add(new JLabel("Destination:"));
        destinationField = new JTextField();
        panel.add(destinationField);

        panel.add(new JLabel("Journey Date (YYYY-MM-DD):"));
        dateField = new JTextField();
        panel.add(dateField);

        panel.add(new JLabel("Number of Seats:"));
        seatsField = new JTextField();
        panel.add(seatsField);

        bookBtn = new JButton("Confirm Booking");
        panel.add(bookBtn);

        add(panel);

        // 🔥 Booking Logic
        bookBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                String source = sourceField.getText();
                String destination = destinationField.getText();
                String date = dateField.getText();
                int seats = Integer.parseInt(seatsField.getText());

                try {
                    Connection con = DBConnection.getConnection();

                    String query = "INSERT INTO bookings(user_id, source, destination, journey_date, seats) VALUES (?, ?, ?, ?, ?)";
                    PreparedStatement pst = con.prepareStatement(query);

                    pst.setInt(1, userId);
                    pst.setString(2, source);
                    pst.setString(3, destination);
                    pst.setString(4, date);
                    pst.setInt(5, seats);

                    pst.executeUpdate();

                    JOptionPane.showMessageDialog(BookTicket.this, "Booking Successful!");

                    con.close();

                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(BookTicket.this, "Booking Failed!");
                }
            }
        });

        setVisible(true);
    }
}