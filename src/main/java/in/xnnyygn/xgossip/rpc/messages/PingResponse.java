package in.xnnyygn.xgossip.rpc.messages;

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
