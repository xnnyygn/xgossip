package in.xnnyygn.xgossip.messages;

import in.xnnyygn.xgossip.MemberEndpoint;

public class ProxyPingRpc extends AbstractMessage {

    private final long pingAt;
    private final MemberEndpoint sourceEndpoint;

    public ProxyPingRpc(long pingAt, MemberEndpoint sourceEndpoint) {
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
