package Server;

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
}
