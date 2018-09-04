package in.xnnyygn.xgossip.messages;

import in.xnnyygn.xgossip.MemberEndpoint;

public class ProxyPingDoneResponse extends AbstractPingResponse {

    private final MemberEndpoint endpoint;

    public ProxyPingDoneResponse(long pingAt, MemberEndpoint endpoint) {
        super(pingAt);
        this.endpoint = endpoint;
    }

    public MemberEndpoint getEndpoint() {
        return endpoint;
    }

    @Override
    public String toString() {
        return "ProxyPingDoneResponse{" +
                "pingAt=" + getPingAt() +
                ", endpoint=" + endpoint +
                '}';
    }
}
