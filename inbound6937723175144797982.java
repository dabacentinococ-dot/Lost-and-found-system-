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
import javax.swing.JTextField;

public class ProfileDialog extends JDialog {

    private static final long serialVersionUID = 1L;
    private final JTextField fullNameField = new JTextField(22);
    private final JTextField contactField = new JTextField(22);
    private final JTextField emailField = new JTextField(22);
    private final JTextField addressField = new JTextField(22);
    private final JLabel addressNote = new JLabel();
    private final User originalUser;
    private User result;

    private ProfileDialog(Frame owner, User user) {
        super(owner, "Update Profile", true);
        originalUser = user;
        buildForm();
        fullNameField.setText(user.getFullName());
        contactField.setText(user.getContactNumber());
        emailField.setText(user.getEmail());
        addressField.setText(user.getAddress());
        addressField.setEditable(user.getAddress().isBlank());
        addressField.setToolTipText("For security purposes, your address can only be edited one time.");
        addressNote.setText(user.getAddress().isBlank()
                ? "For security purposes, your address can only be saved one time."
                : "For security purposes, your saved address cannot be edited again.");
    }

    public static User showDialog(Frame owner, User user) {
        ProfileDialog dialog = new ProfileDialog(owner, user);
        dialog.setVisible(true);
        return dialog.result;
    }

    private void buildForm() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        JPanel form = new JPanel(new GridBagLayout());
        addRow(form, 0, "Full Name:", fullNameField);
        addRow(form, 1, "Contact Number:", contactField);
        addRow(form, 2, "Email:", emailField);
        addRow(form, 3, "Address:", addressField);

        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");
        saveButton.addActionListener(event -> saveProfile());
        cancelButton.addActionListener(event -> dispose());
        JPanel buttons = new JPanel();
        buttons.add(saveButton);
        buttons.add(cancelButton);
        add(addressNote, BorderLayout.NORTH);
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

    // Customer profile dialog: saves customer information.
    private void saveProfile() {
        try {
            String fullName = Validation.required(fullNameField.getText(), "Full name");
            String contact = Validation.contactNumber(contactField.getText());
            String email = Validation.email(Validation.required(emailField.getText(), "Email"));
            String address = Validation.required(addressField.getText(), "Address");
            if (!addressField.isEditable() && !address.equals(originalUser.getAddress())) {
                throw new IllegalArgumentException("For security purposes, your address can only be edited one time.");
            }
            String[] names = fullName.trim().split(" ", 2);
            result = new User(
                    originalUser.getId(),
                    originalUser.getUsername(),
                    names[0],
                    names.length > 1 ? names[1] : "",
                    contact,
                    email,
                    "",
                    originalUser.getRole(),
                    originalUser.getSecurityQuestion(),
                    originalUser.getSecurityAnswer()
            );
            result.setAddress(address);
            dispose();
        } catch (IllegalArgumentException exception) {
            JOptionPane.showMessageDialog(this, exception.getMessage(), "Validation Error", JOptionPane.WARNING_MESSAGE);
        }
    }
}
