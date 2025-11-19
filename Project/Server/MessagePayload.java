public class MessagePayload extends Payload {
    public MessagePayload(String clientId, String message){
        super(clientId, "message", message);
    }
}
