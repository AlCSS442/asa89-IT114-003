public class PointsPayload extends Payload {
    public int points;

    public PointsPayload(String clientId, int points){
        super(clientId, "points", "points update");
        this.points = points;
    }

    @Override
    public String toString(){
        return super.toString() + " points=" + points;
    }
}
