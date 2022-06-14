package geekbrains.simple_chat;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler {
    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String name;
    private Thread clientHandlerThread;

    public ClientHandler(Socket socket, Server server) {
        try {
            this.socket = socket;
            this.server = server;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
            clientHandlerThread = new Thread(() -> {
                try {
                    authentication();
                    readMessages();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    closeConnection();
                }
            });
            clientHandlerThread.start();
        } catch (IOException e) {
            System.out.println("Client handler creation failed");
        }
    }

    public void authentication() throws IOException {
        while(true) {
            String income = in.readUTF();
            System.out.println(income);
            if(income.startsWith("/auth")) {
                String[] parts = income.split("\\s");
                String nick = server.getAuthService().getNickByLoginPass(parts[1], parts[2]);
                System.out.println(nick);
                if(nick != null) {
                    if(!server.isNickBusy(nick)) {
                        sendMessage("/authok " + nick);
                        sendMessage("/shl " + server.showActiveClients());

                        name = nick;
                        server.broadcastMessage(name + " зашел в чат", this);
                        server.subscribe(this);
                        return;
                    }else {
                        sendMessage("Учетная запись уже используется");
                    }
                }else {
                    sendMessage("Неверные логин/пароль");
                }
            }
        }
    }

    public void readMessages() throws IOException {
        while(true) {
            String income = in.readUTF();
            System.out.println("Received: " + income);
            if(income.startsWith("/w\sОтправить всем")) {
                String message = income.substring(18);
                server.broadcastMessage("[от " + this.name + "] " + message, this);
            }

            if(income.startsWith("/w\s")) {
                String[] array = income.split("\\s");
                String toClient = array[1];
                String message = income.substring(4 + toClient.length());
                server.sendToClient(message, toClient, this.getName());
            }
            if(income.equals("/end")) {
                break;
            }
        }
    }

    public void sendMessage(String message) {
        try {
            out.writeUTF(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void closeConnection() {
        server.unsubscribe(this);
        server.broadcastMessage(this.name + " вышел из чата", this);
        server.broadcastMessage("/shl2 " + server.showActiveClients(), this);
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
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getName() {
        return name;
    }
}
