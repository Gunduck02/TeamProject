import java.time.LocalDate;

public abstract class Account {

    protected Customer owner;
    protected String accountNumber;
    protected String accountType;
    protected double totalBalance;
    protected double availableBalance;
    protected LocalDate openDate;

    public Account() {}

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
            System.out.println("0보다 작은 돈을 입금할 수 없습니다.");
        }
    }

    public abstract boolean withdraw(double amount); //void boolean 고민중              

    public abstract String display(); //나중에 당금계좌 저축계좌 구분해서 작성해야하

    public Customer getOwner() {
        return owner;
    }
                                        
    public void setOwner(Customer owner) { //소유자 설정
        this.owner = owner;
    }

    public String getAccountNumber() { //계좌번호
        return accountNumber;
    }

    public String getAccountType() { //계좌유형
        return accountType;
    }

    public double getTotalBalance() { //전체 잔액
        return totalBalance;
    }

    public double getAvailableBalance() { //사용 가능 잔액
        return availableBalance;
    }

    public LocalDate getOpenDate() { //개설일
        return openDate;
    }
} //테스트 2