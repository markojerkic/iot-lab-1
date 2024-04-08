package hr.fer.iot;

public class StoredMessage {
    private final Message message;
    private final String deciveId;
    private final String sensorType;
    private final long time;

    public StoredMessage(String deciveId, Message message, String sensorType, long time) {
        this.deciveId = deciveId;
        this.message = message;
        this.sensorType = sensorType;
        this.time = time;
    }

    public String getDeciveId() {
        return deciveId;
    }

    public String getSensorType() {
        return sensorType;
    }

    public Message getMessage() {
        return message;
    }

    public long getTime() {
        return time;
    }

}
