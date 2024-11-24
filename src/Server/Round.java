package Server;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;

public class Round implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final List<Question> questions;
    private final Map<PlayerHandler, List<Answer>> playerAnswers;

    public Round(List<Question> questions) {
        this.questions = new ArrayList<>(questions); // Create new ArrayList
        this.playerAnswers = new HashMap<>();
    }

    public List<Question> getQuestions() {
        return new ArrayList<>(questions); // Return a copy
    }

    public void recordAnswer(PlayerHandler player, Answer answer) {
        playerAnswers.computeIfAbsent(player, k -> new ArrayList<>()).add(answer);
        System.out.println("Recorded answer from " + player.getUsername() +
                " for question " + answer.getQuestionIndex());
    }

    public boolean isComplete() {
        return playerAnswers.size() == 2 &&
                playerAnswers.values().stream().allMatch(answers ->
                        answers.size() == questions.size());
    }

    public RoundResult getResult() {
        Map<PlayerHandler, Integer> scores = new HashMap<>();

        for (Map.Entry<PlayerHandler, List<Answer>> entry : playerAnswers.entrySet()) {
            PlayerHandler player = entry.getKey();
            List<Answer> answers = entry.getValue();
            int score = calculateScore(answers);
            scores.put(player, score);
            System.out.println("Player " + player.getUsername() + " scored: " + score);
        }

        return new RoundResult(scores);
    }

    private int calculateScore(List<Answer> answers) {
        int score = 0;
        for (int i = 0; i < answers.size(); i++) {
            Answer answer = answers.get(i);
            if (answer.getSelectedOption() == questions.get(answer.getQuestionIndex()).getCorrectOptionIndex()) {
                score++;
            }
        }
        return score;
    }
}