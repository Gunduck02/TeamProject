import java.awt.*;
import java.io.*;
import java.net.*;
import javax.swing.*;

public class BankManager {
    private JFrame frame;
    
    private CardLayout cardLayout;
    private JPanel mainContainer; 
    private JPanel loginPanel;
    private JPanel atmPanel;     

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private JTextField idField;
    private JPasswordField pwField;

    private JButton checkInfoButton; // 전체 조회 (고객 or 계좌)
    private JButton createAccButton;
    private JButton deleteAccButton;
    private JButton addCusButton;
    private JButton delCusButton;
    private JButton depositButton;
    private JButton withdrawButton;
    private JButton exitButton;
    private JButton logoutButton;
    private JButton balanceButton; // 은행 총 잔고 조회
    private JLabel welcomelabel;

    private String currentUserId; // admin

    public BankManager() {
        bankGui();
        connectToServer();
    }

    private void bankGui() {
        frame = new JFrame("CNU Bank Manager");
        frame.setSize(500, 450); 
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null); 

        cardLayout = new CardLayout();
        mainContainer = new JPanel(cardLayout);

        LoginPanel();
        AtmPanel();

        mainContainer.add(loginPanel, "LOGIN");
        mainContainer.add(atmPanel, "Manager");

        frame.add(mainContainer);
        frame.setVisible(true);
    }

    private void connectToServer() {
        try {
            socket = new Socket("192.168.0.6", 9000); 
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            System.out.println("서버 연결 성공");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "서버에 연결할 수 없습니다.\nBankServer를 먼저 실행해주세요.");
            System.exit(0);
        }
    }

    private String sendRequest(String msg) { 
        try {
            out.println(msg);
            return in.readLine();
        } catch (IOException e) {
            return null;
        }
    }

    private void LoginPanel() {
        loginPanel = new JPanel(new GridLayout(4, 1, 10, 10));
        loginPanel.setBorder(BorderFactory.createEmptyBorder(50, 100, 50, 100));

        JLabel title = new JLabel("관리자 로그인 (admin)", SwingConstants.CENTER);
        title.setFont(new Font("맑은 고딕", Font.BOLD, 20));
        
        idField = new JTextField();
        idField.setBorder(BorderFactory.createTitledBorder("ID"));
        
        pwField = new JPasswordField();
        pwField.setBorder(BorderFactory.createTitledBorder("Password"));

        JButton btnLoginAction = new JButton("접속하기");
        btnLoginAction.addActionListener(e -> processLogin());

        loginPanel.add(title);
        loginPanel.add(idField);
        loginPanel.add(pwField);
        loginPanel.add(btnLoginAction);
    }

    private void AtmPanel() {
        atmPanel = new JPanel(new BorderLayout());

        // 버튼 초기화 및 이벤트 연결
        delCusButton = new JButton("고객 삭제");
        addCusButton = new JButton("고객 추가");
        deleteAccButton = new JButton("계좌 삭제");
        createAccButton = new JButton("계좌 개설");
        exitButton = new JButton("종료");
        logoutButton = new JButton("로그아웃");
        
        balanceButton = new JButton("은행 총 잔고 조회");
        checkInfoButton = new JButton("전체 정보 조회 (고객/계좌)");
        
        depositButton = new JButton("입금");
        withdrawButton = new JButton("출금");
        welcomelabel = new JLabel("CNU BANK 매니저 서비스", SwingConstants.CENTER);
        welcomelabel.setFont(new Font("고딕", Font.BOLD, 15));

        // 리스너 연결
        delCusButton.addActionListener(e -> delCustomer());
        addCusButton.addActionListener(e -> addCustomer());
        deleteAccButton.addActionListener(e -> deleteAccount());
        createAccButton.addActionListener(e -> createAccount()); 
        exitButton.addActionListener(e -> System.exit(0));
        logoutButton.addActionListener(e -> processLogout());
        
        // ** 수정된 기능들 **
        balanceButton.addActionListener(e -> checkTotalBankAssets());
        checkInfoButton.addActionListener(e -> infoBox());
        
        depositButton.addActionListener(e -> deposit());
        withdrawButton.addActionListener(e -> withdraw());

        JPanel pL = new JPanel(new GridLayout(5, 1, 5, 5));
        pL.add(addCusButton);
        pL.add(delCusButton);
        pL.add(createAccButton); 
        pL.add(deleteAccButton);
        pL.add(depositButton);

        JPanel pR = new JPanel(new GridLayout(5, 1, 5, 5)); 
        pR.add(checkInfoButton); // 통합 조회 버튼
        pR.add(balanceButton);   // 총 잔액 버튼
        pR.add(withdrawButton);
        pR.add(logoutButton);  
        pR.add(exitButton);

        JPanel pC = new JPanel();
        atmPanel.add(welcomelabel, BorderLayout.NORTH);
        atmPanel.add(pL, BorderLayout.WEST);
        atmPanel.add(pR, BorderLayout.EAST);
        atmPanel.add(pC, BorderLayout.CENTER);
    }

    // === 로그인 처리 ===
    private void processLogin() {
        String id = idField.getText();
        String pw = new String(pwField.getPassword());

        if (id.isEmpty() || pw.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "ID와 비밀번호를 입력하세요.");
            return;
        }

        String response = sendRequest("LOGIN," + id + "," + pw);

        if (response != null && response.equals("매니저 로그인 성공")) {
            currentUserId = id;
            welcomelabel.setText("관리자 모드 접속 중");
            cardLayout.show(mainContainer, "Manager");
            idField.setText("");
            pwField.setText("");
        } else {
            JOptionPane.showMessageDialog(frame, "관리자 로그인 실패.");
        }
    }

    private void checkTotalBankAssets() {
        String response = sendRequest("BANK_TOTAL_ASSETS");
        if (response != null && response.startsWith("BANK_TOTAL_ASSETS")) {
            String[] parts = response.split(",");
            String total = parts[1];
            JOptionPane.showMessageDialog(frame, "현재 은행의 총 자산은\n" + total + "원 입니다.");
        } else {
            JOptionPane.showMessageDialog(frame, "조회 실패");
        }
    }

    private void infoBox() {
        String[] options = {"전체 고객 목록", "전체 계좌 목록"};
        int choice = JOptionPane.showOptionDialog(frame, "조회할 항목을 선택하세요.", "관리자 조회",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

        if (choice == 0) {
            viewAllCustomers();
        } else if (choice == 1) {
            viewAllAccounts();
        }
    }

    private void viewAllCustomers() {
        String response = sendRequest("ALL_CUSTOMERS");
        if (response != null && response.startsWith("ALL_CUSTOMERS")) {
            String[] parts = response.split(",");
            StringBuilder sb = new StringBuilder("=== 전체 고객 목록 ===\n\n");
            
            if (parts.length == 1) sb.append("등록된 고객이 없습니다.");
            else {
                for (int i = 1; i < parts.length; i++) {
                    String[] info = parts[i].split(":");
                    sb.append(i + ". [ID: " + info[0] + "] 이름: " + info[1] + ", Tel: " + info[2] + "\n");
                }
            }
            scrAccount(sb.toString(), "전체 고객 조회");
        }
    }

    private void viewAllAccounts() {
        String response = sendRequest("ALL_ACCOUNTS");
        if (response != null && response.startsWith("ALL_ACCOUNTS")) {
            String[] parts = response.split(",");
            StringBuilder sb = new StringBuilder("=== 전체 계좌 목록 ===\n\n");

            if (parts.length == 1) sb.append("개설된 계좌가 없습니다.");
            else {
                for (int i = 1; i < parts.length; i++) {
                    String[] info = parts[i].split(":");
                    sb.append(i + ". [" + info[2] + "] " + info[0] + " (소유자: " + info[1] + ")\n");
                    sb.append("    잔액: " + info[3] + "원\n");
                    sb.append("    개설일: " + info[4] + "\n");
                }
            }
            scrAccount(sb.toString(), "전체 계좌 조회");
        }
    }

    private void scrAccount(String content, String title) {
        JTextArea textArea = new JTextArea(content);
        textArea.setEditable(false);
        textArea.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(400, 300));
        JOptionPane.showMessageDialog(frame, scrollPane, title, JOptionPane.PLAIN_MESSAGE);
    }
    private void deleteAccount() {
        String accNum = JOptionPane.showInputDialog(frame, "삭제할 계좌 번호를 입력하세요:");
        if (accNum != null && !accNum.trim().isEmpty()) {
            String response = sendRequest("DELETE_ACCOUNT," + currentUserId + "," + accNum);
            JOptionPane.showMessageDialog(frame, response);
        }
    }

    private void createAccount() {

        String targetId = JOptionPane.showInputDialog(frame, "계좌를 개설할 고객 ID를 입력하세요:");
        if(targetId == null) return;
        
        JOptionPane.showMessageDialog(frame, "기능 구현 중: ATM 클라이언트의 계좌 개설 로직을 참고하여 구현 필요");
    }

    private void addCustomer() {
        String custName = JOptionPane.showInputDialog(frame, "추가할 고객 이름:");
        String custId = JOptionPane.showInputDialog(frame, "아이디:");
        String custPw = JOptionPane.showInputDialog(frame, "비밀번호:");
        String custAddr = JOptionPane.showInputDialog(frame, "주소:");
        String custPhone = JOptionPane.showInputDialog(frame, "전화번호:");
        
        if (custName != null && custId != null) {
            String response = sendRequest("ADD_CUSTOMER," + custName + "," + custId + "," + custPw + "," + custAddr + "," + custPhone);
            JOptionPane.showMessageDialog(frame, response);
        }
    }

    private void delCustomer() {
        String custId = JOptionPane.showInputDialog(frame, "삭제할 고객 ID:");
        if (custId != null) {
            String response = sendRequest("DELETE_CUSTOMER," + custId);
            JOptionPane.showMessageDialog(frame, response);
        }
    }

    private void deposit() {
        String accNum = JOptionPane.showInputDialog(frame, "입금할 계좌번호:");
        String amount = JOptionPane.showInputDialog(frame, "입금액:");
        if (accNum != null && amount != null) {
            String response = sendRequest("DEPOSIT," + accNum + "," + amount + ",admin");
            JOptionPane.showMessageDialog(frame, response);
        }
    }

    private void withdraw() {
        String accNum = JOptionPane.showInputDialog(frame, "출금할 계좌번호:");
        String amount = JOptionPane.showInputDialog(frame, "출금액:");
        if (accNum != null && amount != null) {
            String response = sendRequest("WITHDRAW," + accNum + "," + amount);
            JOptionPane.showMessageDialog(frame, response);
        }
    }

    private void processLogout() {
        currentUserId = null;
        JOptionPane.showMessageDialog(frame, "로그아웃 되었습니다.");
        cardLayout.show(mainContainer, "LOGIN");
    }

    public static void main(String[] args) {
        new BankManager();
    }
}