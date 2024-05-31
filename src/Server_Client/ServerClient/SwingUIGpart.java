package Server_Client.ServerClient;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.HashSet;
import java.util.Set;

public class SwingUIGpart extends JFrame {
    private JTextArea chatArea;
    private JTextField messageField;
    private DefaultListModel<String> userListModel;
    private JList<String> userList;
    private MulticastSocket multicastSocket;
    private static final int MULTICAST_PORT = 12346;
    private static final String MULTICAST_ADDRESS = "230.0.0.0";
    private String username;
    private InetAddress group;
    private Set<String> users;

    public SwingUIGpart() {
        setTitle("Chat Application");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        users = new HashSet<>();

        // Prompt username
        username = JOptionPane.showInputDialog(this, "Enter username: ", "Username", JOptionPane.PLAIN_MESSAGE);
        if (username == null || username.trim().isEmpty()) {
            System.exit(0);
        }

        // Main Panel
        JPanel mainPanel = new JPanel(new BorderLayout());
        getContentPane().add(mainPanel);
        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        JScrollPane userListScrollPane = new JScrollPane(userList);
        userListScrollPane.setPreferredSize(new Dimension(150, 0));
        mainPanel.add(userListScrollPane, BorderLayout.WEST);

        // Chat Area
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane chatScrollPane = new JScrollPane(chatArea);
        mainPanel.add(chatScrollPane, BorderLayout.CENTER);

        // Message Input
        JPanel bottomPanel = new JPanel(new BorderLayout());
        messageField = new JTextField();
        JButton disconnectButton = new JButton("Disconnect");
        bottomPanel.add(disconnectButton, BorderLayout.EAST);
        bottomPanel.add(messageField, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        messageField.addActionListener(e -> sendMessage());
        disconnectButton.addActionListener(e -> dispose());

        setupMulticast();

        new Thread(this::receiveMulticastMessages).start();
        sendJoinNotification();
        requestUserList();
    }

    private void setupMulticast() {
        try {
            multicastSocket = new MulticastSocket(MULTICAST_PORT);
            group = InetAddress.getByName(MULTICAST_ADDRESS);
            multicastSocket.joinGroup(group);
            System.out.println("Joined multicast group. Waiting for messages...");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            String fullMessage = "MESSAGE:" + username + ": " + message;
            sendMulticastMessage(fullMessage);
            messageField.setText("");
        }
    }

    private void sendJoinNotification() {
        String joinMessage = "JOIN:" + username;
        sendMulticastMessage(joinMessage);
    }

    private void sendLeaveNotification() {
        String leaveMessage = "LEAVE:" + username;
        sendMulticastMessage(leaveMessage);
    }

    private void requestUserList() {
        String requestMessage = "REQUEST_USER_LIST";
        sendMulticastMessage(requestMessage);
    }

    private void sendMulticastMessage(String message) {
        try {
            DatagramPacket packet = new DatagramPacket(message.getBytes(), message.length(), group, MULTICAST_PORT);
            multicastSocket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void receiveMulticastMessages() {
        byte[] buffer = new byte[1024];
        while (true) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                multicastSocket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength());
                handleMessage(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleMessage(String message) {
        if (message.startsWith("JOIN:")) {
            String newUser = message.substring(5);
            if (users.add(newUser)) {
                SwingUtilities.invokeLater(() -> {
                    userListModel.addElement(newUser);
                    chatArea.append(newUser + " has joined the chat.\n");
                });
            }
            // Send the new user our username
            if (!newUser.equals(username)) {
                sendMulticastMessage("USER:" + username);
            }
        } else if (message.startsWith("LEAVE:")) {
            String leavingUser = message.substring(6);
            if (users.remove(leavingUser)) {
                SwingUtilities.invokeLater(() -> {
                    userListModel.removeElement(leavingUser);
                    chatArea.append(leavingUser + " has left the chat.\n");
                });
            }
        } else if (message.startsWith("MESSAGE:")) {
            String chatMessage = message.substring(8);
            SwingUtilities.invokeLater(() -> chatArea.append(chatMessage + "\n"));
        } else if (message.startsWith("REQUEST_USER_LIST")) {
            sendMulticastMessage("USER:" + username);
        } else if (message.startsWith("USER:")) {
            String user = message.substring(5);
            if (users.add(user)) {
                SwingUtilities.invokeLater(() -> userListModel.addElement(user));
            }
        }
    }

    @Override
    public void dispose() {
        sendLeaveNotification();
        super.dispose();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SwingUIGpart().setVisible(true));
    }
}
