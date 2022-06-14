package geekbrains.simple_chat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server {
    private static final int PORT = 8189;

    private List<ClientHandler> clients;

    private AuthService authService;
    public AuthService getAuthService() {
        return authService;
    }

    public Server() {
        try(ServerSocket server = new ServerSocket(PORT)) {
            System.out.println("Server started");
            clients = new ArrayList<>();
            authService = new BaseAuthService();
            authService.start();
            while(true) {
                System.out.println("Waiting for connection...");
                Socket socket = server.accept();
                System.out.println("Client connected");
                new ClientHandler(socket, this);
            }
        } catch (IOException e) {
            System.out.println("Server error");
        } finally {
            if(authService != null) {
                authService.stop();
            }
        }
    }

    public synchronized boolean isNickBusy(String nick) {
        for(ClientHandler ch : clients) {
            if(ch.getName().equals(nick)) {
                return true;
            }
        }
        return false;
    }

    public synchronized void broadcastMessage(String message, ClientHandler from) {
        for (int i = 0; i < clients.size(); i++) {
            if(!clients.get(i).getName().equals(from.getName())) {
                clients.get(i).sendMessage(message);
            }
        }
    }

    public synchronized void sendToClient(String message, String to, String from) {
        for (int i = 0; i < clients.size(); i++) {
            if(clients.get(i).getName().equals(to)) {
                clients.get(i).sendMessage("[от " + from + "] " + message);
            }
        }
    }

    public String showActiveClients() {
        String clientsList = "";
        for(ClientHandler clientHandler : clients) {
            clientsList = clientsList + clientHandler.getName() + " ";
        }
        return clientsList;
    }

    public synchronized void subscribe(ClientHandler clientHandler) {
        clients.add(clientHandler);
    }
    public synchronized void unsubscribe(ClientHandler clientHandler) {clients.remove(clientHandler);}
}
