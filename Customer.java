import java.util.ArrayList;
import java.util.List;

public class Customer {

    private String name;
    private String customerId;
    private String password;
    private String address;
    private String phone;
    
    private List<Account> accountList;

    public Customer(String name, String customerId, String password, String address, String phone) {
        this.name = name;
        this.customerId = customerId;
        this.password = password;
        this.address = address;
        this.phone = phone;
        this.accountList = new ArrayList<>(); 
    }

    public void addAccount(Account account) {
        this.accountList.add(account);
    }

    public boolean removeAccount(String accountNumber) {
        return this.accountList.removeIf(account -> account.getAccountNumber().equals(accountNumber));
    }

    public Account findAccount(String accountNumber) {
        for (Account account : this.accountList) {
            if (account.getAccountNumber().equals(accountNumber)) {
                return account;
            }
        }
        return null; 
    }

    public int getNumberOfAccounts() {
        return this.accountList.size();
    }

    public double getTotalBalance() {
        double total = 0.0;
        for (Account account : this.accountList) {
            total += account.getTotalBalance();
        }
        return total;
    }

    public boolean checkPassword(String password) {
        return this.password.equals(password);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public List<Account> getAccountList() {
        return accountList;
    }

    public void setPassword(String password) {
        this.password = password;
    }
} //변경내용 테스트