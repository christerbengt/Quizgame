
import Server.Category;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

public class CategoryPanel extends JPanel implements ActionListener {



  // Constructor to initialize the panel with four category names
  public CategoryPanel() {
    setLayout(new BorderLayout());

    List<Category> categories = Category.randomCategories();

    JPanel mainPanel = createCategoryPanel(categories);

    add(mainPanel, BorderLayout.CENTER);
  }


  private JPanel createCategoryPanel(List<Category> categories) {

    JPanel mainPanel = new JPanel(new BorderLayout());
    JPanel centerPanel1 = new JPanel(new GridBagLayout());
    JPanel answerPanel = new JPanel(new GridLayout(4, 1, 20, 20)); // 4 buttons in a single column with spacing


    ArrayList<JButton> answerButtons = new ArrayList<>();
    for (Category category : categories) {
      JButton categoryButton = new JButton(category.toString());
      categoryButton.addActionListener(this);
      answerButtons.add(categoryButton);
    }

    for (JButton button : answerButtons) {
      button.addActionListener(this);
      answerPanel.add(button);
    }


    centerPanel1.add(answerPanel);

    mainPanel.add(centerPanel1, BorderLayout.CENTER);

    JPanel questionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    JLabel questionLabel = new JLabel("Pick a category:", SwingConstants.CENTER);
    questionLabel.setFont(new Font("Arial", Font.PLAIN, 18));
    questionPanel.add(questionLabel);

    mainPanel.add(questionPanel, BorderLayout.NORTH);

    return mainPanel;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    String selectedCategory = ((JButton) e.getSource()).getText(); // Get the selected category
    JOptionPane.showMessageDialog(this, "You selected: " + selectedCategory); // Display the selected category
  }

  public static void main(String[] args) {
      JFrame frame = new JFrame("Category Panel");
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      frame.setSize(400, 400);
      frame.add(new CategoryPanel());
      frame.setLocationRelativeTo(null); // Center the frame
      frame.setVisible(true);
  }
}
