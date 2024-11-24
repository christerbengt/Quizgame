package Server;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;

public class RoundResult implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final Map<String, Integer> scores;
    private final Map<PlayerHandler, Integer> playerScores; // For server-side use

    public RoundResult(Map<PlayerHandler, Integer> playerScores) {
        this.playerScores = new HashMap<>(playerScores);
        this.scores = new HashMap<>();
        for (Map.Entry<PlayerHandler, Integer> entry : playerScores.entrySet()) {
            scores.put(entry.getKey().getUsername(), entry.getValue());
        }
    }

    public Map<String, Integer> getScores() {
        return scores;
    }

    public Map<PlayerHandler, Integer> getPlayerScores() {
        return playerScores;
    }
}