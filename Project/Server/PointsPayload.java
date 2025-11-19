package Project.Server;

public class PointsPayload extends Payload {
    private final long clientId;
    private final int points;

    public PointsPayload(String message, long clientId, int points) {
        super("Points", message, String.valueOf(clientId)); // type
        this.clientId = clientId;
        this.points = points;
    }

    public long getClientId() {
        return clientId;
    }

    public int getPoints() {
        return points;
    }

    @Override
    public String toString() {
        return String.format("PointsPayload[clientId=%d, points=%d, message=%s]", clientId, points, super.getMessage());
    }
}
