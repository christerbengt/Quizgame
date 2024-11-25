package Client;

import Server.*;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;

public class QuizClient {
    private final String SERVER_ADDRESS = "localhost";
    private final int SERVER_PORT = 12345;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Socket socket;
    private JFrame frame;
    private JPanel mainPanel;
    private JPanel questionPanel;
    private List<JButton> answerButtons;
    private JLabel questionLabel;
    private JLabel scoreLabel;
    private JLabel timerLabel;
    private String username;
    private List<Question> currentQuestions;
    private int currentQuestionIndex = 0;
    private javax.swing.Timer questionTimer;
    private final int TIMER_DELAY = 1000;
    private int timeLeft;
    private int currentRound = 1;

    public QuizClient() {
        initializeConnection();
        createGUI();
        startMessageListener();
    }

    private void initializeConnection() {
        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Cannot connect to server!");
            System.exit(1);
        }
    }

    private void createGUI() {
        frame = new JFrame("Quiz Game");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);

        mainPanel = new JPanel(new BorderLayout());
        createLoginPanel();
        frame.add(mainPanel);
        frame.setVisible(true);
    }

    private void createLoginPanel() {
        JPanel loginPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        JLabel usernameLabel = new JLabel("Enter your username:");
        JTextField usernameField = new JTextField(20);
        JButton loginButton = new JButton("Start Game");

        loginButton.addActionListener(e -> {
            String username = usernameField.getText().trim();
            if (!username.isEmpty()) {
                this.username = username;
                try {
                    sendMessage(new Message(MessageType.LOGIN, username));
                    showWaitingScreen();
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(frame, "Error connecting to server");
                }
            }
        });

        gbc.gridx = 0;
        gbc.gridy = 0;
        loginPanel.add(usernameLabel, gbc);
        gbc.gridy = 1;
        loginPanel.add(usernameField, gbc);
        gbc.gridy = 2;
        loginPanel.add(loginButton, gbc);

        mainPanel.add(loginPanel, BorderLayout.CENTER);
    }

    private void showWaitingScreen() {
        mainPanel.removeAll();
        JLabel waitingLabel = new JLabel("Waiting for opponent...", SwingConstants.CENTER);
        waitingLabel.setFont(new Font("Arial", Font.BOLD, 20));
        mainPanel.add(waitingLabel, BorderLayout.CENTER);
        mainPanel.revalidate();
        mainPanel.repaint();
    }

    private void createQuestionPanel() {
        questionPanel = new JPanel(new BorderLayout(10, 10));
        questionPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Top panel for round information and timer
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 5));
        JLabel roundLabel = new JLabel("Round " + currentRound + " of 3", SwingConstants.CENTER);
        roundLabel.setFont(new Font("Arial", Font.BOLD, 16));
        timerLabel = new JLabel("Time: 30");
        timerLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        scoreLabel = new JLabel("Score: 0");
        scoreLabel.setFont(new Font("Arial", Font.PLAIN, 14));

        topPanel.add(roundLabel);
        topPanel.add(timerLabel);
        topPanel.add(scoreLabel);

        // Question display
        questionLabel = new JLabel();
        questionLabel.setHorizontalAlignment(JLabel.CENTER);
        questionLabel.setFont(new Font("Arial", Font.BOLD, 16));

        // Answer buttons
        JPanel buttonPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        answerButtons = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            JButton button = new JButton();
            button.setFont(new Font("Arial", Font.PLAIN, 14));
            final int index = i;
            button.addActionListener(e -> handleAnswer(index));
            answerButtons.add(button);
            buttonPanel.add(button);
        }

        questionPanel.add(topPanel, BorderLayout.NORTH);
        questionPanel.add(questionLabel, BorderLayout.CENTER);
        questionPanel.add(buttonPanel, BorderLayout.SOUTH);
    }

    private void startMessageListener() {
        new Thread(() -> {
            try {
                while (true) {
                    Message message = (Message) in.readObject();
                    handleServerMessage(message);
                }
            } catch (IOException | ClassNotFoundException e) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(frame, "Lost connection to server: " + e.getMessage());
                    System.exit(1);
                });
            }
        }).start();
    }

    private void handleServerMessage(Message message) {
        SwingUtilities.invokeLater(() -> {
            try {
                switch (message.getType()) {
                    case GAME_START -> {
                        System.out.println("Game starting");
                        currentRound = 1;
                        mainPanel.removeAll();
                        createQuestionPanel();
                        mainPanel.add(questionPanel);
                        mainPanel.revalidate();
                        mainPanel.repaint();
                    }
                    case ROUND_START -> {
                        System.out.println("Round " + currentRound + " starting");
                        currentQuestions = (List<Question>) message.getContent();
                        currentQuestionIndex = 0;
                        displayQuestion();
                    }
                    case ROUND_RESULT -> {
                        handleRoundResult((RoundResult) message.getContent());
                        currentRound++;
                    }
                    case GAME_END -> handleGameEnd((GameResult) message.getContent());
                }
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(frame, "Error processing server message: " + e.getMessage());
            }
        });
    }

    private void displayQuestion() {
        if (currentQuestionIndex < currentQuestions.size()) {
            Question question = currentQuestions.get(currentQuestionIndex);

            // Update question number display
            questionLabel.setText("<html><div style='text-align: center'>Question " +
                    (currentQuestionIndex + 1) + " of " + currentQuestions.size() +
                    "<br><br>" + question.getText() + "</div></html>");

            List<String> options = question.getOptions();
            for (int i = 0; i < options.size(); i++) {
                JButton button = answerButtons.get(i);
                button.setText(options.get(i));
                button.setEnabled(true);
                button.setBackground(null);
            }
            startTimer();
        }
    }

    private void startTimer() {
        timeLeft = 30;
        if (questionTimer != null) {
            questionTimer.stop();
        }
        questionTimer = new javax.swing.Timer(TIMER_DELAY, e -> {
            timeLeft--;
            timerLabel.setText("Time: " + timeLeft);
            if (timeLeft <= 0) {
                handleTimeout();
            }
        });
        questionTimer.start();
    }

    private void handleTimeout() {
        questionTimer.stop();
        try {
            sendMessage(new Message(MessageType.ANSWER, new Answer(currentQuestionIndex, -1)));
            moveToNextQuestion();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleAnswer(int selectedOption) {
        questionTimer.stop();
        try {
            sendMessage(new Message(MessageType.ANSWER, new Answer(currentQuestionIndex, selectedOption)));

            // Visual feedback
            Question currentQuestion = currentQuestions.get(currentQuestionIndex);
            if (selectedOption == currentQuestion.getCorrectOptionIndex()) {
                answerButtons.get(selectedOption).setBackground(Color.GREEN);
            } else {
                answerButtons.get(selectedOption).setBackground(Color.RED);
                answerButtons.get(currentQuestion.getCorrectOptionIndex()).setBackground(Color.GREEN);
            }

            // Disable buttons after answer
            answerButtons.forEach(button -> button.setEnabled(false));

            // Wait briefly before moving to next question
            javax.swing.Timer transitionTimer = new javax.swing.Timer(1000, e -> moveToNextQuestion());
            transitionTimer.setRepeats(false);
            transitionTimer.start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void moveToNextQuestion() {
        currentQuestionIndex++;
        if (currentQuestionIndex < currentQuestions.size()) {
            displayQuestion();
        } else {
            try {
                sendMessage(new Message(MessageType.ROUND_COMPLETE, null));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleRoundResult(RoundResult result) {
        mainPanel.removeAll();
        JPanel resultPanel = new JPanel(new BorderLayout());
        resultPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Add round result title
        JLabel titleLabel = new JLabel("Round " + currentRound + " Complete!", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        resultPanel.add(titleLabel, BorderLayout.NORTH);

        // Display scores
        JPanel scoresPanel = new JPanel(new GridLayout(0, 1, 5, 5));
        result.getScores().forEach((player, score) -> {
            JLabel scoreLabel = new JLabel(player + ": " + score, SwingConstants.CENTER);
            scoreLabel.setFont(new Font("Arial", Font.PLAIN, 16));
            scoresPanel.add(scoreLabel);
        });

        // Add padding around scores
        JPanel paddedScoresPanel = new JPanel(new BorderLayout());
        paddedScoresPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        paddedScoresPanel.add(scoresPanel, BorderLayout.CENTER);

        resultPanel.add(paddedScoresPanel, BorderLayout.CENTER);

        // Add "Waiting for next round" message if not the final round
        if (currentRound < 3) {
            JLabel waitingLabel = new JLabel("Waiting for next round...", SwingConstants.CENTER);
            waitingLabel.setFont(new Font("Arial", Font.ITALIC, 14));
            resultPanel.add(waitingLabel, BorderLayout.SOUTH);
        }

        mainPanel.add(resultPanel);
        mainPanel.revalidate();
        mainPanel.repaint();
    }

    private void handleGameEnd(GameResult result) {
        mainPanel.removeAll();
        JPanel endPanel = new JPanel(new BorderLayout());
        endPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Game Over title
        JLabel titleLabel = new JLabel("Game Over!", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(new Color(44, 62, 80));
        endPanel.add(titleLabel, BorderLayout.NORTH);

        // Final scores panel
        JPanel finalScoresPanel = new JPanel(new GridLayout(0, 1, 10, 10));
        finalScoresPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));

        // Add final scores with styled labels
        result.getScores().forEach((player, score) -> {
            JLabel scoreLabel = new JLabel(player + ": " + score, SwingConstants.CENTER);
            scoreLabel.setFont(new Font("Arial", Font.BOLD, 18));
            finalScoresPanel.add(scoreLabel);
        });

        // Add winner announcement
        if (result.getWinnerUsername() != null) {
            JLabel winnerLabel = new JLabel("Winner: " + result.getWinnerUsername(), SwingConstants.CENTER);
            winnerLabel.setFont(new Font("Arial", Font.BOLD, 20));
            winnerLabel.setForeground(new Color(39, 174, 96));
            finalScoresPanel.add(winnerLabel);

            if (result.getWinnerUsername().equals(username)) {
                JLabel congratsLabel = new JLabel("Congratulations, you won!", SwingConstants.CENTER);
                congratsLabel.setFont(new Font("Arial", Font.BOLD, 22));
                congratsLabel.setForeground(new Color(39, 174, 96));
                finalScoresPanel.add(congratsLabel);
            }
        } else {
            JLabel tieLabel = new JLabel("It's a tie!", SwingConstants.CENTER);
            tieLabel.setFont(new Font("Arial", Font.BOLD, 20));
            tieLabel.setForeground(new Color(41, 128, 185));
            finalScoresPanel.add(tieLabel);
        }

        endPanel.add(finalScoresPanel, BorderLayout.CENTER);

        // Play Again button
        JButton newGameButton = new JButton("Play Again");
        newGameButton.setFont(new Font("Arial", Font.BOLD, 16));
        newGameButton.setBackground(new Color(52, 152, 219));
        newGameButton.setForeground(Color.WHITE);
        newGameButton.setFocusPainted(false);
        newGameButton.addActionListener(e -> {
            frame.dispose();
            new QuizClient();
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(newGameButton);
        endPanel.add(buttonPanel, BorderLayout.SOUTH);

        mainPanel.add(endPanel);
        mainPanel.revalidate();
        mainPanel.repaint();
    }

    private void sendMessage(Message message) throws IOException {
        out.writeObject(message);
        out.reset();
        out.flush();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new QuizClient());
    }
}