package Gui;

import Model.User;
import Util.Validation;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class AdminProfileDialog extends JDialog {

    private static final long serialVersionUID = 1L;
    private static final String[] QUESTIONS = {
        "What was your favorite food?",
        "What was the name of your first pet?",
        "What elementary school did you attend?",
        "What is your favorite Color?"
    };
    private final JTextField fullNameField = new JTextField(22);
    private final JTextField contactField = new JTextField(22);
    private final JTextField emailField = new JTextField(22);
    private final JTextField shopAddressField = new JTextField(22);
    private final JTextField shopLandmarkField = new JTextField(22);
    private final JTextArea shopInstructionsArea = new JTextArea(3, 22);
    private final JComboBox<String> questionBox = new JComboBox<>(QUESTIONS);
    private final JTextField answerField = new JTextField(22);
    private final JPasswordField newPasswordField = new JPasswordField(22);
    private final JPasswordField confirmPasswordField = new JPasswordField(22);
    private final boolean defaultPassword;
    private final User originalUser;
    private User result;

    private AdminProfileDialog(Frame owner, User user, boolean defaultPassword) {
        super(owner, "Admin Profile", true);
        originalUser = user;
        this.defaultPassword = defaultPassword;
        fullNameField.setText(user.getFullName());
        contactField.setText(user.getContactNumber());
        emailField.setText(user.getEmail());
        shopAddressField.setText(user.getShopAddress());
        shopLandmarkField.setText(user.getShopLandmark());
        shopInstructionsArea.setText(user.getShopInstructions());
        shopInstructionsArea.setLineWrap(true);
        shopInstructionsArea.setWrapStyleWord(true);
        String question = user.getSecurityQuestion() == null ? "" : user.getSecurityQuestion().trim();
        if (!question.isEmpty()) {
            questionBox.setSelectedItem(question);
        }
        buildForm();
    }

    public static User showDialog(Frame owner, User user, boolean defaultPassword) {
        AdminProfileDialog dialog = new AdminProfileDialog(owner, user, defaultPassword);
        dialog.setVisible(true);
        return dialog.result;
    }

    private void buildForm() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        JLabel note = new JLabel(defaultPassword
                ? "Your account still uses the default password. Change it before saving."
                : "Leave the password fields blank to keep your current password.");
        JPanel form = new JPanel(new GridBagLayout());
        addRow(form, 0, "Full Name:", fullNameField);
        addRow(form, 1, "Contact Number:", contactField);
        addRow(form, 2, "Email:", emailField);
        addRow(form, 3, "Shop Address:", shopAddressField);
        addRow(form, 4, "Shop Landmark:", shopLandmarkField);
        addRow(form, 5, "Pickup Instructions:", new JScrollPane(shopInstructionsArea));
        addRow(form, 6, "Security Question:", questionBox);
        addRow(form, 7, "Security Answer:", answerField);
        addRow(form, 8, "New Password:", newPasswordField);
        addRow(form, 9, "Confirm Password:", confirmPasswordField);

        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");
        saveButton.addActionListener(event -> saveProfile());
        cancelButton.addActionListener(event -> dispose());
        JPanel buttons = new JPanel();
        buttons.add(saveButton);
        buttons.add(cancelButton);
        add(note, BorderLayout.NORTH);
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

    // Admin profile dialog: saves account and shop information.
    private void saveProfile() {
        try {
            String fullName = Validation.fullName(fullNameField.getText());
            String contact = Validation.contactNumber(contactField.getText());
            String email = Validation.email(Validation.required(emailField.getText(), "Email"));
            String shopAddress = shopInfo(shopAddressField.getText(), "Rental shop address", 255);
            String shopLandmark = shopInfo(shopLandmarkField.getText(), "Shop landmark", 255);
            String shopInstructions = shopInfo(shopInstructionsArea.getText(), "Pickup and return instructions", 500);
            String question = Validation.securityQuestion(String.valueOf(questionBox.getSelectedItem()));
            String answer = Validation.securityAnswer(answerField.getText());
            String newPassword = new String(newPasswordField.getPassword());
            String confirmPassword = new String(confirmPasswordField.getPassword());
            if (defaultPassword && newPassword.isBlank()) {
                throw new IllegalArgumentException("Change the default password before saving, or choose Cancel.");
            }
            if (!newPassword.isBlank()) {
                Validation.password(newPassword);
                if (!newPassword.equals(confirmPassword)) {
                    throw new IllegalArgumentException("Passwords do not match.");
                }
            }
            String[] names = fullName.split(" ", 2);
            result = new User(
                    originalUser.getId(),
                    originalUser.getUsername(),
                    names[0],
                    names.length > 1 ? names[1] : "",
                    contact,
                    email,
                    newPassword,
                    originalUser.getRole(),
                    question,
                    answer
            );
            result.setShopAddress(shopAddress);
            result.setShopLandmark(shopLandmark);
            result.setShopInstructions(shopInstructions);
            dispose();
        } catch (IllegalArgumentException exception) {
            JOptionPane.showMessageDialog(this, exception.getMessage(), "Validation Error", JOptionPane.WARNING_MESSAGE);
        }
    }

    private String shopInfo(String value, String fieldName, int maxLength) {
        String text = Validation.required(value, fieldName);
        if (text.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " must not exceed " + maxLength + " characters.");
        }
        return text;
    }
}
