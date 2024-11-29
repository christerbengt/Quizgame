package Server;

import java.io.*;
import java.util.Properties;

public class GameProperties {
    private final Properties properties = new Properties();
    private static final String PROPERTIES_FILE = "game.properties";

    public GameProperties() {
        loadProperties();
    }

    private void loadProperties() {
        try (InputStream input = new FileInputStream(PROPERTIES_FILE)) {
            properties.load(input);
        } catch (IOException e) {
            // use default values if properties file not found.
            setDefaults();
        }
    }

    private void setDefaults() {
        properties.setProperty("rounds.count", "2");
        properties.setProperty("questions.per.round", "2");
        properties.setProperty("answer.timeout.seconds", "30");
    }

    public int getRoundCount() {
        return Integer.parseInt(properties.getProperty("rounds.count"));
    }

    public int getQuestionsPerRound() {
        return Integer.parseInt(properties.getProperty("questions.per.round"));
    }

    public int getAnswerTimeoutSeconds() {
        return Integer.parseInt(properties.getProperty("answer.timeout.seconds"));
    }
}
