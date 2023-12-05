import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GoogleSearchApp {

    private JFrame frame;
    private JButton myPageButton;
    private JButton logoutButton;  // 로그아웃 버튼 추가
    private JPanel resultPanel;
    private List<String> favoriteHospitals;  // 찜한 병원을 저장할 리스트
    private String currentUser;  // 현재 로그인한 사용자

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> createAndShowGUI());
    }

    private static void createAndShowGUI() {
        GoogleSearchApp app = new GoogleSearchApp();
        app.initUI();
    }

    private void initUI() {
        frame = new JFrame("전북_동물병원_조회");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 400);

        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 20));

        JTextField searchField = new JTextField(20);
        JButton searchButton = new JButton("검색");

        resultPanel = new JPanel();
        resultPanel.setLayout(new BoxLayout(resultPanel, BoxLayout.Y_AXIS));

        JScrollPane scrollPane = new JScrollPane(resultPanel);

        searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                performSearch(searchField.getText());
            }
        });

        panel.add(searchField);
        panel.add(searchButton);
        frame.getContentPane().add(BorderLayout.NORTH, panel);
        frame.getContentPane().add(BorderLayout.CENTER, scrollPane);

        JButton signupLoginButton = new JButton("로그인/회원가입");
        signupLoginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isLoggedIn()) {
                    showMyPage();
                } else {
                    showLoginDialog();
                }
            }
        });
        signupLoginButton.setPreferredSize(new Dimension(150, 30));
        panel.add(signupLoginButton);

        myPageButton = new JButton("MyPage");
        myPageButton.setEnabled(false);
        myPageButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showMyPage();
            }
        });
        panel.add(myPageButton);

        // 로그아웃 버튼 추가
        logoutButton = new JButton("로그아웃");
        logoutButton.setEnabled(false);  // 로그인되지 않은 상태에서 비활성화
        logoutButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                performLogout();
            }
        });
        panel.add(logoutButton);

        favoriteHospitals = new ArrayList<>();  // 찜한 병원을 저장할 리스트 초기화

        frame.setVisible(true);
    }

    private void performSearch(String query) {
        System.out.println("검색: " + query);

        try (Connection connection = DriverManager.getConnection("jdbc:oracle:thin:@localhost:1521:XE", "system", "cd43230830");
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT hospital_name, hospital_address, contact_hospital FROM hospital WHERE hospital_name LIKE ? OR hospital_address LIKE ?")) {

            preparedStatement.setString(1, "%" + query + "%");
            preparedStatement.setString(2, "%" + query + "%");

            ResultSet resultSet = preparedStatement.executeQuery();

            resultPanel.removeAll();  // 결과 패널 초기화

            while (resultSet.next()) {
                String hospitalName = resultSet.getString("hospital_name");
                String address = resultSet.getString("hospital_address");
                String phone = resultSet.getString("contact_hospital");

                JPanel hospitalPanel = new JPanel();  // 각 병원 정보와 찜 체크박스를 담을 패널
                hospitalPanel.setLayout(new BoxLayout(hospitalPanel, BoxLayout.Y_AXIS));

                JCheckBox checkBox = new JCheckBox("찜");
                checkBox.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        handleCheckBoxSelection(hospitalName);
                    }
                });

                hospitalPanel.add(new JLabel("병원명: " + hospitalName));
                hospitalPanel.add(new JLabel("주소: " + address));
                hospitalPanel.add(new JLabel("전화번호: " + phone));
                hospitalPanel.add(checkBox);

                resultPanel.add(hospitalPanel);
            }

            if (resultPanel.getComponentCount() == 0) {
                resultPanel.add(new JLabel("병원이 없습니다."));
            }

            resultPanel.revalidate();
            resultPanel.repaint();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void handleCheckBoxSelection(String hospitalName) {
        if (isLoggedIn()) {
            if (favoriteHospitals.contains(hospitalName)) {
                favoriteHospitals.remove(hospitalName);
            } else {
                favoriteHospitals.add(hospitalName);
            }
        } else {
            // 로그인되어 있지 않으면 로그인 창을 띄워줌
            showLoginWarning();
        }
    }

    private void showLoginWarning() {
        JOptionPane.showMessageDialog(frame, "로그인 후에 찜 기능을 사용할 수 있습니다.");
        showLoginDialog();
    }

    private void showLoginDialog() {
        LoginDialog loginDialog = new LoginDialog(this);
        loginDialog.setVisible(true);
    }

    private boolean isLoggedIn() {
        return currentUser != null;
    }

    private void showMyPage() {
        StringBuilder message = new StringBuilder("찜한 병원 목록:\n");
        for (String hospital : favoriteHospitals) {
            message.append(hospital).append("\n");
        }

        JOptionPane.showMessageDialog(frame, message.toString());
    }

    // 로그아웃 메소드 추가
    private void performLogout() {
        currentUser = null;
        myPageButton.setEnabled(false);
        logoutButton.setEnabled(false);  // 로그인되지 않은 상태에서 비활성화
        JOptionPane.showMessageDialog(frame, "로그아웃 되었습니다.");
    }

    public JButton getMyPageButton() {
        return myPageButton;
    }

    public JFrame getFrame() {
        return frame;
    }

    class LoginDialog extends JDialog {

        private JTextField usernameField;
        private JPasswordField passwordField;

        public LoginDialog(GoogleSearchApp parent) {
            super(parent.getFrame(), "로그인", true);
            setLayout(new FlowLayout());

            JLabel usernameLabel = new JLabel("사용자명:");
            JLabel passwordLabel = new JLabel("비밀번호:");

            usernameField = new JTextField(15);
            passwordField = new JPasswordField(15);

            JButton loginButton = new JButton("로그인");
            loginButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    performLogin();
                }
            });

            JButton signupButton = new JButton("회원가입");
            signupButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    performSignup();
                }
            });

            add(usernameLabel);
            add(usernameField);
            add(passwordLabel);
            add(passwordField);
            add(loginButton);
            add(signupButton);

            pack();
            setLocationRelativeTo(parent.getFrame());
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        }

        private void performSignup() {
            String enteredUsername = usernameField.getText();
            String enteredPassword = new String(passwordField.getPassword());

            // 회원가입 로직을 작성
            if (signup(enteredUsername, enteredPassword)) {
                JOptionPane.showMessageDialog(this, "회원가입 성공!");
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "회원가입 실패. 이미 존재하는 사용자명일 수 있습니다.");
            }
        }

        private boolean signup(String username, String password) {
            try (Connection connection = DriverManager.getConnection("jdbc:oracle:thin:@localhost:1521:XE", "system", "cd43230830");
                 PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO member (member_id, password) VALUES (?, ?)")) {

                preparedStatement.setString(1, username);
                preparedStatement.setString(2, password);

                int rowsAffected = preparedStatement.executeUpdate();

                return rowsAffected > 0;

            } catch (SQLException e) {
                e.printStackTrace();
            }
            return false;
        }

        private void performLogin() {
            String enteredUsername = usernameField.getText();
            String enteredPassword = new String(passwordField.getPassword());

            // 데이터베이스에서 입력된 사용자명과 비밀번호 확인
            if (checkLogin(enteredUsername, enteredPassword)) {
                currentUser = enteredUsername;
                myPageButton.setEnabled(true);
                logoutButton.setEnabled(true);  // 로그인된 상태에서 활성화
                JOptionPane.showMessageDialog(this, "로그인 성공!");
                dispose();  // 로그인 창 닫기
            } else {
                JOptionPane.showMessageDialog(this, "로그인 실패. 사용자명 또는 비밀번호를 확인하세요.");
            }
        }

        private boolean checkLogin(String enteredUsername, String enteredPassword) {
            try (Connection connection = DriverManager.getConnection("jdbc:oracle:thin:@localhost:1521:XE", "system", "cd43230830");
                 PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM member WHERE member_id = ? AND password = ?")) {

                preparedStatement.setString(1, enteredUsername);
                preparedStatement.setString(2, enteredPassword);

                ResultSet resultSet = preparedStatement.executeQuery();

                return resultSet.next();

            } catch (SQLException e) {
                e.printStackTrace();
            }
            return false;
        }
    }
}
