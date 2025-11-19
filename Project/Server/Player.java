package Project.Server;
//ass89

public class Player {
    private final String clientName;
    private final String clientId;
    private int points;
    private int strikes;
    private boolean ready;

    public Player(String clientName, String clientId) {
        this.clientName = clientName;
        this.clientId = clientId;
        this.points = 0;
        this.strikes = 0;
        this.ready = false;
    }

    //Getters
    public String getClientName() {
        return clientName;
    }

    public String getClientId() {
        return clientId;
    }

    public int getPoints() {
        return points;
    }

    public int getStrikes() {
        return strikes;
    }

    public boolean isReady() {
        return ready;
    }

    // Setters
    public void setPoints(int points) {
        this.points = points;
    }

    public void addPoints(int pts) {
        this.points += pts;
    }

    public void setStrikes(int strikes) {
        this.strikes = strikes;
    }

    public void addStrike() {
        this.strikes++;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }

    // Resetting
    public void reset() {
        points = 0;
        strikes = 0;
        ready = false;
    }

    // Override toString, as always
    @Override
    public String toString() {
        return clientName + " (Points: " + points + ", Strikes: " + strikes + ", Ready: " + ready + ")";
    }
}
