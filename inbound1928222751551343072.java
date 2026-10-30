package Model;

// ClothingItem model: stores one inventory item and its availability.
public class ClothingItem {

    private int id;
    private int ownerId;
    private String ownerName;
    private String itemName;
    private double price;
    private String category;
    private String size;
    private String color;
    private String status;
    private int stock;
    private String description;

    // Creates a clothing item object from inventory data.
    public ClothingItem(int id, int ownerId, String ownerName, String itemName, double price, String category, String size, String color, String status, int stock, String description) {
        this.id = id;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.itemName = itemName;
        this.price = price;
        this.category = category;
        this.size = size;
        this.color = color;
        this.status = status;
        this.stock = stock;
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(int ownerId) {
        this.ownerId = ownerId;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    //checks whether the item can be rented.
    public boolean isAvailable() {
        return stock > 0 && "Available".equalsIgnoreCase(status);
    }

    public String getAvailabilityStatus() {
        return isAvailable() ? "Available" : "Unavailable";
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}
