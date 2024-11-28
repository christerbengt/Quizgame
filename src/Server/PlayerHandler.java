package Server;

import Pojos.Message;
import Pojos.MessageType;

import java.io.*;
import java.net.*;

public class PlayerHandler implements Runnable, Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    // Transient means the variable is not serialized
    private transient Socket socket;
    private transient Server server;
    private transient ObjectOutputStream out;
    private transient ObjectInputStream in;
    private String username;  // Removed final keyword
    private transient Game currentGame;
    private volatile boolean running = true;

    public PlayerHandler(Socket socket, Server server) throws IOException {
        this.socket = socket;
        this.server = server;
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.out.flush();
        this.in = new ObjectInputStream(socket.getInputStream());
    }

    @Override
    public void run() {
        try {
            handleLogin();
            handleGameMessages();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error in PlayerHandler for " + username + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanup();
        }
    }

    private void handleLogin() throws IOException, ClassNotFoundException {
        Message loginMessage = (Message) in.readObject();
        if (loginMessage.getType() == MessageType.LOGIN) {
            this.username = (String) loginMessage.getContent();
            System.out.println("Player logged in: " + username);
            server.registerPlayer(this.username, this);
        }
    }

    private void handleGameMessages() throws IOException, ClassNotFoundException {
        while (running && !socket.isClosed()) {
            try {
                Message message = (Message) in.readObject();
                System.out.println("Received message from " + username + ": " + message.getType());

                if (currentGame != null) {
                    currentGame.handleMessage(this, message);
                } else {
                    System.out.println("No current game for player: " + username);
                }
            } catch (EOFException e) {
                System.out.println("Client disconnected: " + username);
                break;
            }
        }
    }

    public synchronized void sendMessage(Message message) throws IOException {
        try {
            System.out.println("Sending message type " + message.getType() + " to " + username);
            out.writeObject(message);
            out.reset();
            out.flush();
            System.out.println("Message sent successfully to " + username);
        } catch (IOException e) {
            System.err.println("Error sending message to " + username + ": " + e.getMessage());
            throw e;
        }
    }

    public void setCurrentGame(Game game) {
        this.currentGame = game;
        System.out.println("Set current game for " + username);
    }

    public String getUsername() {
        return username;
    }

    private void cleanup() {
        running = false;
        try {
            System.out.println("Cleaning up resources for " + username);
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.err.println("Error during cleanup for " + username + ": " + e.getMessage());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlayerHandler that = (PlayerHandler) o;
        return username != null && username.equals(that.username);
    }

    @Override
    public int hashCode() {
        return username != null ? username.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Player: " + username;
    }

    // Custom serialization
    @Serial
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
    }
}