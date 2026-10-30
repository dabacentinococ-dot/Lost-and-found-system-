package Model;

public class User {

    private int id;
    private String username;
    private String firstName;
    private String lastName;
    private String contactNumber;
    private String email;
    private String address = "";
    private String shopAddress = "";
    private String shopLandmark = "";
    private String shopInstructions = "";
    private String password;
    private String role;
    private String securityQuestion;
    private String securityAnswer;
    private boolean blocked;
    private int totalRentals;

    
    public User(int id, 
            String username, 
            String firstName, 
            String lastName, 
            String contactNumber, 
            String email, 
            String password, 
            String role, 
            String securityQuestion, 
            String securityAnswer) {
        
        this.id = id;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.contactNumber = contactNumber;
        this.email = email;
        this.password = password;
        this.role = role;
        this.securityQuestion = securityQuestion;
        this.securityAnswer = securityAnswer;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFullName() {
        String name = ((firstName == null ? "" : firstName.trim()) + " " + (lastName == null ? "" : lastName.trim())).trim();
        return name.isEmpty() ? username : name;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address == null ? "" : address;
    }

    public void setAddress(String address) {
        this.address = address == null ? "" : address;
    }

    public String getShopAddress() {
        return shopAddress == null ? "" : shopAddress;
    }

    public void setShopAddress(String shopAddress) {
        this.shopAddress = shopAddress == null ? "" : shopAddress;
    }

    public String getShopLandmark() {
        return shopLandmark == null ? "" : shopLandmark;
    }

    public void setShopLandmark(String shopLandmark) {
        this.shopLandmark = shopLandmark == null ? "" : shopLandmark;
    }

    public String getShopInstructions() {
        return shopInstructions == null ? "" : shopInstructions;
    }

    public void setShopInstructions(String shopInstructions) {
        this.shopInstructions = shopInstructions == null ? "" : shopInstructions;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getSecurityQuestion() {
        return securityQuestion;
    }

    public void setSecurityQuestion(String securityQuestion) {
        this.securityQuestion = securityQuestion;
    }

    public String getSecurityAnswer() {
        return securityAnswer;
    }

    public void setSecurityAnswer(String securityAnswer) {
        this.securityAnswer = securityAnswer;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public int getTotalRentals() {
        return totalRentals;
    }

    public void setTotalRentals(int totalRentals) {
        this.totalRentals = totalRentals;
    }
}
