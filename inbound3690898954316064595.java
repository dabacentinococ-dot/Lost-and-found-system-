package Model;

import java.time.LocalDate;

// Rental model: stores a customer's rental transaction.
public class Rental {

    private int id;
    private int userId;
    private int itemId;
    private String customerName;
    private String ownerName;
    private String itemName;
    private double price;
    private int stock;
    private int quantity;
    private LocalDate rentalDate;
    private LocalDate returnDate;
    private String status;

    // Creates a rental object from transaction data.
    public Rental(int id, int userId, int itemId, String customerName, String ownerName, String itemName, double price, int stock, int quantity, LocalDate rentalDate, LocalDate returnDate, String status) {
        this.id = id;
        this.userId = userId;
        this.itemId = itemId;
        this.customerName = customerName;
        this.ownerName = ownerName;
        this.itemName = itemName;
        this.price = price;
        this.stock = stock;
        this.quantity = quantity;
        this.rentalDate = rentalDate;
        this.returnDate = returnDate;
        this.status = status;
    }

    public Rental(int id, int userId, int itemId, String customerName, String ownerName, String itemName, double price, int stock, LocalDate rentalDate, LocalDate returnDate, String status) {
        this(id, userId, itemId, customerName, ownerName, itemName, price, stock, 1, rentalDate, returnDate, status);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public LocalDate getRentalDate() {
        return rentalDate;
    }

    public void setRentalDate(LocalDate rentalDate) {
        this.rentalDate = rentalDate;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(LocalDate returnDate) {
        this.returnDate = returnDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // Returns the current availability based on stock.
    public String getAvailabilityStatus() {
        return stock > 0 ? "Available" : "Unavailable";
    }
}
