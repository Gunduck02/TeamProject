import java.time.LocalDate;
import java.util.*;
import java.time.LocalTime;

public abstract class Account {

    protected Customer owner;
    protected String accountNumber;
    protected String accountType;
    protected double totalBalance;
    protected double availableBalance;
    protected LocalDate openDate;
    private boolean isFrozen = false; 

    // [추가] 보이스 피싱 대비 동결 상태 확인 및 설정 메서드(오상룡)
    public boolean isFrozen() {
        return isFrozen;
    }

    public void setFrozen(boolean frozen) {
        this.isFrozen = frozen;
    }
    // 거래 기록 리스트(오상룡)
    protected List<String> logs = new ArrayList<>();

    // 기록을 추가하는 기능 (날짜+시간 포함)
    public void addLog(String message) {
        String date = LocalDate.now().toString(); 
        String time = LocalTime.now().toString().substring(0, 8); // 초까지만
        
        // 예: [2025-11-30 14:30:05] 입금: +50000원
        logs.add("[" + date + " " + time + "] " + message);
    }

    // 기록을 꺼내주는 기능  
    public String getLogString() {
        if (logs.isEmpty()) {
            return "거래 내역이 없습니다.";
        }
        // 리스트에 있는 내역들을 줄바꿈(\n)으로 구분된 하나의 문자열로 반환
        return String.join("\n", logs);
    }

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

    public abstract boolean withdraw(double amount); //void boolean 고민중--> boolean결정(오상룡)             

    public abstract void display(); //나중에 당금계좌 저축계좌 구분해서 작성해야하

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
    public void setOpenDate(LocalDate openDate) {
    this.openDate = openDate;
}
} //테스트 2