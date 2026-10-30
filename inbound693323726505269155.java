package Model;

import java.time.LocalDate;

// RentalRequest model: stores the item and dates requested by a customer.
public class RentalRequest {

    private int itemId;
    private int quantity;
    private LocalDate rentalDate;
    private LocalDate returnDate;

    // Creates a rental request object.
    public RentalRequest(int itemId, int quantity, LocalDate rentalDate, LocalDate returnDate) {
        this.itemId = itemId;
        this.quantity = quantity;
        this.rentalDate = rentalDate;
        this.returnDate = returnDate;
    }

    public RentalRequest(int itemId, LocalDate rentalDate, LocalDate returnDate) {
        this(itemId, 1, rentalDate, returnDate);
    }

    public int getItemId() {
        return itemId;
    }

    public int getQuantity() {
        return quantity;
    }

    public LocalDate getRentalDate() {
        return rentalDate;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

}
