package Client;

import Pojos.Message;
import Pojos.MessageType;
import Server.*;
import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;

public class QuizClient {
    private final String SERVER_ADDRESS = "176.10.254.8"; // Christers IPv4
    private final int SERVER_PORT = 12649;
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

    // Method to display the category panel
    private void showCategoryPanel() {
        mainPanel.removeAll();

        JPanel categoryPanel = new JPanel(new BorderLayout());
        JPanel centerPanel1 = new JPanel(new GridBagLayout());
        JPanel answerPanel = new JPanel(new GridLayout(4, 1, 20, 20)); // 4 buttons in a single column with spacing

        List<Category> categories = Category.randomCategories(); // Get 4 random categories

        // Create buttons for each category and add them to the panel
        ArrayList<JButton> answerButtons = new ArrayList<>();
        for (Category category : categories) {
            JButton categoryButton = new JButton(category.toString());
            categoryButton.addActionListener((ActionListener) this); // Set action listener
            answerButtons.add(categoryButton);
        }

        for (JButton button : answerButtons) {
            answerPanel.add(button);
        }

        centerPanel1.add(answerPanel);
        categoryPanel.add(centerPanel1, BorderLayout.CENTER);

        // Create a label for the category selection prompt
        JPanel questionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel questionLabel = new JLabel("Pick a category:", SwingConstants.CENTER);
        questionLabel.setFont(new Font("Arial", Font.PLAIN, 18));
        questionPanel.add(questionLabel);
        categoryPanel.add(questionPanel, BorderLayout.NORTH);

        mainPanel.add(categoryPanel, BorderLayout.CENTER);
        mainPanel.revalidate();
        mainPanel.repaint();
    }



    private void createQuestionPanel() {
        questionPanel = new JPanel(new BorderLayout(10, 10));
        questionPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Create header panel
        JPanel headerPanel = new JPanel(new BorderLayout());

        // Round indicator
        JLabel roundLabel = new JLabel("Round " + currentRound + " of 3", SwingConstants.CENTER);
        roundLabel.setFont(new Font("Arial", Font.BOLD, 18));
        roundLabel.setForeground(new Color(44, 62, 80));
        headerPanel.add(roundLabel, BorderLayout.NORTH);

        // Timer and score panel
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 5));
        timerLabel = new JLabel("Time: 30");
        timerLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        scoreLabel = new JLabel("Score: 0");
        scoreLabel.setFont(new Font("Arial", Font.PLAIN, 14));

        infoPanel.add(timerLabel);
        infoPanel.add(Box.createHorizontalStrut(20));
        infoPanel.add(scoreLabel);
        headerPanel.add(infoPanel, BorderLayout.CENTER);

        // Add padding to header
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));

        // Question display
        questionLabel = new JLabel();
        questionLabel.setHorizontalAlignment(JLabel.CENTER);
        questionLabel.setFont(new Font("Arial", Font.BOLD, 16));

        // Answer buttons panel
        JPanel buttonPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        answerButtons = new ArrayList<>();

        for (int i = 0; i < 4; i++) {
            JButton button = new JButton();
            button.setFont(new Font("Arial", Font.PLAIN, 14));
            button.setBackground(new Color(240, 240, 240));
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(200, 200, 200)),
                    BorderFactory.createEmptyBorder(10, 10, 10, 10)
            ));
            final int index = i;
            button.addActionListener(e -> handleAnswer(index));
            answerButtons.add(button);
            buttonPanel.add(button);
        }

        // Add everything to the question panel
        questionPanel.add(headerPanel, BorderLayout.NORTH);
        questionPanel.add(questionLabel, BorderLayout.CENTER);

        // Add padding around button panel
        JPanel paddedButtonPanel = new JPanel(new BorderLayout());
        paddedButtonPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));
        paddedButtonPanel.add(buttonPanel, BorderLayout.CENTER);
        questionPanel.add(paddedButtonPanel, BorderLayout.SOUTH);

        // Set background color for the whole panel
        questionPanel.setBackground(Color.WHITE);
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

                        // Clear the main panel and create a new question panel
                        mainPanel.removeAll();
                        createQuestionPanel(); // This will create a fresh question panel
                        mainPanel.add(questionPanel);
                        mainPanel.revalidate();
                        mainPanel.repaint();

                        // Start displaying questions
                        displayQuestion();
                    }
                    case ROUND_RESULT -> {
                        System.out.println("Received round " + currentRound + " results");
                        handleRoundResult((RoundResult) message.getContent());
                        currentRound++;
                    }
                    case GAME_END -> {
                        System.out.println("Game ending");
                        handleGameEnd((GameResult) message.getContent());
                    }
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

            // Update question display with question number
            questionLabel.setText("<html><div style='text-align: center; padding: 10px;'>" +
                    "Question " + (currentQuestionIndex + 1) + " of " + currentQuestions.size() +
                    "<br><br>" + question.getText() + "</div></html>");

            List<String> options = question.getOptions();
            for (int i = 0; i < options.size(); i++) {
                JButton button = answerButtons.get(i);
                button.setText(options.get(i));
                button.setEnabled(true);
                button.setBackground(new Color(240, 240, 240));
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

        // Add waiting panel if not the final round
        if (currentRound < 3) {
            JPanel waitingPanel = new JPanel();
            waitingPanel.setLayout(new BoxLayout(waitingPanel, BoxLayout.Y_AXIS));

            // Create a panel for the waiting message and dots
            JPanel messagePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            JLabel waitingLabel = new JLabel("Waiting for next round");
            waitingLabel.setFont(new Font("Arial", Font.ITALIC, 14));
            JLabel dotsLabel = new JLabel("...");
            dotsLabel.setFont(new Font("Arial", Font.ITALIC, 14));
            messagePanel.add(waitingLabel);
            messagePanel.add(dotsLabel);

            // Create loading bar panel
            JProgressBar progressBar = new JProgressBar();
            progressBar.setIndeterminate(true);
            progressBar.setPreferredSize(new Dimension(200, 20));
            progressBar.setString("Waiting for other player");
            progressBar.setStringPainted(true);

            // Add components to waiting panel
            waitingPanel.add(messagePanel);
            waitingPanel.add(Box.createVerticalStrut(10));
            waitingPanel.add(progressBar);

            // Animate the dots
            Timer dotTimer = new Timer(500, e -> {
                String dots = dotsLabel.getText();
                dotsLabel.setText(dots.length() >= 3 ? "." : dots + ".");
            });
            dotTimer.start();

            // Add the waiting panel to the result panel
            JPanel spacedWaitingPanel = new JPanel(new BorderLayout());
            spacedWaitingPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
            spacedWaitingPanel.add(waitingPanel, BorderLayout.CENTER);
            resultPanel.add(spacedWaitingPanel, BorderLayout.SOUTH);
        }

        mainPanel.add(resultPanel);
        mainPanel.revalidate();
        mainPanel.repaint();

        // If this was the final round, show a countdown to game end
        if (currentRound == 3) {
            Timer endTimer = new Timer(2000, e -> {
                JLabel endingLabel = new JLabel("Calculating final results...", SwingConstants.CENTER);
                endingLabel.setFont(new Font("Arial", Font.BOLD, 16));
                resultPanel.add(endingLabel, BorderLayout.SOUTH);
                mainPanel.revalidate();
                mainPanel.repaint();
            });
            endTimer.setRepeats(false);
            endTimer.start();
        }
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