package Service;

import Database.DatabaseConnection;
import Model.User;
import Util.PasswordUtil;
import Util.Validation;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserService {

    private static final String ADMIN_ROLE = "(SELECT id FROM roles WHERE role_name = 'Admin')";
    private static final String CUSTOMER_ROLE = "(SELECT id FROM roles WHERE role_name = 'Customer')";

    //checks account credentials.
    public User authenticate(String username, String password) throws SQLException {
        String value = Validation.required(username, "Username or full name");
        String sql = userSelect() + " WHERE (LOWER(TRIM(u.username)) = LOWER(?) OR LOWER(TRIM(CONCAT_WS(' ', NULLIF(u.first_name, ''), NULLIF(u.last_name, '')))) = LOWER(?)) AND u.user_password = ?";
        String passwordHash = PasswordUtil.hash(password);
        try (Connection connection = DatabaseConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);
            statement.setString(2, value);
            statement.setString(3, passwordHash);
            try (ResultSet result = statement.executeQuery()) {
                User user;
                if (!result.next()) {
                    user = findByNamePart(connection, value, passwordHash);
                } else {
                    user = mapUser(result);
                }
                if (user == null) {
                    return null;
                }
                return user;
            }
        }
    }

    private User findByNamePart(Connection connection, String value, String passwordHash) throws SQLException {
        String sql = userSelect() + " WHERE u.user_password = ? AND (LOWER(TRIM(u.first_name)) = LOWER(?) OR LOWER(TRIM(u.last_name)) = LOWER(?))";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, passwordHash);
            statement.setString(2, value);
            statement.setString(3, value);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return null;
                }
                User user = mapUser(result);
                return result.next() ? null : user;
            }
        }
    }

    //creates a Customer account.
    public void register(User user) throws SQLException {
        String firstName = Validation.namePart(user.getFirstName(), "First name");
        String lastName = Validation.namePart(user.getLastName(), "Last name");
        String fullName = firstName + " " + lastName;
        String email = Validation.email(Validation.required(user.getEmail(), "Email"));
        String password = Validation.password(user.getPassword());
        String question = Validation.securityQuestion(user.getSecurityQuestion());
        String answer = Validation.securityAnswer(user.getSecurityAnswer());
        if (usernameExists(fullName)) {
            throw new IllegalArgumentException("That name is already registered.");
        }

        String sql = "INSERT INTO users (username, first_name, last_name, contact_no, email, user_password, role_id, security_question, security_answer) VALUES (?, ?, ?, ?, ?, ?, "
                + CUSTOMER_ROLE + ", ?, ?)";
        try (Connection connection = DatabaseConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, fullName);
            statement.setString(2, firstName);
            statement.setString(3, lastName);
            statement.setString(4, "");
            statement.setString(5, email);
            statement.setString(6, PasswordUtil.hash(password));
            statement.setString(7, question);
            statement.setString(8, PasswordUtil.hash(answer.toLowerCase()));
            statement.executeUpdate();
        }
    }

    //verifies security details and resets the password.
    public void resetPassword(String username, String question, String answer, String newPassword) throws SQLException {
        String value = Validation.required(username, "Username or full name");
        String selectedQuestion = Validation.securityQuestion(question);
        String selectedAnswer = Validation.securityAnswer(answer);
        String password = Validation.password(newPassword);
        String findSql = "SELECT id FROM users WHERE (LOWER(TRIM(username)) = LOWER(?) OR LOWER(TRIM(CONCAT_WS(' ', NULLIF(first_name, ''), NULLIF(last_name, '')))) = LOWER(?)) AND LOWER(TRIM(security_question)) = LOWER(?) AND security_answer = ?";
        try (Connection connection = DatabaseConnection.getConnection(); PreparedStatement findStatement = connection.prepareStatement(findSql)) {
            findStatement.setString(1, value);
            findStatement.setString(2, value);
            findStatement.setString(3, selectedQuestion);
            findStatement.setString(4, PasswordUtil.hash(selectedAnswer.toLowerCase()));
            try (ResultSet result = findStatement.executeQuery()) {
                if (!result.next()) {
                    throw new IllegalArgumentException("The account details, security question, or answer are incorrect.");
                }
                String updateSql = "UPDATE users SET user_password = ? WHERE id = ?";
                try (PreparedStatement updateStatement = connection.prepareStatement(updateSql)) {
                    updateStatement.setString(1, PasswordUtil.hash(password));
                    updateStatement.setInt(2, result.getInt("id"));
                    updateStatement.executeUpdate();
                }
            }
        }
    }

    public User findById(int id) throws SQLException {
        String sql = userSelect() + " WHERE u.id = ?";
        try (Connection connection = DatabaseConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? mapUser(result) : null;
            }
        }
    }

    public User findByUsername(String username) throws SQLException {
        String sql = userSelect() + " WHERE LOWER(TRIM(u.username)) = LOWER(?) OR LOWER(TRIM(CONCAT_WS(' ', NULLIF(u.first_name, ''), NULLIF(u.last_name, '')))) = LOWER(?) LIMIT 1";
        try (Connection connection = DatabaseConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            statement.setString(2, username);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? mapUser(result) : null;
            }
        }
    }

    public User findAdmin() throws SQLException {
        String sql = userSelect() + " WHERE u.role_id = " + ADMIN_ROLE + " ORDER BY u.id LIMIT 1";
        try (Connection connection = DatabaseConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql); ResultSet result = statement.executeQuery()) {
            return result.next() ? mapUser(result) : null;
        }
    }

    //updates customer information.
    public void updateProfile(User user) throws SQLException {
        String fullName = Validation.fullName(user.getFullName());
        String[] names = fullName.split(" ", 2);
        String firstName = Validation.namePart(names[0], "First name");
        String lastName = names.length > 1 ? Validation.namePart(names[1], "Last name") : "";
        String contactNumber = Validation.contactNumber(user.getContactNumber());
        String email = Validation.email(Validation.required(user.getEmail(), "Email"));
        String address = Validation.required(user.getAddress(), "Address");
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                String currentAddress;
                String checkSql = "SELECT address FROM users WHERE id = ? AND role_id = " + CUSTOMER_ROLE + " FOR UPDATE";
                try (PreparedStatement checkStatement = connection.prepareStatement(checkSql)) {
                    checkStatement.setInt(1, user.getId());
                    try (ResultSet result = checkStatement.executeQuery()) {
                        if (!result.next()) {
                            throw new IllegalArgumentException("Customer profile was not found.");
                        }
                        currentAddress = result.getString("address");
                    }
                }

                boolean addressIsNew = currentAddress == null || currentAddress.isBlank();
                if (!addressIsNew && !currentAddress.equals(address)) {
                    throw new IllegalArgumentException("For security purposes, your address can only be edited one time.");
                }

                String sql = addressIsNew
                        ? "UPDATE users SET first_name = ?, last_name = ?, contact_no = ?, email = ?, address = ? WHERE id = ? AND role_id = " + CUSTOMER_ROLE
                        : "UPDATE users SET first_name = ?, last_name = ?, contact_no = ?, email = ? WHERE id = ? AND role_id = " + CUSTOMER_ROLE;
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    int index = 1;
                    statement.setString(index++, firstName);
                    statement.setString(index++, lastName);
                    statement.setString(index++, contactNumber);
                    statement.setString(index++, email);
                    if (addressIsNew) {
                        statement.setString(index++, address);
                    }
                    statement.setInt(index, user.getId());
                    if (statement.executeUpdate() == 0) {
                        throw new IllegalArgumentException("Customer profile was not updated.");
                    }
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

    public boolean isDefaultAdminPassword(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE id = ? AND role_id = " + ADMIN_ROLE + " AND user_password = ?";
        try (Connection connection = DatabaseConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setString(2, PasswordUtil.hash("admin123"));
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1) > 0;
            }
        }
    }

    // Admin profile service: updates account and shop information.
    public void updateAdminProfile(User user) throws SQLException {
        String fullName = Validation.fullName(user.getFullName());
        String[] names = fullName.split(" ", 2);
        String firstName = Validation.namePart(names[0], "First name");
        String lastName = names.length > 1 ? Validation.namePart(names[1], "Last name") : "";
        String contactNumber = Validation.contactNumber(user.getContactNumber());
        String email = Validation.email(Validation.required(user.getEmail(), "Email"));
        String question = Validation.securityQuestion(user.getSecurityQuestion());
        String answer = Validation.securityAnswer(user.getSecurityAnswer());
        String shopAddress = shopInfo(user.getShopAddress(), "Rental shop address", 255);
        String shopLandmark = shopInfo(user.getShopLandmark(), "Shop landmark", 255);
        String shopInstructions = shopInfo(user.getShopInstructions(), "Pickup and return instructions", 500);
        String password = user.getPassword() == null ? "" : user.getPassword();
        if (!password.isBlank()) {
            Validation.password(password);
        }

        String sql = password.isBlank()
                ? "UPDATE users SET first_name = ?, last_name = ?, contact_no = ?, email = ?, security_question = ?, security_answer = ? WHERE id = ? AND role_id = " + ADMIN_ROLE
                : "UPDATE users SET first_name = ?, last_name = ?, contact_no = ?, email = ?, security_question = ?, security_answer = ?, user_password = ? WHERE id = ? AND role_id = " + ADMIN_ROLE;
        String shopSql = "INSERT INTO shop_information (admin_id, shop_address, shop_landmark, shop_instructions) VALUES (?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE shop_address = VALUES(shop_address), shop_landmark = VALUES(shop_landmark), shop_instructions = VALUES(shop_instructions)";
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    int index = 1;
                    statement.setString(index++, firstName);
                    statement.setString(index++, lastName);
                    statement.setString(index++, contactNumber);
                    statement.setString(index++, email);
                    statement.setString(index++, question);
                    statement.setString(index++, PasswordUtil.hash(answer.toLowerCase()));
                    if (!password.isBlank()) {
                        statement.setString(index++, PasswordUtil.hash(password));
                    }
                    statement.setInt(index, user.getId());
                    if (statement.executeUpdate() == 0) {
                        throw new IllegalArgumentException("Admin profile was not updated.");
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement(shopSql)) {
                    statement.setInt(1, user.getId());
                    statement.setString(2, shopAddress);
                    statement.setString(3, shopLandmark);
                    statement.setString(4, shopInstructions);
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

    //changes a password after security validation.
    public void changePassword(int userId, String securityAnswer, String newPassword) throws SQLException {
        String answer = Validation.securityAnswer(securityAnswer);
        String password = Validation.password(newPassword);
        String sql = "UPDATE users SET user_password = ? WHERE id = ? AND security_answer = ?";
        try (Connection connection = DatabaseConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, PasswordUtil.hash(password));
            statement.setInt(2, userId);
            statement.setString(3, PasswordUtil.hash(answer.toLowerCase()));
            if (statement.executeUpdate() == 0) {
                throw new IllegalArgumentException("The security answer is incorrect.");
            }
        }
    }

    public List<User> getCustomers(String search, int ownerId) throws SQLException {
        List<User> customers = new ArrayList<>();
        String sql = "SELECT u.*, role_lookup.role_name AS role, "
                + "COALESCE(shop.shop_address, '') AS shop_address, COALESCE(shop.shop_landmark, '') AS shop_landmark, "
                + "COALESCE(shop.shop_instructions, '') AS shop_instructions, COUNT(rental.id) AS rental_count "
                + "FROM users u INNER JOIN roles role_lookup ON role_lookup.id = u.role_id "
                + "LEFT JOIN shop_information shop ON shop.admin_id = u.id "
                + "INNER JOIN rentals rental ON rental.user_id = u.id INNER JOIN items i ON i.id = rental.item_id "
                + "WHERE u.role_id = " + CUSTOMER_ROLE + " AND i.owner_id = ? "
                + "AND (LOWER(u.first_name) LIKE ? OR LOWER(u.last_name) LIKE ? "
                + "OR LOWER(TRIM(CONCAT_WS(' ', NULLIF(u.first_name, ''), NULLIF(u.last_name, '')))) LIKE ? "
                + "OR LOWER(u.username) LIKE ? OR LOWER(u.email) LIKE ?) "
                + "GROUP BY u.id, role_lookup.role_name, shop.shop_address, shop.shop_landmark, shop.shop_instructions "
                + "ORDER BY u.first_name, u.last_name";
        String filter = "%" + (search == null ? "" : search.trim().toLowerCase()) + "%";
        try (Connection connection = DatabaseConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, ownerId);
            statement.setString(2, filter);
            statement.setString(3, filter);
            statement.setString(4, filter);
            statement.setString(5, filter);
            statement.setString(6, filter);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    User user = mapUser(result);
                    user.setTotalRentals(result.getInt("rental_count"));
                    customers.add(user);
                }
            }
        }
        return customers;
    }

    private boolean usernameExists(String username) throws SQLException {
        String sql = "SELECT id FROM users WHERE LOWER(TRIM(username)) = LOWER(?) OR LOWER(TRIM(CONCAT_WS(' ', NULLIF(first_name, ''), NULLIF(last_name, '')))) = LOWER(?)";
        try (Connection connection = DatabaseConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            statement.setString(2, username);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    private User mapUser(ResultSet result) throws SQLException {
        User user = new User(
                result.getInt("id"),
                result.getString("username"),
                result.getString("first_name"),
                result.getString("last_name"),
                result.getString("contact_no"),
                result.getString("email"),
                "",
                result.getString("role"),
                result.getString("security_question"),
                ""
        );
        user.setAddress(result.getString("address"));
        user.setShopAddress(result.getString("shop_address"));
        user.setShopLandmark(result.getString("shop_landmark"));
        user.setShopInstructions(result.getString("shop_instructions"));
        user.setBlocked(result.getBoolean("is_blocked"));
        return user;
    }

    private String userSelect() {
        return "SELECT u.*, role_lookup.role_name AS role, "
                + "COALESCE(shop.shop_address, '') AS shop_address, "
                + "COALESCE(shop.shop_landmark, '') AS shop_landmark, "
                + "COALESCE(shop.shop_instructions, '') AS shop_instructions "
                + "FROM users u "
                + "INNER JOIN roles role_lookup ON role_lookup.id = u.role_id "
                + "LEFT JOIN shop_information shop ON shop.admin_id = u.id";
    }

    private String shopInfo(String value, String fieldName, int maxLength) {
        String text = Validation.required(value, fieldName);
        if (text.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " must not exceed " + maxLength + " characters.");
        }
        return text;
    }
}
