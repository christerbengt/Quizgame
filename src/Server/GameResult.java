package Server;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;

public class GameResult implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final Map<String, Integer> scores;
    private final String winnerUsername;
    private final Map<PlayerHandler, Integer> playerScores; // For server-side use

    public GameResult(Map<PlayerHandler, Integer> finalScores, PlayerHandler winner) {
        this.playerScores = new HashMap<>(finalScores);
        this.scores = new HashMap<>();
        for (Map.Entry<PlayerHandler, Integer> entry : finalScores.entrySet()) {
            scores.put(entry.getKey().getUsername(), entry.getValue());
        }
        this.winnerUsername = winner != null ? winner.getUsername() : null;
    }

    public Map<String, Integer> getScores() {
        return scores;
    }

    public String getWinnerUsername() {
        return winnerUsername;
    }

    public Map<PlayerHandler, Integer> getPlayerScores() {
        return playerScores;
    }
}