package Server;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.*;

public class Server {
    private static final int PORT = 55555;
    private final ConcurrentHashMap<String, PlayerHandler> players = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<PlayerHandler> waitingPlayers = new ConcurrentLinkedQueue<>();
    private final Set<Game> activeGames = Collections.newSetFromMap(new ConcurrentHashMap<>()); // Added this line
    private final GameProperties gameProperties;
    private final QuestionDatabase questionDB;

    public Server() {
        this.gameProperties = new GameProperties();
        this.questionDB = new QuestionDatabase();
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT);
            System.out.println("Waiting for players to connect...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected from: " +
                        clientSocket.getInetAddress().getHostAddress());
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
        System.out.println("Current waiting players: " + waitingPlayers.size());
        matchPlayers();
    }

    private void matchPlayers() {
        while (waitingPlayers.size() >= 2) {
            PlayerHandler player1 = waitingPlayers.poll();
            PlayerHandler player2 = waitingPlayers.poll();

            if (player1 != null && player2 != null) {
                System.out.println("Matching players for a new game: " +
                        player1.getUsername() + " vs " + player2.getUsername());
                Game game = new Game(player1, player2, gameProperties, questionDB);
                activeGames.add(game);
                System.out.println("Active games: " + activeGames.size());
                new Thread(() -> {
                    game.start();
                    activeGames.remove(game);
                    System.out.println("Game completed. Active games: " + activeGames.size());
                }).start();
            }
        }
    }

    public void removePlayer(PlayerHandler player) {
        if (player.getUsername() != null) {
            players.remove(player.getUsername());
            waitingPlayers.remove(player);
            System.out.println("Player removed: " + player.getUsername());
            System.out.println("Current players: " + players.size());
            System.out.println("Waiting players: " + waitingPlayers.size());
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.start();
    }
}