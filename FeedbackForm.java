import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class FeedbackForm extends JFrame {

    private final int userId;
    private JComboBox<Integer> ratingCombo;
    private JTextArea feedbackArea;
    private JButton submitBtn;

    public FeedbackForm(int userId) {
        this.userId = userId;

        setTitle("Feedback");
        setSize(500, 350);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel topPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        topPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));

        topPanel.add(new JLabel("Rating:"));
        ratingCombo = new JComboBox<>(new Integer[]{1, 2, 3, 4, 5});
        topPanel.add(ratingCombo);

        topPanel.add(new JLabel("Feedback:"));
        topPanel.add(new JLabel("Write your feedback below"));

        feedbackArea = new JTextArea(8, 30);
        feedbackArea.setLineWrap(true);
        feedbackArea.setWrapStyleWord(true);

        submitBtn = new JButton("Submit Feedback");
        submitBtn.addActionListener(e -> saveFeedback());

        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(feedbackArea), BorderLayout.CENTER);
        add(submitBtn, BorderLayout.SOUTH);

        setVisible(true);
    }

    private void saveFeedback() {
        String feedbackText = feedbackArea.getText().trim();
        if (feedbackText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter your feedback.");
            return;
        }

        String sql = "INSERT INTO feedback(user_id, rating, feedback_text) VALUES (?, ?, ?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {

            if (con == null) {
                JOptionPane.showMessageDialog(this, "Database not connected!");
                return;
            }

            pst.setInt(1, userId);
            pst.setInt(2, (Integer) ratingCombo.getSelectedItem());
            pst.setString(3, feedbackText);
            pst.executeUpdate();

            JOptionPane.showMessageDialog(this, "Feedback saved successfully.");
            dispose();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Could not save feedback.");
        }
    }
}
