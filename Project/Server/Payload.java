package Project.Server;

public abstract class Payload {
    protected String clientId;
    protected String type;
    protected String message;

    public Payload(String clientId, String type, String message){
        this.clientId = clientId;
        this.type = type;
        this.message = message;
    }

    @Override
    public String toString(){
        return "[" + type + "] (" + clientId + "): " + message;
    }
    
}
