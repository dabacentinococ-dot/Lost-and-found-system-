package Util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

//checks user, item, and rental input.
public final class Validation {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private Validation() {
    }

    //rejects empty required fields.
    public static String required(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value.trim();
    }

    public static String fullName(String value) {
        String fullName = required(value, "Full name");
        if (fullName.length() < 2 || fullName.length() > 50) {
            throw new IllegalArgumentException("Full name must contain 2 to 50 characters.");
        }
        return fullName;
    }

    public static String namePart(String value, String fieldName) {
        String name = required(value, fieldName);
        if (name.length() < 2 || name.length() > 50 || !name.matches("[\\p{L}][\\p{L} .'-]*")) {
            throw new IllegalArgumentException(fieldName + " contains invalid characters.");
        }
        return name;
    }

    //checks the minimum password length.
    public static String password(String value) {
        if (value == null || value.length() < 6) {
            throw new IllegalArgumentException("Password must contain at least 6 characters.");
        }
        return value;
    }

    public static String securityQuestion(String value) {
        String question = required(value, "Security question");
        if (question.length() < 5 || question.length() > 150) {
            throw new IllegalArgumentException("Choose a valid security question.");
        }
        return question;
    }

    public static String securityAnswer(String value) {
        String answer = required(value, "Security answer");
        if (answer.length() < 2 || answer.length() > 100) {
            throw new IllegalArgumentException("Security answer must contain 2 to 100 characters.");
        }
        return answer;
    }

    public static int stock(String value) {
        String text = required(value, "Stock");
        try {
            int number = Integer.parseInt(text);
            if (number < 0 || number > 100000) {
                throw new IllegalArgumentException("Stock must be between 0 and 100000.");
            }
            return number;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Stock must be a whole number.");
        }
    }

    // Checks the requested quantity against the selected item's stock.
    public static int quantity(String value, int availableStock) {
        String text = required(value, "Quantity");
        try {
            int number = Integer.parseInt(text);
            if (number < 1) {
                throw new IllegalArgumentException("Quantity must be at least 1.");
            }
            if (number > availableStock) {
                throw new IllegalArgumentException("Quantity cannot exceed the available stock of " + availableStock + ".");
            }
            return number;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Quantity must be a whole number.");
        }
    }

    public static double price(String value) {
        String text = required(value, "Price per day");
        try {
            double amount = Double.parseDouble(text);
            if (!Double.isFinite(amount) || amount < 0 || amount > 1000000) {
                throw new IllegalArgumentException("Price must be between 0 and 1000000.");
            }
            return Math.round(amount * 100.0) / 100.0;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Price must be a valid number.");
        }
    }

    public static String email(String value) {
        String email = value == null ? "" : value.trim();
        if (!email.isEmpty() && !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalArgumentException("Enter a valid email address.");
        }
        return email;
    }

    public static String contactNumber(String value) {
        String contact = value == null ? "" : value.trim();
        if (!contact.isEmpty() && !contact.matches("[0-9+()\\- ]{7,30}")) {
            throw new IllegalArgumentException("Enter a valid contact number.");
        }
        return contact;
    }

    public static LocalDate date(String value, String fieldName) {
        String date = required(value, fieldName);
        try {
            return LocalDate.parse(date, DATE_FORMAT);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(fieldName + " must use the format yyyy-MM-dd.");
        }
    }

    //checks rental and return date order.
    public static void dateRange(LocalDate rentalDate, LocalDate returnDate) {
        if (rentalDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Rental date cannot be in the past.");
        }
        if (returnDate.isBefore(rentalDate)) {
            throw new IllegalArgumentException("Return date cannot be earlier than rental date.");
        }
    }

    public static DateTimeFormatter dateFormat() {
        return DATE_FORMAT;
    }
}
