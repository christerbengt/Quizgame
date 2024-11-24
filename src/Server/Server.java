package Server;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class Server {
    private static final int PORT = 12345;
    private final ConcurrentHashMap<String, PlayerHandler> players = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<PlayerHandler> waitingPlayers = new ConcurrentLinkedQueue<>();
    private final GameProperties gameProperties;
    private final QuestionDatabase questionDB;

    public Server() {
        this.gameProperties = new GameProperties();
        this.questionDB = new QuestionDatabase();
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress().getHostAddress());
                PlayerHandler playerHandler = new PlayerHandler(clientSocket, this);
                new Thread(playerHandler).start();
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void registerPlayer(String username, PlayerHandler handler) {
        System.out.println("Registering player: " + username);
        players.put(username, handler);
        waitingPlayers.offer(handler);
        System.out.println("Waiting players: " + waitingPlayers.size());
        matchPlayers();
    }

    private void matchPlayers() {
        while (waitingPlayers.size() >= 2) {
            PlayerHandler player1 = waitingPlayers.poll();
            PlayerHandler player2 = waitingPlayers.poll();

            if (player1 != null && player2 != null) {
                System.out.println("Matching players: " + player1.getUsername() + " vs " + player2.getUsername());
                Game game = new Game(player1, player2, gameProperties, questionDB);
                player1.setCurrentGame(game);
                player2.setCurrentGame(game);
                new Thread(() -> game.start()).start();
            }
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.start();
    }
}