import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;

public class SwingUI extends JFrame{

    private JTextArea chatArea;
    private JTextField messageField;
    private DefaultListModel<String> userListModel;
    private JList<String> userList;
    private PrintWriter out;
    private BufferedReader in;
    private Socket socket;
    private MulticastSocket multicastSocket;
    private static final int TCP_PORT = 12345;
    private static final int MULTICAST_PORT = 12346;
    private static final String MULTICAST_ADDRESS = "230.0.0.0";
    private String username;

    public SwingUI(){
        setTitle ("Chat Application");
        setSize(600,400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        //Prompt username
        username = JOptionPane.showInputDialog(this,"Enter username: ", "Username", JOptionPane.PLAIN_MESSAGE);
        if(username == null || username.trim().isEmpty()){
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
        bottomPanel.add(messageField, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        messageField.addActionListener(e -> sendMessage());

        setupNetwork();

        new Thread(this::receiveMessages).start();

    }
    private void setupNetwork(){
        try{
            socket = new Socket("localhost", TCP_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.println(username);
        } catch(IOException e){
            e.printStackTrace();
        }
    }
    private void sendMessage(){
        String message = messageField.getText().trim();
        if(!message.isEmpty()){
            out.println(username + ": " + message);
            messageField.setText("");
        }
    }
    private void updateUserList(String userListStr){
        String[] user = userListStr.split(",");
        SwingUtilities.invokeLater(() ->{
            userListModel.clear();
            for(String u : user){
                userListModel.addElement(u);
            }
        });
    }
    private void receiveMessages(){
        try{
            String message;
            while((message = in.readLine()) != null){
                if(message.startsWith("USERLIST:")){
                    updateUserList(message.substring(message.indexOf(":") + 1));
                }else{
                    chatArea.append(message + "\n");
                }
            }
        } catch(IOException e){
            e.printStackTrace();
        }
    }
    public static void main(String args[]){
        SwingUtilities.invokeLater(() -> new SwingUI().setVisible(true));
    }
}
