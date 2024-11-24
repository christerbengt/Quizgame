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

        // Question display
        questionLabel = new JLabel();
        questionLabel.setHorizontalAlignment(JLabel.CENTER);
        questionLabel.setFont(new Font("Arial", Font.BOLD, 16));

        // Timer and score
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        timerLabel = new JLabel("Time: 30");
        scoreLabel = new JLabel("Score: 0");
        infoPanel.add(timerLabel);
        infoPanel.add(Box.createHorizontalStrut(20));
        infoPanel.add(scoreLabel);

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

        questionPanel.add(infoPanel, BorderLayout.NORTH);
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
                        mainPanel.removeAll();
                        createQuestionPanel();
                        mainPanel.add(questionPanel);
                        mainPanel.revalidate();
                        mainPanel.repaint();
                    }
                    case ROUND_START -> {
                        System.out.println("Round starting");
                        currentQuestions = (List<Question>) message.getContent();
                        currentQuestionIndex = 0;
                        displayQuestion();
                    }
                    case ROUND_RESULT -> handleRoundResult((RoundResult) message.getContent());
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
            questionLabel.setText(question.getText());
            List<String> options = question.getOptions();
            for (int i = 0; i < options.size(); i++) {
                answerButtons.get(i).setText(options.get(i));
                answerButtons.get(i).setEnabled(true);
                answerButtons.get(i).setBackground(null);
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
        resultPanel.add(new JLabel("Round Complete!", SwingConstants.CENTER), BorderLayout.NORTH);

        JPanel scoresPanel = new JPanel(new GridLayout(0, 1));
        result.getScores().forEach((player, score) ->
                scoresPanel.add(new JLabel(player + ": " + score, SwingConstants.CENTER)));

        resultPanel.add(scoresPanel, BorderLayout.CENTER);
        mainPanel.add(resultPanel);
        mainPanel.revalidate();
        mainPanel.repaint();
    }

    private void handleGameEnd(GameResult result) {
        mainPanel.removeAll();
        JPanel endPanel = new JPanel(new BorderLayout());
        endPanel.add(new JLabel("Game Over!", SwingConstants.CENTER), BorderLayout.NORTH);

        JPanel finalScoresPanel = new JPanel(new GridLayout(0, 1));
        result.getScores().forEach((player, score) ->
                finalScoresPanel.add(new JLabel(player + ": " + score, SwingConstants.CENTER)));

        if (result.getWinnerUsername() != null) {
            finalScoresPanel.add(new JLabel("Winner: " + result.getWinnerUsername(),
                    SwingConstants.CENTER));

            // Add some visual flair for the winner
            if (result.getWinnerUsername().equals(username)) {
                JLabel congratsLabel = new JLabel("Congratulations, you won!", SwingConstants.CENTER);
                congratsLabel.setFont(new Font("Arial", Font.BOLD, 16));
                congratsLabel.setForeground(new Color(0, 150, 0));
                finalScoresPanel.add(congratsLabel);
            }
        } else {
            finalScoresPanel.add(new JLabel("It's a tie!", SwingConstants.CENTER));
        }

        endPanel.add(finalScoresPanel, BorderLayout.CENTER);

        JButton newGameButton = new JButton("Play Again");
        newGameButton.addActionListener(e -> {
            frame.dispose();
            new QuizClient();
        });
        endPanel.add(newGameButton, BorderLayout.SOUTH);

        mainPanel.add(endPanel);
        mainPanel.revalidate();
        mainPanel.repaint();
    }

    private void sendMessage(Message message) throws IOException {
        out.writeObject(message);
        out.reset(); // Prevent object caching
        out.flush();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new QuizClient());
    }
}