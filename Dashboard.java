import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;

public class Dashboard extends JFrame {

    private final int userId;
    private JButton bookBtn;
    private JButton viewBtn;
    private JButton feedbackBtn;
    private JButton logoutBtn;

    public Dashboard(int userId) {

        this.userId = userId;

        setTitle("Dashboard");
        setSize(500, 430);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JLabel welcome = new JLabel("Welcome to Railway Reservation", JLabel.CENTER);
        welcome.setFont(new Font("Arial", Font.BOLD, 18));

        bookBtn = new JButton("Book Ticket");
        viewBtn = new JButton("View Bookings");
        feedbackBtn = new JButton("Give Feedback");
        logoutBtn = new JButton("Logout");

        JPanel btnPanel = new JPanel(new GridLayout(4, 1, 20, 20));
        btnPanel.setBorder(BorderFactory.createEmptyBorder(50, 100, 50, 100));

        btnPanel.add(bookBtn);
        btnPanel.add(viewBtn);
        btnPanel.add(feedbackBtn);
        btnPanel.add(logoutBtn);

        add(welcome, BorderLayout.NORTH);
        add(btnPanel, BorderLayout.CENTER);

        bookBtn.addActionListener(e -> new BookTicket(this.userId));
        viewBtn.addActionListener(e -> new ViewBookings(this.userId));
        feedbackBtn.addActionListener(e -> new FeedbackForm(this.userId));
        logoutBtn.addActionListener(e -> {
            new Login();
            dispose();
        });

        setVisible(true);
    }
}
