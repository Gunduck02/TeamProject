import java.awt.*;
import java.util.*;
import javax.swing.*;

public class ATM {
    private JFrame frame;
    private JButton transferButton;
    private JButton depositButton;
    private JButton withdrawButton;
    private JButton displayAccountsButton;
    private JButton exitButton;
    private JButton loginButton;
    private JLabel welcomelabel;
    private JLabel imageLabel;
    private java.util.List<String> accounts;

    public ATM() {
        accounts = new ArrayList<>();

        frame = new JFrame("ATM GUI");
        transferButton = new JButton("계좌 이체");
        depositButton = new JButton("입금");
        withdrawButton = new JButton("출금");
        displayAccountsButton = new JButton("계좌 조회");
        exitButton = new JButton("종료");
        loginButton = new JButton("로그인");
        welcomelabel = new JLabel("CNU BANK ATM");

        // 버튼 액션 등록
        transferButton.addActionListener(e -> transfer());
        depositButton.addActionListener(e -> deposit());
        withdrawButton.addActionListener(e -> withdraw());
        displayAccountsButton.addActionListener(e -> displayAccounts());
        exitButton.addActionListener(e -> System.exit(0));
        loginButton.addActionListener(e -> login());

        // 왼쪽 패널
        JPanel pL = new JPanel();
        pL.setLayout(new GridLayout(3, 1));
        pL.add(displayAccountsButton);
        pL.add(transferButton);
        pL.add(loginButton);

        // 오른쪽 패널
        JPanel pR = new JPanel();
        pR.setLayout(new GridLayout(3, 1));
        pR.add(depositButton);
        pR.add(withdrawButton);
        pR.add(exitButton);

        // 중앙 패널
        JPanel pC = new JPanel();
        ImageIcon icon = new ImageIcon("충남대로고.png");
        imageLabel = new JLabel(icon);
        pC.add(imageLabel);

        // 프레임 설정
        frame.setSize(500, 300);
        frame.setLayout(new BorderLayout());
        frame.add(welcomelabel, BorderLayout.NORTH);
        frame.add(pL, BorderLayout.WEST);
        frame.add(pR, BorderLayout.EAST);
        frame.add(pC, BorderLayout.CENTER);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    private void transfer() {
        System.out.println("계좌 이체 기능 실행");
    }

    private void deposit() {
        System.out.println("입금 기능 실행");
    }

    private void withdraw() {
        System.out.println("출금 기능 실행");
    }

    private void displayAccounts() {
        System.out.println("계좌 조회 기능 실행");
    }

    private void login() {
        System.out.println("로그인 기능 실행");
    }

    public static void main(String[] args) {
        new ATM();
    }
}
