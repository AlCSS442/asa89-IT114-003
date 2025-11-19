public class StrikePayload extends Payload{
    
    public int strikes;

    public StrikePayload(String clientId, int strikes){
        super(clientId, "strike", "strike update");
        this.strikes = strikes;
    }

    @Override
    public String toString(){
        return super.toString() + " strikes=" + strikes;
    }
}
