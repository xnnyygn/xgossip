package in.xnnyygn.xgossip.messages;

public abstract class AbstractMessage {

    private final long timestamp;

    public AbstractMessage() {
        this(System.currentTimeMillis());
    }

    public AbstractMessage(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getTimestamp() {
        return timestamp;
    }

}
