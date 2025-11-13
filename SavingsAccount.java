
public class SavingsAccount extends Account {

    private double interestRate;
    private double maxTransferAmountToChecking; //저축 예금 계좌만의 속성

    public SavingsAccount(Customer owner, String accountNumber, double initialDeposit, double interestRate, double maxTransferAmountToChecking) {
        super(owner, accountNumber, "저축 예금", initialDeposit);
        this.interestRate = interestRate;
        this.maxTransferAmountToChecking = maxTransferAmountToChecking;
    }

    public double getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(double interestRate) {
        this.interestRate = interestRate;
    }

    public double getMaxTransferAmountToChecking() {
        return maxTransferAmountToChecking;
    }

    public void setMaxTransferAmountToChecking(double maxTransferAmountToChecking) {
        this.maxTransferAmountToChecking = maxTransferAmountToChecking;
    }

//이자율 적용에 관련 메소드 아직 구현 안함

    @Override
    public void withdraw(double amount) {
        if (amount <= 0) {
            System.out.println("출금 실패: 출금액은 0보다 커야합니다");
            return;
        }

        if (this.availableBalance >= amount) {
            this.availableBalance -= amount;
            this.totalBalance -= amount;
            System.out.println("출금 성공 : " + amount + "원이 출금되었습니다.");
            return;
        } else {
            return;
            System.out.println("출금 실패: 잔액이 부족합니다.");
        }
    }

    @Override
    public void display() {
        System.out.println("계좌 유형: 저축예금 계좌");
        System.out.println("소유자: " + this.owner.getName());
        System.out.println("계좌 번호: " + this.accountNumber);
        System.out.printf("통장 잔액: %.2f\n", this.totalBalance);
        System.out.printf("이용 가능 잔액: %.2f\n", this.availableBalance);
        System.out.printf("이자율: %.2f%%\n", this.interestRate);
    }
}