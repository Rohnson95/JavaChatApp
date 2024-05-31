package Server_Client;
import java.io.*;
import java.net.*;
import java.util.*;
public class ChatServer {

    private static final int PORT = 12345;
    private static Set<PrintWriter> clientWriters = new HashSet<>();
    private static Map<Socket, String>userNames = new HashMap<>();

    public static void main(String[] args){
        System.out.println("Chatserver started...");
        try(ServerSocket serverSocket = new ServerSocket(PORT)){
            while(true){
                new ClientHandler(serverSocket.accept()).start();
            }

        } catch(IOException e){
            e.printStackTrace();
        }
}
    private static class ClientHandler extends Thread {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String username;


        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                synchronized (clientWriters) {
                    clientWriters.add(out);
                }

                //Get usernames
                username = in.readLine();
                synchronized (userNames) {
                    userNames.put(socket, username);
                }
                //UPDATE MEMBERS OF CHAT
                broadcast("SERVER: " + username + "has joined the chat");
                updateClientUserLists();


                String message;
                while((message = in.readLine()) != null) {
                    broadcast(message);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try{
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                synchronized (clientWriters) {
                    clientWriters.remove(out);
                }
                synchronized (userNames) {
                    if(username != null) {
                        userNames.remove(socket);
                        broadcast("SERVER: " + username + " has left the chat");
                        updateClientUserLists();
                    }
                }
            }
        }
        private void broadcast(String message){
            synchronized (clientWriters) {
                for(PrintWriter writer : clientWriters){
                    writer.println(message);
                }
            }
        }
        private void updateClientUserLists(){
            synchronized (clientWriters) {
                for(PrintWriter writer : clientWriters){
                    writer.println("USERLIST: " + String.join(",", userNames.values()));
                }
            }
        }
    }
}
