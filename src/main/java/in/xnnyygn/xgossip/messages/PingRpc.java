package in.xnnyygn.xgossip.messages;

public class PingRpc extends AbstractMessage {

    private final long pingAt;

    public PingRpc() {
        this.pingAt = System.currentTimeMillis();
    }

    public long getPingAt() {
        return pingAt;
    }

    @Override
    public String toString() {
        return "PingRpc{pingAt=" + pingAt + "}";
    }

}
