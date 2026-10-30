package Service;

import Database.DatabaseConnection;
import Model.Rental;
import Model.RentalRequest;
import Util.Validation;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RentalService {

    // Rental service: creates a request and reserves available stock.
    public void requestRental(int userId, RentalRequest request) throws SQLException {
        Validation.dateRange(request.getRentalDate(), request.getReturnDate());
        if (request.getQuantity() < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1.");
        }
        if (refreshCustomerBlock(userId)) {
            throw new IllegalArgumentException
        ("Your account is blocked because you have overdue items. Please return them first before renting again.");
        }

        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                String itemSql = "SELECT status, stock FROM items WHERE id = ? FOR UPDATE";
                try (PreparedStatement itemStatement = connection.prepareStatement(itemSql)) {
                    itemStatement.setInt(1, request.getItemId());
                    try (ResultSet itemResult = itemStatement.executeQuery()) {
                        if (!itemResult.next()) {
                            throw new IllegalArgumentException("The selected item no longer exists.");
                        }
                        int availableStock = itemResult.getInt("stock");
                        if (!"Available".equalsIgnoreCase(itemResult.getString("status")) || availableStock <= 0) {
                            throw new IllegalArgumentException("The selected item is not available.");
                        }
                        if (request.getQuantity() > availableStock) {
                            throw new IllegalArgumentException("Quantity cannot exceed the available stock of " + availableStock + ".");
                        }
                    }
                }

                String reserveSql = "UPDATE items SET stock = stock - ? WHERE id = ? AND status = 'Available' AND stock >= ?";
                try (PreparedStatement reserveStatement = connection.prepareStatement(reserveSql)) {
                    reserveStatement.setInt(1, request.getQuantity());
                    reserveStatement.setInt(2, request.getItemId());
                    reserveStatement.setInt(3, request.getQuantity());
                    if (reserveStatement.executeUpdate() == 0) {
                        throw new IllegalArgumentException("The selected item is no longer available.");
                    }
                }

                String sql = "INSERT INTO rentals (user_id, item_id, quantity, rental_date, return_date, status) VALUES (?, ?, ?, ?, ?, 'Pending')";
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setInt(1, userId);
                    statement.setInt(2, request.getItemId());
                    statement.setInt(3, request.getQuantity());
                    statement.setDate(4, Date.valueOf(request.getRentalDate()));
                    statement.setDate(5, Date.valueOf(request.getReturnDate()));
                    statement.executeUpdate();
                }
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public List<Rental> getCustomerRentals(int userId, String status) throws SQLException {
        String sql = baseRentalQuery() + " WHERE r.user_id = ?";
        List<String> values = new ArrayList<>();
        values.add(String.valueOf(userId));
        if (isStatusSelected(status)) {
            sql += " AND r.status = ?";
            values.add(status);
        }
        sql += " ORDER BY r.id DESC";
        return getRentals(sql, values);
    }

    public List<Rental> getAllRentals(String status, String search, int ownerId) throws SQLException {
        String sql = baseRentalQuery() + " WHERE i.owner_id = ?";
        List<String> values = new ArrayList<>();
        values.add(String.valueOf(ownerId));
        if (isStatusSelected(status)) {
            sql += " AND r.status = ?";
            values.add(status);
        }
        if (search != null && !search.trim().isEmpty()) {
            sql += " AND (LOWER(TRIM(CONCAT_WS(' ', NULLIF(c.first_name, ''), NULLIF(c.last_name, '')))) LIKE ? OR LOWER(c.first_name) LIKE ? OR LOWER(c.last_name) LIKE ? OR LOWER(i.item_name) LIKE ?)";
            String filter = "%" + search.trim().toLowerCase() + "%";
            values.add(filter);
            values.add(filter);
            values.add(filter);
            values.add(filter);
        }
        sql += " ORDER BY r.id DESC";
        return getRentals(sql, values);
    }

    public List<Rental> getRecentRentals(int ownerId) throws SQLException {
        String sql = baseRentalQuery() + " WHERE i.owner_id = ? ORDER BY r.id DESC LIMIT 10";
        return getRentals(sql, List.of(String.valueOf(ownerId)));
    }

    // Admin rental action: approves a pending request.
    public void approve(int rentalId, int ownerId) throws SQLException {
        updateRentalStatus(rentalId, ownerId, "Pending", "Rented");
    }

    // Admin rental action: rejects a pending request.
    public void reject(int rentalId, int ownerId) throws SQLException {
        updateRentalStatus(rentalId, ownerId, "Pending", "Rejected");
    }

    // Admin return action: marks an item as returned and restores stock.
    public void markReturned(int rentalId, int ownerId) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Rental rental = findById(connection, rentalId, ownerId);
                if (rental == null || !"Rented".equalsIgnoreCase(rental.getStatus())) {
                    throw new IllegalArgumentException("Only rented items can be marked as returned.");
                }
                updateRental(connection, rentalId, "Returned");
                increaseStock(connection, rental.getItemId(), rental.getQuantity(), ownerId);
                setCustomerBlocked(connection, rental.getUserId(), hasOverdueRentals(connection, rental.getUserId()));
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public int countByStatus(String status, int ownerId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM rentals r INNER JOIN items i ON i.id = r.item_id WHERE i.owner_id = ? AND r.status = ?";
        try (Connection connection = DatabaseConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, ownerId);
            statement.setString(2, status);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        }
    }

    public int countCustomerByStatus(int userId, String status) throws SQLException {
        String sql = "SELECT COUNT(*) FROM rentals WHERE user_id = ? AND status = ?";
        try (Connection connection = DatabaseConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setString(2, status);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        }
    }

    public int countReturnedToday(int ownerId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM rentals r INNER JOIN items i ON i.id = r.item_id WHERE i.owner_id = ? AND r.status = 'Returned' AND r.return_date = CURRENT_DATE";
        try (Connection connection = DatabaseConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, ownerId);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        }
    }

    private void updateRentalStatus(int rentalId, int ownerId, String expectedStatus, String newStatus) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Rental rental = findById(connection, rentalId, ownerId);
                if (rental == null || !expectedStatus.equalsIgnoreCase(rental.getStatus())) {
                    throw new IllegalArgumentException("The rental is no longer in the expected status.");
                }
                updateRental(connection, rentalId, newStatus);
                if ("Rejected".equals(newStatus)) {
                    increaseStock(connection, rental.getItemId(), rental.getQuantity(), ownerId);
                }
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    private boolean refreshCustomerBlock(int userId) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            boolean overdue = hasOverdueRentals(connection, userId);
            setCustomerBlocked(connection, userId, overdue);
            return overdue;
        }
    }

    private boolean hasOverdueRentals(Connection connection, int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM rentals WHERE user_id = ? AND status = 'Rented' AND return_date < CURRENT_DATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1) > 0;
            }
        }
    }

    private void setCustomerBlocked(Connection connection, int userId, boolean blocked) throws SQLException {
        String sql = "UPDATE users SET is_blocked = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBoolean(1, blocked);
            statement.setInt(2, userId);
            statement.executeUpdate();
        }
    }

    private Rental findById(Connection connection, int rentalId, int ownerId) throws SQLException {
        String sql = baseRentalQuery() + " WHERE r.id = ? AND i.owner_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, rentalId);
            statement.setInt(2, ownerId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? mapRental(result) : null;
            }
        }
    }

    private void updateRental(Connection connection, int rentalId, String status) throws SQLException {
        String sql = "UPDATE rentals SET status = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            statement.setInt(2, rentalId);
            statement.executeUpdate();
        }
    }

    private void increaseStock(Connection connection, int itemId, int quantity, int ownerId) throws SQLException {
        String sql = "UPDATE items SET stock = stock + ? WHERE id = ? AND owner_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, quantity);
            statement.setInt(2, itemId);
            statement.setInt(3, ownerId);
            if (statement.executeUpdate() == 0) {
                throw new IllegalArgumentException("The item no longer belongs to this Admin.");
            }
        }
    }

    private List<Rental> getRentals(String sql, List<String> values) throws SQLException {
        List<Rental> rentals = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < values.size(); index++) {
                String value = values.get(index);
                if (index == 0 && value.matches("\\d+")) {
                    statement.setInt(index + 1, Integer.parseInt(value));
                } else {
                    statement.setString(index + 1, value);
                }
            }
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    rentals.add(mapRental(result));
                }
            }
        }
        return rentals;
    }

    private String baseRentalQuery() {
        return "SELECT r.id, r.user_id, r.item_id, "
                + "COALESCE(NULLIF(TRIM(CONCAT_WS(' ', NULLIF(c.first_name, ''), NULLIF(c.last_name, ''))), ''), c.username) AS customer_name, "
                + "COALESCE(NULLIF(TRIM(CONCAT_WS(' ', NULLIF(o.first_name, ''), NULLIF(o.last_name, ''))), ''), o.username) AS owner_name, "
                + "i.item_name, i.price, i.stock, r.quantity, r.rental_date, r.return_date, r.status "
                + "FROM rentals r INNER JOIN users c ON c.id = r.user_id INNER JOIN items i ON i.id = r.item_id INNER JOIN users o ON o.id = i.owner_id";
    }

    private boolean isStatusSelected(String status) {
        return status != null && !status.isBlank() && !status.startsWith("All ");
    }

    private Rental mapRental(ResultSet result) throws SQLException {
        return new Rental(
                result.getInt("id"),
                result.getInt("user_id"),
                result.getInt("item_id"),
                result.getString("customer_name"),
                result.getString("owner_name"),
                result.getString("item_name"),
                result.getDouble("price"),
                result.getInt("stock"),
                result.getInt("quantity"),
                result.getDate("rental_date").toLocalDate(),
                result.getDate("return_date").toLocalDate(),
                result.getString("status")
        );
    }
}
