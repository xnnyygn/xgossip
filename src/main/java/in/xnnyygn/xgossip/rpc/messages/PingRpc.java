package in.xnnyygn.xgossip.rpc.messages;

public class PingRpc extends AbstractMessage {

    private final long pingAt;

    public PingRpc() {
        this(System.currentTimeMillis());
    }

    public PingRpc(long pingAt) {
        this.pingAt = pingAt;
    }

    public long getPingAt() {
        return pingAt;
    }

    @Override
    public String toString() {
        return "PingRpc{pingAt=" + pingAt + "}";
    }

}
