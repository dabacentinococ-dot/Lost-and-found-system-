package Service;

import Database.DatabaseConnection;
import Model.ClothingItem;
import Util.Validation;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.List;

public class ItemService {

    //retrieves filtered clothing items.
    public List<ClothingItem> getItems(String search, String category, String size, String color, String status, int ownerId) throws SQLException {
        List<ClothingItem> items = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT i.*, COALESCE(NULLIF(TRIM(CONCAT_WS(' ', NULLIF(u.first_name, ''), NULLIF(u.last_name, ''))), ''), u.username) AS owner_name FROM items i INNER JOIN users u ON u.id = i.owner_id WHERE 1 = 1");
        List<String> values = new ArrayList<>();
        if (ownerId > 0) {
            sql.append(" AND i.owner_id = ?");
            values.add(String.valueOf(ownerId));
        }
        if (search != null && !search.trim().isEmpty()) {
            sql.append(" AND (CAST(i.id AS CHAR) LIKE ? OR LOWER(i.item_name) LIKE ? OR LOWER(i.category) LIKE ? OR LOWER(i.color) LIKE ? OR LOWER(u.first_name) LIKE ? OR LOWER(u.last_name) LIKE ? OR LOWER(TRIM(CONCAT_WS(' ', NULLIF(u.first_name, ''), NULLIF(u.last_name, '')))) LIKE ?)");
            String filter = "%" + search.trim().toLowerCase() + "%";
            values.add(filter);
            values.add(filter);
            values.add(filter);
            values.add(filter);
            values.add(filter);
            values.add(filter);
            values.add(filter);
        }
        if (!isAll(category, "All Categories")) {
            sql.append(" AND i.category = ?");
            values.add(category.trim());
        }
        if (!isAll(size, "All Sizes")) {
            sql.append(" AND i.item_size = ?");
            values.add(size.trim());
        }
        if (!isAll(color, "All Colors")) {
            sql.append(" AND i.color = ?");
            values.add(color.trim());
        }
        addStatusFilter(sql, values, status);
        sql.append(" ORDER BY i.id DESC");

        try (Connection connection = DatabaseConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            for (int index = 0; index < values.size(); index++) {
                statement.setString(index + 1, values.get(index));
            }
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    items.add(mapItem(result));
                }
            }
        }
        return items;
    }

    public ClothingItem findById(int id, int ownerId) throws SQLException {
        String sql = "SELECT i.*, COALESCE(NULLIF(TRIM(CONCAT_WS(' ', NULLIF(u.first_name, ''), NULLIF(u.last_name, ''))), ''), u.username) AS owner_name FROM items i INNER JOIN users u ON u.id = i.owner_id WHERE i.id = ?" + (ownerId > 0 ? " AND i.owner_id = ?" : "");
        try (Connection connection = DatabaseConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            if (ownerId > 0) {
                statement.setInt(2, ownerId);
            }
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? mapItem(result) : null;
            }
        }
    }

    //adds or updates a clothing item.
    public void save(ClothingItem item, int ownerId) throws SQLException {
        if (ownerId <= 0) {
            throw new IllegalArgumentException("An Admin account is required to manage inventory.");
        }
        Validation.required(item.getItemName(), "Item name");
        Validation.required(item.getCategory(), "Category");
        Validation.required(item.getSize(), "Size");
        Validation.required(item.getColor(), "Color");
        Validation.required(item.getStatus(), "Status");
        if (item.getStock() < 0 || item.getStock() > 100000) {
            throw new IllegalArgumentException("Stock must be between 0 and 100000.");
        }
        if (!Double.isFinite(item.getPrice()) || item.getPrice() < 0 || item.getPrice() > 1000000) {
            throw new IllegalArgumentException("Price must be between 0 and 1000000.");
        }
        String description = item.getDescription() == null ? "" : item.getDescription().trim();

        if (item.getId() == 0) {
            String sql = "INSERT INTO items (owner_id, item_name, price, category, item_size, color, stock, status, description) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (Connection connection = DatabaseConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, ownerId);
                setItemValues(statement, item, description, 2);
                statement.executeUpdate();
            }
        } else {
            String sql = "UPDATE items SET item_name = ?, price = ?, category = ?, item_size = ?, color = ?, stock = ?, status = ?, description = ? WHERE id = ? AND owner_id = ?";
            try (Connection connection = DatabaseConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
                setItemValues(statement, item, description, 1);
                statement.setInt(9, item.getId());
                statement.setInt(10, ownerId);
                if (statement.executeUpdate() == 0) {
                    throw new IllegalArgumentException("You can only edit your own items.");
                }
            }
        }
    }

    // Inventory service: deletes an Admin-owned item.
    public void delete(int id, int ownerId) throws SQLException {
        String sql = "DELETE FROM items WHERE id = ? AND owner_id = ?";
        try (Connection connection = DatabaseConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.setInt(2, ownerId);
            try {
                if (statement.executeUpdate() == 0) {
                    throw new IllegalArgumentException("You can only delete your own items.");
                }
            } catch (SQLIntegrityConstraintViolationException exception) {
                throw new IllegalArgumentException("This item cannot be deleted because it has rental history.");
            }
        }
    }

    public int countAll(int ownerId) throws SQLException {
        return count("SELECT COUNT(*) FROM items WHERE owner_id = ?", ownerId, null);
    }

    public int countByStatus(String status, int ownerId) throws SQLException {
        return count("SELECT COUNT(*) FROM items WHERE owner_id = ? AND status = ?", ownerId, status);
    }

    private void addStatusFilter(StringBuilder sql, List<String> values, String status) {
        String selectedStatus = status == null ? "" : status.trim();
        if (selectedStatus.isEmpty() || selectedStatus.equalsIgnoreCase("All Status") || selectedStatus.equalsIgnoreCase("All Items")) {
            return;
        }
        if (selectedStatus.equalsIgnoreCase("Available Only") || selectedStatus.equalsIgnoreCase("Available")) {
            sql.append(" AND i.status = 'Available' AND i.stock > 0");
            return;
        }
        if (selectedStatus.equalsIgnoreCase("Unavailable")) {
            sql.append(" AND (i.status <> 'Available' OR i.stock <= 0)");
            return;
        }
        sql.append(" AND i.status = ?");
        values.add(selectedStatus);
    }

    private boolean isAll(String value, String allValue) {
        return value == null || value.trim().isEmpty() || value.trim().equalsIgnoreCase(allValue);
    }

    private void setItemValues(PreparedStatement statement, ClothingItem item, String description, int start) throws SQLException {
        statement.setString(start, item.getItemName().trim());
        statement.setDouble(start + 1, item.getPrice());
        statement.setString(start + 2, item.getCategory().trim());
        statement.setString(start + 3, item.getSize().trim());
        statement.setString(start + 4, item.getColor().trim());
        statement.setInt(start + 5, item.getStock());
        statement.setString(start + 6, item.getStatus().trim());
        statement.setString(start + 7, description);
    }

    private int count(String sql, int ownerId, String status) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, ownerId);
            if (status != null) {
                statement.setString(2, status);
            }
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        }
    }

    private ClothingItem mapItem(ResultSet result) throws SQLException {
        return new ClothingItem(
                result.getInt("id"),
                result.getInt("owner_id"),
                result.getString("owner_name"),
                result.getString("item_name"),
                result.getDouble("price"),
                result.getString("category"),
                result.getString("item_size"),
                result.getString("color"),
                result.getString("status"),
                result.getInt("stock"),
                result.getString("description")
        );
    }
}
