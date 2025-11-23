import java.io.*;
import java.net.*;
import java.util.*;

public class BankServer {

    // 데이터 관리용 맵
    private static Map<String, Customer> customerMap = new HashMap<>();
    private static Map<String, Account> accountMap = new HashMap<>();
    
    // 데이터 파일명
    private static final String FILE_CUSTOMER = "customers.txt";
    private static final String FILE_ACCOUNT = "accounts.txt";

    public static void main(String[] args) {
        loadAllData();
        
        try (ServerSocket serverSocket = new ServerSocket(9000, 50, InetAddress.getByName("127.0.0.1"))) {
            System.out.println("=== 은행 서버 가동 (포트: 9000) ===");
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                 Work work = new  Work(clientSocket);
                new Thread(work).start();
            }
        } catch (IOException e) {}
    }

    // 입금 처리 (동기화)
    public static synchronized void processDeposit(String accountNum, double amount) {
        Account acc = accountMap.get(accountNum);
        if (acc != null) {
            acc.deposit(amount);
            saveAllData(); // 변경사항 파일 저장
        }
    }

    // 출금 처리 (동기화)
    public static synchronized void processWithdraw(String accountNum, double amount) {
        Account acc = accountMap.get(accountNum);
        if (acc != null) {
            acc.withdraw(amount);
            saveAllData();
        }
    }

    // 계좌 이체 처리 (동기화)
    public static synchronized boolean processTransfer(String fromAccNum, String toAccNum, double amount) {
        Account sender = accountMap.get(fromAccNum);
        Account receiver = accountMap.get(toAccNum);

        if (sender == null || receiver == null) return false;

        // 보내는 사람 출금 시도
        double beforeBal = sender.getTotalBalance();
        sender.withdraw(amount); 
        double afterBal = sender.getTotalBalance();

        // 출금이 정상적으로 됐으면 받는 사람 입금
        if (afterBal < beforeBal) {
            receiver.deposit(amount);
            saveAllData();
            return true;
        }
        return false;
    }

    // 고객 추가
    public static synchronized void addCustomer(Customer c) {
        customerMap.put(c.getCustomerId(), c);
        saveAllData();
    }

    // 계좌 추가
    public static synchronized void addAccount(Account a) {
        accountMap.put(a.getAccountNumber(), a);
        a.getOwner().addAccount(a); // 고객 객체에도 계좌 정보 추가
        saveAllData();
    }

    // 모든 데이터를 파일에 저장
    public static synchronized void saveAllData() {
        try {
            //고객 데이터 저장
            PrintWriter custOut = new PrintWriter(new FileWriter(FILE_CUSTOMER));
            for (Customer c : customerMap.values()) {
                // 저장 포맷: ID,이름,비번(임시1234),주소,폰번호
                String line = c.getCustomerId() + "," + c.getName() +"," +c.getPassword()+","+ c.getAddress() + "," + c.getPhone();
                custOut.println(line);
            }
            custOut.close();

            //계좌 데이터 저장
            PrintWriter accOut = new PrintWriter(new FileWriter(FILE_ACCOUNT));
            
            // 먼저 저축계좌(Savings) 저장
            for (Account a : accountMap.values()) {
                if (a instanceof SavingsAccount) {
                    SavingsAccount sa = (SavingsAccount) a;
                    String line = "Savings," + sa.getAccountNumber() + "," + sa.getOwner().getCustomerId() + "," 
                            + sa.getTotalBalance() + "," + sa.getInterestRate() + "," + sa.getMaxTransferAmountToChecking();
                    accOut.println(line);
                }
            }
            // 그 다음 당좌계좌(Checking) 저장
            for (Account a : accountMap.values()) {
                if (a instanceof CheckingAccount) {
                    CheckingAccount ca = (CheckingAccount) a;
                    String linkedId = (ca.getLinkedSavings() != null) ? ca.getLinkedSavings().getAccountNumber() : "null";
                    String line = "Checking," + ca.getAccountNumber() + "," + ca.getOwner().getCustomerId() + "," 
                            + ca.getTotalBalance() + "," + linkedId;
                    accOut.println(line);
                }
            }
            accOut.close();
        } catch (IOException e) { e.printStackTrace(); }
    }

    //파일에서 데이터 불러오기
    private static void loadAllData() {
        File custFile = new File(FILE_CUSTOMER);
        File accFile = new File(FILE_ACCOUNT);

        // 초기 더미 데이터 생성
        if (!custFile.exists() || !accFile.exists()) {
            System.out.println("데이터 파일 없음 -> 초기 데이터 생성");
            loadInitialData();
            return;
        }

        try {
            //고객 정보 읽기
            BufferedReader br = new BufferedReader(new FileReader(custFile));
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split(","); // 콤마로 분리
                // ID, 이름, 비번, 주소, 폰번호
                Customer c = new Customer(p[1], p[0], p[2], p[3], p[4]);
                customerMap.put(c.getCustomerId(), c);
            }
            br.close();

            //계좌 정보 읽기
            br = new BufferedReader(new FileReader(accFile));
            while ((line = br.readLine()) != null) {
                String[] p = line.split(",");
                Customer owner = customerMap.get(p[2]); // 고객 ID로 객체 찾기
                
                if (owner != null) {
                    if (p[0].equals("Savings")) {
                        // 포맷: Savings,계좌번호,ID,잔액,이자율,이체한도
                        SavingsAccount sa = new SavingsAccount(owner, p[1], Double.parseDouble(p[3]), Double.parseDouble(p[4]), Double.parseDouble(p[5]));
                        accountMap.put(p[1], sa);
                        owner.addAccount(sa);
                    } else if (p[0].equals("Checking")) {
                        // 포맷: Checking,계좌번호,ID,잔액,연결계좌번호
                        SavingsAccount linked = null;
                        if (!p[4].equals("null")) {
                            linked = (SavingsAccount) accountMap.get(p[4]);
                        }
                        CheckingAccount ca = new CheckingAccount(owner, p[1], Double.parseDouble(p[3]), linked);
                        accountMap.put(p[1], ca);
                        owner.addAccount(ca);
                    }
                }
            }
            br.close();
            System.out.println("파일 로드 완료.");

        } catch (Exception e) { 
            System.out.println("파일 로드 실패. 초기 데이터로 시작합니다.");
            loadInitialData();
        }
    }
    
    // 초기 테스트용 데이터 생성
    private static void loadInitialData() {
        Customer user1 = new Customer("홍경택", "user1", "1234", "대전", "010-1111-2222");
        SavingsAccount savings = new SavingsAccount(user1, "2222", 50000, 5.0, 100000);
        CheckingAccount checking = new CheckingAccount(user1, "1111", 10000, savings);
        
        user1.addAccount(savings);
        user1.addAccount(checking);
        
        addCustomer(user1);
        addAccount(savings);
        addAccount(checking);
    }

    //클라이언트 처리용 스레드 클래스
    static class Work implements Runnable {
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;

        public  Work(Socket socket) { this.socket = socket; }

        //전체적인 구조
        //ATM gui에서 (login, deposit, withdraw, transfer, balance등등 )할 행동 , 행동에 맞는 매개변수를 문자열로 요청 보냄
        //서버에서는 Work스레드에서 요청을 받아 파싱 해 배열로 저장 후 해당 행동 수행
        //infos가 클라이언트의 요청을 파싱한 배열, infos[0]은 명령어, 그 뒤로는 매개변수들
        //서버는 행동 수행 후 결과를 문자열로 클라이언트에 응답 
        //이런 로직으로 구성했습니다.
        //아직 매니저 관련 GUI는 구현 전이라 매니저 로그인 요청이 오면 그냥 성공으로 응답하게 해놨습니다.
        //전체 계좌 조회 기능과, 내 계좌 총액 확인 기능, 고객 추가 기능, 계좌 추가 기능은 매니저 GUI 구현 후에 작업 예정입니다.

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                
                String inputLine;
                while ((inputLine = in.readLine()) != null) {

                    // 클라이언트 메시지 파싱
                    String[] infos = inputLine.split(",");
                    String cmd = infos[0];

                    if (cmd.equals("LOGIN")) {
                        String id = infos[1];
                        String pw = infos[2];
                        if (id.equals("admin") && pw.equals("admin")) {
                            out.println("LOGIN_SUCCESS_MANAGER");
                        } else if (customerMap.containsKey(id)) {
                            Customer c = customerMap.get(id);
                            if (!c.checkPassword(pw)) {
                                out.println("로그인 실패");
                                continue;
                            }   
                            out.println("로그인 성공," + customerMap.get(id).getName());
                        } else {
                            out.println("로그인 실패");
                        }
                    }
                    else if (cmd.equals("DEPOSIT")) {
                        processDeposit(infos[1], Double.parseDouble(infos[2]));
                        out.println("입금 성공");
                    }
                    else if (cmd.equals("WITHDRAW")) {
                        processWithdraw(infos[1], Double.parseDouble(infos[2]));
                        out.println("출금 성공");
                    }
                    else if (cmd.equals("TRANSFER")) {
                        boolean res = processTransfer(infos[1], infos[2], Double.parseDouble(infos[3]));
                        if(res) out.println("이체 성공");
                        else out.println("이체 실패(잔액부족)");
                    }
                    else if (cmd.equals("BALANCE")) {
                        Account acc = accountMap.get(infos[1]);
                        if(acc != null) out.println("BALANCE," + acc.getTotalBalance());
                        else out.println("계좌없음");
                    }
                    // 관리자 기능: 고객 추가
                    else if (cmd.equals("ADD_CUSTOMER")) {
                        Customer c = new Customer(infos[1], infos[2], infos[3], infos[4], infos[5]);
                        addCustomer(c);
                        out.println("SUCCESS,고객생성완료");
                    }
                    // 관리자 기능: 계좌 추가
                    else if (cmd.equals("ADD_ACCOUNT")) {
                        String type = infos[1];
                        Customer owner = customerMap.get(infos[2]);
                        if (owner != null) {
                            if (type.equals("Savings")) {
                                addAccount(new SavingsAccount(owner, infos[3], Double.parseDouble(infos[4]), Double.parseDouble(infos[5]), 100000));
                            } else {
                                SavingsAccount linked = (SavingsAccount) accountMap.get(infos[5]);
                                addAccount(new CheckingAccount(owner, infos[3], Double.parseDouble(infos[4]), linked));
                            }
                            out.println("계좌생성완료");
                        } else {
                            out.println("고객ID없음");
                        }
                    }
                }
            } catch (IOException e) { 
                System.out.println("클라이언트 연결 종료"); 
            }
        }
    }
}
