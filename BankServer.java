import java.io.*;
import java.net.*;
import java.time.LocalDate;
import java.util.*;
import java.awt.*;       
import javax.swing.*;    

public class BankServer {

    static int count = 0;

    // 데이터 관리용 맵
    private static Map<String, Customer> customerMap = new HashMap<>();
    private static Map<String, Account> accountMap = new HashMap<>();
    
    // 데이터 파일명
    private static final String FILE_CUSTOMER = "customers.txt";
    private static final String FILE_ACCOUNT = "accounts.txt";

    private static JFrame frame;
    private static JTextArea logArea; 

    private static void severGui(){
        frame = new JFrame("CNU Bank Server Monitor");
        frame.setSize(600, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        JPanel top = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setFont(new Font("고딕", Font.BOLD, 20));
                g.drawString("현재 접속자 수: " + count + "명", 20, 35);
            }
        };
        
        top.setPreferredSize(new Dimension(600, 60));
        top.setBackground(new Color(230, 230, 250));
        
        logArea = new JTextArea();
        logArea.setEditable(false); 
        logArea.setFont(new Font("고딕", Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(logArea);
        
        frame.add(top, BorderLayout.NORTH);
        frame.add(scrollPane, BorderLayout.CENTER);

        frame.setVisible(true);
    }

    public static void log(String msg) {
        String time = java.time.LocalTime.now().toString().substring(0, 8); 
        logArea.append("[" + time + "] " + msg + "\n"); 
        try {
            logArea.setCaretPosition(logArea.getDocument().getLength()); 
        } catch (Exception e) {}
    }

    public static void main(String[] args) {
        loadAllData();
        severGui(); 
        
        try (ServerSocket serverSocket = new ServerSocket(9000, 50)) {
            System.out.println("=== 은행 서버 실행 (포트: 9000) ===");
            log("=== 은행 서버 실행 (포트: 9000) ==="); 
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                 Work work = new  Work(clientSocket);
                new Thread(work).start();
                
                count++;
                frame.repaint(); // 화면 전체 다시 그리기
                
                System.out.println("클라이언트 접속: " + clientSocket.getInetAddress());
                System.out.println("현재 접속자 수: " + count);
                log("새로운 클라이언트 접속: " + clientSocket.getInetAddress()); 
            }
        } catch (IOException e) {}
    }
    
    // 입금 처리 (동기화)
    public static synchronized void processDeposit(String accountNum, double amount) {
        Account acc = accountMap.get(accountNum);
        if (acc != null) {
            acc.deposit(amount);
            acc.addLog("입금: +" + (int)amount + "원");
            saveAllData(); // 변경사항 파일 저장
            log("[입금] " + acc.getOwner().getName() + "님이 " + (int)amount + "원 입금 (계좌:" + accountNum + ")");
        }
    }

    // [수정] 출금 처리 (반환형을 boolean으로 변경)
    public static synchronized boolean processWithdraw(String accountNum, double amount) {
        Account acc = accountMap.get(accountNum);
        
        if (acc != null) {
            // 1. 동결 계좌 체크
            if (acc.isFrozen()) {
                log("[차단됨] 동결된 계좌에서 출금 시도: " + accountNum + " (소유자: " + acc.getOwner().getName() + ")");
                return false; // 실패 반환
            }

            // 2. 출금 시도 (한 번만 호출!)
            boolean success = acc.withdraw(amount); 
            
            // 3. 결과 처리
            if (success) {
                saveAllData(); // 성공했을 때만 저장
                log("[출금] " + acc.getOwner().getName() + "님이 " + (int)amount + "원 출금 (계좌:" + accountNum + ")");
                return true; // 성공 반환
            } else {
                log("[출금 실패] 잔액 부족 또는 기타 사유로 출금 실패 (계좌:" + accountNum + ")");
                return false; // 실패 반환
            }
        }
        return false; // 계좌가 없는 경우 실패
    }

    // 계좌 이체 처리 (동기화)
    public static synchronized boolean processTransfer(String fromAccNum, String toAccNum, double amount) {
        Account sender = accountMap.get(fromAccNum);
        Account receiver = accountMap.get(toAccNum);

        if (sender == null || receiver == null) return false;

        if (sender.isFrozen()) {
            log("[차단됨] 동결된 계좌에서 이체 시도: " + fromAccNum + " (소유자: " + sender.getOwner().getName() + ")");
            return false; 
        }

        // 보내는 사람 출금 시도
        double beforeBal = sender.getTotalBalance();
        sender.withdraw(amount); 
        double afterBal = sender.getTotalBalance();

        // 출금이 정상적으로 됐으면 받는 사람 입금
        if (afterBal < beforeBal) {
            receiver.deposit(amount);

            sender.addLog("이체(보냄): -" + (int)amount + "원 (받는분:" + receiver.getOwner().getName() + ")");
            receiver.addLog("이체(받음): +" + (int)amount + "원 (보낸분:" + sender.getOwner().getName() + ")");
            saveAllData();
            log("[이체] " + sender.getOwner().getName() + " -> " + receiver.getOwner().getName() + " (" + (int)amount + "원)");
            return true;
        }
        return false;
    }

