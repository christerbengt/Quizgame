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
            rounds.add(new Round(new ArrayList<>(), null));
        }
    }

    private void sendGameStart() throws IOException {
        System.out.println("Sending game start message to players");
        Message gameStartMessage = new Message(MessageType.GAME_START, null);
        player1.sendMessage(gameStartMessage);
        player2.sendMessage(gameStartMessage);
    }


    private void startNextRound() throws IOException {
        Round round = rounds.get(currentRoundIndex);
        List<Question> roundQuestions = round.getQuestions();

        PlayerHandler categoryChooser;

        if (player1Turn) {
            categoryChooser = player1;
        } else {
            categoryChooser = player2;
        }



        System.out.println("Starting round " + (currentRoundIndex + 1) + " of " + rounds.size());
        System.out.println("Number of questions in round: " + roundQuestions.size());

        Message categoryChoiceMessage = new Message(MessageType.CATEGORY_SELECTED, null);
        categoryChooser.sendMessage(categoryChoiceMessage);

        player1Turn = !player1Turn;

    }

    private void handleCategorySelection(PlayerHandler player, Category selectedCategory) {

        if ((player1Turn && player.equals(player1)) || (!player1Turn && player.equals(player2))) {
            System.out.println("Category chosen by " + player.getUsername() + ": " + selectedCategory);

            List<Question> questions = questionDB.getQuestionsForRound(selectedCategory, properties.getQuestionsPerRound());
            rounds.get(currentRoundIndex).setQuestions(questions);
            try {
                startNextRoundWithQuestions(questions);
            } catch (IOException e) {
                System.err.println("Error starting round: " + e.getMessage());
                e.printStackTrace();
            }

            player1Turn = !player1Turn;

        } else {
            System.out.println("Unexpected category selection from player: " + player.getUsername());
        }
    }

    private void startNextRoundWithQuestions(List<Question> questions) throws IOException {
        System.out.println("Starting round with category-chosen questions");
        Message roundStartMessage = new Message(MessageType.ROUND_START, questions);

        player1.sendMessage(roundStartMessage);
        player2.sendMessage(roundStartMessage);
    }

    private void handleAnswer(PlayerHandler player, Message message) throws IOException {
        Round currentRound = rounds.get(currentRoundIndex);
        Answer answer = (Answer) message.getContent();
        currentRound.recordAnswer(player, answer);
        System.out.println("Recorded answer from " + player.getUsername() + " for question " + answer.getQuestionIndex());
    }

    private void handleRoundComplete(PlayerHandler player) throws IOException {
        Round currentRound = rounds.get(currentRoundIndex);
        System.out.println("Round complete signal from " + player.getUsername() +
                " for round " + (currentRoundIndex + 1));

        if (currentRound.isComplete()) {
            System.out.println("Both players completed round " + (currentRoundIndex + 1));

            // Send round results
            sendRoundResults();

            // Wait for a short delay before starting next round
            try {
                Thread.sleep(3000); // 3 second delay
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Check if there are more rounds
            if (currentRoundIndex < rounds.size() - 1) {
                currentRoundIndex++;
                System.out.println("Moving to round " + (currentRoundIndex + 1));
                startNextRound();
            } else {
                System.out.println("All rounds complete, ending game");
                try {
                    Thread.sleep(2000); // 2 second delay before ending
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                endGame();
            }
        } else {
            System.out.println("Waiting for other player to complete round " + (currentRoundIndex + 1));
        }
    }

    public void handleMessage(PlayerHandler player, Message message) throws IOException {
        System.out.println("Handling message from " + player.getUsername() + ": " + message.getType());
        switch (message.getType()) {
            case ANSWER -> handleAnswer(player, message);
            case ROUND_COMPLETE -> handleRoundComplete(player);
            default -> System.out.println("Unexpected message type: " + message.getType());
        }
    }

    private void sendRoundResults() throws IOException {
        Round round = rounds.get(currentRoundIndex);
        RoundResult result = round.getResult();
        Message resultMessage = new Message(MessageType.ROUND_RESULT, result);

        System.out.println("Sending round " + (currentRoundIndex + 1) + " results to players");
        player1.sendMessage(resultMessage);
        player2.sendMessage(resultMessage);
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

    private void endGame() throws IOException {
        System.out.println("Game ending");
        GameResult result = calculateGameResult();
        Message gameEndMessage = new Message(MessageType.GAME_END, result);
        player1.sendMessage(gameEndMessage);
        player2.sendMessage(gameEndMessage);
    }
}