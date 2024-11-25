package Server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public enum Category {
    HISTORY,
    SCIENCE,
    GEOGRAPHY,
    LITERATURE,
    MATH;

    public static Category fromString(String category) {
        try {
            return valueOf(category.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid category: " + category);
            return HISTORY; // Default category
        }
    }

    public static List<Category> randomCategories() {

        List<Category> categoryList = new ArrayList<>(List.of(Category.values()));

        Collections.shuffle(categoryList);

        List<Category> selectedCategories = new ArrayList<>(categoryList.subList(0, 4));



      return selectedCategories;
    }
}
