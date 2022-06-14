package geekbrains.simple_chat;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class Client extends JFrame {
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 8189;

    private JTextArea chatArea;
    private JTextField msgInputField;
    private JList<String> users;

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private DefaultListModel<String> listModel;
    private boolean isAuthorized;
    private String nick;
    private String selectedUser;

    public Client() {
        try {
            openConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }
        prepareGUI();
        showAuthDialog();
    }

    public void openConnection() throws IOException {
        socket = new Socket(HOST, PORT);
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
        setAuthorized(false);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while(true) {
                        String income = in.readUTF();
                        if(income.startsWith("/authok")) {
                            chatArea.append("Вы успешно авторизовались\n");
                            String[] array = income.split("\\s");
                            nick = array[1];
                            setAuthorized(true);
                            break;
                        }
                        chatArea.append(income + "\n");
                    }
                    while (true) {
                        String income = in.readUTF();
                        if(income.startsWith("/shl\s")&&income.length() > 5) {
                            String clientsList = income.substring(5);
                            String[] array = clientsList.split("\\s");
                            for (int i = 0; i < array.length; i++) {
                                listModel.addElement("  " + array[i]);
                            }
                        }
                        break;
                    }
                    while(true) {
                        String income = in.readUTF();
                        if (income.endsWith(" зашел в чат")) {
                            String contact = income.substring(0, income.length() - " зашел в чат".length());
                            listModel.addElement("  " + contact);
                        }
                        if(income.endsWith("вышел из чата")) {
                            listModel.clear();
                            listModel.addElement("  Отправить всем  ");
                            // остановился вот здесь ниже цикл и отрисовать новый список
                        }
                        if(income.startsWith("/shl2")&&income.length() > 6) {
                            String clientsList = income.substring(6);
                            String[] array = clientsList.split("\\s");
                            for (int i = 0; i < array.length; i++) {
                                if(!array[i].equals(nick)) {
                                    listModel.addElement("  " + array[i]);
                                }
                            }
                        }

                        if(income.equalsIgnoreCase("/end")) {
                            break;
                        }

                        if(!income.startsWith("/shl2")) {
                            chatArea.append(income);
                            chatArea.append("\n");
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void setAuthorized(boolean bool) {
        this.isAuthorized = bool;
    }

    public void closeConnection() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage() {
        if(!msgInputField.getText().trim().isEmpty()) {
            try {
                String outcome = msgInputField.getText();
                msgInputField.setText("");

                if(selectedUser == null && outcome.startsWith("/w\s")) {
                    String[] array = outcome.split("\\s");
                    String toClient = array[1];
                    String message = outcome.substring(4 + toClient.length());
                    chatArea.append("[Вы] " + message);
                    chatArea.append("\n");
                    out.writeUTF(outcome);
                }else if(selectedUser == null) {
                    JOptionPane.showMessageDialog(null, "Контакт не выбран. Выберите получателя");
                } else {
                    //тестовое добавление
                    chatArea.append("[Вы:] " + outcome);
                    out.writeUTF("/w " + selectedUser + " " + outcome);
                    chatArea.append("\n");
                }
                msgInputField.grabFocus();
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "Ошибка отправки сообщения");
            }
        }
    }

    public void prepareGUI() {
        setSize(525, 500);
        setLocationRelativeTo(null);
        setTitle("Корпоративный чат");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        add(new JScrollPane(chatArea), BorderLayout.CENTER);

        listModel = new DefaultListModel<>();
        listModel.addElement("  Отправить всем  ");
        users = new JList<>(listModel);
        add(new JScrollPane(users), BorderLayout.EAST);
        users.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                selectedUser = (String.valueOf(((JList<?>)e.getSource()).getSelectedValue())).trim();
            }
        });

        JPanel bottomPanel = new JPanel(new BorderLayout());
        JButton sendButton = new JButton("Отправить");
        bottomPanel.add(sendButton, BorderLayout.EAST);
        msgInputField = new JTextField();
        bottomPanel.add(msgInputField, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                if (isAuthorized) {
                    sendMessage();
                } else {
                    showAuthorizationRequest();
                }
            }
        });

        msgInputField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                if (isAuthorized) {
                    sendMessage();
                } else {
                    showAuthorizationRequest();
                }
            }
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                try {
                    out.writeUTF("/end");
                    closeConnection();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });

        setVisible(true);
    }

    private void showAuthorizationRequest() {
        int result = JOptionPane.showConfirmDialog(null,"Вы не авторизованы. Хотите авторизоваться?", "Error", JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE);
        if(result == 0) {
            showAuthDialog();
        }
    }

    private class LoginDialog extends JDialog {
        private JTextField tfLogin;
        private JTextField pfPassword;
        private JLabel lbLogin;
        private JLabel lbPassword;
        private JButton btnLogin;
        private JButton btnCancel;

        public LoginDialog(Client client) {
            super(client, "Авторизация", true);
            setResizable(false);
            setLocationRelativeTo(client);
            JPanel panel = new JPanel(new GridBagLayout());
            GridBagConstraints cs = new GridBagConstraints();
            cs.fill = GridBagConstraints.HORIZONTAL;

            lbLogin = new JLabel("Логин: ");
            cs.gridx = 0;
            cs.gridy = 0;
            cs.gridwidth = 1;
            panel.add(lbLogin, cs);

            tfLogin = new JTextField(20);
            cs.gridx = 1;
            cs.gridy = 0;
            cs.gridwidth = 2;
            panel.add(tfLogin, cs);

            lbPassword = new JLabel("Пароль: ");
            cs.gridx = 0;
            cs.gridy = 1;
            cs.gridwidth = 1;
            panel.add(lbPassword, cs);

            pfPassword = new JTextField(20);
            cs.gridx = 1;
            cs.gridy = 1;
            cs.gridwidth = 2;
            panel.add(pfPassword, cs);

            JPanel btmPanel = new JPanel();
            btnLogin = new JButton("Авторизоваться");
            btmPanel.add(btnLogin);
            btnCancel = new JButton("Отмена");
            btmPanel.add(btnCancel);

            getContentPane().add(panel, BorderLayout.CENTER);
            getContentPane().add(btmPanel, BorderLayout.PAGE_END);

            btnLogin.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        out.writeUTF("/auth " + tfLogin.getText() + " " + pfPassword.getText());
                        dispose();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            });

            btnCancel.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    dispose();
                }
            });
        }
    }

    private void showAuthDialog() {
        LoginDialog ld = new LoginDialog(this);
        ld.setSize(320, 135);
        ld.setLocationRelativeTo(null);
        ld.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new Client();
            }
        });
    }
}
