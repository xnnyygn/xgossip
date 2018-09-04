package in.xnnyygn.xgossip.messages;

import in.xnnyygn.xgossip.MemberEndpoint;

public class ProxyPingResponse extends AbstractMessage {

    private final long pingAt;
    private final MemberEndpoint sourceEndpoint;

    public ProxyPingResponse(long pingAt, MemberEndpoint sourceEndpoint) {
        this.pingAt = pingAt;
        this.sourceEndpoint = sourceEndpoint;
    }

    public long getPingAt() {
        return pingAt;
    }

    public MemberEndpoint getSourceEndpoint() {
        return sourceEndpoint;
    }

}
