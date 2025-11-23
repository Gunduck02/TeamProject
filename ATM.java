import java.awt.*;
import java.io.*;
import java.net.*;
import javax.swing.*;

public class ATM {
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

    private JButton transferButton;
    private JButton depositButton;
    private JButton withdrawButton;
    private JButton displayAccountsButton;
    private JButton createAccountButton;
    private JButton exitButton;
    private JButton logoutButton;
    private JLabel welcomelabel;
    private JLabel imageLabel;

    private String currentUserId;

    public ATM() {
        initializeGUI();
        connectToServer();
    }

    private void initializeGUI() {
        frame = new JFrame("CNU Bank ATM");
        frame.setSize(500, 400); 
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null); 

        cardLayout = new CardLayout();
        mainContainer = new JPanel(cardLayout);

        createLoginPanel();
        createAtmPanel();

        mainContainer.add(loginPanel, "LOGIN");
        mainContainer.add(atmPanel, "ATM");

        frame.add(mainContainer);
        frame.setVisible(true);
    }

    private void connectToServer() {
        try {
            socket = new Socket("127.0.0.1", 9000); 
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            System.out.println("서버 연결 성공");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "서버에 연결할 수 없습니다.\n서버(BankServer)를 먼저 실행해주세요.");
            System.exit(0);
        }
    }

    private String sendRequest(String msg) { // 서버에 요청을 보내고 응답을 받는 매서드
        try {
            out.println(msg);
            return in.readLine();
        } catch (IOException e) {
            return null;
        }
    }

    private void createLoginPanel() {
        loginPanel = new JPanel(new GridLayout(4, 1, 10, 10));
        loginPanel.setBorder(BorderFactory.createEmptyBorder(50, 100, 50, 100));

        JLabel title = new JLabel("CNU BANK 로그인", SwingConstants.CENTER);
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

    private void createAtmPanel() {
        atmPanel = new JPanel(new BorderLayout());

        transferButton = new JButton("계좌 이체");
        depositButton = new JButton("입금");
        withdrawButton = new JButton("출금");
        displayAccountsButton = new JButton("계좌 조회");
        createAccountButton = new JButton("계좌 개설"); 
        exitButton = new JButton("종료");
        logoutButton = new JButton("로그아웃");
        welcomelabel = new JLabel("CNU BANK ATM 서비스", SwingConstants.CENTER);
        welcomelabel.setFont(new Font("고딕", Font.BOLD, 15));

        transferButton.addActionListener(e -> transfer());
        depositButton.addActionListener(e -> deposit());
        withdrawButton.addActionListener(e -> withdraw());
        displayAccountsButton.addActionListener(e -> displayAccounts());
        createAccountButton.addActionListener(e -> createAccount()); 
        exitButton.addActionListener(e -> System.exit(0));
        logoutButton.addActionListener(e -> processLogout());

        JPanel pL = new JPanel(new GridLayout(4, 1, 5, 5));
        pL.add(displayAccountsButton);
        pL.add(transferButton);
        pL.add(createAccountButton); 
        pL.add(logoutButton);

        JPanel pR = new JPanel(new GridLayout(4, 1, 5, 5)); 
        pR.add(depositButton);
        pR.add(withdrawButton);
        pR.add(new JLabel("")); 
        pR.add(exitButton);

        JPanel pC = new JPanel();
        try {
            ImageIcon icon = new ImageIcon("충남대 로고.png");
            if (icon.getIconWidth() > 0) pC.add(new JLabel(icon));
        } catch (Exception e) {}

        atmPanel.add(welcomelabel, BorderLayout.NORTH);
        atmPanel.add(pL, BorderLayout.WEST);
        atmPanel.add(pR, BorderLayout.EAST);
        atmPanel.add(pC, BorderLayout.CENTER);
    }

    private void processLogin() {
        String id = idField.getText();
        String pw = new String(pwField.getPassword());

        if (id.isEmpty() || pw.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "ID와 비밀번호를 입력하세요.");
            return;
        }

        String response = sendRequest("LOGIN," + id + "," + pw);

        if (response != null && response.startsWith("로그인 성공")) {
            String[] infos = response.split(",");
            String userName = infos[1];
            
            currentUserId = id;
            welcomelabel.setText("환영합니다, " + userName + "님!");
            
            cardLayout.show(mainContainer, "ATM");
            
            idField.setText("");
            pwField.setText("");
        } else {
            JOptionPane.showMessageDialog(frame, "로그인 실패: ID 또는 비밀번호 오류");
        }
    }

    private void processLogout() {
        currentUserId = null;
        JOptionPane.showMessageDialog(frame, "로그아웃 되었습니다.");
        cardLayout.show(mainContainer, "LOGIN");
    }

    private void createAccount() {
        String[] options = {"저축예금(Savings)", "당좌예금(Checking)"};
        int choice = JOptionPane.showOptionDialog(frame, "개설할 계좌 종류를 선택하세요", "계좌 개설",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

        if (choice == -1) return;

        String type = (choice == 0) ? "Savings" : "Checking";
        
        String newAccNum = JOptionPane.showInputDialog(frame, "새로 사용할 계좌번호를 입력하세요:");
        if (newAccNum == null || newAccNum.trim().isEmpty()) return;

        String balanceStr = JOptionPane.showInputDialog(frame, "초기 입금액을 입력하세요:");
        if (balanceStr == null) return;

        String cmd = "ADD_ACCOUNT," + type + "," + currentUserId + "," + newAccNum + "," + balanceStr;

        if (type.equals("Savings")) {
            cmd += ",5.0"; 
            JOptionPane.showMessageDialog(frame, "저축예금 이자율은 기본 5%로 설정됩니다.");
        } else {
            String linked = JOptionPane.showInputDialog(frame, "연결할 저축예금 계좌번호 (없으면 엔터):");
            if (linked == null || linked.trim().isEmpty()) {
                cmd += ",null";
            } else {
                cmd += "," + linked;
            }
        }

        String response = sendRequest(cmd);
        if (response != null) {
            String[] parts = response.split(",");
            if (parts.length > 1) {
                JOptionPane.showMessageDialog(frame, parts[1]);
            } else {
                JOptionPane.showMessageDialog(frame, response);
            }
        }
    }

    private void deposit() {
        String accNum = JOptionPane.showInputDialog(frame, "입금할 계좌번호를 입력하세요:");
        if (accNum == null) return;
        
        String amount = JOptionPane.showInputDialog(frame, "입금액을 입력하세요:");
        if (amount == null) return;

        String response = sendRequest("DEPOSIT," + accNum + "," + amount);
        
        if (response != null) {
            String msg = response.split(",")[1];
            JOptionPane.showMessageDialog(frame, msg);
        }
    }

    private void withdraw() {
        String accNum = JOptionPane.showInputDialog(frame, "출금할 계좌번호를 입력하세요:");
        if (accNum == null) return;
        String amount = JOptionPane.showInputDialog(frame, "출금액을 입력하세요:");
        if (amount == null) return;

        String response = sendRequest("WITHDRAW," + accNum + "," + amount);
        if (response != null) {
            String msg = response.split(",")[1];
            JOptionPane.showMessageDialog(frame, msg);
        }
    }

    private void transfer() {
        String fromAcc = JOptionPane.showInputDialog(frame, "내 출금 계좌번호:");
        if (fromAcc == null) return;
        String toAcc = JOptionPane.showInputDialog(frame, "받을 분의 계좌번호:");
        if (toAcc == null) return;
        String amount = JOptionPane.showInputDialog(frame, "이체할 금액:");
        if (amount == null) return;

        String response = sendRequest("TRANSFER," + fromAcc + "," + toAcc + "," + amount);
        
        if (response != null) {
            String msg = response.split(",")[1];
            JOptionPane.showMessageDialog(frame, msg);
        }
    }

    private void displayAccounts() {
        String accNum = JOptionPane.showInputDialog(frame, "조회할 계좌번호:");
        if (accNum == null) return;

        String response = sendRequest("BALANCE," + accNum);
        
        if (response != null) {
            String[] infos = response.split(",");
            if (infos[0].equals("BALANCE")) {
                JOptionPane.showMessageDialog(frame, "현재 잔액: " + infos[1] + "원");
            } else {
                JOptionPane.showMessageDialog(frame, infos[1]); 
            }
        }
    }

    public static void main(String[] args) {
        new ATM();
    }
}
