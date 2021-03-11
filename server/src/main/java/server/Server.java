package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {
    private final int PORT = 8189;
    private ServerSocket server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    
    private List<ClientHandler> listOfClients;
    private AuthService authService;
    
    public Server() {
        listOfClients = new CopyOnWriteArrayList<>();
        authService = new SimpleAuthServise();
        
        try {
            server = new ServerSocket(PORT);
            System.out.println("Server started");
            
            while (true) {
                socket = server.accept();
                System.out.println("Client connected");
                System.out.println("client: " + socket.getRemoteSocketAddress());
                new ClientHandler(this, socket);
            }
            
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    public void broadcastMsg(ClientHandler sender, String msg) {
        String message = String.format("[ %s ]: %s", sender.getNickname(), msg);
        for (ClientHandler c : listOfClients) {
            c.sendMsg(message);
        }
    }
    
    public void subscribe(ClientHandler clientHandler) {
        listOfClients.add(clientHandler);
    }
    
    public void unsubscribe(ClientHandler clientHandler) {
        listOfClients.remove(clientHandler);
    }
    
    public AuthService getAuthService() {
        return authService;
    }
    
    public void broadcastMsg(String nickDestination, String nickname, String msg) {
        String message = String.format("[ %s ]: %s", nickname, msg);
        int indexOfDestination = -1;
        int indexOfMaster = -1;
        
        for (int i = 0; i < listOfClients.size(); i++) {
            if (listOfClients.get(i).getNickname().equals(nickname)) {
                indexOfMaster = i;
            }
            if (listOfClients.get(i).getNickname().equals(nickDestination)) {
                indexOfDestination = i;
            }
        }
        
        if (indexOfMaster < 0) {
            throw new RuntimeException("Error: sender does not found");
        }
        if (indexOfDestination < 0) {
            listOfClients.get(indexOfMaster).sendMsg("Error: nickname: [" + nickDestination + "] does not found");
        } else {
            listOfClients.get(indexOfMaster).sendMsg(message);
            listOfClients.get(indexOfDestination).sendMsg(message);
        }
        
    }
}
