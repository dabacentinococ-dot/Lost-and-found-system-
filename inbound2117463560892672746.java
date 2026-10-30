package Gui;

import Model.User;
import Util.Validation;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

public class ChangePasswordDialog extends JDialog {

    private static final long serialVersionUID = 1L;
    private final User user;
    private final JTextField securityAnswerField = new JTextField(22);
    private final JPasswordField newPasswordField = new JPasswordField(22);
    private final JPasswordField confirmPasswordField = new JPasswordField(22);
    private String[] result;

    private ChangePasswordDialog(Frame owner, User user) {
        super(owner, "Change Password", true);
        this.user = user;
        buildForm();
    }

    public static String[] showDialog(Frame owner, User user) {
        ChangePasswordDialog dialog = new ChangePasswordDialog(owner, user);
        dialog.setVisible(true);
        return dialog.result;
    }

    private void buildForm() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        JPanel form = new JPanel(new GridBagLayout());
        addRow(form, 0, "Security Question:", new JLabel(user.getSecurityQuestion()));
        addRow(form, 1, "Security Answer:", securityAnswerField);
        addRow(form, 2, "New Password:", newPasswordField);
        addRow(form, 3, "Confirm Password:", confirmPasswordField);

        JButton saveButton = new JButton("Change Password");
        JButton cancelButton = new JButton("Cancel");
        saveButton.addActionListener(event -> changePassword());
        cancelButton.addActionListener(event -> dispose());
        JPanel buttons = new JPanel();
        buttons.add(saveButton);
        buttons.add(cancelButton);
        add(form, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(getOwner());
    }

    private void addRow(JPanel panel, int row, String label, java.awt.Component component) {
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = row;
        labelConstraints.anchor = GridBagConstraints.WEST;
        labelConstraints.insets = new Insets(5, 5, 5, 10);
        panel.add(new JLabel(label), labelConstraints);

        GridBagConstraints componentConstraints = new GridBagConstraints();
        componentConstraints.gridx = 1;
        componentConstraints.gridy = row;
        componentConstraints.fill = GridBagConstraints.HORIZONTAL;
        componentConstraints.weightx = 1;
        componentConstraints.insets = new Insets(5, 5, 5, 5);
        panel.add(component, componentConstraints);
    }

    // Password dialog: validates the security answer and new password.
    private void changePassword() {
        try {
            String answer = Validation.securityAnswer(securityAnswerField.getText());
            String newPassword = Validation.password(new String(newPasswordField.getPassword()));
            String confirm = new String(confirmPasswordField.getPassword());
            if (!newPassword.equals(confirm)) {
                throw new IllegalArgumentException("Passwords do not match.");
            }
            result = new String[]{answer, newPassword};
            dispose();
        } catch (IllegalArgumentException exception) {
            JOptionPane.showMessageDialog(this, exception.getMessage(), "Validation Error", JOptionPane.WARNING_MESSAGE);
        }
    }
}
