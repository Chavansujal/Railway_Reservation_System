import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class Dashboard extends JFrame {

    int userId;   // store logged-in user id
    JButton bookBtn, viewBtn, logoutBtn;

    public Dashboard(int userId) {

        this.userId = userId;   // save user id

        setTitle("Dashboard");
        setSize(500, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JLabel welcome = new JLabel("Welcome to Railway Reservation", JLabel.CENTER);
        welcome.setFont(new Font("Arial", Font.BOLD, 18));

        bookBtn = new JButton("Book Ticket");
        viewBtn = new JButton("View Bookings");
        logoutBtn = new JButton("Logout");

        JPanel btnPanel = new JPanel(new GridLayout(3,1,20,20));
        btnPanel.setBorder(BorderFactory.createEmptyBorder(50, 100, 50, 100));

        btnPanel.add(bookBtn);
        btnPanel.add(viewBtn);
        btnPanel.add(logoutBtn);

        add(welcome, BorderLayout.NORTH);
        add(btnPanel, BorderLayout.CENTER);

        // 🔥 Book Ticket Button
        bookBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new BookTicket(userId);
            }
        });

        // 🔥 View Bookings Button
        viewBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new ViewBookings(userId);
            }
        });

        // 🔥 Logout Button
        logoutBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new Login();
                dispose();
            }
        });

        setVisible(true);
    }
}