    // 고객 추가
    public static synchronized void addCustomer(Customer c) {
        customerMap.put(c.getCustomerId(), c);
        saveAllData();
        log("[관리자] 신규 고객 등록: " + c.getName()); 
    }

    // 계좌 추가
    public static synchronized void addAccount(Account a) {
        accountMap.put(a.getAccountNumber(), a);
        a.getOwner().addAccount(a); // 고객 객체에도 계좌 정보 추가
        saveAllData();
        log("[관리자] 신규 계좌 개설: " + a.getAccountNumber() + " (" + a.getOwner().getName() + ")"); 
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
                            + sa.getTotalBalance() + "," + sa.getInterestRate() + "," + sa.getMaxTransferAmountToChecking()+ "," + sa.getOpenDate();
                    accOut.println(line);
                }
            }
            // 그 다음 당좌계좌(Checking) 저장
            for (Account a : accountMap.values()) {
                if (a instanceof CheckingAccount) {
                    CheckingAccount ca = (CheckingAccount) a;
                    String linkedId = (ca.getLinkedSavings() != null) ? ca.getLinkedSavings().getAccountNumber() : "null";
                    String line = "Checking," + ca.getAccountNumber() + "," + ca.getOwner().getCustomerId() + "," 
                            + ca.getTotalBalance() + "," + linkedId+ "," + ca.getOpenDate();
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
            initData();
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
                        //Savings,계좌번호,ID,잔액,이자율,이체한도
                        SavingsAccount sa = new SavingsAccount(owner, p[1], Double.parseDouble(p[3]), Double.parseDouble(p[4]), Double.parseDouble(p[5]));
                        if (p.length > 6) {
                            sa.setOpenDate(LocalDate.parse(p[6])); 
                        }
                        accountMap.put(p[1], sa);
                        owner.addAccount(sa);
                    } else if (p[0].equals("Checking")) {
                        //Checking,계좌번호,ID,잔액,연결계좌번호
                        SavingsAccount linked = null;
                        if (!p[4].equals("null")) {
                            linked = (SavingsAccount) accountMap.get(p[4]);
                        }
                        CheckingAccount ca = new CheckingAccount(owner, p[1], Double.parseDouble(p[3]), linked);
                        if (p.length > 5) { // Checking은 저장 항목 개수가 달라서 인덱스가 다름에 주의
                            ca.setOpenDate(LocalDate.parse(p[5]));
                        }
                        accountMap.put(p[1], ca);
                        owner.addAccount(ca);
                    }
                }
            }
            br.close();
            System.out.println("파일 로드 완료.");

        } catch (Exception e) { 
            System.out.println("파일 로드 실패. 초기 데이터로 시작합니다.");
            initData();
        }
    }
    
    // 초기 테스트용 데이터 생성
    private static void initData() {
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
        private String connectedUserId = null; // 현재 접속자 ID 저장 (로그용)

        public  Work(Socket socket) { this.socket = socket; }

        //전체적인 구조
        //ATM gui에서 (login, deposit, withdraw, transfer, balance등등 )할 행동 , 행동에 맞는 매개변수를 문자열로 요청 보냄
        //서버에서는 Work스레드에서 요청을 받아 파싱 해 배열로 저장 후 해당 행동 수행
        //infos가 클라이언트의 요청을 파싱한 배열, infos[0]은 명령어, 그 뒤로는 매개변수들
        //서버는 행동 수행 후 결과를 문자열로 클라이언트에 응답 
        //이런 로직으로 구성했습니다.

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
                            out.println("매니저 로그인 성공");
                            log("[로그인] 관리자(Admin) 로그인"); 
                        } else if (customerMap.containsKey(id)) {
                            Customer c = customerMap.get(id);
                            if (!c.checkPassword(pw)) {
                                out.println("로그인 실패");
                                log("[로그인 실패] ID: " + id + " - 비밀번호 오류"); 
                                continue;
                            }   
                            out.println("로그인 성공," + customerMap.get(id).getName());
                            connectedUserId = id; // 접속자 기록
                            log("[로그인] " + c.getName() + "(" + id + ") 로그인 성공"); 
                        } else {
                            out.println("로그인 실패");
                            log("[로그인 실패] 존재하지 않는 ID 시도: " + id); 
                        }
                    }
                    else if (cmd.equals("BANK_TOTAL_ASSETS")) {
                        double total = 0;
                        for (Account a : accountMap.values()) {
                            total += a.getTotalBalance();
                        }
                        out.println("BANK_TOTAL_ASSETS," + total);
                    }
                    else if (cmd.equals("ALL_CUSTOMERS")) {
                        StringBuilder sb = new StringBuilder("ALL_CUSTOMERS");
                        for (Customer c : customerMap.values()) {
                            sb.append("#");
                            sb.append(c.getCustomerId() + "," + c.getName() + "," + c.getPhone());
                        }
                        out.println(sb.toString());
                    }
                    else if (cmd.equals("ALL_ACCOUNTS")) {
                        StringBuilder sb = new StringBuilder("ALL_ACCOUNTS");
                        for (Account a : accountMap.values()) {
                            sb.append("#");
                            String type = (a instanceof SavingsAccount) ? "저축" : "당좌";
                            sb.append(a.getAccountNumber() + "," + a.getOwner().getName() + "," + type + "," + a.getTotalBalance()+","+a.getOpenDate());
                        }
                        out.println(sb.toString());
                    }
                    else if (cmd.equals("DEPOSIT")) {
                        String accNum = infos[1];
                        String amountStr = infos[2];
                        String requestUserId = infos[3];
                        
                        double amount = Double.parseDouble(amountStr);    
                        //  [수정] 금액이 0 이하인지 가장 먼저 체크 (오상룡)
                    if (amount <= 0) {
                        out.println("이체 실패: 이체 금액은 0보다 커야 합니다.");
                        log("[입금 실패] 잘못된 입금 금액: " + amount + ". 요청자: " + requestUserId);}
                    else { Account acc = accountMap.get(accNum);    
                        // 관리자(admin)이거나, 계좌 소유주가 본인인 경우 허용
                        if (acc != null && (requestUserId.equals("admin") || acc.getOwner().getCustomerId().equals(requestUserId))) {
                            processDeposit(accNum, Double.parseDouble(amountStr));
                            out.println("입금 성공");
                        } else {
                            out.println("입금 실패(권한 없음 또는 계좌 없음)");
                            log("[입금 실패] 권한 없음. 요청자: " + requestUserId); 
                        }
                    }
                }
                    // [수정] 계좌 동결 출금 처리 부분(오상룡)
                    else if (cmd.equals("WITHDRAW")) {
                    String accNum = infos[1];
                    double amount = Double.parseDouble(infos[2]);
                    Account acc = accountMap.get(accNum);

                if (acc != null) {
                    // [여기서 미리 체크해서 클라이언트에게 알려줌]
                    if (acc.isFrozen()) {
                        out.println("출금 실패: 보이스피싱 의심으로 동결된 계좌입니다. 은행에 문의하세요.");
                    } else {
                        boolean success = processWithdraw(accNum, amount);

                        if (success) {
                            out.println("출금 성공");
                            
                        } else {
                            out.println("출금 실패: 잔액 부족 또는 기타 사유로 출금할 수 없습니다.");
                        }
                      }
                    } 
                 else {
                  out.println("출금 실패: 존재하지 않는 계좌입니다.");
                }
         }
                    else if (cmd.equals("TRANSFER")) {
                        String fromAccNum = infos[1];
                        String toAccNum = infos[2];
                        double amount = Double.parseDouble(infos[3]);
                        String requestUserId = infos[4]; 
                        Account sendAcc = accountMap.get(fromAccNum);
                        
                        // 이체금액 0<=amount 검사 (오상룡)
                        if (amount <= 0) {
                        out.println("이체 실패: 이체 금액은 0보다 커야 합니다.");
                        log("[이체 실패] 잘못된 이체 금액: " + amount + ". 요청자: " + requestUserId);} 
                
                        else if (sendAcc == null) {
                            out.println("보내시는 계좌가 존재하지 않습니다.");
                        } 
                        else if (!sendAcc.getOwner().getCustomerId().equals(requestUserId)) {
                            out.println("본인의 계좌에서만 이체할 수 있습니다.");
                            log("[이체 실패] 권한 없음. 요청자: " + requestUserId); 
                        } 
                        else if (sendAcc.isFrozen()) {
                            out.println("이체 실패: 보이스피싱 의심으로 동결된 계좌입니다.");
                            log("[차단됨] 동결된 계좌 이체 시도: " + fromAccNum);
                        }
                        else {
                            // 검증 통과 시 이체 진행
                            boolean trs = processTransfer(fromAccNum, toAccNum, amount);
                            if(trs) out.println("이체 성공");
                            else {
                                out.println("이체 실패(잔액부족)");
                                log("[이체 실패] 잔액 부족. 계좌: " + fromAccNum); 
                            }
                        }
                    }
                    else if (cmd.equals("BALANCE")) {
                        Account acc = accountMap.get(infos[1]);
                        if(acc != null) out.println("BALANCE," + acc.getTotalBalance());
                        else out.println("계좌없음");
                    }//소유한 모든 계좌의 총액 확인
                    else if(cmd.equals("TOTAL_BALANCE")) {
                        String userId = infos[1]; 
                        Customer c = customerMap.get(userId);
    
                        if (c != null) {
                            double total = c.getTotalBalance(); 
                            out.println("TOTAL_BALANCE," + total);
                        } else {
                            out.println("고객 정보를 찾을 수 없습니다.");
                        }
                        
                    }
                    // 관리자 기능: 전체 계좌 조회
                    else if (cmd.equals("ACCOUNT_CHECK")) {
                        String userId = infos[1];
                        Customer c = customerMap.get(userId);

                        if (c != null) {
                            StringBuilder sb = new StringBuilder("ACCOUNT_CHECK");
        
                            for (Account a : c.getAccountList()) {
                                sb.append(","); 
            
                                String type = (a instanceof SavingsAccount) ? "저축" : "당좌";
                               String logData = a.getLogString().replace("\n", "<br>"); // 로그 가져오기 & 줄바꿈(\n)을 특수문자 <br>로 변경 (통신 끊김 방지) (오상룡)

                                // 데이터 조립: [타입 : 계좌번호 : 잔액 : 로그내역]
                                sb.append(type)
                                  .append(":")
                                  .append(a.getAccountNumber())
                                  .append(":")
                                  .append(a.getTotalBalance())
                                  .append(":") // 구분자 추가
                                  .append(logData); // 로그 데이터 추가
                            }
                            out.println(sb.toString()); // 예: ALL_ACCOUNTS,저축:111:5000,당좌:222:1000
                        } else {
                            out.println("고객 정보 없음");
                        }
                    }
                    // 관리자 기능: 고객 추가
                    else if (cmd.equals("ADD_CUSTOMER")) {
                        Customer c = new Customer(infos[1], infos[2], infos[3], infos[4], infos[5]);
                        addCustomer(c);
                        out.println("고객생성완료");
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
                    else if (cmd.equals("DELETE_CUSTOMER")) {
                        String targetId = infos[1];
                        Customer c = customerMap.get(targetId);
                        if (c != null) {
                            // 해당 고객의 모든 계좌 삭제 처리
                            ArrayList<Account> userAccounts = new ArrayList<>(c.getAccountList());
                            for(Account a : userAccounts) {
                                accountMap.remove(a.getAccountNumber());
                            }
                            customerMap.remove(targetId);
                            saveAllData(); // 파일 업데이트
                            out.println("고객 및 관련 계좌 삭제 완료");
                            log("[관리자] 고객 삭제 완료: " + targetId); 
                        } else {
                            out.println("존재하지 않는 고객 ID입니다.");
                        }
                    }
                    else if (cmd.equals("DELETE_ACCOUNT")) {
                        String accNum = infos[2];
                        Account acc = accountMap.get(accNum);
                        if (acc != null) {
                            Customer owner = acc.getOwner();
                            owner.removeAccount(accNum); // 고객 객체 리스트에서 삭제
                            accountMap.remove(accNum);   // 전체 맵에서 삭제
                            saveAllData();
                            out.println("계좌 삭제 완료");
                            log("[관리자] 계좌 삭제 완료: " + accNum); 
                        } else {
                            out.println("존재하지 않는 계좌번호입니다.");
                        }
                    }


                    else if (cmd.equals("TOGGLE_FREEZE")) {
                        String accNum = infos[1]; // 동결할 계좌번호
                        Account acc = accountMap.get(accNum);

                        if (acc != null) {
                        // 상태 반전 (동결 -> 해제, 해제 -> 동결)
                            boolean newState = !acc.isFrozen();
                            acc.setFrozen(newState);
        
                            saveAllData(); 
        
                            String status = newState ? "동결(Freeze)" : "정상(Active)";
                            out.println("계좌 상태 변경 완료: " + status);
                            log("[관리자] 계좌 상태 변경 (" + accNum + ") -> " + status);
                        } else {
                            out.println("존재하지 않는 계좌입니다.");
                        }
                    }                       
                }
            } catch (IOException e) { 
            } finally{
                if(BankServer.count > 0) BankServer.count--;
                frame.repaint(); // 접속 종료 시 repaint로 화면 갱신
                System.out.println("클라이언트 접속 종료");
                System.out.println("현재 접속자 수: " + BankServer.count);
                
                String byeUser = (connectedUserId != null) ? connectedUserId : "익명"; 
                log("클라이언트 접속 종료 (" + byeUser + ")"); 
            }
        }
    }
}
