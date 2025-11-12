import java.time.LocalDate;

public abstract class Account {

    protected Customer owner;
    protected String accountNumber;
    protected String accountType;
    protected double totalBalance;
    protected double availableBalance;
    protected LocalDate openDate;

    public Account(Customer owner, String accountNumber, String accountType, double initialDeposit) {
        this.owner = owner;
        this.accountNumber = accountNumber;
        this.accountType = accountType;
        this.totalBalance = initialDeposit;
        this.availableBalance = initialDeposit;
        this.openDate = LocalDate.now();
    }

    public void deposit(double amount) {
        if (amount > 0) {
            this.totalBalance += amount;
            this.availableBalance += amount;
        } else {
            //아직 미작성
        }
    }

    public abstract boolean withdraw(double amount);

    public abstract String display(); //나중에 당금계좌 저축계좌 구분해서 작성해야하

    public Customer getOwner() {
        return owner;
    }

    public void setOwner(Customer owner) {
        this.owner = owner;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getAccountType() {
        return accountType;
    }

    public double getTotalBalance() {
        return totalBalance;
    }

    public double getAvailableBalance() {
        return availableBalance;
    }

    public LocalDate getOpenDate() {
        return openDate;
    }
} //테스트 2