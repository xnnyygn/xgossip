package in.xnnyygn.xgossip.rpc.messages;

public class AbstractPingResponse extends AbstractMessage {

    private final long pingAt;

    public AbstractPingResponse(long pingAt) {
        this.pingAt = pingAt;
    }

    public long getPingAt() {
        return pingAt;
    }

}
