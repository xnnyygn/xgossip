package in.xnnyygn.xgossip.rpc.messages;

import in.xnnyygn.xgossip.MemberEndpoint;

public class PingRequestRpc extends AbstractMessage {

    private final long pingAt;
    private final MemberEndpoint endpoint;

    public PingRequestRpc(long pingAt, MemberEndpoint endpoint) {
        this.pingAt = pingAt;
        this.endpoint = endpoint;
    }

    public long getPingAt() {
        return pingAt;
    }

    public MemberEndpoint getEndpoint() {
        return endpoint;
    }

    @Override
    public String toString() {
        return "PingRequestRpc{" +
                "pingAt=" + pingAt +
                ", endpoint=" + endpoint +
                '}';
    }

}
