package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import commands.Command;

public class ClientHandler {
    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    
    private String nickname;
    
    public ClientHandler(Server server, Socket socket) {
        try {
            this.server = server;
            this.socket = socket;
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            
            new Thread(() -> {
                try {
                    // case for authentication
                    while (true) {
                        String str = in.readUTF();
                        
                        if (str.equals(Command.END)) {
                            out.writeUTF(Command.END);
                            throw new RuntimeException("the client wants to disconnect ");
                        }
                        if (str.startsWith(Command.AUTH)) {
                            String[] token = str.split("\\s", 3);
                            if (token.length < 3) {
                                continue;
                            }
                            String newNick = server.getAuthService().getNicknameByLoginAndPassword(token[1], token[2]);
                            
                            if (newNick != null) {
                                nickname = newNick;
                                sendMsg(Command.AUTH_OK + " " + nickname);
                                server.subscribe(this);
                                System.out.println(
                                    "client: " + socket.getRemoteSocketAddress() + " connected with nick: " + nickname);
                                break;
                            } else {
                                sendMsg("Wrong login / password");
                            }
                            
                        }
                    }
                    // working case
                    while (true) {
                        String message = in.readUTF();
                        
                        if (message.equals(Command.END)) {
                            out.writeUTF(Command.END);
                            break;
                        }
                        
                        if (!message.startsWith(Command.MARKER_PRIVATE_CHANEL)) {
                            server.broadcastMsg(this, message);
                        } else {
                            
                            String[] array = message.split(" ", 3);
                            if (array.length != 3) {
                                if (array.length == 2) {
                                    server.broadcastMsg(array[1], nickname, Command.WRONG_FORMAT_MESSAGE);
                                } else {
                                    server.broadcastMsg("", nickname, Command.WRONG_FORMAT_MESSAGE);
                                }
                                
                            } else {
                                
                                String nickDestination = array[1];
                                String privateMessage = array[2];
                                
                                server.broadcastMsg(nickDestination, nickname, privateMessage);
                            }
                        }
                        
                    }
                } catch (RuntimeException e) {
                    System.out.println(e.getMessage());
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    server.unsubscribe(this);
                    System.out.println("Client disconnected: " + nickname);
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void sendMsg(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public String getNickname() {
        return nickname;
    }
}
