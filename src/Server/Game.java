package Server;

import java.io.*;
import java.util.*;

public class Game {
    private final PlayerHandler player1;
    private final PlayerHandler player2;
    private final GameProperties properties;
    private final QuestionDatabase questionDB;
    private final List<Round> rounds = new ArrayList<>();
    private int currentRoundIndex = 0;

    public Game(PlayerHandler player1, PlayerHandler player2, GameProperties properties, QuestionDatabase questionDB) {
        this.player1 = player1;
        this.player2 = player2;
        this.properties = properties;
        this.questionDB = questionDB;

        // Set the current game for both players immediately
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
            rounds.add(new Round(questionDB.getQuestionsForRound(properties.getQuestionsPerRound())));
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

        System.out.println("Starting round " + (currentRoundIndex + 1));
        System.out.println("Number of questions in round: " + roundQuestions.size());

        // Debug print questions
        for (int i = 0; i < roundQuestions.size(); i++) {
            Question q = roundQuestions.get(i);
            System.out.println("Question " + (i + 1) + ": " + q.getText());
            System.out.println("Options: " + q.getOptions());
        }

        Message roundStartMessage = new Message(MessageType.ROUND_START, roundQuestions);

        System.out.println("Sending round start message to " + player1.getUsername());
        player1.sendMessage(roundStartMessage);

        System.out.println("Sending round start message to " + player2.getUsername());
        player2.sendMessage(roundStartMessage);
    }

    public void handleMessage(PlayerHandler player, Message message) throws IOException {
        System.out.println("Handling message from " + player.getUsername() + ": " + message.getType());
        switch (message.getType()) {
            case ANSWER:
                handleAnswer(player, message);
                break;
            case ROUND_COMPLETE:
                handleRoundComplete(player);
                break;
        }
    }

    private void handleAnswer(PlayerHandler player, Message message) throws IOException {
        Round currentRound = rounds.get(currentRoundIndex);
        Answer answer = (Answer) message.getContent();
        currentRound.recordAnswer(player, answer);
        System.out.println("Recorded answer from " + player.getUsername() + " for question " + answer.getQuestionIndex());
    }

    private void handleRoundComplete(PlayerHandler player) throws IOException {
        Round currentRound = rounds.get(currentRoundIndex);
        System.out.println("Round complete signal from " + player.getUsername());
        if (currentRound.isComplete()) {
            System.out.println("Round " + currentRoundIndex + " is complete");
            sendRoundResults();
            if (currentRoundIndex < rounds.size() - 1) {
                currentRoundIndex++;
                startNextRound();
            } else {
                endGame();
            }
        }
    }

    private void sendRoundResults() throws IOException {
        Round round = rounds.get(currentRoundIndex);
        RoundResult result = round.getResult();
        Message resultMessage = new Message(MessageType.ROUND_RESULT, result);
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