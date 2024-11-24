package Server;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;

public class Question implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String text;
    private final List<String> options;
    private final int correctOptionIndex;

    public Question(String text, List<String> options, int correctOptionIndex) {
        this.text = text;
        // Create a new ArrayList to ensure serializability
        this.options = new ArrayList<>(options);
        this.correctOptionIndex = correctOptionIndex;
    }

    public String getText() {
        return text;
    }

    public List<String> getOptions() {
        return new ArrayList<>(options); // Return a copy to prevent modification
    }

    public int getCorrectOptionIndex() {
        return correctOptionIndex;
    }

    @Override
    public String toString() {
        return "Question{text='" + text + "', options=" + options +
                ", correctAnswer=" + correctOptionIndex + "}";
    }
}