package Server;

import Pojos.Message;
import Pojos.MessageType;

import java.io.*;
import java.util.*;

public class Game {
    private final PlayerHandler player1;
    private final PlayerHandler player2;
    private final GameProperties properties;
    private final QuestionDatabase questionDB;
    private final List<Round> rounds = new ArrayList<>();
    private int currentRoundIndex = 0;
    private boolean player1Turn = true;

    public Game(PlayerHandler player1, PlayerHandler player2, GameProperties properties, QuestionDatabase questionDB) {
        this.player1 = player1;
        this.player2 = player2;
        this.properties = properties;
        this.questionDB = questionDB;

        player1.setCurrentGame(this);
        player2.setCurrentGame(this);

        System.out.println("Created new game between " + player1.getUsername() + " and " + player2.getUsername());
    }

    public void start() {
        try {
            System.out.println("Starting game between " + player1.getUsername() + " and " + player2.getUsername());
            initializeGame();
            sendGameStart();
            startNextRound();
        } catch (IOException e) {
            System.err.println("Error starting game: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void initializeGame() {
        System.out.println("Initializing game with " + properties.getRoundCount() + " rounds");
        for (int i = 0; i < properties.getRoundCount(); i++) {
            rounds.add(new Round(new ArrayList<>(), Category.HISTORY));
        }
    }

    private void sendGameStart() throws IOException {
        System.out.println("Sending game start message to players");
        Message gameStartMessage = new Message(MessageType.GAME_START, null);
        player1.sendMessage(gameStartMessage);
        player2.sendMessage(gameStartMessage);
    }

    private void handlePlayerForfeit(PlayerHandler forfeitingPlayer) throws IOException {
        PlayerHandler winner = (forfeitingPlayer == player1) ? player2 : player1;
        Map<PlayerHandler, Integer> finalScores = new HashMap<>();
        finalScores.put(winner, 1);
        finalScores.put(forfeitingPlayer, 0);
        endGameWithResult(new GameResult(finalScores, winner)); // Changed method name
    }

    private void startNextRound() throws IOException {
        if (currentRoundIndex >= rounds.size()) {
            endGameWithResult(calculateGameResult());
            return;
        }

        PlayerHandler categoryChooser = player1Turn ? player1 : player2;
        List<Category> randomCategories = Category.randomCategories();

        Message categoryChoiceMessage = new Message(MessageType.CATEGORY_SELECTED, randomCategories);
        categoryChooser.sendMessage(categoryChoiceMessage);
    }


    public void handleCategorySelection(PlayerHandler player, Category selectedCategory) {
        List<Question> questions = questionDB.getQuestionsForRound(selectedCategory, properties.getQuestionsPerRound());

        Round currentRound = new Round(questions, selectedCategory);
        rounds.set(currentRoundIndex, currentRound);

        try {
            Message roundStartMessage = new Message(MessageType.ROUND_START, questions);
            player1.sendMessage(roundStartMessage);
            player2.sendMessage(roundStartMessage);
        } catch (IOException e) {
            System.err.println("Error starting round with questions: " + e.getMessage());
        }
    }


    private void handleAnswer(PlayerHandler player, Message message) throws IOException {
        Round currentRound = rounds.get(currentRoundIndex);
        Answer answer = (Answer) message.getContent();
        currentRound.recordAnswer(player, answer);
        System.out.println("Recorded answer from " + player.getUsername() + " for question " + answer.getQuestionIndex());
    }

    private void handleForfeit(PlayerHandler forfeitingPlayer) throws IOException {
        PlayerHandler winner = (forfeitingPlayer == player1) ? player2 : player1;
        Map<PlayerHandler, Integer> finalScores = new HashMap<>();
        finalScores.put(winner, 1);
        finalScores.put(forfeitingPlayer, 0);
        GameResult result = new GameResult(finalScores, winner);
        endGameWithResult(calculateGameResult());
    }

    public synchronized void handleRoundComplete(PlayerHandler player) throws IOException {
        Round currentRound = rounds.get(currentRoundIndex);
        System.out.println("Round complete signal from " + player.getUsername() +
                " for round " + (currentRoundIndex + 1));

        if (currentRound.isComplete()) {
            System.out.println("Both players completed round " + (currentRoundIndex + 1));
            sendRoundResults();

            currentRoundIndex++;
            if (currentRoundIndex < rounds.size()) {
                System.out.println("Starting next round after delay");
                new Thread(() -> {
                    try {
                        Thread.sleep(3000);
                        startNextRound();
                    } catch (InterruptedException | IOException e) {
                        e.printStackTrace();
                    }
                }).start();
            } else {
                System.out.println("All rounds complete, ending game");
                endGameWithResult(calculateGameResult());
            }
        } else {
            System.out.println("Waiting for other player to complete round");
        }
    }


    public void handleMessage(PlayerHandler player, Message message) throws IOException {
        System.out.println("Handling message from " + player.getUsername() + ": " + message.getType());
        switch (message.getType()) {
            case FORFEIT -> handlePlayerForfeit(player);
            case ROUND_COMPLETE -> handleRoundComplete(player);
            case CATEGORY_SELECTED -> {
                Category selectedCategory = (Category) message.getContent();
                handleCategorySelection(player, selectedCategory);
            }
            case ANSWER -> handleAnswer(player, message);
            default -> System.out.println("Unexpected message type: " + message.getType());
        }
    }

    private void sendRoundResults() throws IOException {
        Round round = rounds.get(currentRoundIndex);
        RoundResult result = round.getResult();

        System.out.println("Sending round " + (currentRoundIndex + 1) + " results to players");
        Message resultMessage = new Message(MessageType.ROUND_RESULT, result);

        player1.sendMessage(resultMessage);
        player2.sendMessage(resultMessage);

        player1Turn = !player1Turn;
    }

    private void calculateTotalScores(Map<PlayerHandler, Integer> totalScores) {
        for (Round round : rounds) {
            RoundResult roundResult = round.getResult();
            Map<PlayerHandler, Integer> roundScores = roundResult.getPlayerScores();

            for (Map.Entry<PlayerHandler, Integer> entry : roundScores.entrySet()) {
                totalScores.merge(entry.getKey(), entry.getValue(), Integer::sum);
            }
        }
    }

    private GameResult calculateGameResult() {
        Map<PlayerHandler, Integer> finalScores = new HashMap<>();
        finalScores.put(player1, 0);
        finalScores.put(player2, 0);

        calculateTotalScores(finalScores);

        PlayerHandler winner;
        int player1Score = finalScores.get(player1);
        int player2Score = finalScores.get(player2);

        if (player1Score > player2Score) {
            winner = player1;
        } else if (player2Score > player1Score) {
            winner = player2;
        } else {
            winner = null; // Tie
        }

        return new GameResult(finalScores, winner);
    }

    private void endGameWithResult(GameResult result) throws IOException {
        System.out.println("Game ending");
        Message gameEndMessage = new Message(MessageType.GAME_END, result);
        player1.sendMessage(gameEndMessage);
        player2.sendMessage(gameEndMessage);
    }
}