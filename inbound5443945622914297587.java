package Gui;

import Model.ClothingItem;
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
import javax.swing.JTextField;

public class ItemDialog extends JDialog {

    private static final long serialVersionUID = 1L;
    private final JTextField itemNameField = new JTextField(22);
    private final JTextField priceField = new JTextField("0.00", 22);
    private final JComboBox<String> categoryBox = new JComboBox<>(new String[]{"Gown", "Barong", "Suit", "Costume", "Dress", "Tuxedo", "Student Uniform"});
    private final JComboBox<String> sizeBox = new JComboBox<>(new String[]{"XS", "S", "M", "L", "XL", "XXL", "Free Size"});
    private final JTextField colorField = new JTextField(22);
    private final JComboBox<String> statusBox = new JComboBox<>(new String[]{"Available", "Unavailable"});
    private final JTextField stockField = new JTextField("1", 22);
    private final JTextField descriptionField = new JTextField(22);
    private final ClothingItem existingItem;
    private ClothingItem result;

    private ItemDialog(Frame owner, ClothingItem item) {
        super(owner, item == null ? "Add Clothing Item" : "Edit Clothing Item", true);
        existingItem = item;
        buildForm();
        fillFields();
    }

    public static ClothingItem showDialog(Frame owner, ClothingItem item) {
        ItemDialog dialog = new ItemDialog(owner, item);
        dialog.setVisible(true);
        return dialog.result;
    }

    private void buildForm() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        JPanel form = new JPanel(new GridBagLayout());
        addRow(form, 0, "Item Name:", itemNameField);
        addRow(form, 1, "Price per Day:", priceField);
        addRow(form, 2, "Category:", categoryBox);
        addRow(form, 3, "Size:", sizeBox);
        addRow(form, 4, "Color:", colorField);
        addRow(form, 5, "Status:", statusBox);
        addRow(form, 6, "Stock:", stockField);
        addRow(form, 7, "Description:", descriptionField);

        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");
        saveButton.addActionListener(event -> saveItem());
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

    private void fillFields() {
        if (existingItem == null) {
            return;
        }
        itemNameField.setText(existingItem.getItemName());
        priceField.setText(String.format("%.2f", existingItem.getPrice()));
        categoryBox.setSelectedItem(existingItem.getCategory());
        sizeBox.setSelectedItem(existingItem.getSize());
        colorField.setText(existingItem.getColor());
        statusBox.setSelectedItem(existingItem.getStatus());
        stockField.setText(String.valueOf(existingItem.getStock()));
        descriptionField.setText(existingItem.getDescription());
    }

    // Item dialog: validates and returns inventory information.
    private void saveItem() {
        try {
            String itemName = Validation.required(itemNameField.getText(), "Item name");
            double price = Validation.price(priceField.getText());
            String color = Validation.required(colorField.getText(), "Color");
            int stock = Validation.stock(stockField.getText());
            String description = descriptionField.getText() == null ? "" : descriptionField.getText().trim();
            result = new ClothingItem(
                    existingItem == null ? 0 : existingItem.getId(),
                    0,
                    "",
                    itemName,
                    price,
                    String.valueOf(categoryBox.getSelectedItem()),
                    String.valueOf(sizeBox.getSelectedItem()),
                    color,
                    String.valueOf(statusBox.getSelectedItem()),
                    stock,
                    description
            );
            dispose();
        } catch (IllegalArgumentException exception) {
            JOptionPane.showMessageDialog(this, exception.getMessage(), "Validation Error", JOptionPane.WARNING_MESSAGE);
        }
    }
}
