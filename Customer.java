import java.util.ArrayList;
import java.util.List;

public class Customer {

    private String name;
    private String customerId;
    private String password;
    private String address;
    private String phone;
    
    private List<Account> accountList; //고객은 여러 계좌 소유 가능함, 리스트로 관리

    public Customer(String name, String customerId, String password, String address, String phone) {
        this.name = name;
        this.customerId = customerId;
        this.password = password;
        this.address = address;
        this.phone = phone;
        this.accountList = new ArrayList<>(); 
    }

    public void addAccount(Account account) { //계좌 추가
        this.accountList.add(account);
    }

    public boolean removeAccount(String accountNumber) { //계좌 삭제
        return this.accountList.removeIf(account -> account.getAccountNumber().equals(accountNumber));
    }

    public Account findAccount(String accountNumber) { //계좌 찾기
        for (Account account : this.accountList) {
            if (account.getAccountNumber().equals(accountNumber)) {
                return account;
            }
        }
        return null; 
    }

    public int getNumberOfAccounts() { //계좌 개수
        return this.accountList.size();
    }

    public double getTotalBalance() { //전체 잔액
        double total = 0.0;
        for (Account account : this.accountList) {
            total += account.getTotalBalance();
        }
        return total;
    }

    public boolean checkPassword(String password) { //비밀번호 확인
        return this.password.equals(password);
    }

    public String getName() { //이름
        return name;
    }

    public void setName(String name) {//이름 변경
        this.name = name;
    }

    public String getCustomerId() { //고객
        return customerId;
    }

    public String getAddress() {//주소
        return address;
    }

    public void setAddress(String address) { //주소 변경
        this.address = address;
    }

    public String getPhone() { //전화번호
        return phone;
    }

    public void setPhone(String phone) { //전화번호 변경
        this.phone = phone;
    }
    
    public List<Account> getAccountList() { //계좌 목록 반환
        return accountList;
    }

    public void setPassword(String password) { //비밀번호 변경
        this.password = password;
    }
    public static void main(String[] args) { //테스트용 메인 메소드
        Customer customer = new Customer("홍길동", "hong123", "password", "서울시 강남구", "010-1234-5678");
        System.out.println("고객 이름: " + customer.getName());
        
        SavingsAccount savingsAccount = new SavingsAccount(customer, "abc2", 1000000, 2.5, 300000);
        CheckingAccount checkingAccount = new CheckingAccount(customer, "abc1", 500000, savingsAccount);
        customer.addAccount(checkingAccount);
        customer.addAccount(savingsAccount);
        checkingAccount.display();
        savingsAccount.display();

    }
} //깃허브 테스트