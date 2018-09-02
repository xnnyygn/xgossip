package in.xnnyygn.xgossip.messages;

import in.xnnyygn.xgossip.MemberEndpoint;

public class ProxyPingRpc extends AbstractMessage {

    private final MemberEndpoint endpoint;

    public ProxyPingRpc(MemberEndpoint endpoint) {
        this.endpoint = endpoint;
    }

    public MemberEndpoint getEndpoint() {
        return endpoint;
    }

}
