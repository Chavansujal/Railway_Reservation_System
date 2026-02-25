import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class Register extends JFrame {

    JTextField nameField, emailField;
    JPasswordField passwordField;
    JButton registerBtn;

    public Register() {

        setTitle("Register User");
        setSize(400, 350);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel panel = new JPanel(new GridLayout(7, 1, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));

        panel.add(new JLabel("Name:"));
        nameField = new JTextField();
        panel.add(nameField);

        panel.add(new JLabel("Email:"));
        emailField = new JTextField();
        panel.add(emailField);

        panel.add(new JLabel("Password:"));
        passwordField = new JPasswordField();
        panel.add(passwordField);

        registerBtn = new JButton("Register");
        panel.add(registerBtn);

        add(panel);

        registerBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                String name = nameField.getText().trim();
                String email = emailField.getText().trim();
                String password = new String(passwordField.getPassword()).trim();

                if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                    JOptionPane.showMessageDialog(Register.this, "Please fill all fields!");
                    return;
                }

                Connection con = DBConnection.getConnection();

                if (con == null) {
                    JOptionPane.showMessageDialog(Register.this, "Database not connected!");
                    return;
                }

                try {
                    String query = "INSERT INTO users(name, email, password) VALUES (?, ?, ?)";
                    PreparedStatement pst = con.prepareStatement(query);

                    pst.setString(1, name);
                    pst.setString(2, email);
                    pst.setString(3, password);

                    int rows = pst.executeUpdate();

                    if (rows > 0) {
                        JOptionPane.showMessageDialog(Register.this, "Registration Successful!");
                        dispose();
                    } else {
                        JOptionPane.showMessageDialog(Register.this, "Registration Failed!");
                    }

                    pst.close();
                    con.close();

                } catch (SQLIntegrityConstraintViolationException ex) {
                    JOptionPane.showMessageDialog(Register.this, "Email already exists!");
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(Register.this, "Error occurred!");
                }
            }
        });

        setVisible(true);
    }
}