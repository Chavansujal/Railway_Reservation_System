import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class Login extends JFrame {

    JTextField emailField;
    JPasswordField passwordField;
    JButton loginBtn, registerBtn;

    public Login() {

        setTitle("Railway Reservation - Login");
        setSize(400, 350);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(5, 1, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

        JLabel title = new JLabel("Railway Reservation System", JLabel.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 16));

        emailField = new JTextField();
        passwordField = new JPasswordField();

        loginBtn = new JButton("Login");
        registerBtn = new JButton("Register");

        panel.add(title);
        panel.add(new JLabel("Email:"));
        panel.add(emailField);
        panel.add(new JLabel("Password:"));
        panel.add(passwordField);

        JPanel btnPanel = new JPanel();
        btnPanel.add(loginBtn);
        btnPanel.add(registerBtn);

        add(panel, BorderLayout.CENTER);
        add(btnPanel, BorderLayout.SOUTH);

        // 🔥 LOGIN BUTTON LOGIC
        loginBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                String email = emailField.getText().trim();
                String password = new String(passwordField.getPassword()).trim();

                if (email.isEmpty() || password.isEmpty()) {
                    JOptionPane.showMessageDialog(Login.this, "Please fill all fields!");
                    return;
                }

                try {
                    Connection con = DBConnection.getConnection();

                    if (con == null) {
                        JOptionPane.showMessageDialog(Login.this, "Database not connected!");
                        return;
                    }

                    String query = "SELECT id FROM users WHERE email = ? AND password = ?";
                    PreparedStatement pst = con.prepareStatement(query);
                    pst.setString(1, email);
                    pst.setString(2, password);

                    ResultSet rs = pst.executeQuery();

                    if (rs.next()) {

                        int userId = rs.getInt("id");

                        JOptionPane.showMessageDialog(Login.this, "Login Successful!");

                        // 🔥 Redirect to Dashboard
                        new Dashboard(userId);
                        dispose();

                    } else {
                        JOptionPane.showMessageDialog(Login.this, "Invalid Email or Password!");
                    }

                    rs.close();
                    pst.close();
                    con.close();

                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(Login.this, "Login Failed!");
                }
            }
        });

        // 🔥 OPEN REGISTER PAGE
        registerBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new Register();
            }
        });

        setVisible(true);
    }

    public static void main(String[] args) {
        new Login();
    }
}