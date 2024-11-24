package Server;

import java.io.Serializable;

public class Answer implements Serializable {
    private final int questionIndex;
    private final int selectedOption;

    public Answer(int questionIndex, int selectedOption) {
        this.questionIndex = questionIndex;
        this.selectedOption = selectedOption;
    }

    public int getQuestionIndex() { return questionIndex; }
    public int getSelectedOption() { return selectedOption; }
}
