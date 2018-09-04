package in.xnnyygn.xgossip.messages;

public class PingResponse extends AbstractPingResponse {

    public PingResponse(long pingAt) {
        super(pingAt);
    }

    @Override
    public String toString() {
        return "PingResponse{" +
                "pingAt=" + getPingAt() +
                '}';
    }

}
