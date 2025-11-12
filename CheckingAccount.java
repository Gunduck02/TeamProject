

public class CheckingAccount extends Account {
    public CheckingAccount() {
        // TODO Auto-generated method stub
    }
    public String display() {return null;}
    public boolean withdraw(double amount) {
        if(amount > 0 && amount <= this.availableBalance) { //pdf의 출금 조건, 수정 필료
            this.totalBalance -= amount;
            this.availableBalance -= amount;
            return true;
        }
        return false;
    }

}