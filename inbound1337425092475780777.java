package Gui;

import Model.ClothingItem;
import Model.RentalRequest;
import Util.Validation;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class RentalRequestDialog extends JDialog {

    private static final long serialVersionUID = 1L;
    private final JComboBox<String> itemBox;
    private final java.util.List<ClothingItem> items;
    private final String shopAddress;
    private final String shopLandmark;
    private final String shopInstructions;
    private final JLabel stockLabel = new JLabel();
    private final JTextField quantityField = new JTextField(12);
    private final JTextField rentalDateField = new JTextField(12);
    private final JTextField returnDateField = new JTextField(12);
    private RentalRequest result;

    private RentalRequestDialog(Frame owner, java.util.List<ClothingItem> items, ClothingItem selectedItem, String shopAddress, String shopLandmark, String shopInstructions) {
        super(owner, "Request Rental", true);
        this.items = items;
        this.shopAddress = shopAddress;
        this.shopLandmark = shopLandmark;
        this.shopInstructions = shopInstructions;
        itemBox = new JComboBox<>();
        for (ClothingItem item : items) {
            itemBox.addItem(item.getItemName() + " - ₱" + String.format("%.2f", item.getPrice()) + "/day - " + item.getCategory() + " - " + item.getSize() + " - " + item.getColor());
        }
        itemBox.addActionListener(event -> {
            quantityField.setText("1");
            updateSelectedItemStock();
        });
        buildForm();
        if (selectedItem != null) {
            for (int index = 0; index < items.size(); index++) {
                if (items.get(index).getId() == selectedItem.getId()) {
                    itemBox.setSelectedIndex(index);
                    break;
                }
            }
        }
        LocalDate today = LocalDate.now();
        quantityField.setText("1");
        rentalDateField.setText(today.plusDays(1).format(Validation.dateFormat()));
        returnDateField.setText(today.plusDays(3).format(Validation.dateFormat()));
        updateSelectedItemStock();
    }

    public static RentalRequest showDialog(Frame owner, java.util.List<ClothingItem> items, ClothingItem selectedItem, String shopAddress, String shopLandmark, String shopInstructions) {
        if (items.isEmpty()) {
            JOptionPane.showMessageDialog(owner, "There are no available items.", "Rental Request", JOptionPane.INFORMATION_MESSAGE);
            return null;
        }
        RentalRequestDialog dialog = new RentalRequestDialog(owner, items, selectedItem, shopAddress, shopLandmark, shopInstructions);
        dialog.setVisible(true);
        return dialog.result;
    }

    private void buildForm() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        JPanel form = new JPanel(new GridBagLayout());
        addRow(form, 0, "Item:", itemBox);
        addRow(form, 1, "Available Stock:", stockLabel);
        addRow(form, 2, "Quantity:", quantityField);
        addRow(form, 3, "Rental Date:", rentalDateField);
        addRow(form, 4, "Return Date:", returnDateField);
        addRow(form, 5, "Pickup Location:", new JLabel(shopAddress));
        addRow(form, 6, "Landmark:", new JLabel(shopLandmark));
        JTextArea instructions = new JTextArea(shopInstructions, 3, 22);
        instructions.setEditable(false);
        instructions.setLineWrap(true);
        instructions.setWrapStyleWord(true);
        addRow(form, 7, "Instructions:", new JScrollPane(instructions));

        JButton submitButton = new JButton("Submit Request");
        JButton cancelButton = new JButton("Cancel");
        submitButton.addActionListener(event -> submitRequest());
        cancelButton.addActionListener(event -> dispose());

        JPanel buttons = new JPanel();
        buttons.add(submitButton);
        buttons.add(cancelButton);
        add(form, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(getOwner());
    }

    private void updateSelectedItemStock() {
        int index = itemBox.getSelectedIndex();
        if (index >= 0 && index < items.size()) {
            stockLabel.setText(items.get(index).getStock() + " unit(s) available");
        } else {
            stockLabel.setText("0 unit(s) available");
        }
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

    // Rental request dialog: validates and submits rental dates.
    private void submitRequest() {
        try {
            int index = itemBox.getSelectedIndex();
            if (index < 0 || index >= items.size()) {
                throw new IllegalArgumentException("Select an item.");
            }
            ClothingItem item = items.get(index);
            if (!item.isAvailable()) {
                throw new IllegalArgumentException("The selected item is no longer available.");
            }
            int quantity = Validation.quantity(quantityField.getText(), item.getStock());
            LocalDate rentalDate = Validation.date(rentalDateField.getText(), "Rental date");
            LocalDate returnDate = Validation.date(returnDateField.getText(), "Return date");
            Validation.dateRange(rentalDate, returnDate);
            result = new RentalRequest(item.getId(), quantity, rentalDate, returnDate);
            dispose();
        } catch (IllegalArgumentException exception) {
            JOptionPane.showMessageDialog(this, exception.getMessage(), "Validation Error", JOptionPane.WARNING_MESSAGE);
        }
    }
}
