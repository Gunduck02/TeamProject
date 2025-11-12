

public class CheckingAccount extends Account {
    public CheckingAccount() {
        // TODO Auto-generated method stub
    }
    public String display() {return null;}
    public boolean withdraw(double amount) {
        if(amount > 0 && amount <= this.availableBalance) {
            this.totalBalance -= amount;
            this.availableBalance -= amount;
            return true;
        }
        return false;
    }

}