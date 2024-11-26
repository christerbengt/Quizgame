package Server;

import java.io.*;
import java.util.*;

public class QuestionDatabase {
    private final Map<Category, List<Question>> questions = new HashMap<>();

    public QuestionDatabase() {
        loadQuestions();
    }

    private void loadQuestions() {
        // Initialize empty lists for each category
        for (Category category : Category.values()) {
            questions.put(category, new ArrayList<>());
        }

        try (BufferedReader reader = new BufferedReader(new FileReader("questions.csv"))) {
            String line;
            boolean firstLine = true;

            while ((line = reader.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    continue; // Skip the header row
                }

                try {
                    String[] parts = line.split(",");
                    if (parts.length >= 7) {
                        String questionText = parts[0].trim();
                        List<String> options = Arrays.asList(
                                parts[1].trim(),
                                parts[2].trim(),
                                parts[3].trim(),
                                parts[4].trim()
                        );
                        String correctAnswer = parts[5].trim();
                        String categoryStr = parts[6].trim();

                        // Find correct answer index
                        int correctIndex = options.indexOf(correctAnswer);
                        if (correctIndex == -1) {
                            System.err.println("Warning: Correct answer '" + correctAnswer +
                                    "' not found in options for question: " + questionText);
                            continue; // Skip this question
                        }

                        try {
                            Category category = Category.valueOf(categoryStr.toUpperCase());
                            Question question = new Question(questionText, options, correctIndex);
                            questions.get(category).add(question);
                            System.out.println("Loaded question for category " + category + ": " + questionText);
                        } catch (IllegalArgumentException e) {
                            System.err.println("Invalid category '" + categoryStr + "' for question: " + questionText);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error parsing line: " + line);
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading questions.csv: " + e.getMessage());
            loadDefaultQuestions();
        }

        // Print summary of loaded questions
        for (Category category : Category.values()) {
            System.out.println("Category " + category + " has " + questions.get(category).size() + " questions");
        }

        // If no questions were loaded, load defaults
        if (questions.values().stream().allMatch(List::isEmpty)) {
            System.out.println("No questions were loaded from file, loading defaults...");
            loadDefaultQuestions();
        }
    }

    private void loadDefaultQuestions() {
        // Add default questions for each category
        addDefaultQuestion(Category.HISTORY, "Which year did World War II end?",
                Arrays.asList("1943", "1944", "1945", "1946"), 2);

        addDefaultQuestion(Category.SCIENCE, "What is the chemical symbol for gold?",
                Arrays.asList("Au", "Ag", "Fe", "Cu"), 0);

        addDefaultQuestion(Category.GEOGRAPHY, "What is the capital of Australia?",
                Arrays.asList("Sydney", "Melbourne", "Perth", "Canberra"), 3);

        addDefaultQuestion(Category.LITERATURE, "Who wrote Romeo and Juliet?",
                Arrays.asList("Charles Dickens", "William Shakespeare", "Jane Austen", "Mark Twain"), 1);

        addDefaultQuestion(Category.MATH, "What is the square root of 144?",
                Arrays.asList("10", "11", "12", "13"), 2);

        // Print the loaded default questions
        for (Category category : Category.values()) {
            System.out.println("Category " + category + " has " + questions.get(category).size() + " default questions");
        }
    }

    private void addDefaultQuestion(Category category, String questionText, List<String> options, int correctIndex) {
        questions.get(category).add(new Question(questionText, options, correctIndex));
        System.out.println("Added default question for category " + category + ": " + questionText);
    }

    public List<Question> getQuestionsForRound(Category category, int count) {
        List<Question> categoryQuestions = new ArrayList<>(questions.get(category));

        if (categoryQuestions.isEmpty()) {
            System.err.println("No questions available for category: " + category);
            return loadEmergencyQuestions(count);
        }

        Collections.shuffle(categoryQuestions);
        List<Question> selectedQuestions = new ArrayList<>();
        for (int i = 0; i < Math.min(count, categoryQuestions.size()); i++) {
            selectedQuestions.add(categoryQuestions.get(i));
        }

        System.out.println("Selected " + selectedQuestions.size() + " questions from category " + category);
        return selectedQuestions;
    }

    private List<Question> loadEmergencyQuestions(int count) {
        List<Question> emergencyQuestions = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            emergencyQuestions.add(new Question(
                    "Emergency Question " + (i + 1),
                    Arrays.asList("Option A", "Option B", "Option C", "Option D"),
                    0
            ));
        }
        return emergencyQuestions;
    }

    private Category getRandomCategory() {
        List<Category> availableCategories = new ArrayList<>();
        for (Map.Entry<Category, List<Question>> entry : questions.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                availableCategories.add(entry.getKey());
            }
        }
        if (availableCategories.isEmpty()) {
            return Category.values()[0];
        }
        return availableCategories.get(new Random().nextInt(availableCategories.size()));
    }
}