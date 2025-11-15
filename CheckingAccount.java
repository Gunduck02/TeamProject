public class CheckingAccount extends Account {

    private SavingsAccount linkedSavings; //당좌 계좌 잔액 부족 시 연결될 계좌

    public CheckingAccount(Customer owner, String accountNumber, double initialDeposit, SavingsAccount linkedAccount) {
        super(owner, accountNumber, "당좌 예금", initialDeposit);
        this.linkedSavings = linkedAccount;
    }

    public SavingsAccount getLinkedSavings() {
        return linkedSavings;
    }

    public void setLinkedSavings(SavingsAccount linkedSavings) {
        this.linkedSavings = linkedSavings;
    }

    @Override
    public void withdraw(double amount) {
        if (amount <= 0) {
            System.out.println("출금 실패: 출금액은 0보다 커야 합니다."); 
            return;
        } //음수 출금 불가

        if (this.availableBalance >= amount) {
            this.availableBalance -= amount;
            this.totalBalance -= amount;
            System.out.println("출금 성공: " + amount + "원이 출금되었습니다.");
            return;
        }//당좌 잔액으로 출금 가능할 때

        if (this.linkedSavings == null) {
            System.out.println("출금 실패: 잔액이 부족하며, 연결된 저축계좌가 없습니다.");
            return;
        }

        double needed = amount - this.availableBalance; //당좌 잔액으로 부족한 금액

        double maxCanPull = this.linkedSavings.getMaxTransferAmountToChecking(); //연결된 저축계좌 최대 이체 한도
        double availableInSavings = this.linkedSavings.getAvailableBalance(); //연결된 계좌 잔액

        if (needed > maxCanPull) {
            System.out.println("출금 실패: 필요한 금액(" + needed + ")이 자동이체 한도(" + maxCanPull + ")를 초과합니다.");
            return;
        }

        if (needed > availableInSavings) {
            System.out.println("출금 실패: 연결된 저축계좌의 잔액(" + availableInSavings + ")이 부족합니다.");
            return;
        } //이체한도보다 부족하거나 잔액 부족 시 출금 불가
        
        this.linkedSavings.availableBalance -= needed;
        this.linkedSavings.totalBalance -= needed;
        
    
        this.availableBalance += needed; 
        this.totalBalance += needed;
        this.availableBalance -= amount;
        this.totalBalance -= amount;

        System.out.println("출금 성공: 저축계좌에서 " + needed + "원 이체 후 " + amount + "원 출금 완료.");
        return;
    }

    @Override
    public void display() {
        String linkedAccNum = (this.linkedSavings != null) ? this.linkedSavings.getAccountNumber() : "None";
        System.out.println("계좌 유형: 당좌 예금 계좌");
        System.out.println("소유자: " + this.owner.getName());
        System.out.println("계좌 번호: " + this.accountNumber);
        System.out.println("통장 잔액: " + this.totalBalance);
        System.out.println("이용 가능 잔액: " + this.availableBalance);
        System.out.println("연결된 계좌: " + linkedAccNum);
    }
}